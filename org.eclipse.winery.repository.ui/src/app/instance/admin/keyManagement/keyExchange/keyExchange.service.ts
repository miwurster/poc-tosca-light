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
import { HttpClient } from '@angular/common/http';
import { backendBaseURL } from '../../../../configuration';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { KeyAssignmentData } from '../keystoreEntity/keystoreEntity.component';
import { CertificateEntity, KeyEntity } from '../keystoreEntity/keystoreEntity.service';

@Injectable()
export class KeyExchangeService {
    private path: string;
    convertStringToInteger = map((theNumber: string) => parseInt(theNumber, 10));

    constructor(private http: HttpClient) {
        this.path = backendBaseURL + '/admin/keyexchange';
    }

    updateListOfKeysGivenToMe(): Observable<UpdatePermissionsResult> {
        return this.http.put<UpdatePermissionsResult>(this.path, null);
    }

    getKeyAssignments(): Observable<KeyAssignmentData[]> {
        return this.http.get<KeyAssignmentData[]>(this.path);
    }

    getAllowedPublicKeyAlgorithmNames(): string[] {
        return ['EC'];
    }

    getMyPublicKeyAlias(): Observable<string> {
        const url = `${this.path}/publickey`;
        return this.http.get(url, { responseType: 'text' });
    }

    setOfficialKeyPairAlias(alias: string): Observable<void> {
        const url = `${this.path}/publickey`;
        const formData: FormData = new FormData();
        formData.append('alias', alias);

        return this.http.put<void>(url, formData);
    }

    givePermission(address: string, alias: string): Observable<void> {
        const formData: FormData = new FormData();
        formData.append('address', address);
        formData.append('alias', alias);

        return this.http.post<void>(this.path, formData);
    }

}

export class UpdatePermissionsResult {
    addedKeysCount: number;
    badGiversCount: number;
}
