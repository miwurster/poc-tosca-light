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
 ********************************************************************************/
import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ServiceCompositionWorkflowComponent } from './serviceCompositionWorkflow.component';
import { WineryModalModule } from '../../../wineryModalModule/winery.modal.module';
import { WineryLoaderModule } from '../../../wineryLoader/wineryLoader.module';
import { SelectModule } from 'ng2-select';

@NgModule({
    imports: [
        BrowserModule,
        CommonModule,
        RouterModule,
        WineryModalModule,
        WineryLoaderModule,
    ],
    exports: [
        ServiceCompositionWorkflowComponent
    ],
    declarations: [
        ServiceCompositionWorkflowComponent
    ]
})
export class ServiceCompositionWorkflowModule {
}