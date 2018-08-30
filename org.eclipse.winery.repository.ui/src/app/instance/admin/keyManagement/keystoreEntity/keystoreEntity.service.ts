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
import { HttpClient, HttpParams } from '@angular/common/http';
import { backendBaseURL } from '../../../../configuration';
import { AddSecretKeyData } from './keystoreEntity.component';
import { WineryRowData } from '../../../../wineryTableModule/wineryTable.component';

@Injectable()
export class KeystoreEntityService {
    private path: string;

    constructor(private http: HttpClient) {
        this.path = backendBaseURL + '/admin/keystore';
    }

    getSecretKeys(): Observable<KeyEntity[]> {
        const keysPath = this.path + '/keys';
        let params = new HttpParams();

        return this.http.get<KeyEntity[]>(keysPath, { params: params });
    }

    getKeyPairs(): Observable<KeyPairEntity[]> {
        const keysPath = this.path + '/keypairs';
        let params = new HttpParams();

        return this.http.get<KeyPairEntity[]>(keysPath, { params: params });
    }

    getCertificates(): Observable<CertificateEntity[]> {
        const keysPath = this.path + '/certificates';
        let params = new HttpParams();

        return this.http.get<CertificateEntity[]>(keysPath, { params: params });
    }

    getSupportedAlgorithms(): Observable<SupportedAlgorithms> {
        const keysPath = this.path + '/algorithms';
        let params = new HttpParams();

        return this.http.get<SupportedAlgorithms>(keysPath, { params: params });
    }

    addKey(data: AddSecretKeyData) {
        const keysPath = this.path + '/keys';

        const formData: FormData = new FormData();
        formData.append('algo', data.algorithm);
        formData.append('keySize', data.keySizeInBits);
        if (data.keyFile !== null && data.keyFile !== undefined) {
            formData.append('keystoreFile', data.keyFile, data.keyFile.name);
        }

        return this.http.post(keysPath, formData);
    }

    removeEntity(keystoreEntityType: string, alias: string) {
        let keysPath = this.path;
        switch (keystoreEntityType) {
            case 'secretkeys':
                keysPath += '/keys/' + alias;
                break;
            case 'keypairs':
                keysPath += '/keypairs/' + alias;
                break;
            case 'certificates':
                keysPath += '/certificates/' + alias;
                break;
        }

        return this.http.delete(keysPath);
    }
}

export interface KeyPairEntity {
    privateKey: KeyEntity;
    publicKey: KeyEntity;
    certificate: CertificateEntity;
}

export interface KeyEntity {
    alias: string;
    algorithm: string;
    keySizeInBits: number;
}

export interface CertificateEntity {
    alias: string;
    serialNumber: string;
    sigAlgName: string;
    issuerDN: string;
    subjectDN: string;
    notBefore: string;
    notAfter: string;
}

export interface SupportedAlgorithms {
    symmetric: SupportedAlgorithm[];
    asymmetric: SupportedAlgorithm[];
}

export interface SupportedAlgorithm {
    name: string;
    keySizeInBits: number;
}
