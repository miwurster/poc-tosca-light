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

import { Injectable } from '@angular/core';
import { NgRedux } from '@angular-redux/store';
import { IWineryState } from '../redux/store/winery.store';
import { TopologyTemplateUtil } from '../models/topologyTemplateUtil';
import { TTopologyTemplate } from '../models/ttopology-template';
import { WineryActions } from '../redux/actions/winery.actions';
import { LiveModelingStates } from '../models/enums';
import { LiveModelingActions } from '../redux/actions/live-modeling.actions';

@Injectable()
export class TopologyService {
    private currentJsonTopology: TTopologyTemplate;
    private _lastSavedJsonTopology = new TTopologyTemplate();
    private _lastDeployedJsonTopology = new TTopologyTemplate();
    private enabled = false;
    private liveModelingState: LiveModelingStates;

    constructor(private ngRedux: NgRedux<IWineryState>,
                private wineryActions: WineryActions,
                private liveModelingActions: LiveModelingActions) {
        this.ngRedux.select(state => state.wineryState.currentJsonTopology)
            .subscribe(currentJsonTopology => {
                this.currentJsonTopology = currentJsonTopology;
                this.checkForSaveChanges();
                this.checkForDeployChanges();
            });
    }

    get lastSavedJsonTopology() {
        return this._lastSavedJsonTopology;
    }

    set lastSavedJsonTopology(topologyTemplate: TTopologyTemplate) {
        this._lastSavedJsonTopology = JSON.parse(JSON.stringify(topologyTemplate));
    }

    get lastDeployedJsonTopology() {
        return this._lastDeployedJsonTopology;
    }

    set lastDeployedJsonTopology(topologyTemplate: TTopologyTemplate) {
        this._lastDeployedJsonTopology = JSON.parse(JSON.stringify(topologyTemplate));
    }

    public enableCheck() {
        this.enabled = true;
    }

    public checkForSaveChanges() {
        if (!this.enabled) {
            return;
        }
        const changed = TopologyTemplateUtil.hasTopologyTemplateChanged(this.currentJsonTopology, this.lastSavedJsonTopology);
        this.ngRedux.dispatch(this.wineryActions.setUnsavedChanges(changed));
    }
    
    public checkForDeployChanges() {
        if (this.liveModelingState === LiveModelingStates.DISABLED) {
            return;
        }
        const changed = TopologyTemplateUtil.hasTopologyTemplateChanged(this.currentJsonTopology, this.lastDeployedJsonTopology);
        this.ngRedux.dispatch(this.liveModelingActions.setDeploymentChanges(changed));
    }
}
