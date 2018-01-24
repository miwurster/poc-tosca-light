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

import {async, ComponentFixture, TestBed} from '@angular/core/testing';

import {CapabilitiesComponent} from './capabilities.component';

describe('CapabilitiesComponent', () => {
    let component: CapabilitiesComponent;
    let fixture: ComponentFixture<CapabilitiesComponent>;

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [CapabilitiesComponent]
        })
            .compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(CapabilitiesComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
