/*******************************************************************************
 * Copyright (c) 2017-2018 Contributors to the Eclipse Foundation
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
import { Component, OnInit } from '@angular/core';
import { InstanceService } from '../../instance.service';
import { backendBaseURL } from '../../../configuration';
import { WineryRepositoryConfigurationService } from '../../../wineryFeatureToggleModule/WineryRepositoryConfiguration.service';

@Component({
    templateUrl: 'serviceCompositionWorkflow.component.html',
})
export class ServiceCompositionWorkflowComponent implements OnInit {

    readonly uiURL = encodeURIComponent(window.location.origin + window.location.pathname + '#/');
    editorUrl: string;

    constructor(public sharedData: InstanceService,
                private configurationService: WineryRepositoryConfigurationService) {
    }

    ngOnInit() {
        let editorConfig = '?repositoryURL=' + encodeURIComponent(backendBaseURL)
            + '&uiURL=' + this.uiURL
            + '&ns=' + encodeURIComponent(this.sharedData.toscaComponent.namespace)
            + '&id=' + this.sharedData.toscaComponent.localName;

        if (!this.sharedData.currentVersion.editable) {
            editorConfig += '&isReadonly=true';
        }

        this.editorUrl = this.configurationService.configuration.endpoints.servicecompositionmodeler + editorConfig;
    }
}
