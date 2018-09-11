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

import { WineryQNameSelectorModule } from '../../../wineryQNameSelector/wineryQNameSelector.module';
import { InterfacesModule } from '../../sharedComponents/interfaces/interfaces.module';
import { RouterModule, Routes } from '@angular/router';
import { NgModule } from '@angular/core';
import { WineryModalModule } from '../../../wineryModalModule/winery.modal.module';
import { WineryDuplicateValidatorModule } from '../../../wineryValidators/wineryDuplicateValidator.module';
import { FileUploadModule } from 'ng2-file-upload';
import { SelectModule } from 'ng2-select';
import { WineryLoaderModule } from '../../../wineryLoader/wineryLoader.module';
import { CommonModule } from '@angular/common';
import { WineryEditorModule } from '../../../wineryEditorModule/wineryEditor.module';
import { FormsModule } from '@angular/forms';
import { WineryTableModule } from '../../../wineryTableModule/wineryTable.module';
import { WineryEditXMLModule } from '../../sharedComponents/editXML/editXML.module';
import { InstanceService } from '../../instance.service';
import { KeystoreEntityComponent } from './keystoreEntity/keystoreEntity.component';
import { TabsModule } from 'ngx-bootstrap/tabs';

export const keyManagementRoutes: Routes = [
    { path: 'secretkeys', component: KeystoreEntityComponent },
    { path: 'keypairs', component: KeystoreEntityComponent },
    { path: 'certificates', component: KeystoreEntityComponent },
    { path: '', redirectTo: 'secretkeys', pathMatch: 'full' }
];

@NgModule({
    imports: [
        FormsModule,
        WineryLoaderModule,
        CommonModule,
        WineryModalModule,
        FileUploadModule,
        SelectModule,
        InterfacesModule,
        WineryTableModule,
        WineryDuplicateValidatorModule,
        WineryEditXMLModule,
        WineryQNameSelectorModule,
        RouterModule,
        WineryEditorModule,
        TabsModule.forRoot()
    ],
    exports: [
        RouterModule
    ],
    declarations: [
        KeystoreEntityComponent
    ],
    providers: [
        InstanceService
    ]
})
export class KeyManagementModule {
}
