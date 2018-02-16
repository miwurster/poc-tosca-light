import { Component, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { QName } from '../../qname';
import { hostURL } from '../../configuration';
import { EntitiesModalService, OpenModalEvent } from '../../canvas/entities-modal/entities-modal.service';
import { ModalVariant } from '../../canvas/entities-modal/modal-model';

@Component({
    selector: 'winery-toscatype-table',
    templateUrl: './toscatype-table.component.html',
    styleUrls: ['./toscatype-table.component.css']
})
export class ToscatypeTableComponent implements OnInit, OnChanges {

    @Input() toscaType: string;
    @Input() currentNodeData: any;
    @Input() toscaTypeData: any;
    currentToscaTypeData;
    currentToscaType;
    latestNodeTemplate?: any = {};

    constructor(private entitiesModalService: EntitiesModalService) {
    }

    ngOnInit() {
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes['toscaTypeData']) {
            this.currentToscaTypeData = this.toscaTypeData;
            console.log(changes);
        }
        if (changes['toscaType']) {
            this.currentToscaType = this.toscaType;
        }
    }

    getLocalName(qName?: string): string {
        const qNameVar = new QName(qName);
        return qNameVar.localName;
    }

    getNamespace(qName?: string): string {
        const qNameVar = new QName(qName);
        return qNameVar.nameSpace;
    }

    clickArtifactRef(artifactRef: string) {
        const url = hostURL
            + '/#/artifacttemplates/'
            + encodeURIComponent(encodeURIComponent(this.getNamespace(artifactRef)))
            + '/' + this.getLocalName(artifactRef);
        window.open(url, '_blank');
    }

    clickArtifactType(artifactType: string) {
        const url = hostURL
            + '/#/artifacttypes/'
            + encodeURIComponent(encodeURIComponent(this.getNamespace(artifactType)))
            + '/' + this.getLocalName(artifactType);
        window.open(url, '_blank');
    }

    clickPolicyRef(policyRef: string) {
        const url = hostURL
            + '/#/policytemplates/'
            + encodeURIComponent(encodeURIComponent(this.getNamespace(policyRef)))
            + '/' + this.getLocalName(policyRef);
        window.open(url, '_blank');
    }

    clickPolicyType(policyType: string) {
        const url = hostURL
            + '/#/policytypes/'
            + encodeURIComponent(encodeURIComponent(this.getNamespace(policyType)))
            + '/' + this.getLocalName(policyType);
        window.open(url, '_blank');
    }

    openPolicyModal(policy) {
        let qName;
        let namespace = '';
        let templateName = '(none)';
        try {
            qName = new QName(policy.policyRef);
            namespace = qName.nameSpace;
            templateName = qName.localName;
        } catch (e) {
            console.log(e);
        }
        const typeQName = new QName(policy.policyType);
        const type = typeQName.localName;
        const name = policy.name;
        const currentNodeId = this.currentNodeData.currentNodeId;
        // push new event onto Subject
        const eventObject: OpenModalEvent = new OpenModalEvent(currentNodeId, ModalVariant.Policies, name, templateName, namespace, type);
        this.entitiesModalService.openModalEvent.next(eventObject);
    }

    openDeploymentArtifactModal(deploymentArtifact) {
        let qName;
        let namespace;
        let templateName = '(none)';
        try {
            qName = new QName(deploymentArtifact.artifactRef);
            namespace = qName.nameSpace;
            templateName = qName.localName;
        } catch (e) {
            console.log(e);
        }
        const typeQName = new QName(deploymentArtifact.artifactType);
        const type = typeQName.localName;
        const name = deploymentArtifact.name;
        const currentNodeId = this.currentNodeData.currentNodeId;
        // push new event onto Subject
        const eventObject: OpenModalEvent = new OpenModalEvent(currentNodeId, ModalVariant.DeploymentArtifacts, name, templateName, namespace, type);
        this.entitiesModalService.openModalEvent.next(eventObject);
    }

}
