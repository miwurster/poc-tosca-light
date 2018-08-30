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
import { ModalDirective, ModalOptions } from 'ngx-bootstrap';
import { WineryModalComponent } from '../../repositoryUiDependencies/wineryModalModule/winery.modal.component';

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
    properties: Subject<string> = new Subject<string>();
    sidebarSubscription;
    groupSidebarState: any;
    sidebarAnimationStatus: string;
    selectedGroup: any;
    keyOfEditedKVProperty: Subject<string> = new Subject<string>();
    subscriptions: Array<Subscription> = [];
    selectedNodes: Array<TNodeTemplate>;
    ngRedux: NgRedux<IWineryState>;
    modalSelectedRadioButton = 'kv';
    backend: BackendService;

    groupProperties: any;

    @Input() groupsModalData: GroupsModalData;

    key: string;

    subscription: Subscription;

    @ViewChild('groupNodesModal') groupNodesModal : ModalDirective;

    constructor(private $ngRedux: NgRedux<IWineryState>,
                private actions: WineryActions,
                private backendService: BackendService) {
        this.backend = backendService;
        this.ngRedux = $ngRedux;
    }

    @Input()
    set SelectedNodes(selectedNodes : Array<TNodeTemplate>){
        this.selectedNodes = selectedNodes;
    }

    addNodes(){
        console.log("adding nodes:");
        console.log(this.selectedNodes);

        let cleanedNodes = new Array();

        for(let item in this.selectedNodes) {
            let copiedNode = JSON.parse(JSON.stringify(this.selectedNodes[item]));
            delete copiedNode["visuals"];
            delete copiedNode["_state"];
            cleanedNodes.push(this.selectedNodes[item].id);
        }

        console.log("After cleaning:")
        console.log(cleanedNodes);

        this.selectedGroup["nodeTemplates"] = JSON.parse(JSON.stringify(cleanedNodes));
    }

    deleteGroup(){
        console.log("Starting of delete selected group");
        console.log(this.selectedGroup);
        let groupId = this.selectedGroup.id;
        console.log("Current groups:");
        console.log(this.groupSidebarState.groups);
        console.log("Size: " + this.groupSidebarState.groups.length);
        let removeIndex = -1;
        for(let index = 0; index < this.groupSidebarState.groups.length; index++){
            console.log("Checking group:");
            console.log(this.groupSidebarState.groups[index]);
            if(this.groupSidebarState.groups[index].id == groupId){
                removeIndex = index;
            }
        }
        console.log("Removing group with index:");
        console.log(removeIndex);

        this.groupSidebarState.groups.splice(removeIndex,1);
    }

    addGroup(){
        console.log("Creating group");
        console.log(this.groupsModalData);
        let groupId = this.groupsModalData.id;
        let groupName = this.groupsModalData.name;
        let groupType = this.groupsModalData.type;


        let newGroup ={id: groupId, any: new Array(), documentation: new Array(), otherAttributes: {}, name: groupName, groupType: JSON.parse(groupType)["id"], properties: this.fetchProperties(groupType)}

        console.log(newGroup);

        this.groupSidebarState.groups.push(newGroup);
    }

    fetchProperties(groupType:any){
        console.log("GroupType to find:");
        console.log(groupType);




        console.log("Property def:");
        //.serviceTemplateOrNodeTypeOrNodeTypeImplementation[0].any[0]

        let props = JSON.parse(groupType)["full"]["serviceTemplateOrNodeTypeOrNodeTypeImplementation"][0].any[0];

        let obj:any = {}
        if(props.propertyDefinitionKVList == null) {
            obj.any = props.any;
        } else {
            let newObj = {};
            for(let key in props.propertyDefinitionKVList){
                let propName = props.propertyDefinitionKVList[key];
                newObj[propName.key] = "";
            }

            obj.kvproperties = newObj;
        }
        console.log(obj);
        return obj;
    }

    saveGroups() {
        this.backendService.saveGroups({group: this.groupSidebarState.groups}).subscribe(res => {console.log(res)});
    }

    openCreateGroupModal() {
        console.log("GroupNodesModal:");
        console.log(this.groupNodesModal);
        this.groupsModalData.visible = true;
        console.log("GroupsModalData:");
        console.log(this.groupsModalData);

        this.groupNodesModal.config = {backdrop:false, keyboard:true};
        this.groupNodesModal.show();
    }

    closeCreateGroupModal(): void {
        this.groupsModalData.visible = false;
        this.groupNodesModal.hide();
    }



    setGroupSelected(group:any) {
        this.selectedGroup = group;

        if (this.selectedGroup.properties.kvproperties) {
           this.groupProperties= this.selectedGroup.properties.kvproperties;
        } else {
           this.groupProperties = this.selectedGroup.properties.any;
        }

        console.log("SelectedGroup:");
        console.log(this.selectedGroup);
        console.log("With Properties:")
        console.log(this.groupProperties);
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
                    console.log("Setting kvproperties to groupProperties");
                    console.log("Value:");
                    console.log(value)
                    this.groupProperties[this.key] = value;
                } else {
                    console.log("Setting any properties to groupProperties");
                    console.log("Value:");
                    console.log(value)
                    this.groupProperties = value;
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
