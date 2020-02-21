/*******************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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

import { Component, OnDestroy } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap';
import { LiveModelingService } from '../../../services/live-modeling.service';
import { ContainerService } from '../../../services/container.service';
import { BackendService } from '../../../services/backend.service';
import { NgRedux } from '@angular-redux/store';
import { IWineryState } from '../../../redux/store/winery.store';
import { LiveModelingActions } from '../../../redux/actions/live-modeling.actions';
import { HttpClient } from '@angular/common/http';
import { LiveModelingStates } from '../../../models/enums';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Component({
    selector: 'winery-live-modeling-enable-modal',
    templateUrl: './enable-modal.component.html',
    styleUrls: ['./enable-modal.component.css']
})
export class EnableModalComponent {
    containerUrl: string;
    currentCsarId: string;

    fetchingServiceTemplateInstances = false;
    noRunningServiceTemplateInstancesFound = false;

    testingContainerUrl = false;
    isContainerUrlInvalid: boolean;

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
                this.ngRedux.dispatch(this.liveModelingActions.setState(LiveModelingStates.UPDATE));
            } else {
                this.ngRedux.dispatch(this.liveModelingActions.setState(LiveModelingStates.INIT));
            }
            this.dismissModal();
        }, error => {
            this.isContainerUrlInvalid = true;
        }).add(() => {
            this.testingContainerUrl = false;
        });
    }

    checkContainerUrl(): Observable<boolean> {
        return this.http.get(this.containerUrl, { observe: 'response' }).pipe(
            map(resp => resp.ok),
        );
    }

    resetErrorsAndAnimations() {
        this.fetchingServiceTemplateInstances = false;
        this.noRunningServiceTemplateInstancesFound = false;
        this.testingContainerUrl = undefined;
        this.isContainerUrlInvalid = undefined;
    }

    dismissModal() {
        this.bsModalRef.hide();
    }
}
