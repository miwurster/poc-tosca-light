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

import { async, TestBed } from '@angular/core/testing';

import {WineryComponent} from './winery.component';
import {TopologyRendererModule} from './topology-renderer/topology-renderer.module';
import {PaletteComponent} from './palette/palette.component';
import {JsPlumbService} from './jsPlumbService';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';

describe('WineryComponent', () => {
    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [
                WineryComponent,
                PaletteComponent
            ],
            imports: [TopologyRendererModule, BrowserAnimationsModule],
            providers: [JsPlumbService]
        }).compileComponents();
    }));

    it('should create the app', async(() => {
        const fixture = TestBed.createComponent(WineryComponent);
        const app = fixture.debugElement.componentInstance;
        expect(app).toBeTruthy();
    }));
});
