/*******************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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
import { InstanceService } from '../../instance.service';
import { Interface, Operation } from '../../../model/interfaces';
import { ModalDirective } from 'ngx-bootstrap';
import { WineryValidatorObject } from '../../../wineryValidators/wineryDuplicateValidator.directive';
import { SelectableListComponent } from '../interfaces/selectableList/selectableList.component';
import { InterfaceDefinitionsService } from './interfaceDefinitions.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
    selector: 'winery-interfaces',
    templateUrl: 'interfaceDefinitions.component.html',
})
export class InterfaceDefinitionsComponent implements OnInit {

    loading = false;

    interfaces: Interface[] = [];
    selectedInterface: Interface;
    selectedOperation: Operation;

    validatorObject: WineryValidatorObject;
    @ViewChild('addModal') addModal: ModalDirective;
    @ViewChild('removeModal') removeModal: ModalDirective;
    modalTitle: string;
    removeModalElement: string;

    @ViewChild('interfacesList') interfacesListComponent: SelectableListComponent;
    @ViewChild('operationsList') operationsListComponent: SelectableListComponent;

    constructor(private interfaceService: InterfaceDefinitionsService, public instanceService: InstanceService) {
    }

    ngOnInit() {
        this.loading = true;
        this.interfaceService.getInterfaces()
            .subscribe(
                data => {
                    this.interfaces = [];
                    data.forEach(item => this.interfaces.push(Object.assign(new Interface(), item)));
                    this.loading = false;
                },
                error => this.handleError(error)
            );
    }

    private handleError(error: HttpErrorResponse) {
        console.error(error);
        this.loading = false;
    }

    save() {
        this.loading = true;
        this.interfaceService.updateInterfaces(this.interfaces)
            .subscribe(
                () => this.loading = false,
                error => this.handleError(error)
            );
    }

    onAddInterface() {
        this.modalTitle = 'Interface';
        this.validatorObject = new WineryValidatorObject(this.interfaces, 'name');
        this.addModal.show();
    }

    onInterfaceSelected(selectedInterface: Interface) {
        this.selectedInterface = selectedInterface;
    }

    onRemoveInterface() {
        this.modalTitle = 'Interface';
        this.removeModalElement = this.selectedInterface.name;
        this.removeModal.show();
    }

    addInterface(name: string) {
        const int = Object.assign(new Interface(), { name: name });
        this.interfaces.push(int);
        this.interfacesListComponent.selectItem(int);
    }

    removeInterface() {
        for (let i = 0; i < this.interfaces.length; i++) {
            if (this.interfaces[i].name === this.selectedInterface.name) {
                this.interfaces.splice(i, 1);
            }
        }
        this.selectedInterface = null;
    }

    onAddOperation() {
        this.modalTitle = 'Operation';
        this.validatorObject = new WineryValidatorObject(this.selectedInterface.operations, 'name');
        this.addModal.show();
    }

    onOperationSelected(selectedOperation: Operation) {
        this.selectedOperation = selectedOperation;
    }

    onRemoveOperation() {
        this.modalTitle = 'Operation';
        this.removeModalElement = this.selectedOperation.name;
        this.removeModal.show();
    }

    addOperation(name: string) {
        if (this.selectedInterface) {
            const op = Object.assign(new Operation(), { name: name });
            this.selectedInterface.operations.push(op);
            this.operationsListComponent.selectItem(op);
        }
    }

    removeOperation() {
        const arr = this.selectedInterface.operations;
        for (let i = 0; i < arr.length; i++) {
            if (arr[i].name === this.selectedOperation.name) {
                arr.splice(i, 1);
            }
        }
        this.selectedOperation = null;
    }
}
