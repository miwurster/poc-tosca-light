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

import {CanvasComponent} from './canvas.component';
import {NodeComponent} from '../node/node.component';
import {AccordionModule} from 'ngx-bootstrap';
import {JsPlumbService} from '../jsPlumbService';

describe('CanvasComponent', () => {
    let component: CanvasComponent;
    let fixture: ComponentFixture<CanvasComponent>;

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [CanvasComponent,
                NodeComponent],
            imports: [AccordionModule.forRoot()],
            providers: [JsPlumbService]
        })
            .compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(CanvasComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should return true if array contains node with the given id', () => {
        component.ngOnInit();
        const trueResult = component.arrayContainsNode(['banana', 'apple', 'kiwi'], 'apple');
        expect(trueResult).toBe(true);
    });
});
