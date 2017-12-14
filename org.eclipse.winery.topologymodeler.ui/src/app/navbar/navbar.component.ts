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

import {Component, OnDestroy} from '@angular/core';
import {WineryAlertService} from '../winery-alert/winery-alert.service';
import {NgRedux} from '@angular-redux/store';
import {TopologyRendererActions} from '../redux/actions/topologyRenderer.actions';
import {ButtonsStateModel} from '../models/buttonsState.model';
import {IWineryState} from '../redux/store/winery.store';
import {BackendService} from '../backend.service';

/**
 * The navbar of the topologymodeler.
 */
@Component({
    selector: 'winery-navbar',
    templateUrl: './navbar.component.html',
    styleUrls: ['./navbar.component.css']
})
export class NavbarComponent implements OnDestroy {

    /**
     * Boolean variables that hold the state {pressed vs. !pressed} of the navbar buttons.
     * @type {boolean}
     */
    navbarButtonsState: ButtonsStateModel;
    navBarButtonsStateSubscription;
    unformattedTopologyTemplate;
    topologyTemplateSubscription;

    constructor (private alert: WineryAlertService,
                 private ngRedux: NgRedux<IWineryState>,
                 private actions: TopologyRendererActions,
                 private backendService: BackendService) {
        this.navBarButtonsStateSubscription = ngRedux.select(state => state.topologyRendererState)
            .subscribe(newButtonsState => this.setButtonsState(newButtonsState));
        this.topologyTemplateSubscription = ngRedux.select(currentState => currentState.wineryState.currentJsonTopology)
            .subscribe(topologyTemplate => this.unformattedTopologyTemplate = topologyTemplate);
    }

    /**
     * Setter for buttonstate
     * @param newButtonsState
     */
    setButtonsState (newButtonsState: ButtonsStateModel): void {
        this.navbarButtonsState = newButtonsState;
    }

    /**
     * Getter for the style of a pressed button.
     * @param buttonPressed
     */
    getStyle (buttonPressed: boolean): string {
        if (buttonPressed) {
            return '#929292';
        }
    }

    /**
     * This function is called whenever a navbar button is clicked.
     * It contains a separate case for each button.
     * It toggles the `pressed` state of a button and publishes the respective
     * button {id and boolean} to the subscribers of the Observable inside
     * SharedNodeNavbarService.
     * @param event -- The click event of a button.
     */
    toggleButton (event) {
        switch (event.target.id) {
            case 'targetLocations': {
                this.ngRedux.dispatch(this.actions.toggleTargetLocations());
                break;
            }
            case 'policies': {
                this.ngRedux.dispatch(this.actions.togglePolicies());
                break;
            }
            case 'requirementsCapabilities': {
                this.ngRedux.dispatch(this.actions.toggleRequirementsCapabilities());
                break;
            }
            case 'deploymentArtifacts': {
                this.ngRedux.dispatch(this.actions.toggleDeploymentArtifacts());
                break;
            }
            case 'properties': {
                this.ngRedux.dispatch(this.actions.toggleProperties());
                break;
            }
            case 'types': {
                this.ngRedux.dispatch(this.actions.toggleTypes());
                break;
            }
            case 'ids': {
                this.ngRedux.dispatch(this.actions.toggleIds());
                break;
            }
            case 'layout': {
                this.ngRedux.dispatch(this.actions.executeLayout());
                break;
            }
            case 'alignh': {
                this.ngRedux.dispatch(this.actions.executeAlignH());
                break;
            }
            case 'alignv': {
                this.ngRedux.dispatch(this.actions.executeAlignV());
            }
        }
    }

    /**
     * Calls the BackendService's saveTopologyTemplate method and displays a success message if successful.
     */
    saveTopologyTemplateToRepository () {
        // Initialization
        let currentTopologyTemplateFromTheRepository = {
            nodeTemplates: [],
            relationshipTemplates: []
        };
        // subsciption first
        this.backendService.serviceTemplate$.subscribe(data => {
            currentTopologyTemplateFromTheRepository = data;
            console.log(currentTopologyTemplateFromTheRepository);
        });
        // call second
        this.backendService.requestServiceTemplate();
        // Prepare for saving by updating the existing topology with the current topology state inside the Redux store
        currentTopologyTemplateFromTheRepository.nodeTemplates = this.unformattedTopologyTemplate.nodeTemplates;
        currentTopologyTemplateFromTheRepository.relationshipTemplates = this.unformattedTopologyTemplate.relationshipTemplates;
        // remove the "Color" field from all nodeTemplates as the REST Api does not recognize it.
        currentTopologyTemplateFromTheRepository.nodeTemplates.map(nodeTemplate => delete nodeTemplate.color);
        const topologyToBeSaved = currentTopologyTemplateFromTheRepository;

        const response = null;
        this.backendService.saveTopologyTemplate(topologyToBeSaved)
            .subscribe(res => window.alert(res));
        response ? this.alert.success('Successfully saved!') : this.alert.info('Something went wrong!');
    }

    /**
     * Angular lifecycle event.
     */
    ngOnDestroy () {
        this.navBarButtonsStateSubscription.unsubscribe();
    }
}
