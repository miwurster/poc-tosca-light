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

import {TargetLocationsComponent} from './target-locations.component';

describe('TargetLocationsComponent', () => {
    let component: TargetLocationsComponent;
    let fixture: ComponentFixture<TargetLocationsComponent>;

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [TargetLocationsComponent]
        })
            .compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(TargetLocationsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
