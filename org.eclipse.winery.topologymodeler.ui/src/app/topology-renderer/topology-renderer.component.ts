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

import {Component, Input, OnInit, ViewContainerRef} from '@angular/core';
import {WineryAlertService} from '../winery-alert/winery-alert.service';

/**
 * This is the parent component of the canvas and navbar component.
 */
@Component({
    selector: 'winery-topology-renderer',
    templateUrl: './topology-renderer.component.html',
    styleUrls: ['./topology-renderer.component.css']
})
export class TopologyRendererComponent implements OnInit {
    @Input() entityTypes: any;

    constructor (vcr: ViewContainerRef, private notify: WineryAlertService) {
        this.notify.init(vcr);
    }

    ngOnInit () {
    }
}