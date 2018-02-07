import { AfterViewInit, Component, Input, OnChanges, OnInit, SimpleChanges, ViewChild } from '@angular/core';
import { ModalDirective } from 'ngx-bootstrap';
import { PoliciesModalData, TPolicy } from '../../models/policiesModalData';
import { DeploymentArtifactsModalData, TDeploymentArtifact } from '../../models/artifactsModalData';
import { GenerateArtifactApiData } from '../../generateArtifactApiData';
import { EntityTypesModel } from '../../models/entityTypesModel';
import { BackendService } from '../../backend.service';
import { IWineryState } from '../../redux/store/winery.store';
import { NgRedux } from '@angular-redux/store';
import { isNullOrUndefined } from 'util';
import { backendBaseURL, hostURL } from '../../configuration';
import { WineryActions } from '../../redux/actions/winery.actions';
import { ExistsService } from '../../exists.service';
import { WineryAlertService } from '../../winery-alert/winery-alert.service';

@Component({
    selector: 'winery-entities-modal',
    templateUrl: './entities-modal.component.html',
    styleUrls: ['./entities-modal.component.css']
})
export class EntitiesModalComponent implements OnInit, AfterViewInit, OnChanges {

    @ViewChild(ModalDirective) public modal: ModalDirective;

    @Input() modalData: ModalData;
    @Input() entityTypes: EntityTypesModel;
    @Input() currentNodeData: any;

    allNamespaces;

    deploymentArtifactModalData: DeploymentArtifactsModalData;
    policiesModalData: PoliciesModalData;

    // deployment artifact modal
    deploymentArtifactSelectedRadioButton = 'createArtifactTemplate';
    artifactTemplateAlreadyExists: boolean;
    artifact: GenerateArtifactApiData = new GenerateArtifactApiData();
    artifactUrl: string;

    // policies modal

    constructor(private backendService: BackendService,
                private ngRedux: NgRedux<IWineryState>,
                private actions: WineryActions,
                private existsService: ExistsService,
                private alert: WineryAlertService) {
    }

    ngOnInit() {
        this.deploymentArtifactModalData = new DeploymentArtifactsModalData();
        this.policiesModalData = new PoliciesModalData();
        this.backendService.requestNamespaces()
            .subscribe(
                data => {
                    this.allNamespaces = data;
                },
                error => this.alert.info((error.toString()))
            );
    }

    ngAfterViewInit() {
    }

    ngOnChanges(changes: SimpleChanges) {
        console.log(this.modalData);
        this.initModal();
    }

    /**
     * Initializes the correct modal
     */
    initModal() {
        switch (this.modalData.modalVariant) {
            case 'deployment_artifacts':
                try {
                    this.deploymentArtifactModalData.artifactTemplates = this.entityTypes.artifactTemplates;
                    this.deploymentArtifactModalData.artifactTypes = this.entityTypes.artifactTypes;
                } catch (e) {
                }
                this.deploymentArtifactModalData.nodeTemplateId = this.currentNodeData.id;
                break;
            case 'policies':
                try {
                    this.policiesModalData.policyTemplates = this.entityTypes.policyTemplates;
                    this.policiesModalData.policyTypes = this.entityTypes.policyTypes;
                } catch (e) {
                }
                this.policiesModalData.nodeTemplateId = this.currentNodeData.id;
                break;
        }
        if (this.modalData.modalVisible) {
            // show actual modal
            if (this.modal !== undefined) {
                this.modal.show();
            }
        }
    }

    /**
     * This method gets called when the add button is pressed inside the "Add Deployment Artifact" modal
     */
    addDeploymentArtifactConfirmed() {
        if (this.deploymentArtifactSelectedRadioButton === 'createArtifactTemplate') {
            this.artifact.autoCreateArtifactTemplate = 'true';
            this.artifact.artifactName = this.deploymentArtifactModalData.artifactName;
            this.artifact.artifactTemplateName = this.deploymentArtifactModalData.artifactTemplateName;
            this.artifact.artifactTemplateNamespace = this.deploymentArtifactModalData.artifactTemplateNameSpace;
            this.artifact.artifactType = this.deploymentArtifactModalData.artifactType;
            // POST to the backend
            // Todo: POST to the backend
            /*this.backendService.createNewArtifact(this.artifact, this.deploymentArtifactModalData.nodeTemplateId)
                .subscribe(res => {
                    res.ok === true ? this.alert.success('<p>Saved the Deployment Artifact!<br>' + 'Response Status: '
                        + res.statusText + ' ' + res.status + '</p>')
                        : this.alert.info('<p>Something went wrong! <br>' + 'Response Status: '
                        + res.statusText + ' ' + res.status + '</p>');
                });*/
            const deploymentArtifactToBeSavedToRedux: TDeploymentArtifact = new TDeploymentArtifact(
                [],
                [],
                {},
                this.deploymentArtifactModalData.artifactName,
                this.deploymentArtifactModalData.artifactType,
                this.deploymentArtifactModalData.artifactTemplateRef
            );
            this.saveDeploymentArtifactsToModel(deploymentArtifactToBeSavedToRedux);
        } else if (this.deploymentArtifactSelectedRadioButton === 'linkArtifactTemplate') {
            // with artifactRef
            const deploymentArtifactToBeSavedToRedux: TDeploymentArtifact = new TDeploymentArtifact(
                [],
                [],
                {},
                this.deploymentArtifactModalData.artifactName,
                this.deploymentArtifactModalData.artifactType,
                this.deploymentArtifactModalData.artifactTemplateRef
            );
            this.saveDeploymentArtifactsToModel(deploymentArtifactToBeSavedToRedux);
        } else if (this.deploymentArtifactSelectedRadioButton === 'skipArtifactTemplate') {
            // without artifactRef
            const deploymentArtifactToBeSavedToRedux: TDeploymentArtifact = new TDeploymentArtifact(
                [],
                [],
                {},
                this.deploymentArtifactModalData.artifactName,
                this.deploymentArtifactModalData.artifactType
            );
            this.saveDeploymentArtifactsToModel(deploymentArtifactToBeSavedToRedux);
        }
        /*this.newArtifact.operationName = this.deploymentArtifactSelectedOperation;
        this.createNewDeploymentArtifact();*/
        this.modal.hide();
    }

    /**
     * Auto-completes other deployment-artifact relevant values when a deployment-artifact type has been selected in
     * the modal
     */
    onChangeDeploymentArtifacts(artifactType: any): void {
        // change the ones affected
        this.entityTypes.artifactTypes.some(artifactCurrentlySelected => {
            if (artifactCurrentlySelected.name === artifactType) {
                this.deploymentArtifactModalData.id = artifactCurrentlySelected.id;
                this.deploymentArtifactModalData.artifactName = artifactCurrentlySelected.id;
                this.deploymentArtifactModalData.artifactType = artifactType;
                return true;
            }
        });
    }

    /**
     * This is required to figure out which artifactTemplateName and Ref have to be pushed to the redux state
     * @param artifactTemplate
     */
    updateDeploymentArtifactModalData(artifactTemplate) {
        const atObject: any = JSON.parse(artifactTemplate);
        console.log(atObject);
        console.log(artifactTemplate);
        this.deploymentArtifactModalData.artifactTemplateNameSpace = atObject.namespace;
        this.deploymentArtifactModalData.artifactTemplateName = atObject.name;
        this.deploymentArtifactModalData.artifactTemplateRef = atObject.qName;

        // if reference to download files is required
        // this.deploymentArtifactModalData.artifactTemplateRef =
        // atObject.full.serviceTemplateOrNodeTypeOrNodeTypeImplementation[0].artifactReferences.artifactReference[0].reference;
        this.deploymentArtifactModalData.artifactType = atObject.full.serviceTemplateOrNodeTypeOrNodeTypeImplementation[0].type;
        console.log(this.deploymentArtifactModalData);
    }

    checkIfArtifactTemplateAlreadyExists(event: any) {
        this.deploymentArtifactModalData.artifactTemplateName = event.target.value;
        if (!isNullOrUndefined(this.deploymentArtifactModalData.artifactTemplateNameSpace &&
            this.deploymentArtifactModalData.artifactTemplateName)) {
            const url = backendBaseURL + '/artifacttemplates/'
                + encodeURIComponent(encodeURIComponent(this.deploymentArtifactModalData.artifactTemplateNameSpace)) + '/'
                + this.deploymentArtifactModalData.artifactTemplateName + '/';
            this.existsService.check(url)
                .subscribe(
                    data => this.artifactTemplateAlreadyExists = true,
                    error => this.artifactTemplateAlreadyExists = false
                );
        }
    }

    updateSelectedNamespaceInDeploymentArtifactModalData(event: any) {
        this.deploymentArtifactModalData.artifactTemplateNameSpace = event.target.value;
    }

    /**
     *
     */
    createNewDeploymentArtifact() {
        // this.backendService.createNewArtifact(this.newArtifact).subscribe(
        //     data => this.handlePostResponse(),
        //     error => this.showError(error)
        // );
    }

    resetDeploymentArtifactModalData(): void {
        this.deploymentArtifactModalData.artifactTemplateNameSpace = '';
        this.deploymentArtifactModalData.artifactTemplateName = '';
        this.deploymentArtifactModalData.artifactName = '';
        this.deploymentArtifactModalData.artifactType = '';
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
        this.resetDeploymentArtifactModalData();
    }

    // POLICIES

    /**
     * Saves a policy template to the model and gets pushed into the Redux state of the application
     */
    savePoliciesToModel(): void {
        const policyToBeAddedToRedux: TPolicy = new TPolicy(
            this.policiesModalData.policyTemplateName,
            this.policiesModalData.policyTemplate,
            this.policiesModalData.policyType,
            [],
            [],
            {});
        const actionObject = {
            nodeId: this.currentNodeData.id,
            newPolicy: policyToBeAddedToRedux
        };
        this.ngRedux.dispatch(this.actions.setPolicy(actionObject));
        this.resetPolicies();
    }

    /**
     * Auto-completes other policy relevant values when a policy template has been selected in the modal
     */
    onChangePolicyTemplate(policyTemplate: string): void {
        this.entityTypes.policyTemplates.some(policy => {
            if (policy.id === policyTemplate) {
                this.policiesModalData.policyTemplateId = policy.id;
                this.policiesModalData.policyTemplateName = policy.name;
                this.policiesModalData.policyTemplateNamespace = policy.namespace;
                this.policiesModalData.policyTemplateQName = policy.qName;
                return true;
            }
        });
    }

    /**
     * Auto-completes other policy relevant values when a policy type has been selected in the modal
     */
    onChangePolicyType(policyType: string): void {
        this.entityTypes.policyTypes.some(policy => {
            if (policy.id === policyType) {
                this.policiesModalData.policyTypeId = policy.id;
                this.policiesModalData.policyTypeName = policy.name;
                this.policiesModalData.policyTypeNamespace = policy.namespace;
                this.policiesModalData.policyTypeQName = policy.qName;
                return true;
            }
        });
    }

    /**
     * Clears the modal values of the corresponding modal type
     */
    resetPolicies(): void {
        this.policiesModalData.policyTemplateName = '';
        this.policiesModalData.policyType = '';
        this.policiesModalData.policyTemplate = '';
        this.modal.hide();
    }

    // util functions
    getHostUrl(): string {
        return hostURL;
    }

    private makeArtifactUrl() {
        this.artifactUrl = backendBaseURL + '/artifacttemplates/' + encodeURIComponent(encodeURIComponent(
            this.deploymentArtifactModalData.artifactTemplateNameSpace)) + '/' + this.deploymentArtifactModalData.artifactTemplateName + '/';
        // TODO: add upload ability "this.uploadUrl = this.artifactUrl + 'files/';"
    }

}

export class ModalData {
    modalVariant: ModalVariant;
    modalVisible: boolean;
    modalTitle: string;
}

export enum ModalVariant {
    Policies = 'policies',
    DeploymentArtifacts = 'deployment_artifacts',
    None = 'none'
}
