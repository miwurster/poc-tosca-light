import { Component } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap';
import { LiveModelingStates } from '../models/enums';
import { LiveModelingService } from '../services/live-modeling.service';
import { NgRedux } from '@angular-redux/store';
import { IWineryState } from '../redux/store/winery.store';
import { ContainerService } from '../services/container.service';
import { BackendService } from '../services/backend.service';
import { WineryActions } from '../redux/actions/winery.actions';

export enum LiveModelingModalComponentViews {
    'ENABLE_LIVE_MODELING' = 'ENABLE_LIVE_MODELING',
    'DISABLE_LIVE_MODELING' = 'DISABLE_LIVE_MODELING'
}

@Component({
    selector: 'winery-live-modeling-modal',
    templateUrl: './live-modeling-modal.component.html',
    styleUrls: ['./live-modeling-modal.component.css']
})
export class LiveModelingModalComponent {
    modalView: LiveModelingModalComponentViews;
    liveModelingModalComponentViews = LiveModelingModalComponentViews;
    containerUrl: string;
    currentCsarId: string;

    selectedServiceTemplateId: string;
    serviceTemplateInstanceIds: string[];
    test = true;

    terminateInstance: boolean;

    private readonly csarEnding = '.csar';

    constructor(private bsModalRef: BsModalRef,
                private liveModelingService: LiveModelingService,
                private containerService: ContainerService,
                private backendService: BackendService,
                private ngRedux: NgRedux<IWineryState>,
                private wineryActions: WineryActions
    ) {
        this.currentCsarId = this.normalizeCsarId(this.backendService.configuration.id);
        this.containerUrl = 'http://' + window.location.hostname + ':1337';
        this.terminateInstance = true;
    }

    private normalizeCsarId(csarId: string) {
        return csarId.endsWith(this.csarEnding) ? csarId : csarId + this.csarEnding;
    }

    fetchServiceTemplateInstances() {
        try {
            this.serviceTemplateInstanceIds = [];
            this.containerService.fetchRunningServiceTemplateInstances(this.containerUrl, this.currentCsarId).subscribe(resp => {
                this.serviceTemplateInstanceIds = resp;
            });
        } catch (error) {
            // handle error
        }
    }

    selectServiceTemplateId(id: string) {
        this.selectedServiceTemplateId = id;
    }

    enableLiveModeling() {
        if (this.selectedServiceTemplateId) {
            this.ngRedux.dispatch(this.wineryActions.setCurrentServiceTemplateInstanceId(this.selectedServiceTemplateId));
        } else {
            this.ngRedux.dispatch(this.wineryActions.setCurrentServiceTemplateInstanceId(null));
        }
        this.ngRedux.dispatch(this.wineryActions.setContainerUrl(this.containerUrl));
        this.ngRedux.dispatch(this.wineryActions.setLiveModelingState(LiveModelingStates.START));
        this.dismissModal();
    }

    disableLiveModeling() {
        if (this.terminateInstance) {
            this.ngRedux.dispatch(this.wineryActions.setLiveModelingState(LiveModelingStates.TERMINATE));
        } else {
            this.ngRedux.dispatch(this.wineryActions.setLiveModelingState(LiveModelingStates.DISABLED));
        }
        this.dismissModal();
    }

    dismissModal() {
        this.bsModalRef.hide();
    }
}

