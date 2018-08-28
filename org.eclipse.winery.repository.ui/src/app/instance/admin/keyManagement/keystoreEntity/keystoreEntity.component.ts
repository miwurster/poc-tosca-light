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
import { SelectData } from '../../../../wineryInterfaces/selectData';
import { SelectItem } from 'ng2-select';

@Component({
    selector: 'winery-instance-keystoreentity',
    templateUrl: 'keystoreEntity.component.html',
    styleUrls: ['keystoreEntity.component.css'],
    providers: [KeystoreEntityService]
})
export class KeystoreEntityComponent implements OnInit {

    keystoreEntityType: string;
    loading = true;
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
    @ViewChild('addModal') addModal: ModalDirective;
    @ViewChild('keyInfoModal') keyInfoModal: ModalDirective;
    @ViewChild('keypairInfoModal') keypairInfoModal: ModalDirective;
    @ViewChild('certificateInfoModal') certificateInfoModal: ModalDirective;
    selectedCell: WineryRowData = {
        row: "",
        column: ""
    };
    supportedAlgorithms: Array<SelectData> = [];
    supportedAlgorithmsKeySizesMap: { [key: string]: SelectData[] } = {};
    currentSelectedItem: AddSecretKeyData = new AddSecretKeyData;
    Object = Object;

    constructor(private service: KeystoreEntityService,
                private notify: WineryNotificationService,
                public route: ActivatedRoute) {
        this.route.url.subscribe(params => {
            this.keystoreEntityType = params[0].path;
        })
    }

    ngOnInit(): void {
        this.loading = true;
        this.service.getSupportedAlgorithms().subscribe(
            data => this.handleAlgorithmData(data),
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
                let kp = new KeyPairTableData(<KeyPairEntity>receivedData[i]);
                this.data[this.keystoreEntityType].push(kp);
            }
        } else {
            this.data[this.keystoreEntityType] = receivedData;
        }
    }

    private handleError(error: HttpErrorResponse) {
        this.loading = false;
        this.notify.error(error.message);
    }

    onAddClick() {
        this.addModal.show();
    }

    onInfoClick() {
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

    onCellSelect(data: WineryRowData) {
        if (data) {
            this.selectedCell = data;
        }
    }

    addKeystoreEntity() {

    }

    private handleAlgorithmData(data: SupportedAlgorithms) {
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
            let addedAlgorithms: string[] = [];
            for (let i = 0; i < dataArray.length; i++) {
                let algo = dataArray[i].name;
                let keySize = dataArray[i].keySizeInBits;
                if (addedAlgorithms.indexOf(algo) === -1) {
                    addedAlgorithms.push(algo);
                    this.supportedAlgorithms.push({ id: algo, text: algo });
                }
                if (!this.supportedAlgorithmsKeySizesMap.hasOwnProperty(algo) || !this.supportedAlgorithmsKeySizesMap[algo]) {
                    this.supportedAlgorithmsKeySizesMap[algo] = [];
                }
                this.supportedAlgorithmsKeySizesMap[algo].push({ id: keySize.toString(), text: keySize.toString() });
            }
        }
    }

    targetAlgorithmSelected(targetObj: SelectItem) {
        this.currentSelectedItem.algorithm = targetObj.id;
    }

    targetKeySizeSelected(targetObj: SelectItem) {
        this.currentSelectedItem.keySizeInBits = targetObj.id;
    }
}

export class KeystoreTableData {
    secretkeys: KeyEntity[];
    keypairs: KeyPairTableData[];
    certificates: CertificateEntity[];
}

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
    keyFile: any;

    constructor() {
        this.algorithm = null;
        this.keySizeInBits = null;
    }
}
