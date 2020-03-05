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

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ServiceTemplates } from '../serviceTemplates';
import { PageParameter } from '../../../../workflowmodeler/src/app/model/page-parameter';
import { Node } from '../../../../workflowmodeler/src/app/model/workflow/node';
import { map } from 'rxjs/operators';

@Injectable({
    providedIn: 'root',
})
export class HttpServiceTemplates {
    private repositoryURL: string;
    private namespace: string;
    private serviceTemplateId: string;
    private plan: string;
    constructor(private http: HttpClient) {}
    getServiceTemplates(): Observable<ServiceTemplates> {
        return this.http.get<ServiceTemplates>('http://localhost:8080/winery/servicecompositions/servicetemplates');
    }
    public setRequestParam(queryParams: PageParameter) {
        this.repositoryURL = queryParams.repositoryURL;
        this.namespace = queryParams.namespace;
        this.serviceTemplateId = queryParams.id;
        this.plan = queryParams.plan;

        if (this.repositoryURL) {
            this.loadPlan();
        }
    }
    private decode(param: string): string {
        return decodeURIComponent(decodeURIComponent(param));
    }

    private encode(param: string): string {
        return encodeURIComponent(encodeURIComponent(param));
    }
    private getFullUrl(relativePath: string) {
        return this.repositoryURL + relativePath;
    }


    getTest() {
        return this.http.get('http://localhost:8080/winery/servicecompositions/servicetemplates');
    }

    public loadPlan() {
        const url = 'servicetemplates/' + this.encode(this.namespace)   + '/' + this.encode(this.serviceTemplateId);
        return this.http.get(this.getFullUrl(url));
    }
}
