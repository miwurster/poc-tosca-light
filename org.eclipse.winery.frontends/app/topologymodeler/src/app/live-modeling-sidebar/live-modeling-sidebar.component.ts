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

import { AfterViewInit, Component, ElementRef, Input, OnDestroy, OnInit, QueryList, TemplateRef, ViewChild, ViewChildren } from '@angular/core';
import { BsModalRef, BsModalService, ProgressbarConfig } from 'ngx-bootstrap';
import { Subscription } from 'rxjs';
import { NgRedux } from '@angular-redux/store';
import { IWineryState } from '../redux/store/winery.store';
import { LiveModelingLogTypes, LiveModelingStates, ServiceTemplateInstanceStates } from '../models/enums';
import { LiveModelingLog } from '../models/liveModelingLog';
import { LiveModelingActions } from '../redux/actions/live-modeling.actions';
import { LiveModelingService } from '../services/live-modeling.service';
import { WineryActions } from '../redux/actions/winery.actions';
import { state, style, trigger } from '@angular/animations';
import { LiveModelingModalComponent, LiveModelingModalComponentViews } from '../live-modeling-modal/live-modeling-modal.component';
import { InputParameter } from '../models/container/input-parameter.model';
import { Csar } from '../models/container/csar.model';
import { ResizeEvent } from 'angular-resizable-element';
import { BackendService } from '../services/backend.service';
import { ToastrService } from 'ngx-toastr';
import { ContainerService } from '../services/container.service';

export function getProgressbarConfig(): ProgressbarConfig {
    return Object.assign(new ProgressbarConfig(), { animate: true, striped: true, max: 100 });
}

@Component({
    selector: 'winery-live-modeling-sidebar',
    templateUrl: './live-modeling-sidebar.component.html',
    styleUrls: ['./live-modeling-sidebar.component.css'],
    providers: [{ provide: ProgressbarConfig, useFactory: getProgressbarConfig }],
    animations: [
        trigger('sidebarContentState', [
            state('shrunk', style({
                display: 'none'
            })),
            state('extended', style({
                display: 'block'
            }))
        ]),
        trigger('sidebarButtonState', [
            state('top', style({
                transform: 'rotate(0deg)'
            })),
            state('right', style({
                transform: 'rotate(90deg) translateY(-100%)'
            })),
        ])
    ]
})
export class LiveModelingSidebarComponent implements OnInit, AfterViewInit, OnDestroy {
    @ViewChild('scrollContainer') private scrollContainer: ElementRef;
    @ViewChildren('logItems') private logItems: QueryList<any>;
    @Input() top: number;
    objectKeys = Object.keys;

    sidebarWidth: number;
    sidebarContentState = 'extended';
    sidebarButtonState = 'right';

    showProgressbar: boolean;

    readonly SCROLL_THRESHOLD = 50;
    isNearBottom = true;

    logs: Array<LiveModelingLog> = [];

    liveModelingState: LiveModelingStates;
    liveModelingStates = LiveModelingStates;
    buildPlanInputParameters: Array<InputParameter>;
    serviceTemplateInstanceState: ServiceTemplateInstanceStates;
    serviceTemplateInstanceId: string;
    currentCsar: Csar;

    subscriptions: Array<Subscription> = [];

    updatingServiceTemplateInstanceState = false;
    modalRef: BsModalRef;
    settings: any;

    selectedServiceTemplateInstanceId: string;
    serviceTemplateInstanceIds: string[];

    constructor(private ngRedux: NgRedux<IWineryState>,
                private wineryActions: WineryActions,
                private liveModelingActions: LiveModelingActions,
                private liveModelingService: LiveModelingService,
                private modalService: BsModalService,
                private backendService: BackendService,
                private alert: ToastrService,
                private containerService: ContainerService) {
    }

    ngOnInit() {
        this.subscriptions.push(this.ngRedux.select(wineryState => wineryState.liveModelingState.logs)
            .subscribe(logs => {
                if (logs.length > 0) {
                    this.displayLiveModelingLog(logs[logs.length - 1]);
                } else {
                    this.logs = [];
                }
            }));
        this.subscriptions.push(this.ngRedux.select(wineryState => wineryState.liveModelingState.state)
            .subscribe(liveModelingState => {
                this.liveModelingState = liveModelingState;
                this.updateProgressbar();
            }));
        this.subscriptions.push(this.ngRedux.select(wineryState => wineryState.liveModelingState.currentServiceTemplateInstanceId)
            .subscribe(serviceTemplateInstanceId => {
                this.serviceTemplateInstanceId = serviceTemplateInstanceId;
            }));
        this.subscriptions.push(this.ngRedux.select(wineryState => wineryState.liveModelingState.currentServiceTemplateInstanceState)
            .subscribe(serviceTemplateInstanceState => {
                this.serviceTemplateInstanceState = serviceTemplateInstanceState;
            }));
        this.subscriptions.push(this.ngRedux.select(wineryState => wineryState.wineryState.liveModelingSidebarOpenedState)
            .subscribe(sidebarOpened => {
                this.updateSidebarState(sidebarOpened);
            }));
        this.subscriptions.push(this.ngRedux.select(wineryState => wineryState.liveModelingState.buildPlanInputParameters)
            .subscribe(inputParameters => {
                this.buildPlanInputParameters = inputParameters;
            }));
        this.subscriptions.push(this.ngRedux.select(wineryState => wineryState.liveModelingState.currentCsar)
            .subscribe(csar => {
                this.currentCsar = csar;
            }));
        this.subscriptions.push(this.ngRedux.select(wineryState => wineryState.liveModelingState.settings)
            .subscribe(settings => {
                this.settings = settings;
            }));
    }

    unsavedChanges(): boolean {
        return this.ngRedux.getState().wineryState.unsavedChanges;
    }

    enableLiveModeling() {
        const initialState = {
            currentModalView: LiveModelingModalComponentViews.ENABLE_LIVE_MODELING
        };
        this.modalService.show(LiveModelingModalComponent, { initialState, ignoreBackdropClick: true });
    }

    saveTopologyAndEnableLiveModeling() {
        this.dismissModal();
        this.ngRedux.dispatch(this.wineryActions.setOverlayContent('Saving topology template. This may take a while.'));
        this.ngRedux.dispatch(this.wineryActions.setOverlayVisibility(true));
        this.backendService.saveTopologyTemplate(this.ngRedux.getState().wineryState.currentJsonTopology)
            .subscribe(res => {
                if (res.ok) {
                    this.alert.success('<p>Saved the topology!<br>' + 'Response Status: '
                        + res.statusText + ' ' + res.status + '</p>');
                    this.ngRedux.dispatch(this.wineryActions.saveTopologyTemplate());
                    this.ngRedux.dispatch(this.wineryActions.checkForUnsavedChanges());
                    this.enableLiveModeling();
                } else {
                    this.alert.info('<p>Something went wrong! <br>' + 'Response Status: '
                        + res.statusText + ' ' + res.status + '</p>');
                }
            }, err => this.alert.error(err.error))
            .add(() => {
                this.ngRedux.dispatch(this.wineryActions.setOverlayVisibility(false));
            });
    }

    disableLiveModeling() {
        const initialState = {
            currentModalView: LiveModelingModalComponentViews.DISABLE_LIVE_MODELING
        };
        this.modalService.show(LiveModelingModalComponent, { initialState, ignoreBackdropClick: true });
    }

    deployNewInstance() {
        this.ngRedux.dispatch(this.liveModelingActions.setState(LiveModelingStates.INIT));
    }

    private displayLiveModelingLog(log) {
        if (this.isNotEmpty(log)) {
            this.logs.push(log);
        }
    }

    private updateProgressbar() {
        switch (this.liveModelingState) {
            case LiveModelingStates.DISABLED:
            case LiveModelingStates.ENABLED:
            case LiveModelingStates.ERROR:
                this.showProgressbar = false;
                break;
            default:
                this.showProgressbar = true;
        }
    }

    private getBadgeBackgroundForLog(type: LiveModelingLogTypes) {
        switch (type) {
            case LiveModelingLogTypes.INFO:
                return '#007bff';
            case LiveModelingLogTypes.SUCCESS:
                return '#28a745';
            case LiveModelingLogTypes.WARNING:
                return '#ffc107';
            case LiveModelingLogTypes.DANGER:
                return '#dc3545';
            case LiveModelingLogTypes.CONTAINER:
                return '#6c757d';
        }
    }

    ngAfterViewInit(): void {
        this.subscriptions.push(this.logItems.changes.subscribe(_ => {
            if (this.liveModelingState !== LiveModelingStates.DISABLED) {
                this.scrollToBottom();
            }
        }));
    }

    private scrolled() {
        const position = this.scrollContainer.nativeElement.scrollTop + this.scrollContainer.nativeElement.offsetHeight;
        const height = this.scrollContainer.nativeElement.scrollHeight;
        this.isNearBottom = position > height - this.SCROLL_THRESHOLD;
    }

    private scrollToBottom(): void {
        if (this.isNearBottom) {
            this.scrollContainer.nativeElement.scroll({
                top: this.scrollContainer.nativeElement.scrollHeight,
                left: 0,
                behavior: 'smooth'
            });
        }
    }

    private isNotEmpty(obj) {
        return !(Object.keys(obj).length === 0);
    }

    private updateServiceTemplateInstanceState() {
        this.updatingServiceTemplateInstanceState = true;
        this.liveModelingService.updateCurrentServiceTemplateInstanceState().then(_ => {
            this.updatingServiceTemplateInstanceState = false;
        });
    }

    private getBadgeBackgroundForState(serviceTemplateInstanceState: ServiceTemplateInstanceStates) {
        switch (serviceTemplateInstanceState) {
            case ServiceTemplateInstanceStates.DELETED:
            case ServiceTemplateInstanceStates.DELETING:
            case ServiceTemplateInstanceStates.ERROR:
                return '#dc3545';
            case ServiceTemplateInstanceStates.MIGRATED:
            case ServiceTemplateInstanceStates.MIGRATING:
            case ServiceTemplateInstanceStates.CREATING:
                return '#007bff';
            case ServiceTemplateInstanceStates.CREATED:
                return '#28a745';
            case ServiceTemplateInstanceStates.INITIAL:
            case ServiceTemplateInstanceStates.NOT_AVAILABLE:
                return '#6c757d';
        }
    }

    openSwitchInstanceModal(templateRef) {
        this.openModal(templateRef);
        this.selectedServiceTemplateInstanceId = this.serviceTemplateInstanceId;
        this.containerService.fetchRunningServiceTemplateInstances(
            this.ngRedux.getState().liveModelingState.containerUrl, 
            this.ngRedux.getState().liveModelingState.currentCsarId
        ).subscribe(resp => {
            this.serviceTemplateInstanceIds = resp.filter(id => id !== this.serviceTemplateInstanceId);
        });
    }

    selectServiceTemplateId(id: string) {
        this.selectedServiceTemplateInstanceId = id;
    }
    
    switchServiceTemplateInstance() {
        this.ngRedux.dispatch(this.liveModelingActions.setCurrentServiceTemplateInstanceId(this.selectedServiceTemplateInstanceId));
        this.ngRedux.dispatch(this.liveModelingActions.clearLogs());
        this.ngRedux.dispatch(this.liveModelingActions.setState(LiveModelingStates.UPDATE));
        this.dismissModal();
    }
    

    redeploy() {
        this.ngRedux.dispatch(this.liveModelingActions.setState(LiveModelingStates.REDEPLOY));
    }

    terminate() {
        this.ngRedux.dispatch(this.liveModelingActions.setState(LiveModelingStates.TERMINATE));
        this.dismissModal();
    }

    refresh() {
        this.ngRedux.dispatch(this.liveModelingActions.setState(LiveModelingStates.UPDATE));
    }

    clearLogs() {
        this.ngRedux.dispatch(this.liveModelingActions.clearLogs());
    }

    setSettings() {
        this.ngRedux.dispatch(this.liveModelingActions.setSettings(this.settings));
        this.dismissModal();
    }

    updateSidebarState(sidebarOpened: boolean) {
        if (sidebarOpened) {
            this.sidebarButtonState = 'top';
            this.sidebarContentState = 'extended';
        } else {
            this.sidebarButtonState = 'right';
            this.sidebarContentState = 'shrunk';
        }
    }

    toggleSidebarState() {
        if (this.sidebarContentState === 'shrunk') {
            this.ngRedux.dispatch(this.wineryActions.sendLiveModelingSidebarOpened(true));
        } else {
            this.ngRedux.dispatch(this.wineryActions.sendLiveModelingSidebarOpened(false));
        }
    }

    validateResize(event: ResizeEvent) {
        const SIDEBAR_MIN_WIDTH = 300;
        return event.rectangle.width >= SIDEBAR_MIN_WIDTH;
    }

    onResizeEnd(event: ResizeEvent): void {
        this.sidebarWidth = event.rectangle.width;
    }

    openModal(template: TemplateRef<any>) {
        this.modalRef = this.modalService.show(template, { ignoreBackdropClick: true });
    }

    dismissModal() {
        this.modalRef.hide();
    }

    ngOnDestroy() {
        this.subscriptions.forEach(subscription => subscription.unsubscribe());
    }
}
