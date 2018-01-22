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

/**
 * Encompasses the artifacts data defined by the user when using the modal
 */
export class ArtifactsModalData {

    constructor(public id?: string,
                public artifactName?: string,
                public artifactType?: string,
                public artifactTypes?: string[],
                public artifactTemplates?: string[],
                public artifactTemplateName?: any,
                public artifactTemplateNameSpace?: any,
                public deploymentArtifacts?: any) { }
}
