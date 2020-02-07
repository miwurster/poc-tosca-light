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
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { backendBaseURL } from '../../../configuration';
import { HttpClient, HttpHeaders, HttpResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { Parameter } from '../../../model/parameters';

@Injectable()
export class ParametersService {

    private readonly path: string;

    constructor(private http: HttpClient, private route: Router) {
        this.path = backendBaseURL + this.route.url + '/';
    }

    public getInputParameters(): Observable<Parameter[]> {
        return this.getJson(this.path + '/inputs');
    }

    public updateInputParameters(data: Parameter[]): void {
        this.putJson(this.path + '/inputs', data)
            .subscribe(() => {
            }, error => console.log(error));
    }

    public getOutputParameters(): Observable<Parameter[]> {
        return this.getJson(this.path + '/outputs');
    }

    public updateOutputParameters(data: Parameter[]): void {
        this.putJson(this.path + '/outputs', data)
            .subscribe(() => {
            }, error => console.log(error));
    }

    private getJson<T>(path: string): Observable<T> {
        return this.http.get<T>(path);
    }

    private putJson<T>(path: string, data: T[]): Observable<HttpResponse<string>> {
        const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
        return this.http.put(path, data, {
            headers: headers,
            observe: 'response',
            responseType: 'text'
        });
    }
}