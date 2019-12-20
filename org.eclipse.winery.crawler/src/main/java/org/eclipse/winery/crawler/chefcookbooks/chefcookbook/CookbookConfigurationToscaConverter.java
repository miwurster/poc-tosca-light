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

package org.eclipse.winery.crawler.chefcookbooks.chefcookbook;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.xml.namespace.QName;

import org.eclipse.winery.common.configuration.Environments;
import org.eclipse.winery.common.ids.definitions.CapabilityTypeId;
import org.eclipse.winery.common.ids.definitions.NodeTypeId;
import org.eclipse.winery.common.ids.definitions.RequirementTypeId;
import org.eclipse.winery.model.tosca.TCapabilityDefinition;
import org.eclipse.winery.model.tosca.TCapabilityType;
import org.eclipse.winery.model.tosca.TNodeType;
import org.eclipse.winery.model.tosca.TRequirementDefinition;
import org.eclipse.winery.model.tosca.TRequirementType;
import org.eclipse.winery.repository.backend.IRepository;
import org.eclipse.winery.repository.backend.RepositoryFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.winery.common.version.WineryVersion.WINERY_NAME_FROM_VERSION_SEPARATOR;
import static org.eclipse.winery.common.version.WineryVersion.WINERY_VERSION_PREFIX;
import static org.eclipse.winery.common.version.WineryVersion.WINERY_VERSION_SEPARATOR;

public class CookbookConfigurationToscaConverter {

    private final Logger LOGGER = LoggerFactory.getLogger(CookbookConfigurationToscaConverter.class);
    private final IRepository repository = RepositoryFactory.getRepository(new File(Environments.getRepositoryConfig().getRepositoryRoot()).toPath());

    public List<TNodeType> convertCookbookConfigurationToToscaNode(ChefCookbookConfiguration cookbookConfiguration, int counter) {
        List<TNodeType> nodeTypes = new ArrayList<>();
        String cookbookName = cookbookConfiguration.getName();
        String version = getVersion(cookbookConfiguration);

        String namespace = buildNamespaceForCookbookConfigs(cookbookName, version);

        TNodeType.Builder nodeTypeBuilder = new TNodeType.Builder(cookbookName + WINERY_VERSION_SEPARATOR + WINERY_VERSION_PREFIX + counter);
        nodeTypeBuilder.setTargetNamespace(namespace);

        TRequirementDefinition platform = convertPlatformToRequirement(cookbookConfiguration.getSupports(), namespace);
        nodeTypeBuilder.addRequirementDefinitions(platform);

        TCapabilityDefinition installedPackage;
        List<TCapabilityDefinition> installedPackages = convertInstalledPackagesToCapabilities(cookbookConfiguration.getInstalledPackages(), namespace);
        for (TCapabilityDefinition aPackage : installedPackages) {
            installedPackage = aPackage;
            nodeTypeBuilder.addCapabilityDefinitions(installedPackage);
        }

        TRequirementDefinition requiredPackage;
        List<TRequirementDefinition> requiredPackages = convertRequiredPackagesToRequirements(cookbookConfiguration.getRequiredPackages(), namespace);

        for (TRequirementDefinition aPackage : requiredPackages) {
            requiredPackage = aPackage;
            nodeTypeBuilder.addRequirementDefinitions(requiredPackage);
        }

        TNodeType platformNodeType = convertPlatformToNodeType(cookbookConfiguration.getSupports());

        nodeTypes.add(nodeTypeBuilder.build());
        nodeTypes.add(platformNodeType);
        return nodeTypes;
    }

    public void saveToscaNodeType(TNodeType tNodeType) {
        try {
            repository.setElement(new NodeTypeId(tNodeType.getQName()), tNodeType);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<TCapabilityDefinition> convertInstalledPackagesToCapabilities(LinkedHashMap<String, ChefPackage> installedPackages, String namespace) {
        List<TCapabilityDefinition> packageCapabilities = new ArrayList<>();
        for (int i = 0; i < installedPackages.size(); i++) {
            ChefPackage chefPackage = ChefCookbookConfiguration.getPackageByIndex(installedPackages, i);
            QName capabilityType = getCapabilityType(chefPackage.getPackageName(), getVersion(chefPackage), namespace, i);
            packageCapabilities.add(new TCapabilityDefinition.Builder(chefPackage.getPackageName(), capabilityType).build());
        }
        return packageCapabilities;
    }

    private List<TRequirementDefinition> convertRequiredPackagesToRequirements(LinkedHashMap<String, ChefPackage> requiredPackages, String namespace) {
        List<TRequirementDefinition> packageRequirements = new ArrayList<>();
        for (int i = 0; i < requiredPackages.size(); i++) {
            ChefPackage chefPackage = ChefCookbookConfiguration.getPackageByIndex(requiredPackages, i);
            QName requirementType = getRequirementType(chefPackage.getPackageName(), getVersion(chefPackage), namespace, i);
            packageRequirements.add(new TRequirementDefinition.Builder(chefPackage.getPackageName(), requirementType).build());
        }
        return packageRequirements;
    }

    private TRequirementDefinition convertPlatformToRequirement(Platform platform, String namespace) {
        TRequirementDefinition.Builder builder = new TRequirementDefinition.Builder(
            "supported platform",
            getRequirementType(platform.getName(), getVersion(platform), namespace, 1)
        );
        return new TRequirementDefinition(builder);
    }

    private TCapabilityDefinition convertPlatformToCapability(Platform platform, String namespace) {
        TCapabilityDefinition.Builder builder = new TCapabilityDefinition.Builder(
            "platform",
            this.getCapabilityType(platform.getName(), getVersion(platform), namespace, 1)
        );
        return new TCapabilityDefinition(builder);
    }

    private QName getCapabilityType(String name, String version, String namespace, int wineryVersion) {
        QName qName = new QName(namespace,
            name + WINERY_NAME_FROM_VERSION_SEPARATOR + version + WINERY_VERSION_SEPARATOR + WINERY_VERSION_PREFIX + wineryVersion);
        CapabilityTypeId id = new CapabilityTypeId(qName);

        if (!repository.exists(id)) {
            TCapabilityType capabilityType = new TCapabilityType();
            capabilityType.setName(qName.getLocalPart());
            capabilityType.setTargetNamespace(qName.getNamespaceURI());

            try {
                repository.setElement(id, capabilityType);
            } catch (IOException e) {
                LOGGER.debug("Could not persist CapabilityType {}", capabilityType, e);
            }
        }

        return qName;
    }

    private QName getRequirementType(String name, String version, String namespace, int wineryVersion) {
        QName requirementTypeQName = new QName(namespace,
            name + "-Req" + WINERY_NAME_FROM_VERSION_SEPARATOR + version + WINERY_VERSION_SEPARATOR + WINERY_VERSION_PREFIX + wineryVersion);
        QName capabilityType = this.getCapabilityType(name, version, namespace, wineryVersion);

        RequirementTypeId requirementTypeId = new RequirementTypeId(requirementTypeQName);

        if (!repository.exists(requirementTypeId)) {
            TRequirementType requirementType = new TRequirementType();
            requirementType.setName(requirementTypeQName.getLocalPart());
            requirementType.setTargetNamespace(requirementTypeQName.getNamespaceURI());
            requirementType.setRequiredCapabilityType(capabilityType);

            try {
                repository.setElement(requirementTypeId, requirementType);
            } catch (IOException e) {
                LOGGER.debug("Could not persist RequirementType {}", requirementType, e);
            }
        }

        return requirementTypeQName;
    }

    /**
     * Build namespace of nodetype
     */
    private String buildNamespaceForCookbookConfigs(String cookbookName, String cookbookVersion) {
        return "https://supermarket.chef.io/api/v1/cookbooks/" + cookbookName + "/versions/" + cookbookVersion;
    }

    private String buildNamespaceForPlatforms() {
        return "https://supermarket.chef.io/api/v1/platforms/";
    }

    private TNodeType convertPlatformToNodeType(Platform platform) {
        String namespace = buildNamespaceForPlatforms();
        TNodeType.Builder configurationNodeType = new TNodeType.Builder(platform.getName() + WINERY_NAME_FROM_VERSION_SEPARATOR + getVersion(platform) + WINERY_VERSION_SEPARATOR + WINERY_VERSION_PREFIX + "1");
        configurationNodeType.setTargetNamespace(namespace);

        configurationNodeType.addCapabilityDefinitions(convertPlatformToCapability(platform, namespace));

        return new TNodeType(configurationNodeType);
    }

    private String getVersion(VersionedChefElement element) {
        return element.getVersion() != null
            ? element.getVersion().replaceAll("/g|<|>|~|=|/", "").replaceAll(" ", "")
            : "";
    }
}
