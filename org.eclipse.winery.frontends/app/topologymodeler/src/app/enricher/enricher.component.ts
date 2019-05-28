/*******************************************************************************
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
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
import { Component } from '@angular/core';
import { NgRedux } from '@angular-redux/store';
import { IWineryState } from '../redux/store/winery.store';
import { TopologyRendererActions } from '../redux/actions/topologyRenderer.actions';
import { WineryActions } from '../redux/actions/winery.actions';
import { TopologyRendererState } from '../redux/reducers/topologyRenderer.reducer';
import { HttpErrorResponse } from '@angular/common/http';
import { ToastrService } from 'ngx-toastr';
import { TTopologyTemplate } from '../models/ttopology-template';
import { Utils } from '../models/utils';
import { EnricherService } from './enricher.service';
import { AvailableFeatureEntity } from './availableFeatureEntity';
import { QName } from '../models/qname';

@Component({
    selector: 'winery-enricher',
    templateUrl: './enricher.component.html',
    styleUrls: ['./enricher.component.css']
})
export class EnricherComponent {

    availableFeatures: AvailableFeatureEntity;
    toApply = [];
    finished = false;

    constructor(private ngRedux: NgRedux<IWineryState>,
                private actions: TopologyRendererActions,
                private wineryActions: WineryActions,
                private alert: ToastrService,
                private enricherService: EnricherService) {
        this.ngRedux.select(state => state.topologyRendererState)
            .subscribe(currentButtonsState => this.checkButtonsState(currentButtonsState));
    }

    private checkButtonsState(currentButtonsState: TopologyRendererState) {
        if (currentButtonsState.buttonsState.enrichmentButton && !this.availableFeatures) {
            this.enricherService.getAvailableFeatures().subscribe(
                data => this.showAvailableFeatures(data),
                error => this.handleError(error)
            );
        }
    }

    featureSelectionChanged(feature, node, event) {
        const isChecked = event.target.checked;
        const nodeType = node.usedTypeInTopology;
        if (isChecked && this.toApply.length === 0) {
            const selectedFeatures = {
                usedTypeInTopology: nodeType,
                features: []
            };
            selectedFeatures.features.push(feature);
            this.toApply.push(selectedFeatures);
        } else if (isChecked && !this.checkIfNodeTypeSelected(nodeType)) {
            const selectedFeatures = {
                usedTypeInTopology: nodeType,
                features: []
            };
            selectedFeatures.features.push(feature);
            this.toApply.push(selectedFeatures);
        } else if (isChecked && this.checkIfNodeTypeSelected(nodeType)) {
            this.toApply[this.checkWhichIndexNodeType(nodeType)].features.push(feature);
        } else if (!event.target.checked && this.checkIfNodeTypeSelected(nodeType)) {
            this.removeFeatureForNodeType(feature, this.checkWhichIndexNodeType(nodeType));
        }
        console.log(this.toApply);
    }

    checkIfNodeTypeSelected(nodeType: QName): boolean {
        for (const element of this.toApply) {
            if (element.usedTypeInTopology === nodeType) {
                return true;
            }
        }
        return false;
    }

    checkWhichIndexNodeType(nodeType: QName): number {
        for (let i = 0; i < this.toApply.length; i++) {
            if (this.toApply[i].usedTypeInTopology === nodeType) {
                return i;
            }
        }
    }

    removeFeatureForNodeType(feature: string, index: number): void {
        for (let i = 0; i < this.toApply[index].features.length; i++) {
            if (this.toApply[index].features[i] === feature) {
                this.toApply[index].features.splice(i, 1);
            }
        }
        // delete entry if no feature selected
        if (this.toApply[index].features.length === 0) {
            this.toApply.splice(index, 1);
        }
    }

    applyEnrichment() {
        this.enricherService.applyAvailableFeatures(this.toApply).subscribe(
            data => this.enrichmentApplied(data),
            error => this.handleError(error)
        );
    }

    showAvailableFeatures(data: AvailableFeatureEntity): void {
        this.availableFeatures = data;
    }

    private handleError(error: HttpErrorResponse) {
        this.alert.error(error.message);
    }

    onHoverOver(entry: AvailableFeatureEntity) {
        const nodeTemplateIds: string[] = [];
        nodeTemplateIds.push(entry.usedTypeInTopology.toString().split('}')[1]);
        this.ngRedux.dispatch(this.actions.highlightNodes(nodeTemplateIds));
    }

    hoverOut() {
        this.ngRedux.dispatch(this.actions.highlightNodes([]));
    }

    cancel() {
        this.ngRedux.dispatch(this.actions.enrichNodeType());
    }

    private enrichmentApplied(data: TTopologyTemplate) {
        Utils.updateTopologyTemplate(this.ngRedux, this.wineryActions, data);
        this.finished = true;
        this.alert.success('Updated Topology Template!');
    }

}
