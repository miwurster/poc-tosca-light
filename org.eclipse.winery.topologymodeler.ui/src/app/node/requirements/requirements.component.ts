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

import { Component,
    EventEmitter,
    Input,
    OnChanges,
    OnInit,
    Output,
    SimpleChanges
} from '@angular/core';
import { EntityTypesModel } from '../../models/entityTypesModel';
import { TNodeTemplate } from '../../models/ttopology-template';

@Component({
    selector: 'winery-requirements',
    templateUrl: './requirements.component.html',
    styleUrls: ['./requirements.component.css']
})
/**
 * This Handles Information about the nodes requirements
 */
export class RequirementsComponent implements OnInit, OnChanges {
    @Output() toggleModalHandler: EventEmitter<any>;
    @Input() currentNodeData: any;
    requirements: any[] = [];
    requirementsExist: boolean;
    entityTypes: EntityTypesModel;
    nodeTemplate: TNodeTemplate;
    tblRowClicked: boolean;
    currentTableRowIndex: number;

    constructor () {
        this.toggleModalHandler = new EventEmitter();
    }

    /**
     * Angular lifecycle event.
     */
    ngOnChanges (changes: SimpleChanges) {
        console.log(changes);
        if (changes.currentNodeData.currentValue.nodeTemplate) {
            this.nodeTemplate = changes.currentNodeData.currentValue.nodeTemplate;
            console.log(this.nodeTemplate);
            if (this.nodeTemplate.requirements) {
                this.requirements = this.nodeTemplate.requirements.requirement;
                console.log(this.requirements);
            }
        }
        if (changes.currentNodeData.currentValue.nodeTemplate.requirements) {
            this.requirements = changes.currentNodeData.currentValue.nodeTemplate.requirements.requirement;
            this.requirementsExist = true;
            console.log(this.requirements);
        }
        if (changes.currentNodeData.currentValue.entityTypes) {
            this.entityTypes = changes.currentNodeData.currentValue.entityTypes;
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
        setTimeout(() => {
            if ($event.srcElement.nextElementSibling) {
                if ($event.srcElement.nextElementSibling.nextElementSibling) {
                    this.currentNodeData.currentReqType = $event.srcElement.nextElementSibling.nextElementSibling.textContent;
                } else {
                    this.currentNodeData.currentReqType = $event.srcElement.nextElementSibling.textContent;
                }
            } else {
                this.currentNodeData.currentReqType = $event.srcElement.textContent;
            }
            this.tblRowClicked = true;
        }, 1);
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
    public toggleModal ($event) {
        this.toggleModalHandler.emit(this.currentNodeData);
    }

    ngOnInit () {
    }

}
