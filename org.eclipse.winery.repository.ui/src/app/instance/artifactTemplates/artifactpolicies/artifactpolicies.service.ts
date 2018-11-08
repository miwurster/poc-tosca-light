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
import { Observable } from 'rxjs/index';
import { InstanceService } from '../../instance.service';
import { HttpClient } from '@angular/common/http';
import { backendBaseURL } from '../../../configuration';

@Injectable()
export class ArtifactPoliciesService {
    private path: string;

    constructor(private http: HttpClient,
                private sharedData: InstanceService) {
        this.path = backendBaseURL + this.sharedData.path;
    }

    getPolicies(): Observable<ModeledPolicies> {
        return this.http.get<ModeledPolicies>(this.path + '/policies', {});
    }

    decryptContents(): Observable<Object> {
        return this.http.post(this.path + '/decrypt', {});
    }
}

export interface QnameApiData {
    localname: string;
    shortLocalname: string;
    namespace: string;
}

export interface PolicyApiData {
    name: string;
    shortName: string;
    type: QnameApiData;
    template: QnameApiData;
}

export interface SecurityPolicyApiData extends PolicyApiData {
    isApplied: boolean;
    isAppliedIcon: string;
}

export interface ModeledPolicies {
    policies: PolicyApiData[];
    securityPolicies: SecurityPolicyApiData[];
}
