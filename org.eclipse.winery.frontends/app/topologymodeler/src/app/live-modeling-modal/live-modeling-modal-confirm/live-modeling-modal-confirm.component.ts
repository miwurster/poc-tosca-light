/*******************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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
import { BsModalRef } from 'ngx-bootstrap';
import { NgRedux } from '@angular-redux/store';
import { IWineryState } from '../../redux/store/winery.store';
import { ContainerService } from '../../services/container.service';
import { LiveModelingActions } from '../../redux/actions/live-modeling.actions';
import { Observable } from 'rxjs';
import { InputParameter } from '../../models/container/input-parameter.model';
import { LiveModelingStates } from '../../models/enums';

@Component({
    selector: 'winery-live-modeling-modal-confirm',
    templateUrl: './live-modeling-modal-confirm.component.html',
    styleUrls: ['./live-modeling-modal-confirm.component.css']
})
export class LiveModelingModalConfirmComponent implements OnInit {

    title: string;
    content: string;
    callback: Function;

    constructor(private bsModalRef: BsModalRef,
    ) {
    }

    ngOnInit(): void {
    }

    callCallback() {
        this.dismissModal();
        this.callback();
    }

    cancel() {
        this.dismissModal();
    }

    dismissModal() {
        this.bsModalRef.hide();
    }
}
