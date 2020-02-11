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
import { HttpServiceTemplates } from '../../services/httpClient';
import { ServiceTemplates } from '../../serviceTemplates';
import { assertLessThan } from '@angular/core/src/render3/assert';
import { Router } from '@angular/router';


@Component({
    selector: 'winery-side-bar',
    templateUrl: './side-bar.component.html',
    styleUrls: ['./side-bar.component.css']
})
export class SideBarComponent implements OnInit {
    public allServiceTemplates: ServiceTemplates;


    constructor(private httpServiceTemplates: HttpServiceTemplates) {
    }

    ngOnInit() {
        this.httpServiceTemplates.getServiceTemplates().subscribe(data => this.allServiceTemplates = data['serviceTemplates']);
        /* this.httpServiceTemplates.getTest().subscribe((data) => {
            console.log(data);
            this.allServiceTemplates = data['serviceTemplates'];
        }); */
    }
    /*
    drop(event: CdkDragDrop<string[]>) {
        moveItemInArray(this.products, event.previousIndex, event.currentIndex);
    }
    */
    public test() {
        console.log(this.allServiceTemplates);
        console.log(this.allServiceTemplates[0].id);
        console.log(this.allServiceTemplates[0].inputParameters[0].name);
    }
}
