/********************************************************************************
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
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

import {
    Component,
    EventEmitter,
    Input,
    OnChanges, OnDestroy,
    OnInit,
    Output,
    SimpleChanges
} from '@angular/core';
import { EntityTypesModel } from '../../models/entityTypesModel';
import { TNodeTemplate } from '../../models/ttopology-template';
import { NgRedux } from '@angular-redux/store';
import { IWineryState } from '../../redux/store/winery.store';
import { Subscription } from 'rxjs/Subscription';
import { RequirementModel } from '../../models/requirementModel';
import { CapabilityModel } from '../../models/capabilityModel';

@Component({
    selector: 'winery-requirements',
    templateUrl: './requirements.component.html',
    styleUrls: ['./requirements.component.css']
})
/**
 * This Handles Information about the nodes requirements
 */
export class RequirementsComponent implements OnInit, OnChanges, OnDestroy {
    @Output() toggleModalHandler: EventEmitter<any>;
    @Input() currentNodeData: any;
    requirements: any[] = [];
    requirementsExist: boolean;
    entityTypes: EntityTypesModel;
    nodeTemplate: TNodeTemplate;
    tblRowClicked: boolean;
    currentTableRowIndex: number;
    subscription: Subscription;
    currentReqId: string;
    currentRequirement: RequirementModel;

    constructor(private ngRedux: NgRedux<IWineryState>) {
        this.toggleModalHandler = new EventEmitter();
        this.subscription = this.ngRedux.select(state => state.wineryState.currentJsonTopology.nodeTemplates)
            .subscribe(currentNodes => this.updateReqs());
    }

    /**
     * Gets called if nodes representation in the store changes
     */
    updateReqs(): void {
        if (this.currentNodeData) {
            if (this.currentNodeData.nodeTemplate.requirements) {
                this.requirements = this.currentNodeData.nodeTemplate.requirements.requirement;
                this.requirementsExist = true;
            }
        }
    }

    /**
     * Angular lifecycle event.
     */
    ngOnChanges(changes: SimpleChanges) {
        if (changes.currentNodeData.currentValue.entityTypes) {
            this.entityTypes = changes.currentNodeData.currentValue.entityTypes;
            this.nodeTemplate = changes.currentNodeData.currentValue.nodeTemplate;
        }
    }

    /**
     * Triggered upon clicking on a table row, needed for setting the requirement type of that row
     * which gets passed to the properties content component, for showing the correct component type
     * under all the requirements
     * @param $event
     */
    public checkForProperties($event) {
        this.tblRowClicked = false;
        this.currentRequirement = null;
        if ($event.srcElement.previousElementSibling) {
            if ($event.srcElement.previousElementSibling.previousElementSibling) {
                this.currentReqId = $event.srcElement.previousElementSibling.previousElementSibling.textContent;
            } else {
                this.currentReqId = $event.srcElement.previousElementSibling.textContent;
            }
        } else {
            this.currentReqId = $event.srcElement.textContent;
        }
        this.requirements.some(req => {
            if (req.id === this.currentReqId) {
                this.currentRequirement = req;
                return true;
            }
        });
        this.tblRowClicked = true;
    }

    /**
     * Triggered upon clicking on a table row, needed for setting the index of the clicked table row
     * for the xml property value update if yet undefined
     * @param $event
     */
    public saveIndex(tblRowIndex: number) {
        this.currentTableRowIndex = tblRowIndex;
    }

    /**
     * Propagates the click event to node.component, where requirements modal gets opened.
     * @param $event
     */
    public toggleModal($event) {
        if ($event.srcElement.innerText === 'Add new') {
            this.currentNodeData.currentRequirement = null;
        } else {
            this.currentNodeData.currentRequirement = this.currentRequirement;
        }
        this.toggleModalHandler.emit(this.currentNodeData);
    }

    ngOnInit() {
    }

    /**
     * Lifecycle event
     */
    ngOnDestroy() {
        this.subscription.unsubscribe();
    }

}
