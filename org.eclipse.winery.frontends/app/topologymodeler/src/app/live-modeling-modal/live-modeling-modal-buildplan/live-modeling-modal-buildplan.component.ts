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
    selector: 'winery-live-modeling-modal-buildplan',
    templateUrl: './live-modeling-modal-buildplan.component.html',
    styleUrls: ['./live-modeling-modal-buildplan.component.css']
})
export class LiveModelingModalBuildplanComponent implements OnInit {

    fetchingBuildPlanParameters = true;
    requiredBuildPlanParameters: InputParameter[];

    constructor(private bsModalRef: BsModalRef,
                private containerService: ContainerService,
                private ngRedux: NgRedux<IWineryState>,
                private liveModelingActions: LiveModelingActions,
    ) {
    }
    
    ngOnInit(): void {
        this.fetchingBuildPlanParameters = true;
        this.requiredBuildPlanParameters = [];
        this.getRequiredBuildPlanParameters().subscribe(resp => {
            this.fetchingBuildPlanParameters = false;
            this.requiredBuildPlanParameters = resp;
        });
    }

    setBuildPlanParameters() {
        this.ngRedux.dispatch(this.liveModelingActions.setBuildPlanInputParameters(this.requiredBuildPlanParameters));
        this.ngRedux.dispatch(this.liveModelingActions.setState(LiveModelingStates.DEPLOY));
        this.dismissModal();
    }

    getRequiredBuildPlanParameters(): Observable<Array<InputParameter>> {
        return this.containerService.getRequiredBuildPlanInputParameters();
    }
    
    cancel() {
        if (!this.ngRedux.getState().liveModelingState.currentServiceTemplateInstanceId) {
            this.ngRedux.dispatch(this.liveModelingActions.setState(LiveModelingStates.DISABLED));
        }
        this.dismissModal();
    }

    dismissModal() {
        this.bsModalRef.hide();
    }
}
