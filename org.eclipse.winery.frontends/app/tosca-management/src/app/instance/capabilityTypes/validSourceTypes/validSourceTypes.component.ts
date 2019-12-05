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

import { Component, OnInit, ViewChild } from '@angular/core';
import { ValidSourceTypesService } from './validSourceTypes.service';
import { WineryTableColumn } from '../../../wineryTableModule/wineryTable.component';
import { InstanceService } from '../../instance.service';
import { WineryNotificationService } from '../../../wineryNotificationModule/wineryNotification.service';
import { SelectData } from '../../../model/selectData';
import { SelectItem } from 'ng2-select';
import { ValidSourceTypesApiData } from './validSourceTypesApiData';
import { HttpErrorResponse } from '@angular/common/http';
import { BsModalRef, BsModalService, ModalDirective } from 'ngx-bootstrap';
import { QName } from '../../../model/qName';

@Component({
    templateUrl: 'validSourceTypes.component.html',
    styleUrls: [
        'validSourceTypes.component.css'
    ],
    providers: [
        ValidSourceTypesService
    ]
})
export class ValidSourceTypesComponent implements OnInit {
    enableOpenNodeTypeButton: boolean;
    loading: boolean;
    nodeTypes: SelectData[];
    initialActiveItem: Array<SelectData>;
    currentSelectedItem: QName;
    validSourceTypes: ValidSourceTypesApiData = new ValidSourceTypesApiData();
    @ViewChild('addModal') addModal: ModalDirective;
    addModalRef: BsModalRef;
    columns: Array<WineryTableColumn> = [
        { title: 'Name', name: 'localPart', sort: true },
        { title: 'Namespace', name: 'namespace', sort: true }
    ];

    constructor(public sharedData: InstanceService,
                private service: ValidSourceTypesService,
                private notify: WineryNotificationService,
                private modalService: BsModalService) {
    }

    ngOnInit(): void {
        this.service
            .getAvailableValidSourceTypes()
            .subscribe(
                data => this.handleNodeTypesData(data),
                error => this.handleError(error)
            )
        ;
    }

    saveToServer() {

    }

    onAddValidSourceType() {
        this.validSourceTypes.nodes.push(this.currentSelectedItem);
    }

    onAddClick() {
        this.addModalRef = this.modalService.show(this.addModal);
    }

    onSelectedValueChanged(value: SelectItem) {
        if (value.id !== null && value.id !== undefined) {
            this.enableOpenNodeTypeButton = true;
            this.currentSelectedItem = QName.stringToQName(value.id);
        } else {
            this.enableOpenNodeTypeButton = false;
            this.currentSelectedItem = null;
        }
    }

    handleNodeTypesData(nodeTypes: SelectData[]) {
        this.nodeTypes = nodeTypes;

        if (nodeTypes !== null && nodeTypes !== undefined && nodeTypes.length > 0 && nodeTypes[0].children.length > 0) {
            this.initialActiveItem = [nodeTypes[0].children[0]];
        }
    }

    private handleError(error: HttpErrorResponse): void {
        this.loading = false;
        this.notify.error(error.message, 'Error');
    }

}
