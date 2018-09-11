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
import { HttpClient } from '@angular/common/http';
import { backendBaseURL } from '../../../../configuration';
import { AddKeypairData, AddSecretKeyData } from './keystoreEntity.component';

@Injectable()
export class KeystoreEntityService {
    private path: string;

    constructor(private http: HttpClient) {
        this.path = backendBaseURL + '/admin/keystore';
    }

    getSecretKeys(): Observable<KeyEntity[]> {
        const keysPath = this.path + '/keys';

        return this.http.get<KeyEntity[]>(keysPath, {});
    }

    getKeyPairs(): Observable<KeyPairEntity[]> {
        const keysPath = this.path + '/keypairs';

        return this.http.get<KeyPairEntity[]>(keysPath, {});
    }

    getCertificates(): Observable<CertificateEntity[]> {
        const keysPath = this.path + '/certificates';

        return this.http.get<CertificateEntity[]>(keysPath, {});
    }

    getSupportedAlgorithms(): Observable<SupportedAlgorithms> {
        const keysPath = this.path + '/algorithms';

        return this.http.get<SupportedAlgorithms>(keysPath, {});
    }

    getPolicyTemplateNamespace() {
        const policyTemplateNSPath = this.path + '/namespaces/secpolicytemplate';

        return this.http.get(policyTemplateNSPath, { responseType: 'text' });
    }

    getSecurityPolicyTemplate(ns: string, alias: string) {
        const policyTemplateNSPath = backendBaseURL + '/policytemplates/' + ns + '/' + alias;

        return this.http.get(policyTemplateNSPath, {});
    }

    generateEncryptionPolicy(alias: string) {
        const policyPath = this.path + '/keys/' + alias + '/encryptionpolicy';

        return this.http.get(policyPath)
            .map((res: Response) => {
                console.log(res);
                return res;
            })
            .flatMap((policyBody) => this.http.put(policyPath, policyBody));
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

    addKeypair(data: AddKeypairData) {
        const keysPath = this.path + '/keypairs';
        // TODO build DN string
        const dn = 'CN=' + data.commonName + ','
            + 'O=' + data.organizationName + ','
            + 'C=' + data.countryName;

        const formData: FormData = new FormData();
        formData.append('algo', data.algorithm);
        formData.append('keySize', data.keySizeInBits);
        formData.append('dn', dn);
        if (data.privateKeyFile  !== null && data.privateKeyFile !== undefined) {
            formData.append('privateKeyFile', data.privateKeyFile, data.privateKeyFile.name);
        }
        if (data.certificateFile  !== null && data.certificateFile !== undefined) {
            formData.append('certificate', data.certificateFile, data.certificateFile.name);
        }

        return this.http.post(keysPath, formData);
    }

    addCertificate(data: File) {
        const keysPath = this.path + '/certificates';

        const formData: FormData = new FormData();
        if (data !== null && data !== undefined) {
            formData.append('certificate', data, data.name);
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

    generateSigningPolicy(alias: string) {
        const policyPath = this.path + '/keypairs/' + alias + '/signingpolicy';

        return this.http.get(policyPath)
            .map((res: Response) => {
                console.log(res);
                return res;
            })
            .flatMap((policyBody) => this.http.put(policyPath, policyBody));
    }
}

export class KeyPairEntity {
    privateKey: KeyEntity;
    publicKey: KeyEntity;
    certificate: CertificateEntity;
}

export class KeyEntity {
    alias: string;
    algorithm: string;
    keyFormat: string;
    keySizeInBits: number;
    base64Key: string;
}

export class CertificateEntity {
    alias: string;
    serialNumber: string;
    sigAlgName: string;
    issuerDN: string;
    subjectDN: string;
    notBefore: string;
    notAfter: string;
    pem: string;
}

export class SupportedAlgorithms {
    symmetric: SupportedAlgorithm[];
    asymmetric: SupportedAlgorithm[];
}

export class SupportedAlgorithm {
    name: string;
    keySizeInBits: number;
}
