import { Component } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap';
import { LiveModelingStates } from '../models/enums';
import { LiveModelingService } from '../services/live-modeling.service';
import { NgRedux } from '@angular-redux/store';
import { IWineryState } from '../redux/store/winery.store';
import { ContainerService } from '../services/container.service';
import { BackendService } from '../services/backend.service';
import { LiveModelingActions } from '../redux/actions/live-modeling.actions';
import { Observable, of } from 'rxjs';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { catchError, map } from 'rxjs/operators';
import { InputParameter } from '../models/container/input-parameter.model';

export enum LiveModelingModalComponentViews {
    'ENABLE_LIVE_MODELING' = 'ENABLE_LIVE_MODELING',
    'DISABLE_LIVE_MODELING' = 'DISABLE_LIVE_MODELING',
    'ENTER_BUILD_PLAN_INPUT_PARAMETER' = 'ENTER_BUILD_PLAN_INPUT_PARAMETER'
}

@Component({
    selector: 'winery-live-modeling-modal',
    templateUrl: './live-modeling-modal.component.html',
    styleUrls: ['./live-modeling-modal.component.css']
})
export class LiveModelingModalComponent {
    currentModalView: LiveModelingModalComponentViews;
    liveModelingModalComponentViews = LiveModelingModalComponentViews;

    containerUrl: string;
    currentCsarId: string;

    fetchingServiceTemplateInstances = false;
    noRunningServiceTemplateInstancesFound = false;

    testingContainerUrl = false;
    isContainerUrlInvalid: boolean;

    fetchingBuildPlanParameters = true;
    requiredBuildPlanParameters: InputParameter[];

    selectedServiceTemplateInstanceId: string;
    serviceTemplateInstanceIds: string[];

    terminateInstance: boolean;

    constructor(private bsModalRef: BsModalRef,
                private liveModelingService: LiveModelingService,
                private containerService: ContainerService,
                private backendService: BackendService,
                private ngRedux: NgRedux<IWineryState>,
                private liveModelingActions: LiveModelingActions,
                private http: HttpClient
    ) {
        this.currentCsarId = this.normalizeCsarId(this.backendService.configuration.id);
        this.containerUrl = 'http://' + window.location.hostname + ':1337';
        this.terminateInstance = true;
    }

    switchCurrentModalView(newModalView: LiveModelingModalComponentViews) {
        switch (newModalView) {
            case LiveModelingModalComponentViews.ENTER_BUILD_PLAN_INPUT_PARAMETER:
                this.currentModalView = newModalView;
                this.fetchingBuildPlanParameters = true;
                this.currentModalView = LiveModelingModalComponentViews.ENTER_BUILD_PLAN_INPUT_PARAMETER;
                this.requiredBuildPlanParameters = [];
                this.getRequiredBuildPlanParameters().subscribe(resp => {
                    this.fetchingBuildPlanParameters = false;
                    this.requiredBuildPlanParameters = resp;
                });
                break;
            default:
                break;
        }
    }

    private normalizeCsarId(csarId: string) {
        const csarEnding = '.csar';
        return csarId.endsWith(csarEnding) ? csarId : csarId + csarEnding;
    }

    fetchServiceTemplateInstances() {
        this.resetErrorsAndAnimations();
        this.serviceTemplateInstanceIds = [];
        this.fetchingServiceTemplateInstances = true;
        this.containerService.fetchRunningServiceTemplateInstances(this.containerUrl, this.currentCsarId).subscribe(resp => {
            this.serviceTemplateInstanceIds = resp;

            if (this.serviceTemplateInstanceIds.length === 0) {
                this.noRunningServiceTemplateInstancesFound = true;
            } else {
                this.noRunningServiceTemplateInstancesFound = false;
            }

            this.isContainerUrlInvalid = false;
        }, error => {
            this.isContainerUrlInvalid = true;
        }).add(() => {
                this.fetchingServiceTemplateInstances = false;
            }
        );
    }

    selectServiceTemplateId(id: string) {
        this.selectedServiceTemplateInstanceId = id;
    }

    deselectServiceTemplateId() {
        this.selectedServiceTemplateInstanceId = null;
    }

    enableLiveModeling() {
        if (this.currentModalView === LiveModelingModalComponentViews.ENABLE_LIVE_MODELING) {
            this.resetErrorsAndAnimations();
            this.testingContainerUrl = true;
            this.checkContainerUrl().subscribe(resp => {
                if (!resp) {
                    this.isContainerUrlInvalid = true;
                    return;
                }
                this.ngRedux.dispatch(this.liveModelingActions.setContainerUrl(this.containerUrl));

                if (this.selectedServiceTemplateInstanceId) {
                    this.ngRedux.dispatch(this.liveModelingActions.setCurrentServiceTemplateInstanceId(this.selectedServiceTemplateInstanceId));
                    this.ngRedux.dispatch(this.liveModelingActions.setState(LiveModelingStates.START));
                    this.dismissModal();
                } else {
                    this.switchCurrentModalView(LiveModelingModalComponentViews.ENTER_BUILD_PLAN_INPUT_PARAMETER);
                }
            }, error => {
                this.isContainerUrlInvalid = true;
            }).add(() => {
                this.testingContainerUrl = false;
            });
        } else {
            this.ngRedux.dispatch(this.liveModelingActions.setBuildPlanInputParameters(this.requiredBuildPlanParameters));
            this.ngRedux.dispatch(this.liveModelingActions.setState(LiveModelingStates.START));
            this.dismissModal();
        }
    }

    checkContainerUrl(): Observable<boolean> {
        return this.http.get(this.containerUrl, { observe: 'response' }).pipe(
            map(resp => resp.ok),
        );
    }

    getRequiredBuildPlanParameters(): Observable<Array<InputParameter>> {
        return this.containerService.getRequiredBuildPlanInputParameters();
    }

    disableLiveModeling() {
        if (this.terminateInstance) {
            this.ngRedux.dispatch(this.liveModelingActions.setState(LiveModelingStates.TERMINATE));
        } else {
            this.ngRedux.dispatch(this.liveModelingActions.setState(LiveModelingStates.DISABLED));
        }
        this.dismissModal();
    }

    resetErrorsAndAnimations() {
        this.fetchingServiceTemplateInstances = false;
        this.noRunningServiceTemplateInstancesFound = false;
        this.testingContainerUrl = undefined;
        this.isContainerUrlInvalid = undefined;
    }

    back() {
        switch (this.currentModalView) {
            case LiveModelingModalComponentViews.ENTER_BUILD_PLAN_INPUT_PARAMETER:
                this.currentModalView = LiveModelingModalComponentViews.ENABLE_LIVE_MODELING;
                break;
            default:
                this.currentModalView = LiveModelingModalComponentViews.ENABLE_LIVE_MODELING;
                break;
        }
    }

    dismissModal() {
        this.bsModalRef.hide();
    }
}
