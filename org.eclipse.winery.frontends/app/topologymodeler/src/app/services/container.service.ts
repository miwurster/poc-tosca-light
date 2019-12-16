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
import { PlanInstance } from '../models/container/plan-instance.model';
import { PlanLogEntry } from '../models/container/plan-log-entry.model';
import { InputParameter } from '../models/container/input-parameter.model';

@Injectable()
export class ContainerService {
    private containerUrl: string;
    private currentCsarId: string;
    private currentServiceTemplateInstanceId: string;
    private buildPlanInputParameters: Array<InputParameter>;

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

    private readonly baseInstallationPayload = [
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
    private readonly hidden_input_parameters = [
        'CorrelationID',
        'csarID',
        'serviceTemplateID',
        'containerApiAddress',
        'instanceDataAPIUrl',
        'planCallbackAddress_invoker',
        'csarEntrypoint',
        'OpenTOSCAContainerAPIServiceInstanceID'
    ];

    constructor(
        private ngRedux: NgRedux<IWineryState>,
        private http: HttpClient,
    ) {
        this.ngRedux.select(state => state.liveModelingState.containerUrl)
            .subscribe(containerUrl => {
                this.containerUrl = containerUrl;
            });
        this.ngRedux.select(state => state.liveModelingState.currentCsarId)
            .subscribe(csarId => {
                this.currentCsarId = csarId;
            });
        this.ngRedux.select(state => state.liveModelingState.currentServiceTemplateInstanceId)
            .subscribe(serviceTemplateInstanceId => {
                this.currentServiceTemplateInstanceId = serviceTemplateInstanceId;
            });
        this.ngRedux.select(state => state.liveModelingState.buildPlanInputParameters)
            .subscribe(buildPlanInputParameters => {
                this.buildPlanInputParameters = buildPlanInputParameters;
            });
    }

    public installApplication(uploadPayload: CsarUpload): Observable<any> {
        return this.http.post(this.combineURLs(this.containerUrl, 'csars'), uploadPayload, this.headerContentJSON);
    }

    public isApplicationInstalled(): Observable<boolean> {
        const csarUrl = this.combineURLs(this.combineURLs(this.containerUrl, 'csars'), this.currentCsarId);
        return this.http.get(csarUrl, { observe: 'response' }).pipe(
            map(resp => resp.ok),
            catchError(() => of(false))
        );
    }

    public deployServiceTemplateInstance(): Observable<string> {
        return this.getBuildPlan().pipe(
            concatMap(resp => this.http.post(resp._links['instances'].href, this.getBuildPlanInputParameters(), {
                headers: new HttpHeaders({
                    'Content-Type': 'application/json'
                }),
                responseType: 'text'
            }))
        );
    }

    private getBuildPlanInputParameters(): any[] {
        return [...this.buildPlanInputParameters, ...this.baseInstallationPayload];
    }

    public waitForServiceTemplateInstanceIdAfterDeployment(correlationId: string, interval: number, timeout: number): Observable<string> {
        return Observable.timer(0, interval)
            .concatMap(() => this.getServiceTemplateInstanceIdAfterDeployment(correlationId))
            .first(resp => resp !== '')
            .timeout(timeout);
    }

    private getServiceTemplateInstanceIdAfterDeployment(correlationId: string): Observable<string> {
        return this.getBuildPlanInstance(correlationId).pipe(
            map(resp => resp.service_template_instance_id.toString()),
            catchError(() => of(''))
        );
    }

    public waitForServiceTemplateInstanceInState(
        state: ServiceTemplateInstanceStates,
        interval: number,
        timeout: number
    ): Observable<boolean> {
        return Observable.timer(0, interval)
            .concatMap(() => this.isServiceTemplateInstanceInState(state))
            .first(resp => resp)
            .timeout(timeout);
    }

    private isServiceTemplateInstanceInState(state: ServiceTemplateInstanceStates): Observable<boolean> {
        return this.getServiceTemplateInstanceState().pipe(
            map(resp => resp === state),
            catchError(() => of(false))
        );
    }

    public terminateServiceTemplateInstance(): Observable<string> {
        return this.getTerminationPlan().pipe(
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

    public executeTransformationPlan(sourceCsarId: string, targetCsarId: string): Observable<string> {
        const planId = this.stripCsarSuffix(this.currentCsarId) + '_transformTo_' + this.stripCsarSuffix(targetCsarId) + '_plan';

        return this.getManagementPlan(planId).pipe(
            concatMap(resp => this.http.post(resp._links['instances'].href, this.transformationPayload, {
                headers: new HttpHeaders({
                    'Content-Type': 'application/json'
                }),
                responseType: 'text'
            }))
        );
    }

    public waitForServiceTemplateInstanceIdAfterMigration(
        correlationId: string,
        interval: number,
        timeout: number
    ): Observable<string> {
        return Observable.timer(0, interval)
            .concatMap(() => this.getServiceTemplateInstanceIdAfterMigration(correlationId))
            .first(resp => resp !== '')
            .timeout(timeout);
    }

    private getServiceTemplateInstanceIdAfterMigration(correlationId: string): Observable<string> {
        return this.getManagementPlans().pipe(
            concatMap(resp => this.http.get<PlanInstanceResources>(
                resp.find(plan => plan.plan_type === PlanTypes.TransformationPlan)._links['instances'].href, this.headerAcceptJSON)
            ),
            map(resp => resp.plan_instances.find(plan => plan.correlation_id === correlationId).outputs.find(output => output.name === 'instanceId').value),
            catchError(() => of(''))
        );
    }

    public getNodeTemplateInstanceState(nodeTemplateId: string): Observable<NodeTemplateInstanceStates> {
        return this.getNodeTemplateInstance(nodeTemplateId).pipe(
            map(resp => NodeTemplateInstanceStates[resp.state])
        );
    }

    public getCsar(): Observable<Csar> {
        const csarUrl = this.combineURLs(this.combineURLs(this.containerUrl, 'csars'), this.currentCsarId);
        return this.http.get<Csar>(csarUrl, this.headerAcceptJSON);
    }

    private getServiceTemplate(): Observable<ServiceTemplate> {
        return this.getCsar().pipe(
            concatMap(resp => this.http.get<ServiceTemplate>(resp._links['servicetemplate'].href, this.headerAcceptJSON))
        );
    }

    public getBuildPlanLogs(correlationId: string): Observable<Array<PlanLogEntry>> {
        return this.getBuildPlanInstance(correlationId).pipe(
            map(resp => resp.logs)
        );
    }

    public getRequiredBuildPlanInputParameters(): Observable<Array<InputParameter>> {
        return this.getAllBuildPlanInputParameters().pipe(
            map(resp => resp.filter(input => this.hidden_input_parameters.indexOf(input.name) === -1))
        );
    }

    public getAllBuildPlanInputParameters(): Observable<Array<InputParameter>> {
        return this.getBuildPlan().pipe(
            map(resp => resp.input_parameters)
        );
    }

    private getBuildPlan(): Observable<Plan> {
        return this.getServiceTemplate().pipe(
            concatMap(resp => this.http.get<PlanResources>(resp._links['buildplans'].href, this.headerAcceptJSON)),
            map(resp => resp.plans.find(plan => plan.plan_type === PlanTypes.BuildPlan))
        );
    }

    private getBuildPlanInstance(correlationId: string): Observable<PlanInstance> {
        return this.getBuildPlan().pipe(
            concatMap(resp => this.http.get<PlanInstanceResources>(resp._links['instances'].href, this.headerAcceptJSON)),
            map(resp => resp.plan_instances.find(planInstance => planInstance.correlation_id.toString() === correlationId)),
        );
    }

    public getServiceTemplateInstanceState(): Observable<ServiceTemplateInstanceStates> {
        return this.getServiceTemplateInstance().pipe(
            map(resp => ServiceTemplateInstanceStates[resp.state]),
        );
    }

    private getServiceTemplateInstance(): Observable<ServiceTemplateInstance> {
        return this.getServiceTemplate().pipe(
            concatMap(resp => this.http.get<ServiceTemplateInstanceResources>(resp._links['instances'].href, this.headerAcceptJSON)),
            concatMap(resp => this.http.get<ServiceTemplateInstance>(
                resp.service_template_instances.find(instance =>
                    instance.id.toString() === this.currentServiceTemplateInstanceId)._links['self'].href, this.headerAcceptJSON)
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
            )
        );
    }

    private getManagementPlans(): Observable<Array<Plan>> {
        return this.getServiceTemplateInstance().pipe(
            concatMap(resp => this.http.get<PlanResources>(resp._links['managementplans'].href, this.headerAcceptJSON)),
            map(resp => resp.plans)
        );
    }

    private getManagementPlan(planId: string): Observable<Plan> {
        return this.getManagementPlans().pipe(
            map(resp => resp.find(plan => plan.id.toString() === planId))
        );
    }

    private getTerminationPlan(): Observable<Plan> {
        return this.getManagementPlans().pipe(
            map(resp => resp.find(plan => plan.plan_type === PlanTypes.TerminationPlan))
        );
    }

    private getNodeTemplateInstance(nodeTemplateId: string): Observable<NodeTemplateInstance> {
        return this.getServiceTemplate().pipe(
            // concatMap(resp => this.http.get<NodeTemplateResources>(resp._links['nodetemplates'].href, this.headerAcceptJSON)),
            // concatMap(resp => this.http.get<NodeTemplate>(resp.node_templates.find(template => template.id === nodeTemplateId)._links['self'].href,
            // this.headerAcceptJSON)), concatMap(resp => this.http.get<NodeTemplateInstanceResources>(resp._links['instances'].href, this.headerAcceptJSON)),
            // map(resp => resp.node_template_instances.find(instance => instance.service_template_instance_id.toString() ===
            // this.currentServiceTemplateInstanceId))

            // TODO: temporary until parsing error fixed
            concatMap(resp => this.http.get<NodeTemplateResources>(resp._links['nodetemplates'].href, this.headerAcceptJSON)),
            concatMap(resp => this.http.get<NodeTemplateInstanceResources>(
                resp.node_templates.find(template => template.id.toString() === nodeTemplateId)._links['self'].href + '/instances', this.headerAcceptJSON)
            ), // todo temp
            concatMap(resp => this.http.get<NodeTemplateInstance>(
                resp.node_template_instances.find(instance =>
                    instance.service_template_instance_id.toString() === this.currentServiceTemplateInstanceId)._links['self'].href, this.headerAcceptJSON)
            )
            // map(resp => resp.node_template_instances.find(instance => instance.service_template_instance_id.toString() === serviceTemplateInstanceId))
        );
    }

    public updateNodeTemplateInstanceState(
        nodeTemplateId: string,
        state: NodeTemplateInstanceStates
    ): Observable<any> {
        return this.getNodeTemplateInstance(nodeTemplateId).pipe(
            concatMap(resp => this.http.put(resp._links['state'].href, state.toString(), this.headerContentTextPlain))
        );
    }

    private combineURLs(baseURL: string, relativeURL: string) {
        return relativeURL
            ? baseURL.replace(/\/+$/, '') + '/' + relativeURL.replace(/^\/+/, '')
            : baseURL;
    }

    private stripCsarSuffix(csarId: string) {
        const csarEnding = '.csar';
        return csarId.endsWith(csarEnding) ? csarId.slice(0, -csarEnding.length) : csarId;
    }
}
