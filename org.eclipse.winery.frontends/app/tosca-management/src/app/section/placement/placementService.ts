/********************************************************************************
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
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { backendBaseURL } from '../../configuration';
import { HttpClient, HttpHeaders, HttpResponse } from '@angular/common/http';

@Injectable()
export class PlacementService {

    private readonly headers = new HttpHeaders({ 'Accept': 'application/xml', 'Content-Type': 'application/xml' });
    private readonly path: string;

    constructor(private http: HttpClient) {
        this.path = backendBaseURL + '/dataflowmodels/';
    }

    createTemplateFromDataFlow(dataFlow: String): Observable<HttpResponse<string>> {
        return this.http.post(this.path, dataFlow, { headers: this.headers, observe: 'response', responseType: 'text' });
    }
}

