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
import { LiveModelingStates, NodeTemplateInstanceStates, ServiceTemplateInstanceStates } from '../models/enums';
import { BackendService } from './backend.service';
import { CsarUpload } from '../models/container/csar-upload.model';
import { ContainerService } from './container.service';
import { ErrorHandlerService } from './error-handler.service';
import { TTopologyTemplate } from '../models/ttopology-template';
import { LiveModelingActions } from '../redux/actions/live-modeling.actions';
import { WineryActions } from '../redux/actions/winery.actions';
import { BsModalService } from 'ngx-bootstrap';
import { OverlayService } from './overlay.service';
import { TopologyService } from './topology.service';
import { LoggingService } from './logging.service';
import { BuildplanModalComponent } from '../live-modeling/modals/buildplan-modal/buildplan-modal.component';
import { catchError, concatMap, distinctUntilChanged, switchMap, takeWhile, tap, timeout } from 'rxjs/operators';
import { forkJoin, Observable, of } from 'rxjs';
import { Csar } from '../models/container/csar.model';
import { PlanInstance } from '../models/container/plan-instance.model';

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
        private liveModelingActions: LiveModelingActions,
        private topologyRendererActions: TopologyRendererActions,
        private wineryActions: WineryActions,
        private containerService: ContainerService,
        private backendService: BackendService,
        private errorHandler: ErrorHandlerService,
        private modalService: BsModalService,
        private overlayService: OverlayService,
        private topologyService: TopologyService,
        private loggingService: LoggingService) {

        this.ngRedux.select(state => state.liveModelingState.settings).subscribe(settings => {
            this.pollInterval = settings.interval;
            this.pollTimeout = settings.timeout;
        });

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
    }

    private async performAction(state: LiveModelingStates) {
        switch (state) {
            case LiveModelingStates.DISABLED:
                this.disable();
                break;
            case LiveModelingStates.INIT:
                this.init();
                break;
            case LiveModelingStates.DEPLOY:
                this.deploy();
                break;
            case LiveModelingStates.UPDATE:
                this.update();
                break;
            case LiveModelingStates.ENABLED:
                this.enable();
                break;
            case LiveModelingStates.REDEPLOY:
                this.redeploy();
                break;
            case LiveModelingStates.TERMINATE:
                this.terminate();
                break;
            case LiveModelingStates.TERMINATED:
                this.reset();
                break;
            case LiveModelingStates.ERROR:
                this.error();
                break;
            default:
        }
    }

    private disable() {
        try {
            this.ngRedux.dispatch(this.liveModelingActions.setContainerUrl(null));
            this.ngRedux.dispatch(this.liveModelingActions.setCurrentCsar(null));
            this.ngRedux.dispatch(this.liveModelingActions.setCurrentCsarId(null));
            this.ngRedux.dispatch(this.liveModelingActions.setCurrentServiceTemplateInstanceId(null));
            this.ngRedux.dispatch(this.liveModelingActions.setCurrentServiceTemplateInstanceState(null));
            this.ngRedux.dispatch(this.liveModelingActions.setBuildPlanInputParameters([]));
            this.ngRedux.dispatch(this.liveModelingActions.setCurrentBuildPlanInstance(null));
            this.setAllNodeTemplateWorkingState(false);
            this.setAllNodeTemplateInstanceState(null);
        } catch (error) {
            this.errorHandler.handleError(error);
            this.ngRedux.dispatch(this.liveModelingActions.setState(LiveModelingStates.ERROR));
        }
    }

    private async init() {
        try {
            // Clear logs
            this.loggingService.clearLogs();
            // Create live modeling service template
            const liveModelingId = await this.createLiveModelingServiceTemplate();
            // Set current csar 
            this.ngRedux.dispatch(this.liveModelingActions.setCurrentCsarId(liveModelingId));
            // Install/upload csar to container
            await this.installCsarIfNeeded();
            // Display build plan parameters modal and advance
            await this.retrieveBuildPlanParametersAndShowModalIfNeeded();
        } catch (error) {
            this.errorHandler.handleError(error);
            this.ngRedux.dispatch(this.liveModelingActions.setState(LiveModelingStates.ERROR));
        }
    }

    private async deploy() {
        try {
            // Initialize node template instance states
            this.setAllNodeTemplateInstanceState(NodeTemplateInstanceStates.INITIAL);
            // Set all node templates into working mode
            this.setAllNodeTemplateWorkingState(true);
            // Deploy service template instance
            await this.deployServiceTemplateInstance();
            this.ngRedux.dispatch(this.liveModelingActions.setState(LiveModelingStates.UPDATE));
        } catch (error) {
            this.errorHandler.handleError(error);
            this.ngRedux.dispatch(this.liveModelingActions.setState(LiveModelingStates.ERROR));
        }
    }

    public async update() {
        try {
            this.updateLiveModelingData().add(() => this.ngRedux.dispatch(this.liveModelingActions.setState(LiveModelingStates.ENABLED)));
        } catch (error) {
            this.errorHandler.handleError(error);
        }
    }

    private async enable() {
        try {
            this.setAllNodeTemplateWorkingState(false);
        } catch (error) {
            this.errorHandler.handleError(error);
        }
    }

    private async redeploy() {
        try {
            // Temporary solution until migration bug is resolved
            this.overlayService.showOverlay('Cleaning up');
            this.setAllNodeTemplateInstanceState(null);
            this.setAllNodeTemplateWorkingState(true);
            await this.terminateServiceTemplateInstance();
            this.overlayService.hideOverlay();
            this.ngRedux.dispatch(this.liveModelingActions.setState(LiveModelingStates.INIT));
            /*
            const sourceCsarId = this.currentCsarId;
            const targetCsarId = await this.createLiveModelingServiceTemplate();
            this.ngRedux.dispatch(this.liveModelingActions.setCurrentCsarId(targetCsarId));
            await this.installCsarIfNeeded();

            const newServiceTemplateInstanceId = await this.deployServiceTemplateInstanceOfGivenCsar(newCsarId);
            this.ngRedux.dispatch(this.liveModelingActions.setCurrentServiceTemplateInstanceId(newServiceTemplateInstanceId));
            
            this.logInfo('generate transformation from ' + sourceCsarId + ' to ' + targetCsarId);
            await this.containerService.generateTransformationPlan(sourceCsarId, targetCsarId).toPromise();
            this.logInfo('execute transformation');
            const correlationId = await this.containerService.executeTransformationPlan(sourceCsarId, targetCsarId).toPromise();

            this.logInfo('wait for migration');
            const instanceId = await this.containerService.waitForServiceTemplateInstanceIdAfterMigration(
                correlationId,
                this.pollInterval,
                this.pollTimeout
            ).toPromise();
            this.ngRedux.dispatch(this.liveModelingActions.setCurrentServiceTemplateInstanceId(instanceId));
            this.logInfo('updating');
            this.ngRedux.dispatch(this.liveModelingActions.setState(LiveModelingStates.UPDATE));
            */
        } catch (error) {
            this.errorHandler.handleError(error);
            this.ngRedux.dispatch(this.liveModelingActions.setState(LiveModelingStates.ERROR));
        }
    }

    private async terminate() {
        try {
            this.setAllNodeTemplateWorkingState(true);
            await this.terminateServiceTemplateInstance();
            this.setAllNodeTemplateWorkingState(false);
            this.ngRedux.dispatch(this.liveModelingActions.setState(LiveModelingStates.TERMINATED));
        } catch (error) {
            this.errorHandler.handleError(error);
            this.ngRedux.dispatch(this.liveModelingActions.setState(LiveModelingStates.ERROR));
        }
    }

    private async reset() {
        try {
            this.setAllNodeTemplateInstanceState(null);
            this.ngRedux.dispatch(this.liveModelingActions.setCurrentServiceTemplateInstanceId(null));
        } catch (error) {
            this.errorHandler.handleError(error);
            this.ngRedux.dispatch(this.liveModelingActions.setState(LiveModelingStates.ERROR));
        }
    }

    private async error() {
        this.updateLiveModelingData().add(() => {
            this.ngRedux.dispatch(this.liveModelingActions.setCurrentServiceTemplateInstanceState(ServiceTemplateInstanceStates.ERROR));
            this.overlayService.hideOverlay();
            this.setAllNodeTemplateWorkingState(false);
        });
    }

    // ------------------- HELPER METHODS -------------------------

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

    private async createLiveModelingServiceTemplate(): Promise<string> {
        try {
            this.overlayService.showOverlay('Creating temporary live modeling template');
            const resp = await this.backendService.createLiveModelingServiceTemplate().toPromise();
            this.overlayService.hideOverlay();
            return this.normalizeCsarId(resp.localname);
        } catch (error) {
            this.loggingService.logError('There was an error while creating a temporary service template');
            throw error;
        }
    }

    private async installCsarIfNeeded() {
        try {
            this.overlayService.showOverlay('Checking whether CSAR file is present');
            const appInstalled = await this.containerService.isApplicationInstalled().toPromise();
            if (!appInstalled) {
                this.loggingService.logWarning('App not found, installing now...');
                this.overlayService.showOverlay('Uploading CSAR file to container');
                const uploadPayload = new CsarUpload(this.getCsarResourceUrl(this.currentCsarId), this.currentCsarId, 'false');
                await this.containerService.installApplication(uploadPayload).toPromise();
            } else {
                this.loggingService.logInfo('App found. Skipping installation');
            }
            this.overlayService.hideOverlay();
        } catch (error) {
            this.loggingService.logError('There was an error while uploading the csar to the container');
            throw error;
        }
    }

    private async retrieveBuildPlanParametersAndShowModalIfNeeded() {
        try {
            const requiredBuildPlanInputParameters = await this.containerService.getRequiredBuildPlanInputParameters().toPromise();
            if (requiredBuildPlanInputParameters.length > 0) {
                this.modalService.show(BuildplanModalComponent, { backdrop: 'static' });
            } else {
                this.ngRedux.dispatch(this.liveModelingActions.setState(LiveModelingStates.DEPLOY));
            }
        } catch (error) {
            this.loggingService.logError('There was an error while retrieving the build plan parameters');
            throw error;
        }
    }

    private setAllNodeTemplateInstanceState(state: NodeTemplateInstanceStates) {
        for (const nodeTemplate of this.topologyService.lastSavedJsonTopology.nodeTemplates) {
            this.setNodeTemplateInstanceState(nodeTemplate.id, state);
        }
    }

    private setNodeTemplateInstanceState(nodeTemplateId: string, state: NodeTemplateInstanceStates) {
        this.ngRedux.dispatch(this.wineryActions.setNodeInstanceState(nodeTemplateId, state));
    }

    private setAllNodeTemplateWorkingState(working: boolean) {
        for (const nodeTemplate of this.topologyService.lastSavedJsonTopology.nodeTemplates) {
            this.setNodeTemplateWorkingState(nodeTemplate.id, working);
        }
    }

    private setNodeTemplateWorkingState(nodeTemplateId: string, working: boolean) {
        this.ngRedux.dispatch(this.wineryActions.setNodeWorking(nodeTemplateId, working));
    }

    private async deployServiceTemplateInstance() {
        let correlationId;
        try {
            this.loggingService.logInfo('Deploying service template instance...');

            this.topologyService.lastDeployedJsonTopology = this.currentTopologyTemplate;

            correlationId = await this.containerService.deployServiceTemplateInstance().toPromise();

            this.loggingService.logInfo('Executing build plan with correlation id ' + correlationId);

            const instanceId = await this.containerService.waitForServiceTemplateInstanceIdAfterDeployment(
                correlationId,
                this.pollInterval,
                this.pollTimeout
            ).toPromise();

            this.ngRedux.dispatch(this.liveModelingActions.setCurrentServiceTemplateInstanceId(instanceId));

            this.loggingService.logInfo('Waiting for deployment of service template instance with id ' + instanceId);

            await new Promise<any>(resolve => {
                Observable.timer(0, this.pollInterval).pipe(
                    concatMap(() => this.containerService.getServiceTemplateInstanceState()),
                    distinctUntilChanged(),
                    timeout(this.pollTimeout),
                    takeWhile(state => state !== ServiceTemplateInstanceStates.CREATED && state !== ServiceTemplateInstanceStates.ERROR, true),
                ).subscribe(state => {
                    this.ngRedux.dispatch(this.liveModelingActions.setCurrentServiceTemplateInstanceState(state));
                    if (state === ServiceTemplateInstanceStates.ERROR) {
                        throw new Error();
                    }
                }, () => {
                    this.loggingService.logError('There was an error while polling service template state');
                    throw new Error();
                }, () => {
                    resolve();
                });
            });
            
            this.loggingService.logSuccess('Successfully deployed service template instance with Id ' + instanceId);
            this.ngRedux.dispatch(this.liveModelingActions.setCurrentServiceTemplateInstanceId(instanceId));
        } catch (error) {
            this.loggingService.logError('There was an error while deploying service template instance');
            throw error;
        }
        finally {
            try {
                const buildPlanLogs = await this.containerService.getBuildPlanLogs(correlationId).toPromise();
                for (const log of buildPlanLogs) {
                    this.loggingService.logContainer(log.message);
                }
            } catch (e) {
            }
        }
    }

    private updateLiveModelingData() {
        this.setAllNodeTemplateWorkingState(true);
        return forkJoin([
            this.updateCsar(),
            this.updateBuildPlanInstance(),
            this.updateCurrentServiceTemplateInstanceState(),
            this.updateNodeTemplateData()
        ]).subscribe(responses => {
            this.ngRedux.dispatch(this.liveModelingActions.setCurrentCsar(responses[0]));
            this.ngRedux.dispatch(this.liveModelingActions.setCurrentBuildPlanInstance(responses[1]));
            this.ngRedux.dispatch(this.liveModelingActions.setCurrentServiceTemplateInstanceState(responses[2]));

            for (let i = 0; i < responses[3].length; i++) {
                this.ngRedux.dispatch(this.wineryActions.setNodeInstanceState(this.currentTopologyTemplate.nodeTemplates[i].id, responses[3][i]));
            }
            this.setAllNodeTemplateWorkingState(false);
        });
    }

    private updateCsar(): Observable<Csar> {
        this.loggingService.logInfo('Fetching csar information');
        return this.containerService.getCsar().pipe(
            catchError(_ => {
                this.loggingService.logError('Unable to fetch csar information');
                return of(null);
            })
        );
    }

    private updateBuildPlanInstance(): Observable<PlanInstance> {
        this.loggingService.logInfo(`Fetching service template instance build plan instance`);
        return this.containerService.getServiceTemplateInstanceBuildPlanInstance().pipe(
            catchError(_ => {
                this.loggingService.logError('Unable to fetch build plan instance');
                return of(null);
            })
        );
    }

    private updateNodeTemplateData(): Observable<NodeTemplateInstanceStates[]> {
        const observables: Observable<NodeTemplateInstanceStates>[] = [];
        for (const nodeTemplate of this.topologyService.lastSavedJsonTopology.nodeTemplates) {
            observables.push(this.containerService.getNodeTemplateInstanceState(nodeTemplate.id).pipe(
                catchError(_ => {
                    this.loggingService.logError(`Unable to fetch data for node ${nodeTemplate.id}`);
                    return of(NodeTemplateInstanceStates.NOT_AVAILABLE);
                })
            ));
        }
        return forkJoin(observables);
    }

    public updateCurrentServiceTemplateInstanceState(): Observable<ServiceTemplateInstanceStates> {
        this.loggingService.logInfo(`Fetching service template instance state`);
        return this.containerService.getServiceTemplateInstanceState().pipe(
            catchError(_ => {
                this.loggingService.logError('Unable to fetch service template instance state');
                return of(ServiceTemplateInstanceStates.NOT_AVAILABLE);
            })
        );
    }

    private async terminateServiceTemplateInstance() {
        try {
            this.loggingService.logInfo(`Terminating service template instance ${this.currentServiceTemplateInstanceId}`);

            await this.containerService.terminateServiceTemplateInstance().toPromise();

            this.loggingService.logInfo(`Waiting for deletion of service template instance with instance id ${this.currentServiceTemplateInstanceId}`);

            await new Promise<any>(resolve => {
                Observable.timer(0, this.pollInterval).pipe(
                    concatMap(() => this.containerService.getServiceTemplateInstanceState()),
                    distinctUntilChanged(),
                    timeout(this.pollTimeout),
                    takeWhile(state => state !== ServiceTemplateInstanceStates.DELETED && state !== ServiceTemplateInstanceStates.ERROR, true),
                ).subscribe(state => {
                    this.ngRedux.dispatch(this.liveModelingActions.setCurrentServiceTemplateInstanceState(state));
                    if (state === ServiceTemplateInstanceStates.ERROR) {
                        throw new Error();
                    }
                }, () => {
                    this.loggingService.logError('There was an error while polling service template state');
                    throw new Error();
                }, () => {
                    resolve();
                });
            });

            this.loggingService.logSuccess('Instance has been successfully deleted');
        } catch (error) {
            this.loggingService.logError('There was an error while terminating the service template instance');
            throw error;
        }
    }

    /*
    private async createTemporaryServiceTemplate(): Promise<string> {
        this.logInfo('Creating temporary service template...');
        const temporaryServiceTemplateId = await this.backendService.createTemporaryServiceTemplate(this.currentTopologyTemplate).toPromise();
        return temporaryServiceTemplateId.xmlId.decoded;
    }

    public async test() {
        this.ngRedux.dispatch(this.liveModelingActions.setContainerUrl('http://localhost:1337'));
        this.ngRedux.dispatch(this.liveModelingActions.setCurrentCsarId('MyTinyToDo_Bare_Docker.csar'));
        const inputs = await this.containerService.getRequiredBuildPlanInputParameters().toPromise();
        console.log(inputs);
    }
     */

    /*
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
        const topologyTemplate = { ...this.topologyService.lastSavedJsonTopology };
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
        const currentNodes = this.topologyService.lastSavedJsonTopology.nodeTemplates.filter(node => node.instanceState === targetNodeTemplateInstanceState).map(node => node.id);
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
     */
}
