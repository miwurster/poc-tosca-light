/*******************************************************************************
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
 *******************************************************************************/

import { Component, OnInit } from '@angular/core';
import { CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import {products} from '../../serviceTemplates';
import {HttpServiceTemplates} from '../../services/httpClient';

@Component({
  selector: 'winery-side-bar',
  templateUrl: './side-bar.component.html',
  styleUrls: ['./side-bar.component.css']
})
export class SideBarComponent implements OnInit {
    products = products;
    serviceTemplates: any;
  constructor(private httpServiceTemplates: HttpServiceTemplates ) {}

  ngOnInit() {
      this.httpServiceTemplates.getServiceTemplates().subscribe((data: any) => {
          console.log(data);
          this.serviceTemplates = data['ServiceTemplates']; }); }
    /*
    drop(event: CdkDragDrop<string[]>) {
        moveItemInArray(this.products, event.previousIndex, event.currentIndex);
    }
    */
}
