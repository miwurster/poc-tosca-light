/********************************************************************************
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
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

package org.eclipse.winery.repository.security.csar;

import org.eclipse.winery.common.RepositoryFileReference;
import org.eclipse.winery.common.ids.definitions.*;
import org.eclipse.winery.model.csar.toscametafile.TOSCAMetaFileAttributes;
import org.eclipse.winery.model.tosca.*;
import org.eclipse.winery.model.tosca.constants.QNames;
import org.eclipse.winery.repository.JAXBSupport;
import org.eclipse.winery.repository.backend.BackendUtils;
import org.eclipse.winery.repository.backend.IRepository;
import org.eclipse.winery.repository.backend.RepositoryFactory;
import org.eclipse.winery.repository.backend.filebased.FilebasedRepository;
import org.eclipse.winery.repository.configuration.Environment;
import org.eclipse.winery.repository.datatypes.ids.elements.ArtifactTemplateFilesDirectoryId;
import org.eclipse.winery.repository.datatypes.ids.elements.DirectoryId;
import org.eclipse.winery.repository.export.CsarExporter;
import org.eclipse.winery.repository.export.ToscaMetaFirstBlockEntry;
import org.eclipse.winery.repository.export.ToscaMetaEntry;
import org.eclipse.winery.repository.security.csar.exceptions.GenericKeystoreManagerException;
import org.eclipse.winery.repository.security.csar.exceptions.GenericSecurityProcessorException;
import org.eclipse.winery.repository.security.csar.support.SupportedDigestAlgorithm;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.util.*;

public class SecurityRequirementsExportEnforcer {
    private SecurityProcessor securityProcessor;
    private KeystoreManager keystoreManager;
    private String digestAlgorithm;
    private IRepository repository;
    private Map<RepositoryFileReference, String> addedReferencesToPathInCSARMap;
    private Collection<DefinitionsChildId> addedIdsForImports;

    public SecurityRequirementsExportEnforcer(IRepository repository, String digestAlgorithm) {
        this.securityProcessor = new BCSecurityProcessor();
        this.keystoreManager = new JCEKSKeystoreManager();
        this.repository = repository;
        this.digestAlgorithm = digestAlgorithm;
        this.addedReferencesToPathInCSARMap = new HashMap<>();
        this.addedIdsForImports = new ArrayList<>();
    }

    public SecurityRequirementsExportEnforcer(IRepository repository) {
        this(repository, SupportedDigestAlgorithm.SHA256.name());
    }

    public Map<RepositoryFileReference, String> getAddedReferencesToPathInCSARMap() {
        return addedReferencesToPathInCSARMap;
    }

    public Collection<DefinitionsChildId> getAddedIdsForImports() {
        return addedIdsForImports;
    }

    public void enforceSecurityPolicies(DefinitionsChildId tcId, Definitions entryDefinitions, Collection<DefinitionsChildId> referencedDefinitionsChildIds) {
        // enforce properties-related policies 
        for (TExtensibleElements element : entryDefinitions.getServiceTemplateOrNodeTypeOrNodeTypeImplementation()) {
            if (element instanceof TServiceTemplate) {
                TServiceTemplate serviceTemplate = (TServiceTemplate) element;
                for (TNodeTemplate nodeTemplate : serviceTemplate.getTopologyTemplate().getNodeTemplates()) {
                    boolean signingNeeded = propertiesSigningRequired(nodeTemplate);
                    boolean encryptionNeeded = propertiesEncryptionRequired(nodeTemplate);
                    if (signingNeeded && encryptionNeeded) {
                        List<String> overlappingPropertyNames = getOverlappingProperties(nodeTemplate);
                        if (overlappingPropertyNames.size() > 0) {
                            // handle properties overlap
                            Map<String, String> nodeTemplateKVProperties = getEntityTemplateKVProperties(nodeTemplate);
                            Map<String, String> overlappingPropsDigestsBeforeEncryption = calculatePropertiesDigests(overlappingPropertyNames, nodeTemplateKVProperties);
                            enforcePropertyEncryption(nodeTemplate, referencedDefinitionsChildIds);
                            enforcePropertySigning(tcId, serviceTemplate, nodeTemplate, referencedDefinitionsChildIds, overlappingPropsDigestsBeforeEncryption);
                        } else {
                            // properties do not overlap, handle in a regular fashion
                            enforcePropertyEncryption(nodeTemplate, referencedDefinitionsChildIds);
                            enforcePropertySigning(tcId, serviceTemplate, nodeTemplate, referencedDefinitionsChildIds, null);
                        }
                    } else if (signingNeeded) {
                        // handle plain signing
                        enforcePropertySigning(tcId, serviceTemplate, nodeTemplate, referencedDefinitionsChildIds, null);
                    } else if (encryptionNeeded) {
                        // handle plain encryption
                        enforcePropertyEncryption(nodeTemplate, referencedDefinitionsChildIds);
                    }
                }
            }
        }

        // enforce files-related policies
        for (TArtifactTemplate element : entryDefinitions.getArtifactTemplates()) {
            enforceArtifactsSigningAndEncryption((ArtifactTemplateId) tcId, element, referencedDefinitionsChildIds);
        }
    }

    private boolean propertiesEncryptionRequired(TNodeTemplate nodeTemplate) {
        final TEntityType nodeType = repository.getTypeForTemplate(nodeTemplate);
        if (Objects.nonNull(nodeType.getPolicies())) {
            TPolicy encTypeLevelPolicy = nodeType.getPolicyByQName(QNames.WINERY_ENCRYPTION_POLICY_TYPE);
            TPolicy encTypeLevelPropsPolicy = nodeType.getPolicyByQName(QNames.WINERY_ENCRYPTEDPROP_POLICY_TYPE);
            // only combination of these policies can be used
            if (Objects.nonNull(encTypeLevelPolicy) && Objects.nonNull(encTypeLevelPropsPolicy)) {
                TPolicy encTemplateLevelPolicy = nodeTemplate.getPolicyByQName(QNames.WINERY_ENCRYPTION_POLICY_TYPE);
                return Objects.isNull(encTemplateLevelPolicy) || (!encTemplateLevelPolicy.getIsApplied());
            }
        }
        return false;
    }

    private boolean propertiesSigningRequired(TNodeTemplate nodeTemplate) {
        final TEntityType nodeType = repository.getTypeForTemplate(nodeTemplate);
        if (Objects.nonNull(nodeType.getPolicies())) {
            TPolicy signingTypeLevelPolicy = nodeType.getPolicyByQName(QNames.WINERY_SIGNING_POLICY_TYPE);
            TPolicy signingTypeLevelPropsPolicy = nodeType.getPolicyByQName(QNames.WINERY_SIGNEDPROP_POLICY_TYPE);
            // only combination of these policies can be used
            if (Objects.nonNull(signingTypeLevelPolicy) && Objects.nonNull(signingTypeLevelPropsPolicy)) {
                TPolicy signingTemplateLevelPolicy = nodeTemplate.getPolicyByQName(QNames.WINERY_SIGNING_POLICY_TYPE);
                return Objects.isNull(signingTemplateLevelPolicy) || (!signingTemplateLevelPolicy.getIsApplied());
            }
        }
        return false;
    }

    private List<String> getPropertyNamesForProtection(TPolicyTemplate propsPolicyTemplate) {
        String spaceSeparatedPropertyNames = getEntityTemplateKVProperty(propsPolicyTemplate, SecureCSARConstants.SEC_POL_PROPGROUPING_PROPERTY);
        if (Objects.nonNull(spaceSeparatedPropertyNames) && spaceSeparatedPropertyNames.length() > 0) {
            return new ArrayList<>(Arrays.asList(spaceSeparatedPropertyNames.split("\\s+")));
        }
        return null;
    }

    private List<String> getOverlappingProperties(TNodeTemplate nodeTemplate) {
        final TEntityType nodeType = repository.getTypeForTemplate(nodeTemplate);
        TPolicy encPropsPolicy = nodeType.getPolicyByQName(QNames.WINERY_ENCRYPTEDPROP_POLICY_TYPE);
        TPolicy signPropsPolicy = nodeType.getPolicyByQName(QNames.WINERY_SIGNEDPROP_POLICY_TYPE);
        PolicyTemplateId encPropsPolicyTemplateId = BackendUtils.getDefinitionsChildId(PolicyTemplateId.class, encPropsPolicy.getPolicyRef());
        PolicyTemplateId signPropsPolicyTemplateId = BackendUtils.getDefinitionsChildId(PolicyTemplateId.class, signPropsPolicy.getPolicyRef());

        TPolicyTemplate encPropsPolicyTemplate = repository.getElement(encPropsPolicyTemplateId);
        TPolicyTemplate signPropsPolicyTemplate = repository.getElement(signPropsPolicyTemplateId);

        List<String> propertyNamesForEncryption = getPropertyNamesForProtection(encPropsPolicyTemplate);
        List<String> propertyNamesForSigning = getPropertyNamesForProtection(signPropsPolicyTemplate);
        if (Objects.nonNull(propertyNamesForEncryption) && Objects.nonNull(propertyNamesForSigning)) {
            if (propertyNamesForEncryption.size() > propertyNamesForSigning.size()) {
                if (propertyNamesForEncryption.retainAll(propertyNamesForSigning)) {
                    return propertyNamesForEncryption;
                }
            }
            else {
                if (propertyNamesForSigning.retainAll(propertyNamesForEncryption)) {
                    return propertyNamesForSigning;
                }
            }
        }
        return new ArrayList<>();
    }

    private boolean verifyPropertiesCorrespondence(Map<String, String> actualEntityProperties, List<String> propertyNamesForProtection) {
        if (Objects.isNull(actualEntityProperties) || propertyNamesForProtection.isEmpty()) {
            return false;
        }
        List<String> actualEntityPropertyNames = new ArrayList<>(actualEntityProperties.keySet());
        for (String p : propertyNamesForProtection) {
            if (!actualEntityPropertyNames.contains(p) || actualEntityProperties.get(p).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private Map<String, String> getEntityTemplateKVProperties(TEntityTemplate entityTemplate) {
        if (Objects.nonNull(entityTemplate.getProperties())) {
            return entityTemplate.getProperties().getKVProperties();
        }
        return null;
    }

    private String getEntityTemplateKVProperty(TEntityTemplate entityTemplate, String key) {
        if (Objects.nonNull(entityTemplate.getProperties())) {
            return entityTemplate.getProperties().getKVProperties().get(key);
        }
        return null;
    }

    private void addPolicyToNodeTemplate(TNodeTemplate nodeTemplate, TPolicy policy) {
        if (nodeTemplate.getPolicies() == null) {
            nodeTemplate.setPolicies(new TNodeTemplate.Policies());
        }
        nodeTemplate.getPolicies().getPolicy().add(policy);
    }

    private void enforcePropertyEncryption(TNodeTemplate nodeTemplate, Collection<DefinitionsChildId> referencedDefinitionsChildIds) {
        // get the respective NodeType
        final TEntityType nodeType = repository.getTypeForTemplate(nodeTemplate);
        TPolicy encTypeLevelPolicy = nodeType.getPolicyByQName(QNames.WINERY_ENCRYPTION_POLICY_TYPE);
        TPolicy encTypeLevelPropsPolicy = nodeType.getPolicyByQName(QNames.WINERY_ENCRYPTEDPROP_POLICY_TYPE);

        prepareSecurityPolicyForExport(encTypeLevelPolicy, referencedDefinitionsChildIds);
        PolicyTemplateId encPropsPolicyTemplateId = prepareSecurityPolicyForExport(encTypeLevelPropsPolicy, referencedDefinitionsChildIds);
        TPolicyTemplate encPropsPolicyTemplate = repository.getElement(encPropsPolicyTemplateId);

        String keyAlias = encTypeLevelPolicy.getPolicyRef().getLocalPart();
        List<String> propertyNamesForEncryption = getPropertyNamesForProtection(encPropsPolicyTemplate);

        if (this.keystoreManager.entityExists(keyAlias) && Objects.nonNull(propertyNamesForEncryption)) {
            Map<String, String> nodeTemplateKVProperties = getEntityTemplateKVProperties(nodeTemplate);
            // only proceed if property verification is successful
            boolean propertiesAreValid = verifyPropertiesCorrespondence(nodeTemplateKVProperties, propertyNamesForEncryption);
            if (propertiesAreValid) {
                int numPropsSecured = 0;
                try {
                    Key encryptionKey = this.keystoreManager.loadKey(keyAlias);
                    for (String p : propertyNamesForEncryption) {
                        String encValue = this.securityProcessor.encryptString(encryptionKey, nodeTemplateKVProperties.get(p));
                        nodeTemplateKVProperties.replace(p, encValue);
                        numPropsSecured++;
                    }
                    nodeTemplate.getProperties().setKVProperties(nodeTemplateKVProperties);
                    if (numPropsSecured > 0) {
                        encTypeLevelPolicy.setIsApplied(true);
                        addPolicyToNodeTemplate(nodeTemplate, encTypeLevelPolicy);
                    }
                } catch (GenericKeystoreManagerException | GenericSecurityProcessorException e) {
                    e.printStackTrace();
                }
            } else {
                // properties were not set or do not match
                // TODO this is an exception, abort export
            }
        }
    }

    private void enforcePropertySigning(DefinitionsChildId tcId, TServiceTemplate serviceTemplate, TNodeTemplate nodeTemplate, Collection<DefinitionsChildId> referencedDefinitionsChildIds, Map<String, String> overlappingPropsDigestsBeforeEncryption) {
        final TEntityType nodeType = repository.getTypeForTemplate(nodeTemplate);

        TPolicy signTypeLevelPolicy = nodeType.getPolicyByQName(QNames.WINERY_SIGNING_POLICY_TYPE);
        TPolicy signTypeLevelPropsPolicy = nodeType.getPolicyByQName(QNames.WINERY_SIGNEDPROP_POLICY_TYPE);

        prepareSecurityPolicyForExport(signTypeLevelPolicy, referencedDefinitionsChildIds);
        PolicyTemplateId signPropsPolicyTemplateId = prepareSecurityPolicyForExport(signTypeLevelPropsPolicy, referencedDefinitionsChildIds);
        TPolicyTemplate signPropsPolicyTemplate = repository.getElement(signPropsPolicyTemplateId);

        String keyAlias = signTypeLevelPolicy.getPolicyRef().getLocalPart();
        List<String> propertyNamesForSigning = getPropertyNamesForProtection(signPropsPolicyTemplate);

        if (this.keystoreManager.entityExists(keyAlias) && Objects.nonNull(propertyNamesForSigning)) {
            Map<String, String> nodeTemplateKVProperties = getEntityTemplateKVProperties(nodeTemplate);
            // only proceed if property verification is successful
            boolean propertiesAreValid = verifyPropertiesCorrespondence(nodeTemplateKVProperties, propertyNamesForSigning);
            if (propertiesAreValid) {
                try {
                    Key signingKey = this.keystoreManager.loadKey(keyAlias);

                    // Generate Atempl and add it to referencedDefinitionsChildIds so that it's exported
                    String signatureATName = generateSignatureArtifactTemplateName(nodeTemplate.getId(), signPropsPolicyTemplate.getName());
                    ArtifactTemplateId signatureArtifactTemplateId = BackendUtils.generateArtifactTemplate(repository, QNames.WINERY_SIGNATURE_ARTIFACT_TYPE, signatureATName, true);
                    TArtifactTemplate signatureArtifactTemplate = repository.getElement(signatureArtifactTemplateId);
                    ArtifactTypeId signatureArtifactTypeId = BackendUtils.getDefinitionsChildId(ArtifactTypeId.class, signatureArtifactTemplate.getType());

                    // generate properties manifest, signature file, and a signature block file
                    Map<String, String> propertiesDigestsAfterEncryption = calculatePropertiesDigests(propertyNamesForSigning, nodeTemplateKVProperties);
                    String signedPropertiesManifestContent = generateSignedPropertiesManifestContent(propertiesDigestsAfterEncryption, overlappingPropsDigestsBeforeEncryption);
                    RepositoryFileReference manifestPathRef = generateSignedPropertiesManifest(signatureArtifactTemplateId, signatureArtifactTemplate, signedPropertiesManifestContent);
                    RepositoryFileReference signedPropertiesSignatureFileRef = generatePropertiesManifestSignatureFile(signatureArtifactTemplateId, signatureArtifactTemplate, manifestPathRef, propertiesDigestsAfterEncryption, overlappingPropsDigestsBeforeEncryption);
                    generatePropertiesBlockSignatureFile(signatureArtifactTemplateId, signatureArtifactTemplate, signedPropertiesSignatureFileRef, signingKey);

                    // create a deployment artifact which references a generated signature artifact template
                    // and add it to the node template
                    if (Objects.isNull(nodeTemplate.getDeploymentArtifacts())) {
                        nodeTemplate.setDeploymentArtifacts(new TDeploymentArtifacts());
                    }
                    String daName = SecureCSARConstants.DA_PREFIX.concat(signTypeLevelPropsPolicy.getName());
                    TDeploymentArtifact propsSignatureDA = new TDeploymentArtifact
                        .Builder(daName, QNames.WINERY_SIGNATURE_ARTIFACT_TYPE)
                        .setArtifactRef(signatureArtifactTemplateId.getQName())
                        .build();
                    nodeTemplate.getDeploymentArtifacts().getDeploymentArtifact().add(propsSignatureDA);

                    // signify that a policy was applied on the level of the node template
                    signTypeLevelPolicy.setIsApplied(true);
                    addPolicyToNodeTemplate(nodeTemplate, signTypeLevelPolicy);

                    // update the service template
                    BackendUtils.persist(tcId, serviceTemplate);

                    // prepare generate signature artifact for export
                    referencedDefinitionsChildIds.add(signatureArtifactTypeId);
                    referencedDefinitionsChildIds.add(signatureArtifactTemplateId);
                    this.addedIdsForImports.add(signatureArtifactTypeId);
                    this.addedIdsForImports.add(signatureArtifactTemplateId);
                } catch (GenericKeystoreManagerException | GenericSecurityProcessorException | IOException e) {
                    e.printStackTrace();
                }

            } else {
                // properties were not set or do not match
                // TODO this is an exception, abort export
            }

        }
    }

    private RepositoryFileReference generateSignedPropertiesManifest(ArtifactTemplateId sigArtifactTemplateId, TArtifactTemplate sigArtifactTemplate, String manifestContent) {
        // generate Properties Digests Manifest
        String manifestName = generateSignedPropertiesManifestName(sigArtifactTemplate.getId());
        RepositoryFileReference manifestPathRef = BackendUtils.addFileToArtifactTemplate(sigArtifactTemplateId, sigArtifactTemplate, manifestName, manifestContent);
        String manifestPath = BackendUtils.getPathInsideRepo(manifestPathRef);
        this.addedReferencesToPathInCSARMap.put(manifestPathRef, manifestPath);

        return manifestPathRef;
    }

    private RepositoryFileReference generatePropertiesManifestSignatureFile(ArtifactTemplateId sigArtifactTemplateId, TArtifactTemplate sigArtifactTemplate, RepositoryFileReference manifestPathRef, Map<String, String> propertiesDigests, Map<String, String> plainDigests) throws IOException, GenericSecurityProcessorException {
        byte[] manifestBytes = Files.readAllBytes(((FilebasedRepository) repository).ref2AbsolutePath(manifestPathRef));
        String manifestDigest = this.securityProcessor.calculateDigest(manifestBytes, this.digestAlgorithm);
        String signedPropertiesSignatureFileContent = generateSignedPropertiesSignatureFile(manifestDigest, propertiesDigests, plainDigests);
        String signedPropertiesSignatureFileName = generatePropertiesSignatureFileName(sigArtifactTemplate.getId());
        RepositoryFileReference signedPropertiesSignatureFileRef = BackendUtils.addFileToArtifactTemplate(sigArtifactTemplateId, sigArtifactTemplate, signedPropertiesSignatureFileName, signedPropertiesSignatureFileContent);
        String signedPropertiesSignatureFilePath = BackendUtils.getPathInsideRepo(signedPropertiesSignatureFileRef);
        this.addedReferencesToPathInCSARMap.put(signedPropertiesSignatureFileRef, signedPropertiesSignatureFilePath);

        return signedPropertiesSignatureFileRef;
    }

    private void generatePropertiesBlockSignatureFile(ArtifactTemplateId sigArtifactTemplateId, TArtifactTemplate sigArtifactTemplate, RepositoryFileReference signatureFile, Key signingKey) throws IOException, GenericSecurityProcessorException {
        byte[] signatureFileBytes = Files.readAllBytes(((FilebasedRepository) repository).ref2AbsolutePath(signatureFile));
        byte[] blockSignatureFileContent = this.securityProcessor.signBytes(signingKey, signatureFileBytes);
        String blockSignatureFileName = sigArtifactTemplate.getId().concat(SecureCSARConstants.ARTIFACT_SIGNEXTENSION);
        RepositoryFileReference blockSignatureFileRef = BackendUtils.addFileToArtifactTemplate(sigArtifactTemplateId, sigArtifactTemplate, blockSignatureFileName, blockSignatureFileContent);
        String blockSignatureFilePath = BackendUtils.getPathInsideRepo(blockSignatureFileRef);
        this.addedReferencesToPathInCSARMap.put(blockSignatureFileRef, blockSignatureFilePath);
    }

    private void enforceArtifactsSigningAndEncryption(ArtifactTemplateId tcId, TArtifactTemplate artifactTemplate, Collection<DefinitionsChildId> referencedDefinitionsChildIds) {
        final TPolicy signingPolicy = artifactTemplate.getSigningPolicy();
        final TPolicy encPolicy = artifactTemplate.getEncryptionPolicy();

        DirectoryId fileDir = new ArtifactTemplateFilesDirectoryId(tcId);
        Set<RepositoryFileReference> files = RepositoryFactory.getRepository().getContainedFiles(fileDir);

        if (Objects.nonNull(signingPolicy) && !signingPolicy.getIsApplied()) {
            prepareSecurityPolicyForExport(signingPolicy, referencedDefinitionsChildIds);
            TArtifactTemplate.ArtifactReferences refs = artifactTemplate.getArtifactReferences();
            if (Objects.nonNull(refs) && refs.getArtifactReference().size() > 0) {
                String signingKeyAlias = signingPolicy.getPolicyRef().getLocalPart();
                try {
                    Key signingKey = this.keystoreManager.loadKey(signingKeyAlias);
                    signFilesOfArtifactTemplate(tcId, artifactTemplate, signingKey, files, SecureCSARConstants.ARTIFACT_SIGN_MODE_PLAIN);

                    if (Objects.nonNull(encPolicy) && !encPolicy.getIsApplied()) {
                        prepareSecurityPolicyForExport(encPolicy, referencedDefinitionsChildIds);
                        String ecnryptionKeyAlias = encPolicy.getPolicyRef().getLocalPart();
                        Key encryptionKey = this.keystoreManager.loadKey(ecnryptionKeyAlias);
                        encryptFilesOfArtifactTemplate(artifactTemplate, encryptionKey, files);
                        // Sign encrypted artifact as well
                        signFilesOfArtifactTemplate(tcId, artifactTemplate, signingKey, files, SecureCSARConstants.ARTIFACT_SIGN_MODE_ENCRYPTED);
                    }

                } catch (GenericKeystoreManagerException e) {
                    e.printStackTrace();
                }
            }
        } else {
            if (Objects.nonNull(encPolicy) && !encPolicy.getIsApplied()) {
                prepareSecurityPolicyForExport(encPolicy, referencedDefinitionsChildIds);
                try {
                    String ecnryptionKeyAlias = encPolicy.getPolicyRef().getLocalPart();
                    Key encryptionKey = this.keystoreManager.loadKey(ecnryptionKeyAlias);
                    encryptFilesOfArtifactTemplate(artifactTemplate, encryptionKey, files);
                } catch (GenericKeystoreManagerException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void signFilesOfArtifactTemplate(ArtifactTemplateId tcId, TArtifactTemplate artifactTemplate, Key signingKey, Set<RepositoryFileReference> files, String mode) {
        try {
            for (RepositoryFileReference fileRef : files) {
                if (!fileRef.getFileName().contains(SecureCSARConstants.ARTIFACT_SIGN_MODE_PLAIN) || !fileRef.getFileName().contains(SecureCSARConstants.ARTIFACT_SIGN_MODE_ENCRYPTED)) {
                    byte[] fileBytes = Files.readAllBytes(((FilebasedRepository) repository).ref2AbsolutePath(fileRef));
                    byte[] blockSignatureFileContent = this.securityProcessor.signBytes(signingKey, fileBytes);
                    // generate signature block file                
                    String blockSignatureFileName = fileRef.getFileName().concat(mode).concat(SecureCSARConstants.ARTIFACT_SIGNEXTENSION);
                    RepositoryFileReference blockSignatureFileRef = BackendUtils.addFileToArtifactTemplate(tcId, artifactTemplate, blockSignatureFileName, blockSignatureFileContent);
                    String blockSignatureFilePath = BackendUtils.getPathInsideRepo(blockSignatureFileRef);
                    this.addedReferencesToPathInCSARMap.put(blockSignatureFileRef, blockSignatureFilePath);

                    artifactTemplate.getSigningPolicy().setIsApplied(true);
                }
            }
        } catch (IOException | GenericSecurityProcessorException e) {
            e.printStackTrace();
        }
    }

    private void encryptFilesOfArtifactTemplate(TArtifactTemplate artifactTemplate, Key encryptionKey, Set<RepositoryFileReference> files) {
        try {
            for (RepositoryFileReference fileRef : files) {
                if (!fileRef.getFileName().contains(SecureCSARConstants.ARTIFACT_SIGN_MODE_PLAIN) || !fileRef.getFileName().contains(SecureCSARConstants.ARTIFACT_SIGN_MODE_ENCRYPTED)) {
                    Path p = ((FilebasedRepository) repository).ref2AbsolutePath(fileRef);
                    byte[] fileBytes = Files.readAllBytes(p);
                    byte[] encryptedFileBytes = this.securityProcessor.encryptByteArray(encryptionKey, fileBytes);
                    // side-effect: file in the repository is encrypted now
                    Files.write(p, encryptedFileBytes);

                    artifactTemplate.getEncryptionPolicy().setIsApplied(true);
                }
            }
        } catch (IOException | GenericSecurityProcessorException e) {
            e.printStackTrace();
        }
    }

    private Map<String, String> calculatePropertiesDigests(List<String> propertyNames, Map<String, String> artifactTemplateKVProperties) {
        Map<String, String> propDigests = new HashMap<>();
        String digest;
        for (String p : propertyNames) {
            try {
                digest = this.securityProcessor.calculateDigest(artifactTemplateKVProperties.get(p), this.digestAlgorithm);
                propDigests.put(p, digest);
            } catch (GenericSecurityProcessorException e) {
                e.printStackTrace();
            }
        }
        return propDigests;
    }

    private String generateSignedPropertiesManifestContent(Map<String, String> propertiesDigestsAfterEncryption, Map<String, String> overlappingPropsDigestsBeforeEncryption) {
        if (propertiesDigestsAfterEncryption.size() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        ToscaMetaFirstBlockEntry firstBlock = new ToscaMetaFirstBlockEntry
            .Builder(TOSCAMetaFileAttributes.TOSCA_PROPS_META_VERSION, TOSCAMetaFileAttributes.TOSCA_PROPS_META_VERSION_VALUE)
            .createdBy(TOSCAMetaFileAttributes.CREATED_BY)
            .creatorName(TOSCAMetaFileAttributes.CREATOR_NAME)
            .creatorVersion(Environment.getVersion())
            .build();

        sb.append(firstBlock.toString());

        for (String p : propertiesDigestsAfterEncryption.keySet()) {
            ToscaMetaEntry entry;
            if (Objects.nonNull(overlappingPropsDigestsBeforeEncryption) && overlappingPropsDigestsBeforeEncryption.containsKey(p)) {
                entry = new ToscaMetaEntry.Builder(p)
                    .digestAlgorithm(digestAlgorithm)
                    .digestValue(overlappingPropsDigestsBeforeEncryption.get(p))
                    .digestOfEncryptedProperty(propertiesDigestsAfterEncryption.get(p))
                    .build();
            } else {
                entry = new ToscaMetaEntry.Builder(p)
                    .digestAlgorithm(digestAlgorithm)
                    .digestValue(propertiesDigestsAfterEncryption.get(p))
                    .build();
            }
            sb.append(entry.toString());
        }

        return sb.toString();
    }

    private String generateSignedPropertiesSignatureFile(String manifestDigest, Map<String, String> propsDigests, Map<String, String> overlappingPropsDigestsBeforeEncryption) {
        StringBuilder sb = new StringBuilder();
        ToscaMetaFirstBlockEntry firstBlock = new ToscaMetaFirstBlockEntry
            .Builder(TOSCAMetaFileAttributes.TOSCA_PROPSSIGNATURE_VERSION, TOSCAMetaFileAttributes.TOSCA_PROPSSIGNATURE_VERSION_VALUE)
            .digestAlgorithm(this.digestAlgorithm)
            .digestManifest(manifestDigest)
            .createdBy(TOSCAMetaFileAttributes.CREATED_BY)
            .creatorName(TOSCAMetaFileAttributes.CREATOR_NAME)
            .creatorVersion(Environment.getVersion())
            .build();

        sb.append(firstBlock.toString());

        String digest;
        for (String p : propsDigests.keySet()) {
            ToscaMetaEntry entry;
            try {
                if (Objects.nonNull(overlappingPropsDigestsBeforeEncryption) && overlappingPropsDigestsBeforeEncryption.containsKey(p)) {
                    digest = this.securityProcessor.calculateDigest(propsDigests.get(p), this.digestAlgorithm);
                    String digestEncrypted = this.securityProcessor.calculateDigest(overlappingPropsDigestsBeforeEncryption.get(p), this.digestAlgorithm);
                    entry = new ToscaMetaEntry.Builder(p)
                        .digestAlgorithm(digestAlgorithm)
                        .digestValue(digest)
                        .digestOfEncryptedProperty(digestEncrypted)
                        .build();
                } else {
                    digest = this.securityProcessor.calculateDigest(propsDigests.get(p), this.digestAlgorithm);
                    entry = new ToscaMetaEntry.Builder(p)
                        .digestAlgorithm(digestAlgorithm)
                        .digestValue(digest)
                        .build();
                }
                sb.append(entry.toString());
            } catch (GenericSecurityProcessorException e) {
                e.printStackTrace();
            }
        }

        return sb.toString();
    }

    /**
     * Prepares PolicyTypeId and PolicyTemplateId of a given TPolicy for export.
     * This includes adding them into imports of a provided Definitions and in the list of referenced DefinitionsChildId
     * which are further processed in subsequent export methods.
     *
     * @param secPolicy                     the TPolicy attached to TOSCA entity
     * @param referencedDefinitionsChildIds List of referenced DefinitionsChildIds which has to be processed at export
     * @return a PolicyTemplateId of the given TPolicy
     */
    private PolicyTemplateId prepareSecurityPolicyForExport(TPolicy secPolicy, Collection<DefinitionsChildId> referencedDefinitionsChildIds) {
        PolicyTypeId secPolicyTypeId = BackendUtils.getDefinitionsChildId(PolicyTypeId.class, secPolicy.getPolicyType());
        PolicyTemplateId secPolicyTemplateId = BackendUtils.getDefinitionsChildId(PolicyTemplateId.class, secPolicy.getPolicyRef());
        referencedDefinitionsChildIds.add(secPolicyTypeId);
        referencedDefinitionsChildIds.add(secPolicyTemplateId);
        this.addedIdsForImports.add(secPolicyTypeId);
        this.addedIdsForImports.add(secPolicyTemplateId);

        return secPolicyTemplateId;
    }

    private String generateSignatureArtifactTemplateName(String nodeTemplateName, String signedPropsPolicyName) {
        return nodeTemplateName.concat("_").concat(signedPropsPolicyName);
    }

    private String generateSignedPropertiesManifestName(String fileName) {
        return fileName.concat(SecureCSARConstants.ARTIFACT_SIGNPROP_MANIFEST_EXTENSION);
    }

    private String generatePropertiesSignatureFileName(String fileName) {
        return fileName.concat(SecureCSARConstants.ARTIFACT_SIGNPROP_SF_EXTENSION);
    }

    public Map.Entry<String, String> calculateDefinitionDigest(Definitions entryDefinitions, DefinitionsChildId tcId) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            Marshaller m = JAXBSupport.createMarshaller(true);
            m.marshal(entryDefinitions, byteArrayOutputStream);
            String checksum = securityProcessor.calculateDigest(byteArrayOutputStream.toByteArray(), this.digestAlgorithm);
            String defName = CsarExporter.getDefinitionsPathInsideCSAR(repository, tcId);
            return new AbstractMap.SimpleEntry<>(defName, checksum);
        } catch (GenericSecurityProcessorException | JAXBException e) {
            e.printStackTrace();
        }
        return null;
    }
}
