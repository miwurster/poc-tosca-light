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

import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';
import { RouterModule } from '@angular/router';
import { BsDropdownModule } from 'ngx-bootstrap/dropdown';
import { AccordionModule } from 'ngx-bootstrap/accordion';
import { JsPlumbService } from './jsPlumbService';
import { WineryComponent } from './winery.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { WineryAlertModule } from './winery-alert/winery-alert.module';
import { ToastModule, ToastOptions } from 'ng2-toastr/ng2-toastr';
import { WineryCustomOption } from './winery-alert/winery-alert-options';
import { PaletteComponent } from './palette/palette.component';
import { SidebarComponent } from './sidebar/sidebar.component';
import { TopologyRendererModule } from './topology-renderer/topology-renderer.module';
import { PrintViewComponent } from './print-view/print-view.component';
import { DevToolsExtension, NgRedux, NgReduxModule } from '@angular-redux/store';
import { INITIAL_IWINERY_STATE, IWineryState, rootReducer } from './redux/store/winery.store';
import { WineryActions } from './redux/actions/winery.actions';
import { TopologyRendererActions } from './redux/actions/topologyRenderer.actions';
import { LoadedService } from './loaded.service';
import { AppReadyEventService } from './app-ready-event.service';
import { HotkeyModule } from 'angular2-hotkeys';
import { BackendService } from './backend.service';
import { WineryModalModule } from '../repositoryUiDependencies/wineryModalModule/winery.modal.module';
import { TypeaheadModule } from 'ngx-bootstrap/typeahead';
import { ExistsService } from './exists.service';

@NgModule({
    declarations: [
        WineryComponent,
        PaletteComponent,
        SidebarComponent,
        PrintViewComponent,
    ],
    exports: [],
    imports: [
        BrowserModule,
        FormsModule,
        HttpModule,
        RouterModule.forRoot([{path: '**', redirectTo: '', pathMatch: 'full'}]),
        BrowserAnimationsModule,
        NgReduxModule,
        BsDropdownModule.forRoot(),
        WineryAlertModule.forRoot(),
        ToastModule.forRoot(),
        AccordionModule.forRoot(),
        TopologyRendererModule.forRoot(),
        HotkeyModule.forRoot(),
        WineryModalModule,
        TypeaheadModule.forRoot()
    ],
    providers: [
        {provide: ToastOptions, useClass: WineryCustomOption},
        JsPlumbService,
        WineryActions,
        TopologyRendererActions,
        LoadedService,
        AppReadyEventService,
        BackendService,
        ExistsService
    ],
    bootstrap: [WineryComponent]
})
export class WineryModule {
    constructor(ngRedux: NgRedux<IWineryState>,
                devTools: DevToolsExtension) {
        const storeEnhancers = devTools.isEnabled() ?
            [devTools.enhancer()] :
            [];

        ngRedux.configureStore(
            rootReducer,
            INITIAL_IWINERY_STATE,
            [],
            storeEnhancers);
    }
}
