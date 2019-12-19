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
    templateUrl: 'serviceCompositionDeployment.component.html',
})
export class ServiceCompositionDeploymentComponent implements OnInit {

    private readonly path: string;
    private readonly header = new HttpHeaders({ 'Content-Type': 'application/json' });

    constructor(public sharedData: InstanceService, private notify: WineryNotificationService
                , private route: Router, private http: HttpClient) {
        this.path = backendBaseURL + this.route.url + '/';
    }

    ngOnInit() { }

    deployServiceComposition() {
        // TODO
        console.log(this.path);

        this.http.post(this.path, null, { headers: this.header, observe: 'response', responseType: 'text' })
            .subscribe(
                data => this.notify.success('Deployment is in progress!'),
                error => this.notify.error('Deployment failed with error: ' + error.error)
            );
    }
}
