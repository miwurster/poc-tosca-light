/********************************************************************************
 * Copyright (c) 2017-2018 Contributors to the Eclipse Foundation
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
 ********************************************************************************/

import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { Component, EventEmitter, OnDestroy, OnInit, Output } from '@angular/core';
import { NgRedux } from '@angular-redux/store';
import { IWineryState } from '../redux/store/winery.store';
import { WineryActions } from '../redux/actions/winery.actions';
import { Subject, Subscription } from 'rxjs';

import { animate, state, style, transition, trigger } from '@angular/animations';
import { QName } from '../models/qname';
import { urlElement } from '../models/enums';
import { BackendService } from '../services/backend.service';
import { GroupsComponent} from '../node/groups/groups.component';
import { GroupsModalData } from '../models/groupsModalData';

/**
 * This is the right sidebar, node groups can be managed
 */
@Component({
    selector: 'winery-group-sidebar',
    templateUrl: './sidebar-group.component.html',
    styleUrls: ['./sidebar-group.component.css'],
    animations: [
        trigger('sidebarAnimationStatus', [
            state('in', style({transform: 'translateX(0)'})),
            transition('void => *', [
                style({transform: 'translateX(100%)'}),
                animate('100ms cubic-bezier(0.86, 0, 0.07, 1)')
            ]),
            transition('* => void', [
                animate('200ms cubic-bezier(0.86, 0, 0.07, 1)', style({
                    opacity: 0,
                    transform: 'translateX(100%)'
                }))
            ])
        ])
    ]
})
export class SidebarGroupComponent implements OnInit, OnDestroy {
    // ngRedux sidebarSubscription
    sidebarSubscription;
    groupSidebarState: any;
    sidebarAnimationStatus: string;
    selectedGroup: any;

    subscription: Subscription;

    constructor(private $ngRedux: NgRedux<IWineryState>,
                private actions: WineryActions,
                private backendService: BackendService) {

    }


    /**
     * Closes the sidebar.
     */
    closeSidebar() {
        this.$ngRedux.dispatch(this.actions.openGroupSidebar({
            groupSidebarContents: {
                sidebarVisible: false,
                nodeClicked: false,
                id: '',
                nameTextFieldValue: '',
                type: ''
            }
        }));
    }

    setGroupSelected(group:any) {
        this.selectedGroup = group;
        console.log("SelectedGroup:");
        console.log(this.selectedGroup);
    }

    /**
     * Angular lifecycle event.
     * initializes the sidebar with the correct data, also implements debounce time for a smooth user experience
     */
    ngOnInit() {
        this.sidebarSubscription = this.$ngRedux.select(wineryState => wineryState.wineryState.groupSidebarContents)
            .subscribe(sidebarContents => {
                    this.groupSidebarState = sidebarContents;
                }
            );
    }

    ngOnDestroy() {
        this.subscription.unsubscribe();
    }
}
