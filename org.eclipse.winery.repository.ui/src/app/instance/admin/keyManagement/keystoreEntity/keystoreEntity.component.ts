/********************************************************************************
 * Copyright (c) 2018-2019 Contributors to the Eclipse Foundation
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
import { KeyExchangeService } from '../keyExchange/keyExchange.service';
import { ConfigurationService } from '../../accountability/configuration/configuration.service';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

export class KeyAssignmentData {
    keyAlias: string;
    keyGivers: string[];
    keyTakers: string[];

    constructor(ka: KeyAssignmentData) {
        this.keyAlias = ka.keyAlias;
        this.keyGivers = ka.keyGivers;
        this.keyTakers = ka.keyTakers;
    }
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

export class KeyTableData {
    alias: string;
    algorithm: string;
    sizeInBits: number;
    isShared: boolean;
    keyGivers: string[];
    keyTakers: string[];
    originalKey: KeyEntity;

    constructor(key: KeyEntity) {
        this.alias = key.alias;
        this.algorithm = key.algorithm;
        this.sizeInBits = key.keySizeInBits;
        this.originalKey = key;
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
    organizationName: string;
    countryName: string;
    setAsMaster: boolean;
    privateKeyFile: File;
    certificateFile: File;

    constructor() {
        this.algorithm = null;
        this.keySizeInBits = null;
    }
}

export class KeystoreTableData {
    secretkeys: KeyTableData[];
    keypairs: KeyPairTableData[];
    certificates: CertificateEntity[];
}

@Component({
    selector: 'winery-instance-keystoreentity',
    templateUrl: 'keystoreEntity.component.html',
    styleUrls: ['keystoreEntity.component.css'],
    providers: [KeystoreEntityService, KeyExchangeService]
})
export class KeystoreEntityComponent implements OnInit {

    keystoreEntityType: string;
    loading = true;
    modalLoading = false;
    columns = {
        'secretkeys': [
            { title: 'Alias', name: 'alias' },
            { title: 'Algorithm', name: 'algorithm' },
            { title: 'Size In Bits', name: 'sizeInBits' },
            { title: 'Shared Key?', name: 'isShared' }
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
        ],
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
    keyAssignmentData: KeyAssignmentData[] = [];
    givenPermissionsCount = 0;
    takenPermissionsTotalCount = 0;
    takenPermissionsNotAddedCount = 0;
    myIdentity: string = undefined;
    myPublicKeyAlias = '';
    selectedPublicKeyAlias: SelectItem = undefined;
    selectedSecretKeyAlias: SelectItem = undefined;
    selectedParticipant: string = undefined;
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
    @ViewChild('setOfficialKeypairModal') setOfficialKeypairModal: ModalDirective;
    @ViewChild('givePermissionsModal') givePermissionsModal: ModalDirective;

    constructor(private service: KeystoreEntityService,
                private notify: WineryNotificationService,
                public route: ActivatedRoute,
                private accountabilityConfigService: ConfigurationService,
                private keyExchangeService: KeyExchangeService) {
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
                this.keyExchangeService.getKeyAssignments().pipe(
                    map(data => {
                        this.keyAssignmentData = data;
                    })
                ).flatMap(() => this.service.getSecretKeys()
                ).subscribe(
                    data => this.handleKeystoreData(data, 'secretkeys', false),
                    error => this.handleError(error)
                );
                break;
            // we want to show certain key pairs when we show the key exchange tab
            case 'keyexchange':
                this.getKeyExchangeData().subscribe(
                    (data: [KeyPairEntity[], KeyEntity[]]) => {
                        this.handleKeystoreData(data[0], 'keypairs', true);
                        this.handleKeystoreData(data[1], 'secretkeys', true);
                        this.refreshCounts();
                        this.loading = false;
                    },
                    error => this.handleError(error)
                );
                break;
            case 'keypairs':
                this.service.getKeyPairs().subscribe(
                    data => this.handleKeystoreData(data, 'keypairs', false),
                    error => this.handleError(error)
                );
                break;
            case 'certificates':
                this.service.getCertificates().subscribe(
                    data => this.handleKeystoreData(data, 'certificates', false),
                    error => this.handleError(error)
                );
                break;
        }
    }

    private handleKeystoreData(receivedData: any[], entityType: string, stillLoading: boolean) {
        console.debug(receivedData);
        if (!stillLoading) {
            this.loading = false;
        }
        // console.log(receivedData);
        if (entityType === 'keypairs') {
            this.data[entityType] = [];

            for (let i = 0; i < receivedData.length; i++) {
                const kp = new KeyPairTableData(<KeyPairEntity>receivedData[i]);
                this.data[entityType].push(kp);
            }
        } else if (entityType === 'secretkeys') {
            this.data[entityType] = [];

            for (let i = 0; i < receivedData.length; i++) {
                const key = new KeyTableData(<KeyEntity>receivedData[i]);
                const relatedEntries = this.keyAssignmentData
                    .filter(assignment =>
                        assignment.keyAlias === key.alias &&
                        (assignment.keyTakers.length > 0 || assignment.keyGivers.length > 0))
                key.isShared = relatedEntries.length > 0;
                key.keyGivers = relatedEntries.length > 0 ? relatedEntries[0].keyGivers : [];
                key.keyTakers = relatedEntries.length > 0 ? relatedEntries[0].keyTakers : [];
                this.data[entityType].push(key);
            }
        } else {
            this.data[entityType] = receivedData;
        }
    }

    private getKeyExchangeData(): Observable<[KeyPairEntity[], KeyEntity[]]> {
        return Observable.forkJoin([
            this.keyExchangeService.getMyPublicKeyAlias().pipe(
                map(data => {
                    this.myPublicKeyAlias = data;
                })
            ),
            this.accountabilityConfigService.retrieveMyIdentity().pipe(
                map(data => {
                    this.myIdentity = data;
                })
            ),
            this.keyExchangeService.getKeyAssignments().pipe(
                map(data => {
                    this.keyAssignmentData = data;
                })
            )])
            .pipe(catchError(error => of(this.notify.error(error.error))))
            .flatMap(() => Observable.forkJoin([
                    this.service.getKeyPairs(),
                    this.service.getSecretKeys()
                ]
            ));
    }

    private handleError(error: HttpErrorResponse, timeout = -1) {
        this.loading = false;
        this.modalLoading = false;

        if (timeout >= 0) {
            this.notify.error(error.error, 'Error', { 'timeout': timeout });
        } else {
            this.notify.error(error.error);
        }
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

    onRemoveClick() {
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
            () => {
                this.handleSave();
            },
            error => this.handleError(error)
        );
    }

    addKeypair() {
        this.loading = true;
        console.debug(this.addKeypairData.setAsMaster);
        this.service.addKeypair(this.addKeypairData).subscribe(
            () => {
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
            () => this.handleRemove(),
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
            () => {
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
            () => {
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
            () => {
                this.handleSave();
            },
            error => this.handleError(error)
        );
    }

    setAsMaster(keyPairAlias: string) {
        this.closeInfoModal();
        this.loading = true;
        this.service.setAsMaster(keyPairAlias)
            .flatMap(() => this.service.getKeyPairs())
            .subscribe((keyPairs: KeyPairEntity[]) => {
                this.handleKeystoreData(keyPairs, 'keypairs', false);
                this.handleSuccess();
            }, error => {
                this.handleError(error);
            });
    }

    getKeyPairAliases(): SelectItem[] {
        return this.data.keypairs
            .filter(keyPair =>
                this.keyExchangeService.getAllowedPublicKeyAlgorithmNames().includes(keyPair.algorithm))
            .map(keyPair => new SelectItem(keyPair.alias));
    }

    getSecretKeyAliases(): SelectItem[] {
        return this.data.secretkeys
            .map((key) => new SelectItem(key.alias));
    }

    updateAliasSelection() {
        if (this.myPublicKeyAlias !== '') {
            const aliases = this.getKeyPairAliases();
            this.selectedPublicKeyAlias = aliases.filter(aliasItem => aliasItem.text === this.myPublicKeyAlias)[0];
        } else {
            this.selectedPublicKeyAlias = undefined;
        }
    }

    setOfficialKeyPair(): void {
        this.loading = true;
        this.keyExchangeService.setOfficialKeyPairAlias(this.selectedPublicKeyAlias.text).subscribe(
            () => {
                this.handleSuccess();
                this.myPublicKeyAlias = this.selectedPublicKeyAlias.text;
            },
            error => this.handleError(error)
        );
    }

    givePermission(): void {
        this.loading = true;
        this.keyExchangeService
            .givePermission(this.selectedParticipant, this.selectedSecretKeyAlias.text)
            .flatMap(() => this.keyExchangeService.getKeyAssignments())
            .subscribe(
                (assignments) => {
                    this.keyAssignmentData = assignments;
                    this.refreshCounts();
                    this.handleSuccess();
                },
                error => this.handleError(error)
            );
    }

    updateTakenPermissions(): void {
        this.loading = true;
        let addedCount: number;
        let errorCount: number;
        this.keyExchangeService.updateListOfKeysGivenToMe()
            .flatMap(updateResult => {
                addedCount = updateResult.addedKeysCount;
                errorCount = updateResult.badGiversCount;

                return this.service.getSecretKeys();
            })
            .flatMap(secretKeys => {
                this.handleKeystoreData(secretKeys, 'secretkeys', true);

                return this.keyExchangeService.getKeyAssignments();
            })
            .subscribe(data => {
                    this.keyAssignmentData = data;
                    this.refreshCounts();
                    this.loading = false;

                    if (addedCount > 0) {
                        this.notify.success(`(${addedCount}) new keys were added to the key store!`, 'Success');
                    }

                    if (errorCount > 0) {
                        this.notify
                            .warning(`(${this.getTakenPermissionsNotAddedCount() - addedCount}) ` +
                                `keys from (${errorCount}) participant failed to decrypt (have you changed your official public key?)`);
                    }

                    if (addedCount === 0 && errorCount === 0) {
                        this.notify.success('No new keys were retrieved.');
                    }
                },
                (error: any) => this.handleError(error, 10000)
            );
    }

    showSetOfficialKeyPairModal(): void {
        this.updateAliasSelection();
        this.setOfficialKeypairModal.show();
    }

    showGivePermissionsModal(): void {
        this.selectedSecretKeyAlias = undefined;
        this.selectedParticipant = undefined;
        this.givePermissionsModal.show();
    }

    onAliasSelected(event: SelectItem) {
        this.selectedPublicKeyAlias = event;
    }

    onSKAliasSelected(event: SelectItem) {
        this.selectedSecretKeyAlias = event;
    }

    private refreshCounts(): void {
        this.takenPermissionsNotAddedCount = this.getTakenPermissionsNotAddedCount();
        this.takenPermissionsTotalCount = this.getTakenPermissionsTotalCount();
        this.givenPermissionsCount = this.getGivenPermissionsCount();
    }

    private getTakenPermissionsTotalCount(): number {
        if (this.myIdentity !== undefined && this.keyAssignmentData !== undefined) {
            return this.keyAssignmentData
                .filter(assignment => assignment.keyTakers !== undefined &&
                    assignment.keyTakers.includes(this.myIdentity)).length;
        } else {
            return 0;
        }
    }

    private getTakenPermissionsNotAddedCount(): number {
        if (this.myIdentity !== undefined && this.keyAssignmentData !== undefined) {
            const result = this.keyAssignmentData
                .filter(assignment =>
                    assignment.keyTakers !== undefined &&
                    assignment.keyTakers.includes(this.myIdentity) &&
                    !this.data.secretkeys.map(key => key.alias).includes(assignment.keyAlias)
                );
            return result.length;
        } else {
            return 0;
        }
    }

    private getGivenPermissionsCount(): number {
        if (this.myIdentity !== undefined && this.keyAssignmentData !== undefined) {
            const result = this.keyAssignmentData
                .filter(assignment => assignment.keyGivers !== undefined &&
                    assignment.keyGivers.includes(this.myIdentity)
                );
            return result.length;
        } else {
            return 0;
        }
    }

}
