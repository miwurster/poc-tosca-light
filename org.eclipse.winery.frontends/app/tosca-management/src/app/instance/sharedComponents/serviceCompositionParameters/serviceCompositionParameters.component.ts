/*******************************************************************************
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
 *******************************************************************************/
import { Component, OnInit } from '@angular/core';
import { InstanceService } from '../../instance.service';
import { WineryNotificationService } from '../../../wineryNotificationModule/wineryNotification.service';
import { InterfaceParameter } from '../../../model/parameters';
import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';
import { backendBaseURL } from '../../../configuration';
import { ParametersData } from './parametersData';

@Component({
    templateUrl: 'serviceCompositionParameters.component.html',
})
export class ServiceCompositionParametersComponent implements OnInit {

    loading = false;

    parametersData: ParametersData;
    inputParameters: InterfaceParameter[];
    outputParameters: InterfaceParameter[];

    private readonly path: string;
    private readonly header = new HttpHeaders({ 'Content-Type': 'application/json' });

    constructor(public sharedData: InstanceService, private notify: WineryNotificationService
                , private route: Router, private http: HttpClient) {
        this.path = backendBaseURL + this.route.url + '/';
    }

    ngOnInit() {
        this.inputParameters = [];
        this.outputParameters = [];

        this.http.get<ParametersData>(this.path).subscribe(
            data => this.handleParametersApiData(data),
            error => this.handleError(error)
        );
    }

    save() {
        this.loading = true;
        this.parametersData = new ParametersData(this.inputParameters, this.outputParameters);

        this.http.post(
                this.path,
                this.parametersData,
                { headers: this.header, observe: 'response', responseType: 'text' }
                ).subscribe(
            data => this.handleSave(),
            error => this.handleError(error)
        );
    }

    private handleSave() {
        this.loading = false;
        this.notify.success('Changes saved!');
    }

    private handleError(error: HttpErrorResponse) {
        this.loading = false;
        this.notify.error(error.error);
    }

    private handleParametersApiData(data: ParametersData) {
        if (data.inputParameters != null) {
            this.inputParameters = data.inputParameters;
        }
        if (data.outputParameters != null) {
            this.outputParameters = data.outputParameters;
        }
    }
}
