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
