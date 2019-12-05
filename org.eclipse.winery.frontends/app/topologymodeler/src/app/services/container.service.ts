/*******************************************************************************
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache Software License 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *******************************************************************************/

import { Injectable } from '@angular/core';
import { CsarUpload } from '../models/container/csar-upload.model';
import { of } from 'rxjs';
import { Observable } from 'rxjs/Rx';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { catchError, concatMap, map, tap } from 'rxjs/operators';
import { NodeTemplateInstanceStates, PlanTypes, ServiceTemplateInstanceStates } from '../models/enums';
import { Csar } from '../models/container/csar.model';
import { ServiceTemplate } from '../models/container/service-template.model';
import { PlanResources } from '../models/container/plan-resources.model';
import { PlanInstanceResources } from '../models/container/plan-instance-resources.model';
import { ServiceTemplateInstance } from '../models/container/service-template-instance';
import { ServiceTemplateInstanceResources } from '../models/container/service-template-instance-resources.model';
import { Plan } from '../models/container/plan.model';
import { NodeTemplateResources } from '../models/container/node-template-resources.model';
import { NodeTemplateInstanceResources } from '../models/container/node-template-instance-resources.model';
import { NodeTemplateInstance } from '../models/container/node-template-instance.model';
import { NgRedux } from '@angular-redux/store';
import { IWineryState } from '../redux/store/winery.store';

@Injectable()
export class ContainerService {
    private containerUrl: string;

    private readonly csarEnding = '.csar';

    private readonly headerAcceptJSON = {
        headers: new HttpHeaders({
            'Accept': 'application/json'
        })
    };

    private readonly headerContentJSON = {
        headers: new HttpHeaders({
            'Content-Type': 'application/json'
        })
    };

    private readonly headerContentTextPlain = {
        headers: new HttpHeaders({
            'Content-Type': 'text/plain'
        })
    };

    private readonly installationPayload = [
        { 'name': 'instanceDataAPIUrl', 'type': 'String', 'required': 'YES' },
        { 'name': 'csarEntrypoint', 'type': 'String', 'required': 'YES' },
        { 'name': 'CorrelationID', 'type': 'String', 'required': 'YES' }
    ];

    private readonly terminationPayload = [
        { 'name': 'instanceDataAPIUrl', 'type': 'String', 'required': 'YES' },
        { 'name': 'OpenTOSCAContainerAPIServiceInstanceURL', 'type': 'String', 'required': 'YES' },
        { 'name': 'CorrelationID', 'type': 'String', 'required': 'YES' }
    ];

    private readonly transformationPayload = [
        { 'name': 'CorrelationID', 'type': 'String', 'required': 'YES' },
        { 'name': 'instanceDataAPIUrl', 'type': 'String', 'required': 'YES' },
        { 'name': 'planCallbackAddress_invoker', 'type': 'String', 'required': 'YES' },
        { 'name': 'csarEntrypoint', 'type': 'String', 'required': 'YES' }
    ];

    constructor(
        private ngRedux: NgRedux<IWineryState>,
        private http: HttpClient,
    ) {
        this.ngRedux.select(state => state.wineryState.liveModelingData.containerUrl)
            .subscribe(containerUrl => {
                this.containerUrl = containerUrl;
            });
    }

    public installApplication(uploadPayload: CsarUpload): Observable<any> {
        return this.http.post(this.combineURLs(this.containerUrl, 'csars'), uploadPayload, this.headerContentJSON);
    }

    public isApplicationInstalled(csarId: string): Observable<boolean> {
        const csarUrl = this.combineURLs(this.combineURLs(this.containerUrl, 'csars'), csarId);
        return this.http.get(csarUrl, { observe: 'response' }).pipe(
            map(resp => resp.ok),
            catchError(() => of(false))
        );
    }

    public deployServiceTemplateInstance(csarId: string): Observable<string> {
        return this.getBuildPlan(csarId).pipe(
            concatMap(resp => this.http.post(resp._links['instances'].href, this.installationPayload, {
                headers: new HttpHeaders({
                    'Content-Type': 'application/json'
                }),
                responseType: 'text'
            }))
        );
    }

    public waitForServiceTemplateInstanceIdAfterDeployment(csarId: string, correlationId: string, interval: number, timeout: number): Observable<string> {
        return Observable.interval(interval)
            .flatMap(() => this.getServiceTemplateInstanceIdAfterDeployment(csarId, correlationId))
            .first(resp => resp !== '')
            .timeout(timeout);
    }

    private getServiceTemplateInstanceIdAfterDeployment(csarId: string, correlationId: string): Observable<string> {
        return this.getBuildPlan(csarId).pipe(
            concatMap(resp => this.http.get<PlanInstanceResources>(resp._links['instances'].href, this.headerAcceptJSON)),
            map(resp => resp.plan_instances.find(planInstance => planInstance.correlation_id.toString() === correlationId)),
            map(resp => resp.service_template_instance_id.toString()),
            catchError(() => of(''))
        );
    }

    public waitForServiceTemplateInstanceInState(
        csarId: string,
        serviceTemplateInstanceId: string,
        state: ServiceTemplateInstanceStates,
        interval: number,
        timeout: number
    ): Observable<boolean> {
        return Observable.interval(interval)
            .flatMap(() => this.isServiceTemplateInstanceInState(csarId, serviceTemplateInstanceId, state))
            .first(resp => resp)
            .timeout(timeout);
    }

    private isServiceTemplateInstanceInState(csarId: string, serviceTemplateInstanceId: string, state: ServiceTemplateInstanceStates): Observable<boolean> {
        return this.getServiceTemplateInstance(csarId, serviceTemplateInstanceId).pipe(
            map(resp => resp.state === state),
            catchError(() => of(false))
        );
    }

    public terminateServiceTemplateInstance(csarId: string, serviceTemplateInstanceId: string): Observable<string> {
        return this.getTerminationPlan(csarId, serviceTemplateInstanceId).pipe(
            concatMap(resp => this.http.post(resp._links['instances'].href, this.terminationPayload, {
                headers: new HttpHeaders({
                    'Content-Type': 'application/json'
                }),
                responseType: 'text'
            }))
        );
    }

    public generateTransformationPlan(sourceCsarId: string, targetCsarId: string): Observable<any> {
        const transformPayload = {
            'source_csar_name': sourceCsarId,
            'target_csar_name': targetCsarId
        };

        const endpoint = this.combineURLs(this.containerUrl, 'csars/transform');
        return this.http.post(endpoint, transformPayload, this.headerContentJSON);
    }

    public executeTransformationPlan(serviceTemplateInstanceId: string, sourceCsarId: string, targetCsarId: string): Observable<string> {
        const planId = this.stripCsarSuffix(sourceCsarId) + '_transformTo_' + this.stripCsarSuffix(targetCsarId) + '_plan';

        return this.getManagementPlan(sourceCsarId, serviceTemplateInstanceId, planId).pipe(
            concatMap(resp => this.http.post(resp._links['instances'].href, this.transformationPayload, {
                headers: new HttpHeaders({
                    'Content-Type': 'application/json'
                }),
                responseType: 'text'
            }))
        );
    }

    public waitForServiceTemplateInstanceIdAfterMigration(
        sourceCsarId: string,
        serviceTemplateInstanceId: string,
        correlationId: string,
        interval: number,
        timeout: number
    ): Observable<string> {
        return Observable.interval(interval)
            .flatMap(() => this.getServiceTemplateInstanceIdAfterMigration(sourceCsarId, serviceTemplateInstanceId, correlationId))
            .first(resp => resp !== '')
            .timeout(timeout);
    }

    private getServiceTemplateInstanceIdAfterMigration(sourceCsarId: string, serviceTemplateInstanceId: string, correlationId: string): Observable<string> {
        return this.getManagementPlans(sourceCsarId, serviceTemplateInstanceId).pipe(
            concatMap(resp => this.http.get<PlanInstanceResources>(
                resp.find(plan => plan.plan_type === PlanTypes.TransformationPlan)._links['instances'].href, this.headerAcceptJSON)
            ),
            map(resp => resp.plan_instances.find(plan => plan.correlation_id === correlationId).outputs.find(output => output.name === 'instanceId').value),
            catchError(() => of(''))
        );
    }

    public getNodeTemplateInstanceState(csarId: string, serviceTemplateInstanceId: string, nodeTemplateId: string): Observable<NodeTemplateInstanceStates> {
        return this.getNodeTemplateInstance(csarId, serviceTemplateInstanceId, nodeTemplateId).pipe(
            map(resp => NodeTemplateInstanceStates[resp.state])
        );
    }

    private getCsar(csarId: string): Observable<Csar> {
        const csarUrl = this.combineURLs(this.combineURLs(this.containerUrl, 'csars'), csarId);
        return this.http.get<Csar>(csarUrl, this.headerAcceptJSON);
    }

    private getServiceTemplate(csarId: string): Observable<ServiceTemplate> {
        return this.getCsar(csarId).pipe(
            concatMap(resp => this.http.get<ServiceTemplate>(resp._links['servicetemplate'].href, this.headerAcceptJSON))
        );
    }

    private getBuildPlan(csarId: string): Observable<Plan> {
        return this.getServiceTemplate(csarId).pipe(
            concatMap(resp => this.http.get<PlanResources>(resp._links['buildplans'].href, this.headerAcceptJSON)),
            map(resp => resp.plans.find(plan => plan.plan_type === PlanTypes.BuildPlan))
        );
    }

    private getServiceTemplateInstance(csarId: string, serviceTemplateInstanceId: string): Observable<ServiceTemplateInstance> {
        return this.getServiceTemplate(csarId).pipe(
            concatMap(resp => this.http.get<ServiceTemplateInstanceResources>(resp._links['instances'].href, this.headerAcceptJSON)),
            concatMap(resp => this.http.get<ServiceTemplateInstance>(
                resp.service_template_instances.find(instance =>
                    instance.id.toString() === serviceTemplateInstanceId)._links['self'].href, this.headerAcceptJSON)
            )
        );
    }

    public fetchRunningServiceTemplateInstances(containerUrl: string, csarId: string): Observable<any> {
        const csarUrl = this.combineURLs(this.combineURLs(containerUrl, 'csars'), csarId);
        return this.http.get<Csar>(csarUrl, this.headerAcceptJSON).pipe(
            concatMap(resp => this.http.get<ServiceTemplate>(resp._links['servicetemplate'].href, this.headerAcceptJSON)),
            concatMap(resp => this.http.get<ServiceTemplateInstanceResources>(resp._links['instances'].href, this.headerAcceptJSON)),
            map(resp => resp.service_template_instances.filter(
                instance => instance.state === ServiceTemplateInstanceStates.CREATED).map(instance => instance.id.toString())
            ),
        );
    }

    private getManagementPlans(csarId: string, serviceTemplateInstanceId: string): Observable<Array<Plan>> {
        return this.getServiceTemplateInstance(csarId, serviceTemplateInstanceId).pipe(
            concatMap(resp => this.http.get<PlanResources>(resp._links['managementplans'].href, this.headerAcceptJSON)),
            map(resp => resp.plans)
        );
    }

    private getManagementPlan(csarId: string, serviceTemplateInstanceId: string, planId: string): Observable<Plan> {
        return this.getManagementPlans(csarId, serviceTemplateInstanceId).pipe(
            map(resp => resp.find(plan => plan.id.toString() === planId))
        );
    }

    private getTerminationPlan(csarId: string, serviceTemplateInstanceId: string): Observable<Plan> {
        return this.getManagementPlans(csarId, serviceTemplateInstanceId).pipe(
            map(resp => resp.find(plan => plan.plan_type === PlanTypes.TerminationPlan))
        );
    }

    private getNodeTemplateInstance(csarId: string, serviceTemplateInstanceId: string, nodeTemplateId: string): Observable<NodeTemplateInstance> {
        return this.getServiceTemplate(csarId).pipe(
            // concatMap(resp => this.http.get<NodeTemplateResources>(resp._links['nodetemplates'].href, this.headerAcceptJSON)),
            // concatMap(resp => this.http.get<NodeTemplate>(resp.node_templates.find(template => template.id === nodeTemplateId)._links['self'].href,
            // this.headerAcceptJSON)), concatMap(resp => this.http.get<NodeTemplateInstanceResources>(resp._links['instances'].href, this.headerAcceptJSON)),
            // map(resp => resp.node_template_instances.find(instance => instance.service_template_instance_id.toString() === serviceTemplateInstanceId))

            // TODO: temporary until parsing error fixed
            concatMap(resp => this.http.get<NodeTemplateResources>(resp._links['nodetemplates'].href, this.headerAcceptJSON)),
            concatMap(resp => this.http.get<NodeTemplateInstanceResources>(
                resp.node_templates.find(template => template.id.toString() === nodeTemplateId)._links['self'].href + '/instances', this.headerAcceptJSON)
            ), // todo temp
            concatMap(resp => this.http.get<NodeTemplateInstance>(
                resp.node_template_instances.find(instance =>
                    instance.service_template_instance_id.toString() === serviceTemplateInstanceId)._links['self'].href, this.headerAcceptJSON)
            )
            // map(resp => resp.node_template_instances.find(instance => instance.service_template_instance_id.toString() === serviceTemplateInstanceId))
        );
    }

    public updateNodeTemplateInstanceState(
        csarId: string,
        serviceTemplateInstanceId: string,
        nodeTemplateId: string,
        state: NodeTemplateInstanceStates
    ): Observable<any> {
        return this.getNodeTemplateInstance(csarId, serviceTemplateInstanceId, nodeTemplateId).pipe(
            tap(resp => console.log(resp)),
            concatMap(resp => this.http.put(resp._links['state'].href, state.toString(), this.headerContentTextPlain))
        );
    }

    private combineURLs(baseURL: string, relativeURL: string) {
        return relativeURL
            ? baseURL.replace(/\/+$/, '') + '/' + relativeURL.replace(/^\/+/, '')
            : baseURL;
    }

    private stripCsarSuffix(csarId: string) {
        return csarId.endsWith(this.csarEnding) ? csarId.slice(0, -this.csarEnding.length) : csarId;
    }
}
