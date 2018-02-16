export class ModalVariantAndState {
    modalVariant: ModalVariant;
    modalVisible: boolean;
    modalTitle: string;
}

export enum ModalVariant {
    Policies = 'policies',
    DeploymentArtifacts = 'deployment_artifacts',
    None = 'none'
}

/**
 * Encompasses the variety of values that are displayed inside and entered into the modal.
 */
export class DeploymentArtifactOrPolicyModalData {
    constructor(
        // id of the nodeTemplate the actions are performed on
        public nodeTemplateId?: string,
        // the id of
        public id?: string,
        // value of the name text field in the modal
        public modalName?: string,
        // value of the type dropdown in the modal
        public modalType?: string,
        // all DA types
        public artifactTypes?: string[],
        // all policyTypes
        public policyTypes?: string[],
        // the selected artifactTemplate or policyTemplate
        public modalTemplate?: any,
        // all artifactTemplates
        public artifactTemplates?: string[],
        // all policyTemplates
        public policyTemplates?: string[],
        // name of the selected artifactTemplate or policyTemplate
        public modalTemplateName?: any,
        // ref of the selected artifactTemplate or policyTemplate
        public modalTemplateRef?: any,
        // the selected namespace inside the modal
        public modalTemplateNameSpace?: any,
        // all namespaces
        public namespaces?: string[]
    ) {
    }
}
