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
import { NgRedux } from '@angular-redux/store';
import { IWineryState } from '../redux/store/winery.store';
import { TopologyRendererActions } from '../redux/actions/topologyRenderer.actions';
import { LiveModelingStates, ServiceTemplateInstanceStates } from '../models/enums';
import { BackendService } from './backend.service';
import { CsarUpload } from '../models/container/csar-upload.model';
import { ContainerService } from './container.service';
import { ErrorHandlerService } from './error-handler.service';
import { WineryActions } from '../redux/actions/winery.actions';
import { TTopologyTemplate } from '../models/ttopology-template';
import { LiveModelingData } from '../models/liveModelingData';
import { LiveModelingLoggerService } from './live-modeling-logger.service';
import { LiveModelingLog, LiveModelingLogTypes } from '../models/liveModelingLog';

@Injectable()
export class LiveModelingService {
    private currentCsarId: string;
    private currentServiceTemplateInstanceId: string;

    private pollInterval: number;
    private pollTimeout: number;

    private currentTopologyTemplate: TTopologyTemplate;

    private readonly csarEnding = '.csar';

    constructor(
        private ngRedux: NgRedux<IWineryState>,
        private wineryActions: WineryActions,
        private topologyRendererActions: TopologyRendererActions,
        private containerService: ContainerService,
        private backendService: BackendService,
        private errorHandler: ErrorHandlerService,
        private liveModelingLoggerService: LiveModelingLoggerService) {

        this.currentCsarId = this.normalizeCsarId(this.backendService.configuration.id);
        this.pollInterval = 1000;
        this.pollTimeout = 60000;

        this.ngRedux.select(state => state.topologyRendererState.liveModelingState)
            .subscribe(state => this.performAction(state));

        this.ngRedux.select(state => state.wineryState.currentJsonTopology)
            .subscribe(topologyTemplate => {
                this.currentTopologyTemplate = topologyTemplate;
            });
    }

    private getCsarResourceUrl(csarId: string): string {
        const csarQueryString = '?csar';
        return this.backendService.configuration.repositoryURL + '/' +
            this.backendService.configuration.parentPath + '/' +
            encodeURIComponent(encodeURIComponent(this.backendService.configuration.ns)) + '/' +
            this.stripCsarSuffix(csarId) + csarQueryString;
    }

    private normalizeCsarId(csarId: string) {
        return csarId.endsWith(this.csarEnding) ? csarId : csarId + this.csarEnding;
    }

    private stripCsarSuffix(csarId: string) {
        return csarId.endsWith(this.csarEnding) ? csarId.slice(0, -this.csarEnding.length) : csarId;
    }

    public configureContainerUrl(containerUrl: string) {
        this.containerService.containerUrl = containerUrl;
    }

    private performAction(state: LiveModelingStates) {
        switch (state) {
            case LiveModelingStates.DISABLED:
                break;
            case LiveModelingStates.START:
                this.start();
                break;
            case LiveModelingStates.TERMINATE:
                this.terminate();
                break;
            case LiveModelingStates.ENABLED:
                break;
            case LiveModelingStates.REDEPLOY:
                this.redeploy();
                break;
            case LiveModelingStates.UPDATE:
                this.update();
                break;
            case LiveModelingStates.ERROR:
                this.terminate();
                break;
            default:
        }
    }

    private async start() {
        try {
            await this.installCsarIfNeeded(this.currentCsarId);
            const newInstanceId = await this.deployServiceTemplateInstanceOfGivenCsar(this.currentCsarId);
            this.currentServiceTemplateInstanceId = newInstanceId;
            this.initiateNodeTemplateData();
            this.ngRedux.dispatch(this.topologyRendererActions.setLiveModelingState(LiveModelingStates.ENABLED));
        } catch (error) {
            this.errorHandler.handleError(error);
            this.ngRedux.dispatch(this.topologyRendererActions.setLiveModelingState(LiveModelingStates.ERROR));
        }
    }

    private async installCsarIfNeeded(csarId: string) {
        const appInstalled = await this.containerService.isApplicationInstalled(csarId).toPromise();

        if (!appInstalled) {
            console.log('App not found, installing...');
            let uploadPayload = new CsarUpload(this.getCsarResourceUrl(csarId), csarId, 'false');
            await this.containerService.installApplication(uploadPayload).toPromise();
        } else {
            console.log('App is already installed');
        }
    }

    private async deployServiceTemplateInstanceOfGivenCsar(csarId: string): Promise<string> {
        console.log('Deploying service template instance...');

        const correlationId = await this.containerService.deployServiceTemplateInstance(csarId).toPromise();

        console.log('Executing build plan with correlation id ' + correlationId);

        const instanceId = await this.containerService.waitForServiceTemplateInstanceIdAfterDeployment(csarId, correlationId, this.pollInterval, this.pollTimeout).toPromise();

        console.log('Waiting for deployment of service template instance with id ' + instanceId);

        await this.containerService.waitForServiceTemplateInstanceInState(csarId, instanceId, ServiceTemplateInstanceStates.CREATED, this.pollInterval, this.pollTimeout).toPromise();

        console.log('Successfully deployed instance with Id ' + instanceId);

        return instanceId;
    }

    private initiateNodeTemplateData() {
        for (let nodeTemplate of this.currentTopologyTemplate.nodeTemplates) {
            const actionObject = {
                nodeId: nodeTemplate.id,
                liveModelingData: new LiveModelingData()
            };
            this.ngRedux.dispatch(this.wineryActions.setLiveModelingData(actionObject));
        }
    }

    private async terminate() {
        try {
            if (this.currentServiceTemplateInstanceId) {
                await this.terminateServiceTemplateInstance(this.currentCsarId, this.currentServiceTemplateInstanceId);
                this.deleteNodeTemplateData();
                this.currentServiceTemplateInstanceId = null;
            }
            // this.ngRedux.dispatch(this.topologyRendererActions.setLiveModelingState(LiveModelingStates.DISABLED));
        } catch (error) {
            this.errorHandler.handleError(error);
            this.ngRedux.dispatch(this.topologyRendererActions.setLiveModelingState(LiveModelingStates.ERROR));
        }
    }

    private deleteNodeTemplateData() {
        for (let nodeTemplate of this.currentTopologyTemplate.nodeTemplates) {
            const actionObject = {
                nodeId: nodeTemplate.id,
                liveModelingData: null
            };
            this.ngRedux.dispatch(this.wineryActions.setLiveModelingData(actionObject));
        }
    }

    private async terminateServiceTemplateInstance(csarId: string, instanceId: string) {
        console.log('Terminating service template instance ' + instanceId);

        await this.containerService.terminateServiceTemplateInstance(csarId, instanceId).toPromise();

        console.log('Waiting for deletion of instance with instance id ' + instanceId);

        await this.containerService.waitForServiceTemplateInstanceInState(csarId, instanceId, ServiceTemplateInstanceStates.DELETED, this.pollInterval, this.pollTimeout).toPromise();

        console.log('Instance has been successfully deleted');
    }

    private async redeploy() {
        try {
            if (this.isCurrentTopologyInvalid()) {

            }

            if (this.didTopologyChange()) {

            }

            let newCsarId = await this.createTemporaryServiceTemplate();
            newCsarId = this.normalizeCsarId(newCsarId);
            await this.installCsarIfNeeded(newCsarId);

            // ---------------- TEMPORARY ------------------- //

            await this.terminateServiceTemplateInstance(this.currentCsarId, this.currentServiceTemplateInstanceId);
            this.currentCsarId = newCsarId;

            const newInstanceId = await this.deployServiceTemplateInstanceOfGivenCsar(newCsarId);
            this.currentServiceTemplateInstanceId = newInstanceId;

            // ---------------------------------------------- //

            // console.log('Generating transformation plan from  ' + this.csarId + ' to ' + targetCsarName);
            //
            // // Generate transform plan
            // await this.containerService.generateTransformationPlan(this.csarId, targetCsarName).toPromise();
            //
            // console.log('Transformation plan successfully created. Executing now...');
            //
            // // Execute transform plan
            // const correlationId = await this.containerService.executeTransformationPlan(this.instanceId, this.csarId, targetCsarName).toPromise();
            //
            // console.log('Plan correlation id ' + correlationId + '. Waiting for migrated state');
            //
            // // Wait until old instance has state MIGRATED
            // await Observable.interval(1000)
            //     .startWith(false)
            //     .flatMap(() => this.containerService.isServiceTemplateInstanceInState(this.csarId, this.instanceId, InstanceStates.MIGRATED))
            //     .first(resp => resp)
            //     .timeout(60000).toPromise();
            //
            // console.log('Instance successfully migrated. Fetching new instance id...');
            //
            // // Get new service template instance id
            // this.instanceId = await this.containerService.getInstanceIdAfterMigration(this.csarId, this.instanceId, correlationId).toPromise();
            //
            // console.log('Found new instance id: ' + this.instanceId);

            this.ngRedux.dispatch(this.topologyRendererActions.setLiveModelingState(LiveModelingStates.ENABLED));
        } catch (error) {
            this.errorHandler.handleError(error);
            this.ngRedux.dispatch(this.topologyRendererActions.setLiveModelingState(LiveModelingStates.ERROR));
        }
    }

    private isCurrentTopologyInvalid(): boolean {
        //TODO
        return false;
    }

    private didTopologyChange(): boolean {
        //TODO
        return false;
    }

    private async createTemporaryServiceTemplate(): Promise<string> {
        console.log('Creating temporary service template...');
        const serviceTemplateId = await this.backendService.createTemporaryServiceTemplate(this.currentTopologyTemplate).toPromise();
        return serviceTemplateId.xmlId.decoded;
    }

    public async update() {
        try {
            await this.updateNodeTemplateData();
        } catch (error) {
            this.errorHandler.handleError(error);
        } finally {
            this.ngRedux.dispatch(this.topologyRendererActions.setLiveModelingState(LiveModelingStates.ENABLED));
        }
    }

    private async updateNodeTemplateData() {
        for (let nodeTemplate of this.currentTopologyTemplate.nodeTemplates) {
            const state = await this.containerService.getNodeTemplateInstanceState(this.currentCsarId, this.currentServiceTemplateInstanceId, nodeTemplate.id).toPromise();
            for (let nodeTemplate of this.currentTopologyTemplate.nodeTemplates) {
                const actionObject = {
                    nodeId: nodeTemplate.id,
                    liveModelingData: Object.assign(nodeTemplate.liveModelingData, {state: state})
                };
                this.ngRedux.dispatch(this.wineryActions.setLiveModelingData(actionObject));
            }
        }
    }

    public async test() {
        this.liveModelingLoggerService.logSuccess('SUCCESS');
    }
}
