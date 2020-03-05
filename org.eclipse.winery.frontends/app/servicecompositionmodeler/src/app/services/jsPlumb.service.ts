/********************************************************************************
 * Copyright (c) 2017-2018 Contributors to the Eclipse Foundation
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

import { Injectable } from '@angular/core';
import { isNullOrUndefined } from 'util';
import { jsPlumb, jsPlumbInstance } from 'jsplumb';
import { BroadcastService } from '../services/broadcast.service';

/**
 * Defines the JSPlumb instance which is used over the complete project.
 */
@Injectable()
export class JsPlumbService {
    jsPlumbInstance: jsPlumbInstance;
    constructor(private broadcastService: BroadcastService) {}

    getJsPlumbInstance(): any {
        jsPlumb.getInstance().ready(() => {
            this.jsPlumbInstance = jsPlumb.getInstance();
            this.jsPlumbInstance.importDefaults({
                Connector : [ 'Flowchart', { curviness: 150 } ],
                Anchors : [ 'RightMiddle', 'LeftMiddle' ],
                Container: 'canvas',
            });
            this.broadcastService.broadcast(this.broadcastService.jsPlumbInstance,
                this.jsPlumbInstance);
        });
        return this.jsPlumbInstance;
    }
    public testDraggable() {
        this.jsPlumbInstance.setContainer(('.model-area'));
        const e1 = this.jsPlumbInstance.addEndpoint( 'dragDropWindow1');
        const e2  = this.jsPlumbInstance.addEndpoint( 'dragDropWindow2');
        const e3  = this.jsPlumbInstance.addEndpoint( 'dragDropWindow3');
        const e4  = this.jsPlumbInstance.addEndpoint( 'dragDropWindow4');
        this.jsPlumbInstance.draggable('.window');
        this.jsPlumbInstance.connect({uuids: ['test1', 'test3']});
        this.jsPlumbInstance.connect({
            source: 'dragDropWindow1',
            target: 'dragDropWindow2',
        });
        this.jsPlumbInstance.connect({
            source: 'dragDropWindow2',
            target: 'dragDropWindow4',
        });
    }
}
