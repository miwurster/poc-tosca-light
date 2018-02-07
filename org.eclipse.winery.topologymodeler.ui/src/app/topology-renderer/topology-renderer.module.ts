/********************************************************************************
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
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

import { ModuleWithProviders, NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';
import { RouterModule } from '@angular/router';
import { BsDropdownModule } from 'ngx-bootstrap/dropdown';
import { AccordionModule } from 'ngx-bootstrap/accordion';
import { NavbarComponent } from '../navbar/navbar.component';
import { NodeComponent } from '../node/node.component';
import { CanvasComponent } from '../canvas/canvas.component';
import { LayoutDirective } from '../layout.directive';
import { WineryAlertModule } from '../winery-alert/winery-alert.module';
import { ToastModule, ToastOptions } from 'ng2-toastr/ng2-toastr';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { JsPlumbService } from '../jsPlumbService';
import { WineryCustomOption } from '../winery-alert/winery-alert-options';
import { TopologyRendererComponent } from './topology-renderer.component';
import { NgReduxModule } from '@angular-redux/store';
import { PropertiesComponent } from '../node/properties/properties.component';
import { RequirementsComponent } from '../node/requirements/requirements.component';
import { TargetLocationsComponent } from '../node/target-locations/target-locations.component';
import { PoliciesComponent } from '../node/policies/policies.component';
import { DeploymentArtifactsComponent } from '../node/deployment-artifacts/deployment-artifacts.component';
import { WineryModalModule } from '../../repositoryUiDependencies/wineryModalModule/winery.modal.module';
import { CapabilitiesComponent } from '../node/capabilities/capabilities.component';
import { PropertiesContentComponent } from '../node/properties-content/properties-content.component';
import { KeysPipe } from '../keys.pipe';
import { TypeaheadModule } from 'ngx-bootstrap/typeahead';
import { ToscatypeTableComponent } from '../node/toscatype-table/toscatype-table.component';
import { EntitiesModalComponent } from '../canvas/entities-modal/entities-modal.component';

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        HttpModule,
        BrowserAnimationsModule,
        BsDropdownModule.forRoot(),
        WineryAlertModule.forRoot(),
        ToastModule.forRoot(),
        AccordionModule.forRoot(),
        NgReduxModule,
        RouterModule,
        WineryModalModule,
        TypeaheadModule.forRoot()
    ],
    declarations: [
        NavbarComponent,
        NodeComponent,
        CanvasComponent,
        LayoutDirective,
        TopologyRendererComponent,
        PropertiesComponent,
        RequirementsComponent,
        TargetLocationsComponent,
        PoliciesComponent,
        DeploymentArtifactsComponent,
        CapabilitiesComponent,
        PropertiesContentComponent,
        ToscatypeTableComponent,
        EntitiesModalComponent,
        KeysPipe
    ],
    exports: [
        TopologyRendererComponent,
        KeysPipe
    ],
})
export class TopologyRendererModule {

    static forRoot(): ModuleWithProviders {
        return {
            ngModule: TopologyRendererModule,
            providers: [
                {provide: ToastOptions, useClass: WineryCustomOption},
                JsPlumbService
            ]
        };
    }

    constructor() {
    }
}
