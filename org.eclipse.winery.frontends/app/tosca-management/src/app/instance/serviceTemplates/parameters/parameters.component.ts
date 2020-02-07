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
import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ParametersService } from './parameters.service';
import { Parameter } from '../../../model/parameters';
import { InstanceService } from '../../instance.service';
import { ModalDirective } from 'ngx-bootstrap';
import { WineryTableColumn } from '../../../wineryTableModule/wineryTable.component';
import { WineryValidatorObject } from '../../../wineryValidators/wineryDuplicateValidator.directive';

export enum ParameterMode {
    INPUT,
    OUTPUT,
}

@Component({
    templateUrl: 'parameters.component.html',
    providers: [ParametersService]
})
export class ParametersComponent implements OnInit {

    mode: ParameterMode;
    modeType = ParameterMode;

    inputParameters: Parameter[] = [];
    outputParameters: Parameter[] = [];

    columnsInputParameters: Array<WineryTableColumn> = [
        { title: 'Name', name: 'key', sort: true },
        { title: 'Type', name: 'type', sort: false },
        { title: 'Required', name: 'required', sort: false },
        { title: 'Default Value', name: 'defaultValue', sort: false },
        { title: 'Description', name: 'description', sort: false },
    ];
    columnsOutputParameters: Array<WineryTableColumn> = [
        { title: 'Name', name: 'key', sort: true },
        { title: 'Type', name: 'type', sort: false },
        { title: 'Required', name: 'required', sort: false },
        { title: 'Value', name: 'value', sort: false },
        { title: 'Description', name: 'description', sort: false },
    ];

    @ViewChild('parameterModal')
    parameterModal: ModalDirective;
    @ViewChild('confirmRemoveModal')
    confirmDeleteModal: ModalDirective;
    parameterModalTitle: string;
    validatorObject: WineryValidatorObject;
    @ViewChild('nameInput') nameInput: ElementRef;

    param: Parameter = new Parameter();
    selectedParam: Parameter;

    loading = false;

    constructor(private parametersService: ParametersService, public instanceService: InstanceService) {
    }

    ngOnInit() {
        this.parametersService.getInputParameters()
            .subscribe(
                data => data.forEach(item => this.inputParameters.push(Object.assign(new Parameter(), item))),
                error => console.error(error)
            );
        this.parametersService.getOutputParameters()
            .subscribe(
                data => data.forEach(item => this.outputParameters.push(Object.assign(new Parameter(), item))),
                error => console.log(error)
            );
    }

    openParameterModal(mode: ParameterMode) {
        this.mode = mode;
        this.param = new Parameter();
        if (mode === ParameterMode.INPUT) {
            this.validatorObject = new WineryValidatorObject(this.inputParameters, 'key');
            this.parameterModalTitle = 'Add Input Parameter';
        } else {
            this.validatorObject = new WineryValidatorObject(this.outputParameters, 'key');
            this.parameterModalTitle = 'Add Output Parameter';
        }
        this.parameterModal.show();
    }

    openConfirmRemoveModal(param: Parameter, mode: ParameterMode) {
        this.mode = mode;
        if (param === null || param === undefined) {
            return;
        }
        this.selectedParam = param;
        this.confirmDeleteModal.show();
    }

    onModalShown() {
        this.nameInput.nativeElement.focus();
    }

    addParameter(param: Parameter) {
        const o = Object.assign(new Parameter(), param);
        if (this.mode === ParameterMode.INPUT) {
            this.inputParameters.push(o);
        }
        if (this.mode === ParameterMode.OUTPUT) {
            this.outputParameters.push(o);
        }
        this.mode = null;
    }

    removeParameter() {
        let arr: Parameter[] = [];
        if (this.mode === ParameterMode.INPUT) {
            arr = this.inputParameters;
        }
        if (this.mode === ParameterMode.OUTPUT) {
            arr = this.outputParameters;
        }
        for (let i = 0; i < arr.length; i++) {
            if (arr[i].key === this.selectedParam.key) {
                arr.splice(i, 1);
            }
        }
        this.confirmDeleteModal.hide();
        this.selectedParam = null;
        this.mode = null;
    }

    save(mode: ParameterMode) {
        this.loading = true;
        if (mode === ParameterMode.INPUT) {
            this.parametersService.updateInputParameters(this.inputParameters)
                .subscribe(
                    () => this.loading = false,
                    error => console.log(error)
                );
        }
        if (mode === ParameterMode.OUTPUT) {
            this.parametersService.updateOutputParameters(this.outputParameters)
                .subscribe(
                    () => this.loading = false,
                    error => console.log(error)
                );
        }
    }
}
