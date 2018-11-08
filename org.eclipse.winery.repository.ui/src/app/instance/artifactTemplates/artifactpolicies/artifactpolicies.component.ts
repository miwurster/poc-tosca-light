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
import { InstanceService } from '../../instance.service';
import { WineryNotificationService } from '../../../wineryNotificationModule/wineryNotification.service';
import { backendBaseURL } from '../../../configuration';
import {
    ArtifactPoliciesService, ModeledPolicies, PolicyApiData, SecurityPolicyApiData
} from './artifactpolicies.service';
import { HttpErrorResponse } from '@angular/common/http';
import { WineryRowData } from '../../../wineryTableModule/wineryTable.component';
import { ModalDirective } from 'ngx-bootstrap';

@Component({
    selector: 'winery-instance-keystoreentity',
    templateUrl: 'artifactpolicies.component.html',
    styleUrls: ['artifactpolicies.component.css'],
    providers: [ArtifactPoliciesService]
})
export class ArtifactPoliciesComponent implements OnInit {
    loading = true;
    filesPath: string;
    policies: PolicyApiData[] = [];
    securityPolicies: SecurityPolicyApiData[] = [];
    selectedPolicy: SecurityPolicyApiData;
    policiesColumns = [
        { title: 'Name', name: 'name' },
        { title: 'Policy Type', name: 'type.localname' },
        { title: 'Policy Template', name: 'template.localname' },
    ];
    securityPoliciesColumns = [
        { title: 'Name', name: 'shortName' },
        { title: 'Policy Type', name: 'type.localname' },
        { title: 'Policy Template', name: 'template.localname' },
        { title: 'Applied', name: 'isAppliedIcon' }
    ];
    iconApplied = '<div class="text-center"><i class=\'glyphicon glyphicon-ok text-success\'></i></div>';
    iconNotApplied = '<div class="text-center"><i class=\'glyphicon glyphicon-remove text-danger\'></i></div>';
    @ViewChild('addPolicyModal') addPolicyModal: ModalDirective;

    constructor(private service: ArtifactPoliciesService, public sharedData: InstanceService, private notify: WineryNotificationService) {
        this.filesPath = backendBaseURL + this.sharedData.path + '/files/zip';
    }

    ngOnInit(): void {
        this.service.getPolicies().subscribe(
            data => this.processPoliciesData(data),
            error => this.handleError(error)
        );
        this.loading = false;
    }

    private handleError(error: HttpErrorResponse) {
        this.loading = false;
        this.notify.error(error.message);
    }

    private processPoliciesData(data: ModeledPolicies) {
        this.policies = data.policies;
        this.securityPolicies = [];

        for (const p of data.securityPolicies) {
            p.shortName = p.name.slice(0, 25) + '...';
            p.template.shortLocalname = p.template.localname.slice(0, 25) + '...';
            if (p.isApplied) {
                p.isAppliedIcon = this.iconApplied;
            } else {
                p.isAppliedIcon = this.iconNotApplied;
            }
            this.securityPolicies.push(
                {
                    'name': p.name,
                    'shortName': p.shortName,
                    'type': p.type,
                    'template': p.template,
                    'isApplied': p.isApplied,
                    'isAppliedIcon': p.isAppliedIcon
                }
            );
        }
        this.loading = false;
    }

    onCellSelect(data: WineryRowData) {
        if (data) {
            this.selectedPolicy = data.row;
            console.log(this.selectedPolicy);
        }
    }

    onDecryptClick() {
        this.loading = true;
        this.service.decryptContents().subscribe(
            data => {
                this.selectedPolicy = null;
                this.service.getPolicies().subscribe(
                    data => this.processPoliciesData(data),
                    error => this.handleError(error)
                );
            },
            error => this.handleError(error)
        );
    }

    onAddClick() {
        this.addPolicyModal.show();
    }

    onRemoveClick(event: any) {

    }

    addPolicy() {

    }
}
