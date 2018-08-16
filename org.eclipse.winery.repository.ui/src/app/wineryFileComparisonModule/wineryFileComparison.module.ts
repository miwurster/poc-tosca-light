import { FormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { WineryFileComparisonComponent } from './wineryFileComparison.component';

import { DiffMatchPatchModule } from 'ng-diff-match-patch';
import { SelectModule } from 'ng2-select';
import { AdminModule } from '../wineryMainModules/admin/admin.module';
import { WineryNotificationModule } from '../wineryNotificationModule/wineryNotification.module';

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

@NgModule({
    imports: [
        BrowserModule,
        FormsModule,
        DiffMatchPatchModule,
        SelectModule,
        WineryNotificationModule
    ],
    exports: [
        WineryFileComparisonComponent
    ],
    declarations: [WineryFileComparisonComponent]
})
export class WineryFileComparisonModule {
}
