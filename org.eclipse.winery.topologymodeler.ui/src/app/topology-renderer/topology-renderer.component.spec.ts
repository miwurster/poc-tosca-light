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

import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { TopologyRendererComponent } from './topology-renderer.component';
import { NavbarComponent } from '../navbar/navbar.component';
import { CanvasComponent } from '../canvas/canvas.component';
import { NodeComponent } from '../node/node.component';
import { AccordionModule } from 'ngx-bootstrap';
import { WineryAlertModule } from '../winery-alert/winery-alert.module';
import { ToastModule } from 'ng2-toastr';
import { JsonService } from '../jsonService/json.service';
import { JsPlumbService } from '../jsPlumbService';
import { MockJsonService } from '../jsonService/mock-json.service';

describe('TopologyRendererComponent', () => {
  let component: TopologyRendererComponent;
  let jsonService: JsonService;
  let fixture: ComponentFixture<TopologyRendererComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [TopologyRendererComponent, NavbarComponent, CanvasComponent, NodeComponent],
      imports: [AccordionModule.forRoot(), WineryAlertModule.forRoot(), ToastModule.forRoot()],
      providers: [{provide: JsonService, useClass: MockJsonService }, JsPlumbService]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    jsonService = TestBed.get(JsonService);
    fixture = TestBed.createComponent(TopologyRendererComponent);
    component = fixture.componentInstance;
    component.ngOnInit();
    fixture.detectChanges();

  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
