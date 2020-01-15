/********************************************************************************
 * Copyright (c) 2017-2019 Contributors to the Eclipse Foundation
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

import { Component, ElementRef, Input, OnDestroy, TemplateRef, ViewChild } from '@angular/core';
import { animate, style, transition, trigger } from '@angular/animations';
import { ToastrService } from 'ngx-toastr';
import { NgRedux } from '@angular-redux/store';
import { TopologyRendererActions } from '../redux/actions/topologyRenderer.actions';
import { IWineryState } from '../redux/store/winery.store';
import { BackendService } from '../services/backend.service';
import { Subscription } from 'rxjs';
import { Hotkey, HotkeysService } from 'angular2-hotkeys';
import { TopologyRendererState } from '../redux/reducers/topologyRenderer.reducer';
import { WineryActions } from '../redux/actions/winery.actions';
import { StatefulAnnotationsService } from '../services/statefulAnnotations.service';
import { FeatureEnum } from '../../../../tosca-management/src/app/wineryFeatureToggleModule/wineryRepository.feature.direct';
import { TopologyTemplateUtil } from '../models/topologyTemplateUtil';

/**
 * The navbar of the topologymodeler.
 */
@Component({
    selector: 'winery-navbar',
    templateUrl: './navbar.component.html',
    styleUrls: ['./navbar.component.css'],
    animations: [
        trigger('navbarInOut', [
            transition('void => *', [
                style({ transform: 'translateY(-100%)' }),
                animate('200ms ease-out')
            ]),
            transition('* => void', [
                animate('200ms ease-in', style({ transform: 'translateY(-100%)' }))
            ])
        ])
    ]
})
export class NavbarComponent implements OnDestroy {

    @Input() hideNavBarState: boolean;
    @Input() readonly: boolean;

    @ViewChild('exportCsarButton')
    private exportCsarButtonRef: ElementRef;

    @ViewChild('enableLiveModelingModalTemplate')
    private enableLiveModelingModalRef: TemplateRef<any>;

    @ViewChild('disableLiveModelingModalTemplate')
    private disableLiveModelingModalRef: TemplateRef<any>;

    navbarButtonsState: TopologyRendererState;
    unformattedTopologyTemplate;
    subscriptions: Array<Subscription> = [];
    exportCsarUrl: string;
    splittingOngoing: boolean;
    matchingOngoing: boolean;
    placingOngoing: boolean;
    configEnum = FeatureEnum;
    unsavedChanges: boolean;

    constructor(private alert: ToastrService,
                private ngRedux: NgRedux<IWineryState>,
                private actions: TopologyRendererActions,
                private wineryActions: WineryActions,
                private backendService: BackendService,
                private statefulService: StatefulAnnotationsService,
                private hotkeysService: HotkeysService) {
        this.subscriptions.push(ngRedux.select(state => state.topologyRendererState)
            .subscribe(newButtonsState => this.setButtonsState(newButtonsState)));
        this.subscriptions.push(ngRedux.select(currentState => currentState.wineryState.currentJsonTopology)
            .subscribe(topologyTemplate => this.unformattedTopologyTemplate = topologyTemplate));
        this.subscriptions.push(ngRedux.select(currentState => currentState.wineryState.unsavedChanges)
            .subscribe(unsavedChanges => this.unsavedChanges = unsavedChanges));
        this.hotkeysService.add(new Hotkey('mod+s', (event: KeyboardEvent): boolean => {
            event.stopPropagation();
            this.saveTopologyTemplateToRepository();
            return false; // Prevent bubbling
        }, undefined, 'Save the Topology Template'));
        this.hotkeysService.add(new Hotkey('mod+l', (event: KeyboardEvent): boolean => {
            event.stopPropagation();
            this.ngRedux.dispatch(this.actions.executeLayout());
            return false; // Prevent bubbling
        }, undefined, 'Apply the layout directive to the Node Templates'));
        this.exportCsarUrl = this.backendService.serviceTemplateURL + '/?csar';
    }

    /**
     * Setter for buttonstate
     * @param newButtonsState
     */
    setButtonsState(newButtonsState: TopologyRendererState): void {
        this.navbarButtonsState = newButtonsState;
        if (!this.navbarButtonsState.buttonsState.splitTopologyButton) {
            this.splittingOngoing = false;
        }
        if (!this.navbarButtonsState.buttonsState.matchTopologyButton) {
            this.matchingOngoing = false;
        }
        if (!this.navbarButtonsState.buttonsState.placeComponentsButton) {
            this.placingOngoing = false;
        }
    }

    /**
     * Getter for the style of a pressed button.
     * @param buttonPressed
     */
    getStyle(buttonPressed: boolean): string {
        if (buttonPressed) {
            return '#AAEEAA';
        }
    }

    /**
     * Exports the service template as a CSAR file
     * @param event
     */
    exportCsar(event) {
        let url = this.exportCsarUrl;
        if (event.ctrlKey) {
            url = url.replace(/csar$/, 'definitions');
            console.log(url);
        }
        window.open(url);
    }

    /**
     * This function is called whenever a navbar button is clicked.
     * It contains a separate case for each button.
     * It toggles the `pressed` state of a button and publishes the respective
     * button id and boolean to the subscribers of the Observable inside
     * SharedNodeNavbarService.
     * @param event -- The click event of a button.
     */
    toggleButton(event) {
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
                break;
            }
            case 'importTopology': {
                this.ngRedux.dispatch(this.actions.importTopology());
                break;
            }
            case 'threatModeling': {
                this.ngRedux.dispatch(this.actions.threatModeling());
                break;
            }
            case 'split': {
                this.ngRedux.dispatch(this.actions.splitTopology());
                this.splittingOngoing = true;
                break;
            }
            case 'match': {
                this.ngRedux.dispatch(this.actions.matchTopology());
                this.matchingOngoing = true;
                break;
            }
            case 'problemdetection': {
                this.ngRedux.dispatch(this.actions.detectProblems());
                break;
            }
            case 'enrichment': {
                this.ngRedux.dispatch(this.actions.enrichNodeTemplates());
                break;
            }
            case 'substituteTopology':
                this.ngRedux.dispatch(this.actions.substituteTopology());
                break;
            case 'refineTopology':
                this.readonly = true;
                this.ngRedux.dispatch(this.wineryActions.sendPaletteOpened(false));
                this.ngRedux.dispatch(this.actions.refineTopology());
                break;
            case 'refineTopologyWithTests':
                this.readonly = true;
                this.ngRedux.dispatch(this.wineryActions.sendPaletteOpened(false));
                this.ngRedux.dispatch(this.actions.addTestRefinements());
                break;
            case 'determineStatefulComponents':
                this.ngRedux.dispatch(this.actions.determineStatefulComponents());
                break;
            case 'determineFreezableComponents':
                this.ngRedux.dispatch(this.actions.determineFreezableComponents());
                break;
            case 'cleanFreezableComponents':
                this.ngRedux.dispatch(this.actions.cleanFreezableComponents());
                break;
            case 'placement':
                this.ngRedux.dispatch(this.actions.placeComponents());
                this.placingOngoing = true;
                break;
            case 'test':
                this.ngRedux.dispatch(this.wineryActions.checkForUnsavedChanges());
                break;
        }
    }

    /**
     * Calls the BackendService's saveTopologyTemplate method and displays a success message if successful.
     */
    saveTopologyTemplateToRepository() {
        this.ngRedux.dispatch(this.wineryActions.setOverlayContent('Saving topology template. This may take a while.'));
        this.ngRedux.dispatch(this.wineryActions.setOverlayVisibility(true));
        this.backendService.saveTopologyTemplate(this.unformattedTopologyTemplate)
            .subscribe(res => {
                if (res.ok) {
                    this.alert.success('<p>Saved the topology!<br>' + 'Response Status: '
                        + res.statusText + ' ' + res.status + '</p>');
                    this.ngRedux.dispatch(this.wineryActions.saveTopologyTemplate());
                    this.ngRedux.dispatch(this.wineryActions.checkForUnsavedChanges());
                } else {
                    this.alert.info('<p>Something went wrong! <br>' + 'Response Status: '
                        + res.statusText + ' ' + res.status + '</p>');
                }
            }, err => this.alert.error(err.error))
            .add(() => {
                this.ngRedux.dispatch(this.wineryActions.setOverlayVisibility(false));
            });
    }

    /**
     * Angular lifecycle event.
     */
    ngOnDestroy() {
        this.subscriptions.forEach(subscription => subscription.unsubscribe());
    }

    openManagementUi() {
        window.open(this.backendService.serviceTemplateUiUrl, '_blank');
    }
}
