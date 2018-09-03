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
import { Component, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { NgRedux } from '@angular-redux/store';
import { IWineryState } from '../redux/store/winery.store';
import { WineryActions } from '../redux/actions/winery.actions';
import { Subject, Subscription } from 'rxjs';

import { animate, state, style, transition, trigger } from '@angular/animations';

import { BackendService } from '../services/backend.service';
import { GroupsModalData } from '../models/groupsModalData';
import { TNodeTemplate } from '../models/ttopology-template';
import { ModalDirective } from 'ngx-bootstrap';

/**
 * This is the right sidebar, node groups can be managed
 */
@Component({
    selector: 'winery-group-sidebar',
    templateUrl: './sidebar-group.component.html',
    styleUrls: ['./sidebar-group.component.css'],
    animations: [
        trigger('sidebarAnimationStatus', [
            state('in', style({ transform: 'translateX(0)' })),
            transition('void => *', [
                style({ transform: 'translateX(100%)' }),
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
    properties: Subject<string> = new Subject<string>();
    sidebarSubscription;
    groupSidebarState: any;
    sidebarAnimationStatus: string;
    selectedGroup: any;
    keyOfEditedKVProperty: Subject<string> = new Subject<string>();
    subscriptions: Array<Subscription> = [];
    selectedNodes: Array<TNodeTemplate>;
    ngRedux: NgRedux<IWineryState>;
    backend: BackendService;

    groupProperties: any;

    @Input() groupsModalData: GroupsModalData;

    key: string;

    subscription: Subscription;

    @ViewChild('groupNodesModal') groupNodesModal: ModalDirective;

    constructor(private $ngRedux: NgRedux<IWineryState>,
                private actions: WineryActions,
                private backendService: BackendService) {
        this.backend = backendService;
        this.ngRedux = $ngRedux;
    }

    @Input()
    set SelectedNodes(selectedNodes: Array<TNodeTemplate>) {
        this.selectedNodes = selectedNodes;
    }

    addNodes() {
        const cleanedNodes = new Array();

        for (const item in this.selectedNodes) {
            if (item != null) {
                const node = this.selectedNodes[item];
                const jsonNode = JSON.parse(JSON.stringify(node));
                delete jsonNode.visuals;
                delete jsonNode._state;
                delete jsonNode.otherAttributes;
                cleanedNodes.push(jsonNode);
            }
        }

        this.selectedGroup['nodeTemplates'] = JSON.parse(JSON.stringify(cleanedNodes));
    }

    deleteGroup() {
        const groupId = this.selectedGroup.id;
        let removeIndex = -1;
        for (let index = 0; index < this.groupSidebarState.groups.length; index++) {
            if (this.groupSidebarState.groups[index].id === groupId) {
                removeIndex = index;
            }
        }

        this.groupSidebarState.groups.splice(removeIndex, 1);
    }

    addGroup() {
        const groupId = this.groupsModalData.id;
        const groupName = this.groupsModalData.name;
        const groupType = this.groupsModalData.type;

        const newGroup = {
            id: groupId, any: new Array(), documentation: new Array(), otherAttributes: {}, name: groupName,
            type: '{' + JSON.parse(groupType)['namespace'] + '}' + JSON.parse(groupType)['id'],
            properties: this.fetchProperties(groupType)
        };

        this.groupSidebarState.groups.push(newGroup);
    }

    fetchProperties(groupType: any) {
        const props = JSON.parse(groupType)['full']['serviceTemplateOrNodeTypeOrNodeTypeImplementation'][0].any[0];

        if (props === undefined || props === null) {
            return {};
        }

        const obj: any = {};
        if (props.propertyDefinitionKVList == null) {
            obj.any = props.any;
        } else {
            const newObj = {};

            for (const key in props.propertyDefinitionKVList) {
                if (key != null) {
                    const propName = props.propertyDefinitionKVList[key];
                    newObj[propName.key] = '';
                }
            }
            obj.kvproperties = newObj;
        }

        return obj;
    }

    saveGroups() {
        this.backendService.saveGroups({ group: this.groupSidebarState.groups }).subscribe(res => {
        });
    }

    openCreateGroupModal() {
        this.groupsModalData.visible = true;
        this.groupNodesModal.config = { backdrop: false, keyboard: true };
        this.groupNodesModal.show();
    }

    closeCreateGroupModal(): void {
        this.groupsModalData.visible = false;
        this.groupNodesModal.hide();
    }

    setGroupSelected(group: any) {
        this.selectedGroup = group;
        if (this.selectedGroup.properties.kvproperties) {
            this.groupProperties = this.selectedGroup.properties.kvproperties;
        } else {
            this.groupProperties = this.selectedGroup.properties.any;
        }
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

        this.subscriptions.push(this.keyOfEditedKVProperty.pipe(
            debounceTime(200),
            distinctUntilChanged(), )
            .subscribe(key => {
                this.key = key;
            }));

        this.subscriptions.push(this.properties.pipe(
            debounceTime(300),
            distinctUntilChanged(), )
            .subscribe(value => {
                if (this.selectedGroup.properties.kvproperties) {
                    this.groupProperties[this.key] = value;
                } else {
                    this.groupProperties.any = value;
                }
                this.$ngRedux.dispatch(this.actions.setGroup({
                    groupProperty: {
                        group: this.selectedGroup
                    }
                }));
            }));
    }

    ngOnDestroy() {
        this.subscription.unsubscribe();
    }
}
