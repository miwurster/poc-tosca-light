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

import { Component, OnInit, ViewChild } from '@angular/core';
import {
    CertificateEntity, KeyEntity, KeyPairEntity, KeystoreEntityService, SupportedAlgorithms
} from './keystoreEntity.service';
import { WineryNotificationService } from '../../../../wineryNotificationModule/wineryNotification.service';
import { ActivatedRoute } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { ModalDirective } from 'ngx-bootstrap';
import { WineryRowData } from '../../../../wineryTableModule/wineryTable.component';
import { SelectItem } from 'ng2-select';

export class KeyPairTableData {
    alias: string;
    algorithm: string;
    publicKeySize: number;
    certificate: boolean;
    originalObject: KeyPairEntity;

    constructor(kp: KeyPairEntity) {
        this.alias = kp.privateKey.alias;
        this.algorithm = kp.privateKey.algorithm;
        this.publicKeySize = kp.publicKey.keySizeInBits;
        this.certificate = kp.certificate !== null;
        this.originalObject = kp;
    }
}

export class AddSecretKeyData {
    algorithm: string;
    keySizeInBits: string;
    keyFile: File;

    constructor() {
        this.algorithm = null;
        this.keySizeInBits = null;
    }
}

export class AddKeypairData {
    algorithm: string;
    keySizeInBits: string;
    commonName: string;
    localityName: string;
    stateOrProvinceName: string;
    organizationalUnitName: string;
    organizationName: string;
    countryName: string;
    privateKeyFile: File;
    certificateFile: File;

    constructor() {
        this.algorithm = null;
        this.keySizeInBits = null;
    }
}

export class KeystoreTableData {
    secretkeys: KeyEntity[];
    keypairs: KeyPairTableData[];
    certificates: CertificateEntity[];
}

@Component({
    selector: 'winery-instance-keystoreentity',
    templateUrl: 'keystoreEntity.component.html',
    styleUrls: ['keystoreEntity.component.css'],
    providers: [KeystoreEntityService]
})
export class KeystoreEntityComponent implements OnInit {

    keystoreEntityType: string;
    loading = true;
    modalLoading = false;
    columns = {
        'secretkeys': [
            { title: 'Alias', name: 'alias' },
            { title: 'Algorithm', name: 'algorithm' },
            { title: 'Size In Bits', name: 'keySizeInBits' }
        ],
        'keypairs': [
            { title: 'Alias', name: 'alias' },
            { title: 'Algorithm', name: 'algorithm' },
            { title: 'Size In Bits', name: 'publicKeySize' },
            { title: 'Certificate', name: 'certificate' }
        ],
        'certificates': [
            { title: 'Alias', name: 'alias' },
            { title: 'Signature Algorithm', name: 'sigAlgName' },
            { title: 'Valid From', name: 'notBefore' },
            { title: 'Valid Until', name: 'notAfter' }
        ]
    };
    data: KeystoreTableData = {
        'secretkeys': [],
        'keypairs': [],
        'certificates': []
    };
    selectedCell: WineryRowData = {
        row: '',
        column: ''
    };
    selectedEntitySecPolicyTemplate: any = undefined;
    supportedAlgorithms: string[] = [];
    secPolicyTemplateNameSpace: string;
    supportedAlgorithmsKeySizesMap: { [key: string]: string[] } = {};
    addKeyData: AddSecretKeyData = new AddSecretKeyData();
    addKeypairData: AddKeypairData = new AddKeypairData();
    addCertificateData: File = undefined;

    @ViewChild('addKeyModal') addKeyModal: ModalDirective;
    @ViewChild('addKeypairModal') addKeypairModal: ModalDirective;
    @ViewChild('addCertificateModal') addCertificateModal: ModalDirective;
    @ViewChild('confirmDeleteModal') confirmDeleteModal: ModalDirective;
    @ViewChild('keyInfoModal') keyInfoModal: ModalDirective;
    @ViewChild('keypairInfoModal') keypairInfoModal: ModalDirective;
    @ViewChild('certificateInfoModal') certificateInfoModal: ModalDirective;

    constructor(private service: KeystoreEntityService,
                private notify: WineryNotificationService,
                public route: ActivatedRoute) {
        this.route.url.subscribe(params => {
            this.keystoreEntityType = params[0].path;
        });
    }

    ngOnInit(): void {
        this.loading = true;
        this.service.getSupportedAlgorithms().subscribe(
            data => this.handleAlgorithmData(data),
            error => this.handleError(error)
        );
        this.service.getPolicyTemplateNamespace().subscribe(
            data => this.secPolicyTemplateNameSpace = data,
            error => this.handleError(error)
        );
        switch (this.keystoreEntityType) {
            case 'secretkeys':
                this.service.getSecretKeys().subscribe(
                    data => this.handleKeystoreData(data),
                    error => this.handleError(error)
                );
                break;
            case 'keypairs':
                this.service.getKeyPairs().subscribe(
                    data => this.handleKeystoreData(data),
                    error => this.handleError(error)
                );
                break;
            case 'certificates':
                this.service.getCertificates().subscribe(
                    data => this.handleKeystoreData(data),
                    error => this.handleError(error)
                );
                break;
        }
    }

    private handleKeystoreData(receivedData: any[]) {
        this.loading = false;
        // console.log(receivedData);
        if (this.keystoreEntityType === 'keypairs') {
            this.data[this.keystoreEntityType] = [];
            for (let i = 0; i < receivedData.length; i++) {
                const kp = new KeyPairTableData(<KeyPairEntity>receivedData[i]);
                this.data[this.keystoreEntityType].push(kp);
            }
        } else {
            this.data[this.keystoreEntityType] = receivedData;
        }
    }

    private handleError(error: HttpErrorResponse) {
        this.loading = false;
        this.modalLoading = false;
        this.notify.error(error.message);
    }

    private handleSuccess(): void {
        this.loading = false;
        this.notify.success('Operation was successful', 'Success');
    }

    onAddClick() {
        switch (this.keystoreEntityType) {
            case 'secretkeys':
                this.addKeyModal.show();
                break;
            case 'keypairs':
                this.addKeypairModal.show();
                break;
            case 'certificates':
                this.addCertificateModal.show();
                break;
        }
    }

    onRemoveClick(event: any) {
        this.confirmDeleteModal.show();
    }

    onInfoClick() {
        if (this.selectedCell) {
            this.modalLoading = true;
            this.service.getSecurityPolicyTemplate(this.secPolicyTemplateNameSpace, this.selectedCell.row.alias).subscribe(
                policyTemplate => this.handlePolicyTemplateData(policyTemplate),
                error => {
                    this.modalLoading = false;
                    if (error.status !== 404) {
                        this.handleError(error);
                    }
                }
            );
            switch (this.keystoreEntityType) {
                case 'secretkeys':
                    this.keyInfoModal.show();
                    break;
                case 'keypairs':
                    this.keypairInfoModal.show();
                    break;
                case 'certificates':
                    this.certificateInfoModal.show();
                    break;
            }
        }
    }

    onCellSelect(data: WineryRowData) {
        if (data) {
            this.selectedCell = data;
        }
    }

    addKey() {
        this.loading = true;
        this.service.addKey(this.addKeyData).subscribe(
            data => {
                this.handleSave();
            },
            error => this.handleError(error)
        );
    }

    addKeypair() {
        this.loading = true;
        this.service.addKeypair(this.addKeypairData).subscribe(
            data => {
                this.handleSave();
            },
            error => this.handleError(error)
        );
    }

    private handleSave() {
        this.handleSuccess();
        this.ngOnInit();
    }

    private handleRemove() {
        this.handleSuccess();
        this.ngOnInit();
    }

    private handleAlgorithmData(data: SupportedAlgorithms) {
        if (this.supportedAlgorithms.length === 0) {
            let dataArray = null;
            let processData = false;
            switch (this.keystoreEntityType) {
                case 'secretkeys':
                    dataArray = data.symmetric;
                    processData = true;
                    break;
                case 'keypairs':
                    dataArray = data.asymmetric;
                    processData = true;
                    break;
            }
            if (processData) {
                const addedAlgorithms: string[] = [];
                for (let i = 0; i < dataArray.length; i++) {
                    const algo = dataArray[i].name;
                    const keySize = dataArray[i].keySizeInBits;
                    if (addedAlgorithms.indexOf(algo) === -1) {
                        addedAlgorithms.push(algo);
                        this.supportedAlgorithms.push(algo);
                    }
                    if (!this.supportedAlgorithmsKeySizesMap.hasOwnProperty(algo) || !this.supportedAlgorithmsKeySizesMap[algo]) {
                        this.supportedAlgorithmsKeySizesMap[algo] = [];
                    }
                    this.supportedAlgorithmsKeySizesMap[algo].push(keySize.toString());
                }
            }
        }
    }

    targetAlgorithmSelected(targetObj: string) {
        console.log(targetObj);
        switch (this.keystoreEntityType) {
            case 'secretkeys':
                this.addKeyData.algorithm = targetObj;
                break;
            case 'keypairs':
                this.addKeypairData.algorithm = targetObj;
                break;
        }
    }

    targetKeySizeSelected(targetObj: SelectItem) {
        switch (this.keystoreEntityType) {
            case 'secretkeys':
                this.addKeyData.keySizeInBits = targetObj.id;
                break;
            case 'keypairs':
                this.addKeypairData.keySizeInBits = targetObj.id;
                break;
        }
    }

    keySelected(event: any) {
        const files = event.target.files;

        if (files.length > 0) {
            this.addKeyData.keyFile = files[0];
        } else {
            this.addKeyData.keyFile = undefined;
        }
    }

    keypairSelected(event: any) {
        const files = event.target.files;

        if (files.length > 0) {
            this.addKeypairData.privateKeyFile = files[0];
        } else {
            this.addKeypairData.privateKeyFile = undefined;
        }
    }

    certificateSelected(event: any) {
        const files = event.target.files;
        switch (this.keystoreEntityType) {
            case 'keypairs':
                if (files.length > 0) {
                    this.addKeypairData.privateKeyFile = files[0];
                } else {
                    this.addKeypairData.privateKeyFile = undefined;
                }
                break;
            case 'certificates':
                if (files.length > 0) {
                    this.addCertificateData = files[0];
                } else {
                    this.addCertificateData = undefined;
                }
                break;
        }
    }

    deleteEntity() {
        this.confirmDeleteModal.hide();
        this.service.removeEntity(this.keystoreEntityType, this.selectedCell.row.alias).subscribe(
            data => this.handleRemove(),
            error => this.handleError(error)
        );
        this.selectedCell = null;
    }

    private handlePolicyTemplateData(policyTemplate: Object) {
        this.modalLoading = false;
        if (policyTemplate) {
            this.selectedEntitySecPolicyTemplate = policyTemplate;
        } else {
            this.selectedEntitySecPolicyTemplate = null;
        }
    }

    generateEncryptionPolicy() {
        this.modalLoading = true;
        this.service.generateEncryptionPolicy(this.selectedCell.row.alias).subscribe(
            res => {
                this.modalLoading = false;
                this.service.getSecurityPolicyTemplate(this.secPolicyTemplateNameSpace, this.selectedCell.row.alias).subscribe(
                    data => this.handlePolicyTemplateData(data),
                    error => {
                        if (error.status !== 404) {
                            this.handleError(error);
                        }
                    }
                );
            },
            error => this.handleError(error)
        );
    }

    closeInfoModal() {
        this.selectedCell = null;
        this.selectedEntitySecPolicyTemplate = null;
        this.modalLoading = false;
        switch (this.keystoreEntityType) {
            case 'secretkeys':
                this.keyInfoModal.hide();
                break;
            case 'keypairs':
                this.keypairInfoModal.hide();
                break;
        }

    }

    generateSigningPolicy() {
        this.modalLoading = true;
        this.service.generateSigningPolicy(this.selectedCell.row.alias).subscribe(
            res => {
                this.modalLoading = false;
                this.service.getSecurityPolicyTemplate(this.secPolicyTemplateNameSpace, this.selectedCell.row.alias).subscribe(
                    data => this.handlePolicyTemplateData(data),
                    error => {
                        if (error.status !== 404) {
                            this.handleError(error);
                        }
                    }
                );
            },
            error => this.handleError(error)
        );
    }

    addCertificate() {
        this.loading = true;
        this.service.addCertificate(this.addCertificateData).subscribe(
            data => {
                this.handleSave();
            },
            error => this.handleError(error)
        );
    }
}
