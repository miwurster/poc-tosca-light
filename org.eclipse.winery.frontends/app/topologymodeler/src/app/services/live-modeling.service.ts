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
import { LiveModelingLogTypes, LiveModelingStates, NodeTemplateInstanceStates, ServiceTemplateInstanceStates } from '../models/enums';
import { BackendService } from './backend.service';
import { CsarUpload } from '../models/container/csar-upload.model';
import { ContainerService } from './container.service';
import { ErrorHandlerService } from './error-handler.service';
import { TRelationshipTemplate, TTopologyTemplate } from '../models/ttopology-template';
import { LiveModelingActions } from '../redux/actions/live-modeling.actions';
import { LiveModelingNodeTemplateData } from '../models/liveModelingNodeTemplateData';
import { LiveModelingLog } from '../models/liveModelingLog';

@Injectable()
export class LiveModelingService {
    private currentCsarId: string;
    private currentServiceTemplateInstanceId: string;

    private pollInterval: number;
    private pollTimeout: number;

    private currentTopologyTemplate: TTopologyTemplate;
    private currentNodeTemplatesData: LiveModelingNodeTemplateData[];

    private readonly csarEnding = '.csar';

    constructor(
        private ngRedux: NgRedux<IWineryState>,
        private liveModelingActions: LiveModelingActions,
        private topologyRendererActions: TopologyRendererActions,
        private containerService: ContainerService,
        private backendService: BackendService,
        private errorHandler: ErrorHandlerService) {

        this.ngRedux.dispatch(liveModelingActions.setCurrentCsarId(this.normalizeCsarId(this.backendService.configuration.id)));

        this.pollInterval = 1000;
        this.pollTimeout = 60000;

        this.ngRedux.select(state => state.liveModelingState.state)
            .subscribe(state => this.performAction(state));

        this.ngRedux.select(state => state.wineryState.currentJsonTopology)
            .subscribe(topologyTemplate => {
                this.currentTopologyTemplate = topologyTemplate;
            });

        this.ngRedux.select(state => state.liveModelingState.currentServiceTemplateInstanceId)
            .subscribe(serviceTemplateInstanceId => {
                this.currentServiceTemplateInstanceId = serviceTemplateInstanceId;
            });

        this.ngRedux.select(state => state.liveModelingState.currentCsarId)
            .subscribe(csarId => {
                this.currentCsarId = csarId;
            });

        this.ngRedux.select(state => state.liveModelingState.nodeTemplatesData)
            .subscribe(nodeTemplatesData => {
                this.currentNodeTemplatesData = nodeTemplatesData;
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

    private async performAction(state: LiveModelingStates) {
        switch (state) {
            case LiveModelingStates.DISABLED:
                this.disable();
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
                break;
            default:
        }
    }

    private disable() {
        if (this.currentTopologyTemplate) {
            this.deleteNodeTemplateData();
        }
    }

    private async start() {
        try {
            if (!this.currentServiceTemplateInstanceId) {
                await this.installCsarIfNeeded();
                await this.deployServiceTemplateInstance();
            } else {
                // Retrieve build plan parameters of already running instance
                const buildPlanParameters = await this.containerService.getRequiredBuildPlanInputParameters().toPromise();
                this.ngRedux.dispatch(this.liveModelingActions.setBuildPlanInputParameters(buildPlanParameters));
            }

            const csar = await this.containerService.getCsar().toPromise();
            this.ngRedux.dispatch(this.liveModelingActions.setCurrentCsar(csar));

            this.initiateNodeTemplateData();
            this.ngRedux.dispatch(this.liveModelingActions.setState(LiveModelingStates.UPDATE));
        } catch (error) {
            this.errorHandler.handleError(error);
            this.ngRedux.dispatch(this.liveModelingActions.setState(LiveModelingStates.ERROR));
        }
    }

    private async installCsarIfNeeded() {
        const appInstalled = await this.containerService.isApplicationInstalled().toPromise();

        if (!appInstalled) {
            this.logWarning('App not found, installing now...');
            const uploadPayload = new CsarUpload(this.getCsarResourceUrl(this.currentCsarId), this.currentCsarId, 'false');
            await this.containerService.installApplication(uploadPayload).toPromise();
        } else {
            this.logInfo('App found. Skipping installation');
        }
    }

    private async deployServiceTemplateInstance() {
        const newServiceTemplateInstanceId = await this.deployServiceTemplateInstanceOfGivenCsar();
        this.ngRedux.dispatch(this.liveModelingActions.setCurrentServiceTemplateInstanceId(newServiceTemplateInstanceId));
    }

    private async deployServiceTemplateInstanceOfGivenCsar(): Promise<string> {
        let correlationId;
        try {
            this.logInfo('Deploying service template instance...');

            correlationId = await this.containerService.deployServiceTemplateInstance().toPromise();

            this.logInfo('Executing build plan with correlation id ' + correlationId);

            const instanceId = await this.containerService.waitForServiceTemplateInstanceIdAfterDeployment(
                correlationId,
                this.pollInterval,
                this.pollTimeout
            ).toPromise();

            this.ngRedux.dispatch(this.liveModelingActions.setCurrentServiceTemplateInstanceId(instanceId));

            this.logInfo('Waiting for deployment of service template instance with id ' + instanceId);

            await this.containerService.waitForServiceTemplateInstanceInState(
                ServiceTemplateInstanceStates.CREATED,
                this.pollInterval,
                this.pollTimeout
            ).toPromise();

            this.logSuccess('Successfully deployed service template instance with Id ' + instanceId);

            return instanceId;
        } catch (error) {
            this.logError('There was an error while deploying service template instance');
            throw error;
        } finally {
            try {
                const buildPlanLogs = await this.containerService.getBuildPlanLogs(correlationId).toPromise();
                for (const log of buildPlanLogs) {
                    this.logContainerInfo(log.message);
                }
            } catch (e) {
            }
        }
    }

    private initiateNodeTemplateData() {
        for (const nodeTemplate of this.currentTopologyTemplate.nodeTemplates) {
            this.ngRedux.dispatch(this.liveModelingActions.setNodeTemplateData(LiveModelingNodeTemplateData.initial(nodeTemplate.id)));
        }
    }

    private async terminate() {
        try {
            if (this.currentServiceTemplateInstanceId) {
                await this.terminateServiceTemplateInstance();
                this.ngRedux.dispatch(this.liveModelingActions.setCurrentServiceTemplateInstanceId(null));
            }
            this.ngRedux.dispatch(this.liveModelingActions.setState(LiveModelingStates.DISABLED));
        } catch (error) {
            this.errorHandler.handleError(error);
            this.ngRedux.dispatch(this.liveModelingActions.setState(LiveModelingStates.ERROR));
        }
    }

    private deleteNodeTemplateData() {
        this.ngRedux.dispatch(this.liveModelingActions.deleteNodeTemplateData());
    }

    private async terminateServiceTemplateInstance() {
        this.logInfo(`Terminating service template instance ${this.currentServiceTemplateInstanceId}`);

        await this.containerService.terminateServiceTemplateInstance().toPromise();

        this.logInfo(`Waiting for deletion of service template instance with instance id ${this.currentServiceTemplateInstanceId}`);

        await this.containerService.waitForServiceTemplateInstanceInState(
            ServiceTemplateInstanceStates.DELETED,
            this.pollInterval,
            this.pollTimeout
        ).toPromise();

        this.logSuccess('Instance has been successfully deleted');
    }

    private async redeploy() {
        /*
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
          this.ngRedux.dispatch(this.liveModelingActions.setCurrentCsarId(newCsarId));

          const newServiceTemplateInstanceId = await this.deployServiceTemplateInstanceOfGivenCsar(newCsarId);
          this.ngRedux.dispatch(this.liveModelingActions.setCurrentServiceTemplateInstanceId(newServiceTemplateInstanceId));

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

          this.ngRedux.dispatch(this.liveModelingActions.setState(LiveModelingStates.ENABLED));
      } catch (error) {
          this.errorHandler.handleError(error);
          this.ngRedux.dispatch(this.liveModelingActions.setState(LiveModelingStates.ERROR));
      }
   */
    }

    private isCurrentTopologyInvalid(): boolean {
        // TODO
        return false;
    }

    private didTopologyChange(): boolean {
        // TODO
        return false;
    }

    private async createTemporaryServiceTemplate(): Promise<string> {
        this.logInfo('Creating temporary service template...');
        const temporaryServiceTemplateId = await this.backendService.createTemporaryServiceTemplate(this.currentTopologyTemplate).toPromise();
        return temporaryServiceTemplateId.xmlId.decoded;
    }

    public async update() {
        try {
            await this.updateNodeTemplateData();
            await this.updateCurrentServiceTemplateInstanceState();
        } catch (error) {
            this.errorHandler.handleError(error);
        } finally {
            this.ngRedux.dispatch(this.liveModelingActions.setState(LiveModelingStates.ENABLED));
        }
    }

    private async updateNodeTemplateData() {
        const numNodeTemplates = this.currentTopologyTemplate.nodeTemplates.length;
        for (let i = 0; i < numNodeTemplates; i++) {
            const nodeTemplate = this.currentTopologyTemplate.nodeTemplates[i];
            this.logInfo(`Fetching data for node ${nodeTemplate.id} (${i + 1}/${numNodeTemplates})`);
            const state = await this.containerService.getNodeTemplateInstanceState(
                nodeTemplate.id
            ).toPromise();
            this.ngRedux.dispatch(this.liveModelingActions.setNodeTemplateData(new LiveModelingNodeTemplateData(nodeTemplate.id, state)));
        }
    }

    public async updateCurrentServiceTemplateInstanceState() {
        this.logInfo(`Fetching service template instance state`);
        try {
            const serviceTemplateInstanceState = await this.containerService.getServiceTemplateInstanceState().toPromise();
            this.ngRedux.dispatch(this.liveModelingActions.setCurrentServiceTemplateInstanceState(serviceTemplateInstanceState));
        } catch (error) {
            this.ngRedux.dispatch(this.liveModelingActions.setCurrentServiceTemplateInstanceState(ServiceTemplateInstanceStates.NOT_AVAILABLE));
        }
    }

    public async test() {
        this.ngRedux.dispatch(this.liveModelingActions.setContainerUrl('http://localhost:1337'));
        this.ngRedux.dispatch(this.liveModelingActions.setCurrentCsarId('MyTinyToDo_Bare_Docker.csar'));
        const inputs = await this.containerService.getRequiredBuildPlanInputParameters().toPromise();
        console.log(inputs);
    }

    public async startNode(nodeTemplateId: string) {
        const adaptationArray = this.calculateAdaptationArray(nodeTemplateId, NodeTemplateInstanceStates.STARTED);
        await this.setNodeTemplateInstanceStates(adaptationArray);
        this.ngRedux.dispatch(this.liveModelingActions.setState(LiveModelingStates.UPDATE));
    }

    public async stopNode(nodeTemplateId: string) {
        const adaptationArray = this.calculateAdaptationArray(nodeTemplateId, NodeTemplateInstanceStates.STOPPED);
        await this.setNodeTemplateInstanceStates(adaptationArray);
        this.ngRedux.dispatch(this.liveModelingActions.setState(LiveModelingStates.UPDATE));
    }

    private calculateAdaptationArray(
        nodeTemplateId: string,
        targetNodeTemplateInstanceState: NodeTemplateInstanceStates): Array<[string, NodeTemplateInstanceStates]> {
        const topologyTemplate = { ...this.currentTopologyTemplate };
        const requiredSet: Set<string> = new Set<string>();
        const workingArray: Array<string> = [];
        const workingRel: Array<TRelationshipTemplate> = [...topologyTemplate.relationshipTemplates];
        workingArray.push(nodeTemplateId);

        // recursively calculate all nodes that depend on the source node
        while (workingArray.length > 0) {
            const nodeId = workingArray.shift();
            requiredSet.add(nodeId);
            let tempRelationships: TRelationshipTemplate[];
            if (targetNodeTemplateInstanceState === NodeTemplateInstanceStates.STARTED) {
                tempRelationships = workingRel.filter(rel => rel.sourceElement.ref === nodeId);
            } else if (targetNodeTemplateInstanceState === NodeTemplateInstanceStates.STOPPED) {
                tempRelationships = workingRel.filter(rel => rel.targetElement.ref === nodeId);
            }
            for (const tempRel of tempRelationships) {
                if (targetNodeTemplateInstanceState === NodeTemplateInstanceStates.STARTED) {
                    workingArray.push(tempRel.targetElement.ref);
                } else if (targetNodeTemplateInstanceState === NodeTemplateInstanceStates.STOPPED) {
                    workingArray.push(tempRel.sourceElement.ref);
                }
            }
        }

        // find all other nodes that are already in the same target state (so we do not start/stop nodes that are independent from the source node)
        const requiredNodes = Array.from(requiredSet);
        const currentNodes = this.currentNodeTemplatesData.filter(node => node.state === targetNodeTemplateInstanceState).map(node => node.id);
        const mergeArray = Array.from(new Set(requiredNodes.concat(...currentNodes)));
        const adaptationArray: Array<[string, NodeTemplateInstanceStates]> = [];

        // calculate adaptation set which contains all nodes with their respective target state
        for (const nodeTemplate of topologyTemplate.nodeTemplates) {
            if (mergeArray.indexOf(nodeTemplate.id) > -1) {
                adaptationArray.push([nodeTemplate.id, targetNodeTemplateInstanceState]);
            } else {
                if (targetNodeTemplateInstanceState === NodeTemplateInstanceStates.STARTED) {
                    adaptationArray.push([nodeTemplate.id, NodeTemplateInstanceStates.STOPPED]);
                } else if (targetNodeTemplateInstanceState === NodeTemplateInstanceStates.STOPPED) {
                    adaptationArray.push([nodeTemplate.id, NodeTemplateInstanceStates.STARTED]);
                }
            }
        }

        return adaptationArray;
    }

    private async setNodeTemplateInstanceStates(adaptationArray: Array<[string, NodeTemplateInstanceStates]>) {
        for (const nodeTemplate of adaptationArray) {
            await this.containerService.updateNodeTemplateInstanceState(
                nodeTemplate[0],
                nodeTemplate[1]
            ).toPromise();
        }
    }

    private logInfo(message: string) {
        this.ngRedux.dispatch(this.liveModelingActions.sendLog(new LiveModelingLog(message, LiveModelingLogTypes.INFO)));
    }

    private logContainerInfo(message: string) {
        this.ngRedux.dispatch(this.liveModelingActions.sendLog(new LiveModelingLog(message, LiveModelingLogTypes.CONTAINER)));
    }

    private logSuccess(message: string) {
        this.ngRedux.dispatch(this.liveModelingActions.sendLog(new LiveModelingLog(message, LiveModelingLogTypes.SUCCESS)));
    }

    private logWarning(message: string) {
        this.ngRedux.dispatch(this.liveModelingActions.sendLog(new LiveModelingLog(message, LiveModelingLogTypes.WARNING)));
    }

    private logError(message: string) {
        this.ngRedux.dispatch(this.liveModelingActions.sendLog(new LiveModelingLog(message, LiveModelingLogTypes.DANGER)));
    }
}
