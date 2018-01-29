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
    OnChanges,
    OnInit,
    Output,
    SimpleChanges
} from '@angular/core';
import { EntityTypesModel } from '../../models/entityTypesModel';
import { TNodeTemplate } from '../../models/ttopology-template';
import { Subject } from 'rxjs/Subject';

@Component({
    selector: 'winery-capabilities',
    templateUrl: './capabilities.component.html',
    styleUrls: ['./capabilities.component.css']
})
/**
 * This Handles Information about the node capabilities
 */
export class CapabilitiesComponent implements OnInit, OnChanges {
    @Output() toggleModalHandler: EventEmitter<any>;
    @Input() currentNodeData: any;
    capabilities: any[] = [];
    capabilitiesExist: boolean;
    entityTypes: EntityTypesModel;
    nodeTemplate: TNodeTemplate;
    tblRowClicked: boolean;
    currentTableRowIndex: number;

    constructor() {
        this.toggleModalHandler = new EventEmitter();
    }

    /**
     * Angular lifecycle event.
     */
    ngOnChanges(changes: SimpleChanges) {
        if (changes.currentNodeData.currentValue.nodeTemplate) {
            this.nodeTemplate = changes.currentNodeData.currentValue.nodeTemplate;
        }
        if (changes.currentNodeData.currentValue.nodeTemplate.capabilities) {
            this.capabilities = changes.currentNodeData.currentValue.nodeTemplate.capabilities.capability;
            this.capabilitiesExist = true;
        }
        if (changes.currentNodeData.currentValue.entityTypes) {
            this.entityTypes = changes.currentNodeData.currentValue.entityTypes;
        }
    }

    /**
     * Triggered upon clicking on a table row, needed for setting the capability type of that row
     * which gets passed to the properties content component, for showing the correct component type
     * under all the capabilities
     * @param $event
     */
    public checkForProperties($event) {
        this.tblRowClicked = false;
        setTimeout(() => {
            if ($event.srcElement.nextElementSibling) {
                if ($event.srcElement.nextElementSibling.nextElementSibling) {
                    this.currentNodeData.currentCapType = $event.srcElement.nextElementSibling.nextElementSibling.textContent;
                } else {
                    this.currentNodeData.currentCapType = $event.srcElement.nextElementSibling.textContent;
                }
            } else {
                this.currentNodeData.currentCapType = $event.srcElement.textContent;
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
     * Propagates the click event to node.component, where capabilities modal gets opened.
     * @param $event
     */
    public toggleModal($event) {
        this.toggleModalHandler.emit(this.currentNodeData);
    }

    ngOnInit() {
    }
}
