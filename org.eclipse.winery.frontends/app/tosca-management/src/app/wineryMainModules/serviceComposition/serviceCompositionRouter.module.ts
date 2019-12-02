import { ToscaTypes } from '../../model/enums';
import { RouterModule, Routes } from '@angular/router';
import { SectionComponent } from '../../section/section.component';
import { SectionResolver } from '../../section/section.resolver';
import { InstanceComponent } from '../../instance/instance.component';
import { InstanceResolver } from '../../instance/instance.resolver';
import { WineryReadmeComponent } from '../../wineryReadmeModule/wineryReadme.component';
import { WineryLicenseComponent } from '../../wineryLicenseModule/wineryLicense.component';
import { FilesComponent } from '../../instance/artifactTemplates/filesTag/files.component';
import { SourceComponent } from '../../instance/sharedComponents/artifactSource/source.component';
import { PropertiesComponent } from '../../instance/sharedComponents/properties/properties.component';
import { PropertyConstraintsComponent } from '../../instance/serviceTemplates/boundaryDefinitions/propertyConstraints/propertyConstraints.component';
import { DocumentationComponent } from '../../instance/sharedComponents/documentation/documentation.component';
import { EditXMLComponent } from '../../instance/sharedComponents/editXML/editXML.component';
import { NgModule } from '@angular/core';
import { TopologyTemplateComponent } from '../../instance/sharedComponents/topologyTemplate/topologyTemplate.component';
import { RelationMappingsComponent } from '../../instance/refinementModels/relationshipMappings/relationMappings.component';

/*******************************************************************************
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
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
 *******************************************************************************/

const toscaType = ToscaTypes.ServiceComposition;

const serviceCompositionRoutes: Routes = [
    { path: toscaType, component: SectionComponent, resolve: { resolveData: SectionResolver } },
    { path: toscaType + '/:namespace', component: SectionComponent, resolve: { resolveData: SectionResolver } },
    {
        path: toscaType + '/:namespace/:localName',
        component: InstanceComponent,
        resolve: { resolveData: InstanceResolver },
        children: [
            { path: 'readme', component: WineryReadmeComponent },
            { path: 'license', component: WineryLicenseComponent },
            { path: 'detector', component: TopologyTemplateComponent },
            { path: 'servicefragment', component: TopologyTemplateComponent },
            { path: 'documentation', component: DocumentationComponent },
            { path: 'relationmappings', component: RelationMappingsComponent },
            { path: 'xml', component: EditXMLComponent },
            { path: '', redirectTo: 'readme', pathMatch: 'full' }
        ]
    }
];

@NgModule({
    imports: [
        RouterModule.forChild(serviceCompositionRoutes),
    ],
    exports: [
        RouterModule
    ],
    providers: [
        SectionResolver,
        InstanceResolver
    ],
})

export class ServiceCompositionRouterModule{
    
}
