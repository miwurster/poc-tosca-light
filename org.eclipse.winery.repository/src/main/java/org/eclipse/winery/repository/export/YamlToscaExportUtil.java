/*******************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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

package org.eclipse.winery.repository.export;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import org.eclipse.winery.common.ids.definitions.DefinitionsChildId;
import org.eclipse.winery.common.ids.definitions.NodeTypeId;
import org.eclipse.winery.common.ids.definitions.RelationshipTypeId;
import org.eclipse.winery.common.ids.definitions.ServiceTemplateId;
import org.eclipse.winery.model.tosca.Definitions;
import org.eclipse.winery.model.tosca.TArtifacts;
import org.eclipse.winery.model.tosca.TImport;
import org.eclipse.winery.model.tosca.TNodeTemplate;
import org.eclipse.winery.model.tosca.TServiceTemplate;
import org.eclipse.winery.repository.backend.BackendUtils;
import org.eclipse.winery.repository.backend.IRepository;
import org.eclipse.winery.repository.exceptions.RepositoryCorruptException;
import org.eclipse.winery.repository.export.entries.YAMLDefinitionsBasedCsarEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YamlToscaExportUtil extends ToscaExportUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(YamlToscaExportUtil.class);

    @Override
    protected Collection<DefinitionsChildId> processDefinitionsElement(IRepository repository, DefinitionsChildId tcId, CsarContentProperties definitionsFileProperties)
        throws RepositoryCorruptException, IOException {
        if (!repository.exists(tcId)) {
            String error = "Component instance " + tcId.toReadableString() + " does not exist.";
            LOGGER.error(error);
            throw new RepositoryCorruptException(error);
        }

        this.getPrepareForExport(repository, tcId);

        Definitions entryDefinitions = repository.getDefinitions(tcId);
        Collection<DefinitionsChildId> referencedDefinitionsChildIds = repository.getReferencedDefinitionsChildIds(tcId);

        // adjust imports: add imports of definitions to it
        Collection<TImport> imports = new ArrayList<>();
        for (DefinitionsChildId id : referencedDefinitionsChildIds) {
            this.addToImports(repository, id, imports);
        }
        entryDefinitions.getImport().addAll(imports);

        // END: Definitions modification

        this.referencesToPathInCSARMap.put(definitionsFileProperties, new YAMLDefinitionsBasedCsarEntry(entryDefinitions));

        return referencedDefinitionsChildIds;
    }

    /**
     * Prepares the given id for export. Mostly, the contained files are added to the CSAR.
     */
    private void getPrepareForExport(IRepository repository, DefinitionsChildId id) throws RepositoryCorruptException, IOException {
        if (id instanceof ServiceTemplateId) {
            this.prepareForExport(repository, (ServiceTemplateId) id);
        } else if (id instanceof RelationshipTypeId) {
            this.addVisualAppearanceToCSAR(repository, (RelationshipTypeId) id);
        } else if (id instanceof NodeTypeId) {
            this.addVisualAppearanceToCSAR(repository, (NodeTypeId) id);
        }
    }

    /**
     * Synchronizes the plan model references and adds the plans to the csar (putRefAsReferencedItemInCsar)
     */
    private void prepareForExport(IRepository repository, ServiceTemplateId id) throws IOException {
        // ensure that the plans stored locally are the same ones as stored in the definitions
        BackendUtils.synchronizeReferences(id);
        TServiceTemplate st = repository.getElement(id);

        for (TNodeTemplate n : st.getTopologyTemplate().getNodeTemplates()) {
            TArtifacts artifacts = n.getArtifacts();
            if (Objects.nonNull(artifacts)) {
                artifacts.getArtifact().forEach(a -> {
                    // TODO create entries for all artifacts 
                    a.getFile();
                });
            }
        }
    }
}
