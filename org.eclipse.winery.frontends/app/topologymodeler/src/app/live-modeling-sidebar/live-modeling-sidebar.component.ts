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

import { AfterViewInit, Component, ElementRef, OnInit, QueryList, ViewChild, ViewChildren } from '@angular/core';
import { ProgressbarConfig } from 'ngx-bootstrap';
import { Subscription } from 'rxjs';
import { NgRedux } from '@angular-redux/store';
import { IWineryState } from '../redux/store/winery.store';
import { LiveModelingStates } from '../models/enums';
import { LiveModelingLog, LiveModelingLogTypes } from '../models/liveModelingLog';

export function getProgressbarConfig(): ProgressbarConfig {
    return Object.assign(new ProgressbarConfig(), { animate: true, striped: true, max: 100 });
}

@Component({
    selector: 'winery-live-modeling-sidebar',
    templateUrl: './live-modeling-sidebar.component.html',
    styleUrls: ['./live-modeling-sidebar.component.css'],
    providers: [{ provide: ProgressbarConfig, useFactory: getProgressbarConfig }]
})
export class LiveModelingSidebarComponent implements OnInit, AfterViewInit {
    @ViewChild('scrollContainer') private scrollContainer: ElementRef;
    @ViewChildren('logItems') private logItems: QueryList<any>;

    showProgressbar: boolean;

    readonly SCROLL_THRESHOLD = 50;
    isNearBottom: boolean = true;

    logs: Array<LiveModelingLog> = [];

    liveModelingState: LiveModelingStates;
    liveModelingStates = LiveModelingStates;

    subscriptions: Array<Subscription> = [];

    constructor(private ngRedux: NgRedux<IWineryState>) {
    }

    ngOnInit() {
        this.subscriptions.push(this.ngRedux.select(wineryState => wineryState.wineryState.liveModelingLog)
            .subscribe(log => this.displayLiveModelingLog(log)));
        this.subscriptions.push(this.ngRedux.select(topologyRendererState => topologyRendererState.topologyRendererState.liveModelingState)
            .subscribe(state => {
                this.liveModelingState = state;
                this.updateProgressbar();
            }));
    }

    private displayLiveModelingLog(log) {
        if (this.isNotEmpty(log)) {
            this.logs.push(log);
        }
    }

    private updateProgressbar() {
        switch (this.liveModelingState) {
            case (LiveModelingStates.DISABLED || LiveModelingStates.ENABLED || LiveModelingStates.ERROR):
                this.showProgressbar = true;
            default:
                this.showProgressbar = false;
        }
    }

    private getBackgroundOfType(type) {
        switch (type) {
            case LiveModelingLogTypes.INFO:
                return '#007bff';
            case LiveModelingLogTypes.SUCCESS:
                return '#28a745';
            case LiveModelingLogTypes.WARNING:
                return '#ffc107';
            case LiveModelingLogTypes.DANGER:
                return '#dc3545';
            default:
                return '#6c757d';
        }
    }

    ngAfterViewInit(): void {
        this.subscriptions.push(this.logItems.changes.subscribe(_ => this.scrollToBottom()));
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

    ngOnDestroy() {
        this.subscriptions.forEach(subscription => subscription.unsubscribe());
    }
}
