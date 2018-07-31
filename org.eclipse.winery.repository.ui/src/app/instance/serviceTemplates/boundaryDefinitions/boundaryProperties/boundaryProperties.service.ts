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

import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { backendBaseURL } from '../../../../configuration';
import { Router } from '@angular/router';
import { KeyValueItem } from '../../../../model/keyValueItem';
import { Observable } from 'rxjs/Rx';

@Injectable()
export class BoundaryPropertiesService {

    private path: string;

    constructor(private http: HttpClient,
                private route: Router) {
        this.path = backendBaseURL + this.route.url + '/';
    }

    getBoundaryDefinitionsProperties(): Observable<KeyValueItem[]> {
        const headers = new HttpHeaders({ 'Accept': 'application/json' });

        return this.http.get<KeyValueItem[]>(this.path, { headers: headers });
    }

    addBoundaryProperty(kv: KeyValueItem) {
        const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
        return this.http
            .post(
                this.path,
                kv,
                { headers: headers, observe: 'response', responseType: 'text' }
            );
    }

    editBoundaryProperty(currentSelectedItem: KeyValueItem) {
        const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
        const kvPath = this.path.replace('properties/', 'properties/' + currentSelectedItem.key);
        return this.http
            .put(
                kvPath,
                currentSelectedItem,
                { headers: headers, observe: 'response', responseType: 'text' }
            );
    }

    removeBoundaryProperty(kv: KeyValueItem) {
        const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
        const kvPath = this.path.replace('properties/', 'properties/' + kv.key);
        return this.http
            .delete(
                kvPath,
                { headers: headers, observe: 'response', responseType: 'text' }
            );
    }

    removeBoundaryPropertyMapping(kv: KeyValueItem) {
        const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
        const kvPath = this.path.replace('properties/', 'propertymappings/' + kv.key);
        return this.http
            .delete(kvPath, { headers: headers, observe: 'response', responseType: 'text' });
    }
}
