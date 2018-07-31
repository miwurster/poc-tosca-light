/********************************************************************************
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
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
import { WineryNotificationService } from '../../../../wineryNotificationModule/wineryNotification.service';
import { InstanceService } from '../../../instance.service';
import { BoundaryPropertiesService } from './boundaryProperties.service';
import { HttpErrorResponse } from '@angular/common/http';
import { KeyValueItem } from '../../../../model/keyValueItem';
import { WineryRowData, WineryTableColumn } from '../../../../wineryTableModule/wineryTable.component';
import { Property } from '../propertyMappings/propertyMappings.service';
import { ModalDirective } from 'ngx-bootstrap';
import { NgForm } from '@angular/forms';

@Component({
    selector: 'winery-instance-boundary-kv-properties',
    templateUrl: 'boundaryProperties.component.html',
    providers: [BoundaryPropertiesService]
})

export class BoundaryPropertiesComponent implements OnInit {

    loading = true;
    properties: KeyValueItem[] = [];
    columns: WineryTableColumn[] = [
        { title: 'Property', name: 'key', sort: true },
        { title: 'Value', name: 'value', sort: true }
    ];
    @ViewChild('addPropertyModal') addPropertyModal: ModalDirective;
    @ViewChild('boundaryPropertyForm') boundaryPropertyForm: NgForm;
    @ViewChild('confirmDeleteModal') confirmDeleteModal: ModalDirective;
    addOrUpdate = 'Add';
    currentSelectedItem: KeyValueItem = { "key": '', "value": '' };

    constructor(private service: BoundaryPropertiesService,
                private notify: WineryNotificationService,
                private instanceService: InstanceService) {
    }

    ngOnInit(): void {
        this.getProperties();
    }

    private getProperties() {
        this.service.getBoundaryDefinitionsProperties().subscribe(
            data => this.handleProperties(data),
            error => this.handleError(error)
        );
    }

    addProperty(addOrUpdate: string) {
        if (addOrUpdate === 'Add') {
            this.service.addBoundaryProperty(this.currentSelectedItem)
                .subscribe(
                    data => this.handleSuccess('Added new boundary defintions property'),
                    error => this.handleError(error)
                );
        }
        else {
            this.service.editBoundaryProperty(this.currentSelectedItem)
                .subscribe(
                    data => this.handleSuccess('Added new boundary defintions property'),
                    error => this.handleError(error)
                );
        }
        this.addPropertyModal.hide();
    }

    onCellSelected(selectedItem: WineryRowData) {
        let kv: KeyValueItem = { key: selectedItem.row.key, value: selectedItem.row.value };
        this.currentSelectedItem = kv;
    }

    onRemoveClick(elementToRemove: Property) {
        if (elementToRemove && this.currentSelectedItem) {
            this.confirmDeleteModal.show();
        } else {
            this.notify.warning('No property was selected!');
        }
    }

    onAddClick() {
        this.addOrUpdate = 'Add';
        this.currentSelectedItem = { "key": '', "value": '' };
        this.boundaryPropertyForm.reset();
        this.addPropertyModal.show();
    }

    onEditClick() {
        if (this.currentSelectedItem) {
            this.addOrUpdate = 'Update';
            this.addPropertyModal.show();
        } else {
            this.notify.warning('No property is selected');
        }
    }

    removeConfirmed() {
        this.service.removeBoundaryProperty(this.currentSelectedItem).subscribe(
            data => {
                this.handleSuccess('Deleted boundary property');
                this.service.removeBoundaryPropertyMapping(this.currentSelectedItem).subscribe(
                    data => this.handleSuccess('Deleted corresponding boundary property mapping'),
                    error => {
                        if (error.status !== 404) {
                            this.handleError(error)
                        }
                    }
                );
            },
            error => this.handleError(error)
        );
    }

    handleProperties(data: KeyValueItem[]) {
        this.properties = data;
        this.loading = false;
    }

    handleError(error: HttpErrorResponse) {
        this.notify.error(error.message);
    }

    handleSuccess(message: string) {
        this.getProperties();
        this.notify.success(message);
    }
}
