/**
 * Copyright (c) 2017 University of Stuttgart.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and the Apache License 2.0 which both accompany this distribution,
 * and are available at http://www.eclipse.org/legal/epl-v10.html
 * and http://www.apache.org/licenses/LICENSE-2.0
 *
 * Contributors:
 *     Josip Ledic - ledicjp@gmail.com
 *     Yannic Sowoidnich - Modals
 *     Thommy Zelenik Added Properties and Refactoring
 */
import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';

@Component({
    selector: 'winery-policies',
    templateUrl: './policies.component.html',
    styleUrls: ['./policies.component.css']
})
/**
 * This Handles Information about the nodes policies
 */
export class PoliciesComponent implements OnInit {
    @Output() toggleModalHandler: EventEmitter<any>;
    @Input() currentNodeData: any;

    constructor() {
        this.toggleModalHandler = new EventEmitter();
    }

    /**
     * Propagates the click event to node.component, where policies modal gets opened.
     * @param $event
     */
    public toggleModal($event) {
        this.toggleModalHandler.emit(this.currentNodeData);
    }

    ngOnInit() {
    }

}
