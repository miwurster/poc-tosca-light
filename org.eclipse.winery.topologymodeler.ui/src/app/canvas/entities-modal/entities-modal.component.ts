import {
    AfterViewInit,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnInit,
    Output,
    SimpleChanges,
    ViewChild
} from '@angular/core';
import { ModalDirective } from 'ngx-bootstrap';
import { TPolicy } from '../../models/policiesModalData';
import { TDeploymentArtifact } from '../../models/artifactsModalData';
import { QNameWithTypeApiData } from '../../generateArtifactApiData';
import { EntityTypesModel } from '../../models/entityTypesModel';
import { BackendService } from '../../backend.service';
import { IWineryState } from '../../redux/store/winery.store';
import { NgRedux } from '@angular-redux/store';
import { isNullOrUndefined } from 'util';
import { backendBaseURL, hostURL } from '../../configuration';
import { WineryActions } from '../../redux/actions/winery.actions';
import { ExistsService } from '../../exists.service';
import { WineryAlertService } from '../../winery-alert/winery-alert.service';
import { DeploymentArtifactOrPolicyModalData, ModalVariant, ModalVariantAndState } from './modal-model';
import { EntitiesModalService, OpenModalEvent } from './entities-modal.service';

@Component({
    selector: 'winery-entities-modal',
    templateUrl: './entities-modal.component.html',
    styleUrls: ['./entities-modal.component.css']
})
export class EntitiesModalComponent implements OnInit, AfterViewInit, OnChanges {

    @ViewChild('modal') public modal: ModalDirective;

    @Input() modalVariantAndState: ModalVariantAndState;
    @Input() entityTypes: EntityTypesModel;
    @Input() currentNodeData: any;

    @Output() modalDataChange = new EventEmitter<ModalVariantAndState>();

    allNamespaces;

    /*    deploymentArtifactModalData: DeploymentArtifactsModalData;
        policiesModalData: PoliciesModalData;*/

    deploymentArtifactOrPolicyModalData: DeploymentArtifactOrPolicyModalData;
    modalSelectedRadioButton = 'createArtifactTemplate';
    artifactTemplateAlreadyExists: boolean;
    // artifact creation
    artifact: QNameWithTypeApiData = new QNameWithTypeApiData();
    artifactUrl: string;
    // needed for edit and delete tasks
    modalVariantForEditDeleteTasks = '(none)';

    // this is required for some reason
    ModalVariant = ModalVariant;

    constructor(private backendService: BackendService,
                private ngRedux: NgRedux<IWineryState>,
                private actions: WineryActions,
                private existsService: ExistsService,
                private entitiesModalService: EntitiesModalService,
                private alert: WineryAlertService) {
    }

    ngOnInit() {
        this.deploymentArtifactOrPolicyModalData = new DeploymentArtifactOrPolicyModalData();
        this.entitiesModalService.requestNamespaces()
            .subscribe(
                data => {
                    this.allNamespaces = data;
                },
                error => this.alert.info((error.toString()))
            );
        this.entitiesModalService.openModalEvent.subscribe((newEvent: OpenModalEvent) => {
            try {
                this.deploymentArtifactOrPolicyModalData.artifactTypes = this.entityTypes.artifactTypes;
                this.deploymentArtifactOrPolicyModalData.policyTypes = this.entityTypes.policyTypes;
                this.deploymentArtifactOrPolicyModalData.artifactTemplates = this.entityTypes.artifactTemplates;
                this.deploymentArtifactOrPolicyModalData.policyTemplates = this.entityTypes.policyTemplates;
            } catch (e) {
                console.log(e);
            }
            this.deploymentArtifactOrPolicyModalData.nodeTemplateId = newEvent.currentNodeId;
            this.deploymentArtifactOrPolicyModalData.modalTemplateNameSpace = newEvent.modalTemplateNameSpace;
            this.deploymentArtifactOrPolicyModalData.modalTemplateName = newEvent.modalTemplateName;
            this.deploymentArtifactOrPolicyModalData.modalName = newEvent.modalName;
            this.deploymentArtifactOrPolicyModalData.modalType = newEvent.modalType;
            this.modalVariantForEditDeleteTasks = newEvent.modalVariant;
            this.modal.show();
        });
    }

    ngAfterViewInit() {
    }

    ngOnChanges(changes: SimpleChanges) {
        this.updateModal();
    }

    /**
     * Updates the modal state when needed
     */
    updateModal() {
        if (this.modalVariantAndState.modalVariant === 'policies') {
            this.modalSelectedRadioButton = 'linkpolicies';
        } else {
            this.modalSelectedRadioButton = 'createArtifactTemplate';
        }
        if (this.entityTypes !== undefined) {
            try {
                this.deploymentArtifactOrPolicyModalData.artifactTemplates = this.entityTypes.artifactTemplates;
                this.deploymentArtifactOrPolicyModalData.artifactTypes = this.entityTypes.artifactTypes;
                this.deploymentArtifactOrPolicyModalData.policyTemplates = this.entityTypes.policyTemplates;
                this.deploymentArtifactOrPolicyModalData.policyTypes = this.entityTypes.policyTypes;
                this.deploymentArtifactOrPolicyModalData.nodeTemplateId = this.currentNodeData.id;
            } catch (e) {
                console.log(e);
            }
        }
        if (this.modalVariantAndState.modalVisible) {
            // show actual modal
            if (this.modal !== undefined) {
                this.modal.show();
            }
        }
    }

    loggerino(thisshiet) {
        console.log(thisshiet);
    }

    /**
     * This method gets called when the add button is pressed inside the "Add Deployment Artifact" modal
     */
    addDeploymentArtifactOrPolicy() {
        if (this.modalSelectedRadioButton === 'createArtifactTemplate') {
            this.artifact.localname = this.deploymentArtifactOrPolicyModalData.modalTemplateName;
            this.artifact.namespace = this.deploymentArtifactOrPolicyModalData.modalTemplateNameSpace;
            this.artifact.type = this.deploymentArtifactOrPolicyModalData.modalType;
            const deploymentArtifactToBeSavedToRedux: TDeploymentArtifact = new TDeploymentArtifact(
                [],
                [],
                {},
                this.deploymentArtifactOrPolicyModalData.modalName,
                this.deploymentArtifactOrPolicyModalData.modalType,
                this.deploymentArtifactOrPolicyModalData.modalTemplateRef
            );
            // POST to the backend
            this.backendService.createNewArtifact(this.artifact)
                .subscribe(res => {
                    if (res.ok === true) {
                        this.alert.success('<p>Saved the Deployment Artifact!<br>' + 'Response Status: '
                            + res.statusText + ' ' + res.status + '</p>');
                        // if saved successfully to backend, also add to topologyTemplate
                        this.saveDeploymentArtifactsToModel(deploymentArtifactToBeSavedToRedux);
                    } else {
                        this.alert.info('<p>Something went wrong! The DA was not added to the Topology Template!<br>' + 'Response Status: '
                            + res.statusText + ' ' + res.status + '</p>');
                    }
                });
        } else if (this.modalSelectedRadioButton === 'link' + 'deployment_artifacts') {
            // with artifactRef
            const deploymentArtifactToBeSavedToRedux: TDeploymentArtifact = new TDeploymentArtifact(
                [],
                [],
                {},
                this.deploymentArtifactOrPolicyModalData.modalName,
                this.deploymentArtifactOrPolicyModalData.modalType,
                this.deploymentArtifactOrPolicyModalData.modalTemplateRef
            );
            this.saveDeploymentArtifactsToModel(deploymentArtifactToBeSavedToRedux);
        } else if (this.modalSelectedRadioButton === 'skip' + 'deployment_artifacts') {
            // without artifactRef
            const deploymentArtifactToBeSavedToRedux: TDeploymentArtifact = new TDeploymentArtifact(
                [],
                [],
                {},
                this.deploymentArtifactOrPolicyModalData.modalName,
                this.deploymentArtifactOrPolicyModalData.modalType
            );
            this.saveDeploymentArtifactsToModel(deploymentArtifactToBeSavedToRedux);
        } else if (this.modalSelectedRadioButton === 'link' + 'policies') {
            const policyToBeAddedToRedux: TPolicy = new TPolicy(
                this.deploymentArtifactOrPolicyModalData.modalName,
                this.deploymentArtifactOrPolicyModalData.modalTemplateRef,
                this.deploymentArtifactOrPolicyModalData.modalType,
                [],
                [],
                {});
            this.savePoliciesToModel(policyToBeAddedToRedux);
        } else if (this.modalSelectedRadioButton === 'skip' + 'policies') {
            // without policyRef
            const policyToBeAddedToRedux: TPolicy = new TPolicy(
                this.deploymentArtifactOrPolicyModalData.modalName,
                null,
                this.deploymentArtifactOrPolicyModalData.modalType,
                [],
                [],
                {});
            this.savePoliciesToModel(policyToBeAddedToRedux);
        }
        /*this.newArtifact.operationName = this.deploymentArtifactSelectedOperation;
        this.createNewDeploymentArtifact();*/
        this.resetModalData();
        this.modal.hide();
    }

    /**
     * Auto-completes other relevant values when a deployment-artifact or policy type has been selected in
     * the modal
     */
    onChangeArtifactTypeOrPolicyTypeInModal(artifactTypeOrPolicyType: any, variant: string): void {
        let artifactTypesOrPolicyTypes: string;
        variant === 'deployment_artifacts' ? artifactTypesOrPolicyTypes = 'artifactTypes' : artifactTypesOrPolicyTypes = 'policyTypes';
        // change the ones affected
        this.entityTypes[artifactTypesOrPolicyTypes].some(currentlySelectedOne => {
            if (currentlySelectedOne.name === artifactTypeOrPolicyType) {
                this.deploymentArtifactOrPolicyModalData.id = currentlySelectedOne.id;
                this.deploymentArtifactOrPolicyModalData.modalName = currentlySelectedOne.id;
                this.deploymentArtifactOrPolicyModalData.modalType = artifactTypeOrPolicyType;
                return true;
            }
        });
    }

    /**
     * This is required to figure out which templateName and Ref have to be pushed to the redux state
     * @param template - either an artifactTemplate or a policyTemplate
     */
    updatedTemplateToBeLinkedInModal(template) {
        const templateObject: any = JSON.parse(template);
        this.deploymentArtifactOrPolicyModalData.modalTemplateNameSpace = templateObject.namespace;
        this.deploymentArtifactOrPolicyModalData.modalTemplateName = templateObject.name;
        this.deploymentArtifactOrPolicyModalData.modalTemplateRef = templateObject.qName;
        this.deploymentArtifactOrPolicyModalData.modalType = templateObject.full.serviceTemplateOrNodeTypeOrNodeTypeImplementation[0].type;
    }

    checkIfArtifactTemplateAlreadyExists(event: any, changedField: string) {
        if (changedField === 'templateName') {
            this.deploymentArtifactOrPolicyModalData.modalTemplateName = event.target.value;
        } else if (changedField === 'namespace') {
            this.deploymentArtifactOrPolicyModalData.modalTemplateNameSpace = event.target.value;
        }
        if (!isNullOrUndefined(this.deploymentArtifactOrPolicyModalData.modalTemplateNameSpace &&
            this.deploymentArtifactOrPolicyModalData.modalTemplateName)) {
            this.deploymentArtifactOrPolicyModalData.modalTemplateRef = '{' +
                this.deploymentArtifactOrPolicyModalData.modalTemplateNameSpace + '}' + this.deploymentArtifactOrPolicyModalData.modalTemplateName;
            const url = backendBaseURL + '/artifacttemplates/'
                + encodeURIComponent(encodeURIComponent(this.deploymentArtifactOrPolicyModalData.modalTemplateNameSpace)) + '/'
                + this.deploymentArtifactOrPolicyModalData.modalTemplateName + '/';
            this.existsService.check(url)
                .subscribe(
                    data => this.artifactTemplateAlreadyExists = true,
                    error => this.artifactTemplateAlreadyExists = false
                );
        }
    }

    resetDeploymentArtifactOrPolicyModalData(): void {
        this.deploymentArtifactOrPolicyModalData.modalTemplateNameSpace = '';
        this.deploymentArtifactOrPolicyModalData.modalTemplateName = '';
        this.deploymentArtifactOrPolicyModalData.modalName = '';
        this.deploymentArtifactOrPolicyModalData.modalType = '';
        this.resetModalData();
        this.modal.hide();
    }

    /**
     * Saves a deployment artifacts template to the model and gets pushed into the Redux state of the application
     */
    saveDeploymentArtifactsToModel(deploymentArtifactToBeSavedToRedux: TDeploymentArtifact): void {
        const actionObject = {
            nodeId: this.currentNodeData.id,
            newDeploymentArtifact: deploymentArtifactToBeSavedToRedux
        };
        this.ngRedux.dispatch(this.actions.setDeploymentArtifact(actionObject));
        this.resetDeploymentArtifactOrPolicyModalData();
    }

    /**
     * Saves a policy to the nodeTemplate model and gets pushed into the Redux state of the application
     */
    savePoliciesToModel(policyToBeSavedToRedux: TPolicy): void {
        const actionObject = {
            nodeId: this.currentNodeData.id,
            newPolicy: policyToBeSavedToRedux
        };
        this.ngRedux.dispatch(this.actions.setPolicy(actionObject));
        this.resetDeploymentArtifactOrPolicyModalData();
    }

    // util functions
    getHostUrl(): string {
        return hostURL;
    }

    resetModalData() {
        // reset variant to none and hide
        this.modalVariantAndState.modalVariant = ModalVariant.None;
        this.modalDataChange.emit(this.modalVariantAndState);
    }

    deleteDeploymentArtifactOrPolicy() {}

    private makeArtifactUrl() {
        this.artifactUrl = backendBaseURL + '/artifacttemplates/' + encodeURIComponent(encodeURIComponent(
            this.deploymentArtifactOrPolicyModalData.modalTemplateNameSpace))
            + '/' + this.deploymentArtifactOrPolicyModalData.modalTemplateName + '/';
        // TODO: add upload ability "this.uploadUrl = this.artifactUrl + 'files/';"
    }

}

