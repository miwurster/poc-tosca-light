import { Component, OnInit } from '@angular/core';
import { NgRedux } from '@angular-redux/store';
import { IWineryState } from '../redux/store/winery.store';
import { TopologyRendererActions } from '../redux/actions/topologyRenderer.actions';
import { WineryActions } from '../redux/actions/winery.actions';
import { ToastrService } from 'ngx-toastr';
import { MultiParticipantsService } from '../services/multi-participants.service';
import { TopologyRendererState } from '../redux/reducers/topologyRenderer.reducer';
import { WineryRepositoryConfigurationService } from '../../../../tosca-management/src/app/wineryFeatureToggleModule/WineryRepositoryConfiguration.service';
import { backendBaseURL } from '../../../../tosca-management/src/app/configuration';
import { TopologyModelerConfiguration } from '../models/topologyModelerConfiguration';
import { BackendService } from '../services/backend.service';
import { ErrorHandlerService } from '../services/error-handler.service';

@Component({
    selector: 'winery-multi-participants',
    templateUrl: './multi-participants.component.html'
})
export class MultiParticipantsComponent implements OnInit {

    readonly uiURL = encodeURIComponent(window.location.origin + window.location.pathname + '#/');
    private readonly configuration: TopologyModelerConfiguration;
    private editorConfiguration;

    constructor(private ngRedux: NgRedux<IWineryState>,
                private actions: TopologyRendererActions,
                private wineryActions: WineryActions,
                private alert: ToastrService,
                private errorHandlerService: ErrorHandlerService,
                private multiParticipantsService: MultiParticipantsService,
                private wineryConfigurationService: WineryRepositoryConfigurationService,
                private backendService: BackendService) {
        this.configuration = backendService.configuration;
        this.ngRedux.select(state => state.topologyRendererState).subscribe(
            currentButtonState => this.checkButtonsState(currentButtonState)
        );
    }

    /**
     * This method checks the current button state of Winery UI to take action when the Generate Placeholder Button was
     * clicked.
     * @param currentButtonsState TopologyRendererState object containt state of Winery UI Buttons
     */
    private checkButtonsState(currentButtonsState: TopologyRendererState) {
        // check if Generate Placeholder Button is clicked
        if (currentButtonsState.buttonsState.generateGDM) {
            this.multiParticipantsService.postNewVersion('createplaceholderversion').subscribe(
                newVersion => {
                    this.alert.success('Successfully created placeholders for tolopgy template');
                    this.ngRedux.dispatch(this.actions.generatePlaceholder());
                    // build config for new version
                    const editorConfig = '?repositoryURL=' + this.configuration.repositoryURL
                        + '&uiURL=' + encodeURIComponent(backendBaseURL)
                        + '&ns=' + newVersion.namespace
                        + '&id=' + newVersion.localname;
                    this.editorConfiguration = editorConfig;
                    // add placeholders for participants to newly created version
                    this.multiParticipantsService.postPlaceholders(newVersion.localname).subscribe(
                        placeholderResponse => {
                            window.open(this.wineryConfigurationService.configuration.endpoints.topologymodeler + this.editorConfiguration);
                        },
                        error => {
                            this.errorHandlerService.handleError(error);
                        }
                    );
                },
                error => {
                    this.errorHandlerService.handleError(error);
                }
            );
        } else if (currentButtonsState.buttonsState.extractLDM) {
            // create version for each participant
            this.multiParticipantsService.postNewVersion('createparticipantsversion').subscribe(
                participantVersions => {
                    this.alert.success('Successfully extracted partner LDM');
                    this.ngRedux.dispatch(this.actions.extractLDM());
                    // open each version in a separate tab
                    for (const participantVersion of participantVersions) {
                        const editorConfiguration = '?repositoryURL=' + this.configuration.repositoryURL
                            + '&uiURL=' + encodeURIComponent(backendBaseURL)
                            + '&ns=' + participantVersion.entity.namespace
                            + '&id=' + participantVersion.entity.localname;
                        window.open(this.wineryConfigurationService.configuration.endpoints.topologymodeler + editorConfiguration);
                    }
                },
                error => {
                    this.errorHandlerService.handleError(error);
                }
            );
        } else if (currentButtonsState.buttonsState.generatePlaceholderSubs) {
            // create version with substitution stack for partner placeholder
            this.multiParticipantsService.postNewVersion('placeholdersubstitution').subscribe(
                placeholderSubstitution => {
                    this.alert.success('Successfully substituted placeholder');
                    this.ngRedux.dispatch(this.actions.generatePlaceholderSubs());
                    const editorConfig = '?repositoryURL=' + this.configuration.repositoryURL
                        + '&uiURL=' + encodeURIComponent(backendBaseURL)
                        + '&ns=' + placeholderSubstitution.namespace
                        + '&id=' + placeholderSubstitution.localname;
                    window.open(this.wineryConfigurationService.configuration.endpoints.topologymodeler + editorConfig);
                },
                error => {
                    this.errorHandlerService.handleError(error);
                }
            );
        }
    }

    ngOnInit() {
    }

}
