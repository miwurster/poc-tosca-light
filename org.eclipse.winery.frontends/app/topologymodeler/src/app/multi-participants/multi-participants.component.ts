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

@Component({
    selector: 'winery-multi-participants',
    templateUrl: './multi-participants.component.html',
    styleUrls: ['./multi-participants.component.css']
})
export class MultiParticipantsComponent implements OnInit {

    readonly uiURL = encodeURIComponent(window.location.origin + window.location.pathname + '#/');
    private readonly configuration: TopologyModelerConfiguration;
    private editorConfiguration;

    constructor(private ngRedux: NgRedux<IWineryState>,
                private actions: TopologyRendererActions,
                private wineryActions: WineryActions,
                private alert: ToastrService,
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
        if (currentButtonsState.buttonsState.generatePlaceholder) {
            this.multiParticipantsService.postNewVersion().subscribe(
                response => {
                    const editorConfig = '?repositoryURL=' + this.configuration.repositoryURL
                        + '&uiURL=' + encodeURIComponent(backendBaseURL)
                        + '&ns=' + response.namespace
                        + '&id=' + response.localname;
                    this.editorConfiguration = editorConfig;
                    this.multiParticipantsService.postPlaceholders(response.localname).subscribe(
                        response => {
                            this.multiParticipantsService.postParticipantsVersion(response.localname, response.namespace).subscribe(
                                resps => {
                                    window.open(this.wineryConfigurationService.configuration.endpoints.topologymodeler + this.editorConfiguration);
                                    for (const resp of resps) {
                                        const editorConfiguration = '?repositoryURL=' + this.configuration.repositoryURL
                                            + '&uiURL=' + encodeURIComponent(backendBaseURL)
                                            + '&ns=' + resp.entity.namespace
                                            + '&id=' + resp.entity.localname;
                                        window.open(this.wineryConfigurationService.configuration.endpoints.topologymodeler + editorConfiguration);
                                    }
                                },
                                error => {
                                    console.log(error);
                                }
                            );
                        },
                        error => {
                            // TODO: fix error when returning entities
                            this.multiParticipantsService.postParticipantsVersion(error.error.text).subscribe(
                                responses => {
                                    window.open(this.wineryConfigurationService.configuration.endpoints.topologymodeler + this.editorConfiguration);
                                    for (const resp of responses) {
                                        const editorConf = '?repositoryURL=' + this.configuration.repositoryURL
                                            + '&uiURL=' + encodeURIComponent(backendBaseURL)
                                            + '&ns=' + resp.entity.namespace
                                            + '&id=' + resp.entity.localname;
                                        window.open(this.wineryConfigurationService.configuration.endpoints.topologymodeler + editorConf);
                                    }

                                },
                                er => {
                                    console.log(er);
                                }
                            );
                        },
                    );
                },
                error => {
                    console.log(error);
                }
            );
        }
    }

    ngOnInit() {
    }

}
