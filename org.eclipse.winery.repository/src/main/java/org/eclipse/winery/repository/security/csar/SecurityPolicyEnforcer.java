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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.eclipse.winery.common.HashingUtil;
import org.eclipse.winery.common.RepositoryFileReference;
import org.eclipse.winery.common.constants.MimeTypes;
import org.eclipse.winery.common.ids.definitions.ArtifactTemplateId;
import org.eclipse.winery.common.ids.definitions.ArtifactTypeId;
import org.eclipse.winery.common.ids.definitions.DefinitionsChildId;
import org.eclipse.winery.common.ids.definitions.PolicyTemplateId;
import org.eclipse.winery.common.ids.definitions.PolicyTypeId;
import org.eclipse.winery.common.ids.definitions.ServiceTemplateId;
import org.eclipse.winery.model.csar.toscametafile.TOSCAMetaFileAttributes;
import org.eclipse.winery.model.tosca.Definitions;
import org.eclipse.winery.model.tosca.TArtifactTemplate;
import org.eclipse.winery.model.tosca.TDeploymentArtifact;
import org.eclipse.winery.model.tosca.TDeploymentArtifacts;
import org.eclipse.winery.model.tosca.TEntityTemplate;
import org.eclipse.winery.model.tosca.TEntityType;
import org.eclipse.winery.model.tosca.TExtensibleElements;
import org.eclipse.winery.model.tosca.TImport;
import org.eclipse.winery.model.tosca.TNodeTemplate;
import org.eclipse.winery.model.tosca.TPolicy;
import org.eclipse.winery.model.tosca.TPolicyTemplate;
import org.eclipse.winery.model.tosca.TServiceTemplate;
import org.eclipse.winery.model.tosca.constants.Namespaces;
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
import org.eclipse.winery.repository.export.MetaFileEntry;
import org.eclipse.winery.repository.export.ToscaExportUtil;
import org.eclipse.winery.repository.export.ToscaMetaEntry;
import org.eclipse.winery.repository.export.ToscaMetaFirstBlockEntry;
import org.eclipse.winery.repository.export.entries.BytesBasedCsarEntry;
import org.eclipse.winery.repository.export.entries.CsarEntry;
import org.eclipse.winery.repository.export.entries.DefinitionsBasedCsarEntry;
import org.eclipse.winery.repository.export.entries.StringBasedCsarEntry;
import org.eclipse.winery.security.BCSecurityProcessor;
import org.eclipse.winery.security.KeystoreManager;
import org.eclipse.winery.security.SecurityProcessor;
import org.eclipse.winery.security.exceptions.GenericKeystoreManagerException;
import org.eclipse.winery.security.exceptions.GenericSecurityProcessorException;
import org.eclipse.winery.security.support.DigestAlgorithmEnum;

import org.apache.commons.io.Charsets;

public class SecurityPolicyEnforcer {
    private SecurityProcessor securityProcessor;
    private KeystoreManager keystoreManager;
    private String digestAlgorithm;
    private IRepository repository;
    private Map<MetaFileEntry, CsarEntry> addedReferences;
    private Map<String, DefinitionsChildId> addedIdsForImports;
    private Map<RepositoryFileReference, String> encryptedFilesDigests;

    public SecurityPolicyEnforcer(IRepository repository, String digestAlgorithm) {
        this.securityProcessor = new BCSecurityProcessor();
        this.keystoreManager = KeystoreManagerFactory.getInstance();
        this.repository = repository;
        this.digestAlgorithm = digestAlgorithm;
        this.addedReferences = new HashMap<>();
        this.addedIdsForImports = new HashMap<>();
        this.encryptedFilesDigests = new HashMap<>();
    }

    public SecurityPolicyEnforcer(IRepository repository) {
        this(repository, DigestAlgorithmEnum.SHA256.name());
    }

    public void enforceSecurityPolicies(Map<MetaFileEntry, CsarEntry> refMap) {
        for (MetaFileEntry m : refMap.keySet()) {
            CsarEntry currentEntry = refMap.get(m);
            if (currentEntry instanceof DefinitionsBasedCsarEntry) {
                this.enforceSecurityPolicies((DefinitionsBasedCsarEntry) currentEntry);
            }
        }
        refMap.putAll(addedReferences);
    }

    private void enforceSecurityPolicies(DefinitionsBasedCsarEntry entry) {
        DefinitionsChildId definitionsChildId = entry.getDefinitionsChildId();
        Definitions entryDefinitions = entry.getDefinitions();

        // enforce property-related policies if DefinitionsBasedCsarEntry contains a service template 
        if (definitionsChildId instanceof ServiceTemplateId) {
            // enforce properties-related policies
            for (TExtensibleElements element : entryDefinitions.getServiceTemplateOrNodeTypeOrNodeTypeImplementation()) {
                if (element instanceof TServiceTemplate) {
                    TServiceTemplate serviceTemplate = (TServiceTemplate) element;
                    List<TNodeTemplate> nodeTemplates = serviceTemplate.getTopologyTemplate().getNodeTemplates();

                    for (TNodeTemplate nodeTemplate : nodeTemplates) {
                        EntitySecurityState nodeTemplateSecState = new EntitySecurityState(nodeTemplate);

                        if (!nodeTemplateSecState.secPolicySpecIsCorrupt) {
                            if (nodeTemplateSecState.hasEncPolicy && !nodeTemplateSecState.encryptionRequired) {
                                // TODO node properties are encrypted
                            }

                            if (nodeTemplateSecState.hasSignPolicy && !nodeTemplateSecState.signingRequired) {
                                // TODO node properties are signed 
                                // we need to verify signatures and export them if they are correct
                                // otherwise, we need to delete these signatures, clean respective node template,
                                // and regenerate signatures if the key is present (terminate, if no key is present)
                            }

                            if (nodeTemplateSecState.signingRequired && nodeTemplateSecState.encryptionRequired) {
                                List<String> overlappingPropNames = getOverlappingProperties(nodeTemplate);
                                if (overlappingPropNames.size() > 0) {
                                    // handle properties overlap
                                    Map<String, String> kvProperties = getKVProperties(nodeTemplate);
                                    Map<String, String> overlappingPropDigests = calculatePropDigests(overlappingPropNames, kvProperties);
                                    enforcePropertyEncryption(nodeTemplate);
                                    enforcePropertySigning(nodeTemplate, overlappingPropDigests);
                                } else {
                                    // properties do not overlap, handle in a regular fashion
                                    enforcePropertyEncryption(nodeTemplate);
                                    enforcePropertySigning(nodeTemplate, null);
                                }
                            } else if (nodeTemplateSecState.signingRequired) {
                                // handle plain signing
                                enforcePropertySigning(nodeTemplate, null);
                            } else if (nodeTemplateSecState.encryptionRequired) {
                                // handle plain encryption
                                enforcePropertyEncryption(nodeTemplate);
                            }
                        } else {
                            // TODO security policies are specified incorrectly
                        }
                    }

                    Collection<TImport> addedImports = new ArrayList<>();
                    for (DefinitionsChildId id : this.addedIdsForImports.values()) {
                        ToscaExportUtil.addToImports(repository, id, addedImports, null);
                    }
                    entryDefinitions.getImport().addAll(addedImports);
                    this.addedIdsForImports.clear();
                }
            }
        }

        // enforce files-related policies
        if (definitionsChildId instanceof ArtifactTemplateId) {
            enforceArtifactsSigningAndEncryption(entry);
        }
    }

    private List<String> getPropertyNamesForProtection(TPolicyTemplate propsPolicyTemplate) {
        String spaceSeparatedPropertyNames = getKVProperty(propsPolicyTemplate, SecureCSARConstants.SEC_POL_PROPGROUPING_PROPERTY);
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
            } else {
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

    private Map<String, String> getKVProperties(TEntityTemplate entityTemplate) {
        if (Objects.nonNull(entityTemplate.getProperties())) {
            return entityTemplate.getProperties().getKVProperties();
        }
        return null;
    }

    private String getKVProperty(TEntityTemplate entityTemplate, String key) {
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

    private void enforcePropertyEncryption(TNodeTemplate nodeTemplate) {
        // get the respective NodeType
        final TEntityType nodeType = repository.getTypeForTemplate(nodeTemplate);
        TPolicy encTypeLevelPolicy = nodeType.getPolicyByQName(QNames.WINERY_ENCRYPTION_POLICY_TYPE);
        TPolicy encTypeLevelPropsPolicy = nodeType.getPolicyByQName(QNames.WINERY_ENCRYPTEDPROP_POLICY_TYPE);

        prepareSecurityPolicyForExport(encTypeLevelPolicy);
        PolicyTemplateId encPropsPolicyTemplateId = prepareSecurityPolicyForExport(encTypeLevelPropsPolicy);
        TPolicyTemplate encPropsPolicyTemplate = repository.getElement(encPropsPolicyTemplateId);

        String keyAlias = encTypeLevelPolicy.getPolicyRef().getLocalPart();
        List<String> propertyNamesForEncryption = getPropertyNamesForProtection(encPropsPolicyTemplate);

        if (this.keystoreManager.entityExists(keyAlias) && Objects.nonNull(propertyNamesForEncryption)) {
            Map<String, String> nodeTemplateKVProperties = getKVProperties(nodeTemplate);
            // only proceed if property verification is successful
            boolean propertiesAreValid = verifyPropertiesCorrespondence(nodeTemplateKVProperties, propertyNamesForEncryption);
            if (propertiesAreValid) {
                int numPropsSecured = 0;
                try {
                    Key encryptionKey = this.keystoreManager.loadKey(keyAlias);
                    for (String p : propertyNamesForEncryption) {
                        assert nodeTemplateKVProperties != null;
                        byte[] encValue = this.securityProcessor.encryptBytes(encryptionKey, nodeTemplateKVProperties.get(p).getBytes(Charsets.UTF_8));
                        nodeTemplateKVProperties.replace(p, Base64.getEncoder().encodeToString(encValue));
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

    private void enforcePropertySigning(TNodeTemplate nodeTemplate, Map<String, String> overlappingPropDigests) {
        final TEntityType nodeType = repository.getTypeForTemplate(nodeTemplate);

        TPolicy signTypeLevelPolicy = nodeType.getPolicyByQName(QNames.WINERY_SIGNING_POLICY_TYPE);
        TPolicy signTypeLevelPropsPolicy = nodeType.getPolicyByQName(QNames.WINERY_SIGNEDPROP_POLICY_TYPE);

        prepareSecurityPolicyForExport(signTypeLevelPolicy);
        PolicyTemplateId signPropsPolicyTemplateId = prepareSecurityPolicyForExport(signTypeLevelPropsPolicy);
        TPolicyTemplate signPropsPolicyTemplate = repository.getElement(signPropsPolicyTemplateId);

        String keyAlias = signTypeLevelPolicy.getPolicyRef().getLocalPart();
        List<String> propertyNamesForSigning = getPropertyNamesForProtection(signPropsPolicyTemplate);

        if (this.keystoreManager.entityExists(keyAlias) && Objects.nonNull(propertyNamesForSigning)) {
            Map<String, String> nodeTemplateKVProperties = getKVProperties(nodeTemplate);
            // only proceed if property verification is successful
            boolean propertiesAreValid = verifyPropertiesCorrespondence(nodeTemplateKVProperties, propertyNamesForSigning);
            if (propertiesAreValid) {
                try {
                    Key signingKey = this.keystoreManager.loadKey(keyAlias);

                    // Generate Atempl and add it to referencedDefinitionsChildIds so that it's exported
                    String signatureATName = generateSignatureArtifactTemplateName(nodeTemplate.getId(), signPropsPolicyTemplate.getName());
                    ArtifactTemplateId sigATemplateId = BackendUtils.getDefinitionsChildId(ArtifactTemplateId.class, Namespaces.URI_OPENTOSCA_ARTIFACTTEMPLATE, signatureATName, false);

                    Definitions aTempDefinitions = this.repository.getDefinitions(sigATemplateId);
                    TArtifactTemplate sigATemplate = aTempDefinitions.getArtifactTemplates().get(0);
                    sigATemplate.setType(QNames.WINERY_SIGNATURE_ARTIFACT_TYPE);
                    sigATemplate.setName(signatureATName);
                    BackendUtils.initializeProperties(repository, sigATemplate);

                    String aTempPathInCSAR = CsarExporter.getDefinitionsPathInsideCSAR(repository, sigATemplateId);
                    MetaFileEntry sigATempEntry = new MetaFileEntry(aTempPathInCSAR, MimeTypes.MIMETYPE_TOSCA_DEFINITIONS);
                    addedReferences.put(sigATempEntry, new DefinitionsBasedCsarEntry(aTempDefinitions, sigATemplateId));

                    ArtifactTypeId sigArtifactTypeId = BackendUtils.getDefinitionsChildId(ArtifactTypeId.class, sigATemplate.getType());
                    Definitions aTypeDefinitions = this.repository.getDefinitions(sigArtifactTypeId);
                    String aTypePathInCSAR = CsarExporter.getDefinitionsPathInsideCSAR(repository, sigArtifactTypeId);
                    MetaFileEntry sigATypeEntry = new MetaFileEntry(aTypePathInCSAR, MimeTypes.MIMETYPE_TOSCA_DEFINITIONS);
                    addedReferences.put(sigATypeEntry, new DefinitionsBasedCsarEntry(aTypeDefinitions, sigArtifactTypeId));

                    // generate properties manifest, signature file, and a signature block file
                    Map<String, String> propertiesDigestsAfterEncryption = calculatePropDigests(propertyNamesForSigning, nodeTemplateKVProperties);

                    String propManifestContent = generateSignedPropsManifestContent(propertiesDigestsAfterEncryption, overlappingPropDigests);
                    generatePropertiesManifest(sigATemplateId, sigATemplate, propManifestContent);

                    String propManifestSigContent = generatePropsManifestSignature(sigATemplateId, sigATemplate, propManifestContent, propertiesDigestsAfterEncryption, overlappingPropDigests);

                    generatePropsBlockSignature(sigATemplateId, sigATemplate, propManifestSigContent, signingKey);

                    // create a deployment artifact which references a generated signature artifact template
                    // and add it to the node template
                    if (Objects.isNull(nodeTemplate.getDeploymentArtifacts())) {
                        nodeTemplate.setDeploymentArtifacts(new TDeploymentArtifacts());
                    }
                    String daName = SecureCSARConstants.DA_PREFIX.concat(signTypeLevelPropsPolicy.getName());
                    TDeploymentArtifact propsSignatureDA = new TDeploymentArtifact
                        .Builder(daName, QNames.WINERY_SIGNATURE_ARTIFACT_TYPE)
                        .setArtifactRef(sigATemplateId.getQName())
                        .build();
                    nodeTemplate.getDeploymentArtifacts().getDeploymentArtifact().add(propsSignatureDA);

                    // signify that a policy was applied on the level of the node template
                    signTypeLevelPolicy.setIsApplied(true);
                    addPolicyToNodeTemplate(nodeTemplate, signTypeLevelPolicy);

                    this.addedIdsForImports.put(sigArtifactTypeId.getQName().toString(), sigArtifactTypeId);
                    this.addedIdsForImports.put(sigATemplateId.getQName().toString(), sigATemplateId);
                } catch (GenericKeystoreManagerException | GenericSecurityProcessorException e) {
                    e.printStackTrace();
                }
            } else {
                // properties were not set or do not match
                // TODO this is an exception, abort export
            }
        }
    }

    private void generatePropertiesManifest(ArtifactTemplateId sigATempId, TArtifactTemplate sigATemp, String manifestContent) {
        // generate Properties Digests Manifest
        String manifestName = generateSignedPropertiesManifestName(sigATemp.getId());
        String manifestPath = BackendUtils.addFileRefToArtifactTemplate(sigATempId, sigATemp, manifestName);

        MetaFileEntry manifestEntry = new MetaFileEntry(manifestPath, MimeTypes.MIMETYPE_MANIFEST);
        addedReferences.put(manifestEntry, new StringBasedCsarEntry(manifestContent));
    }

    private String generatePropsManifestSignature(ArtifactTemplateId sigATempId, TArtifactTemplate sigATemp, String manifestContent, Map<String, String> propertiesDigests, Map<String, String> plainDigests) throws GenericSecurityProcessorException {
        String manifestDigest = this.securityProcessor.calculateDigest(manifestContent.getBytes(), this.digestAlgorithm);
        String sigPropManifestContent = generateSignedPropertiesSignatureFile(manifestDigest, propertiesDigests, plainDigests);
        String sigPropManifestName = generatePropertiesSignatureFileName(sigATemp.getId());
        String sigPropSigFilePath = BackendUtils.addFileRefToArtifactTemplate(sigATempId, sigATemp, sigPropManifestName);

        MetaFileEntry sigManifestEntry = new MetaFileEntry(sigPropSigFilePath, MimeTypes.MIMETYPE_MANIFEST);
        addedReferences.put(sigManifestEntry, new StringBasedCsarEntry(sigPropManifestContent));

        return sigPropManifestContent;
    }

    private void generatePropsBlockSignature(ArtifactTemplateId sigATempId, TArtifactTemplate sigATemp, String propManifestSigContent, Key signingKey) throws GenericSecurityProcessorException {
        byte[] blockSigFileContent = this.securityProcessor.signBytes(signingKey, propManifestSigContent.getBytes());
        String blockSigFileName = sigATemp.getId().concat(SecureCSARConstants.ARTIFACT_SIGNEXTENSION);
        String blockSigFilePath = BackendUtils.addFileRefToArtifactTemplate(sigATempId, sigATemp, blockSigFileName);

        MetaFileEntry blockSigEntry = new MetaFileEntry(blockSigFilePath, MimeTypes.MIMETYPE_OCTET_STREAM);
        addedReferences.put(blockSigEntry, new BytesBasedCsarEntry(blockSigFileContent));
    }

    private void enforceArtifactsSigningAndEncryption(DefinitionsBasedCsarEntry entry) {
        ArtifactTemplateId aTempId = (ArtifactTemplateId) entry.getDefinitionsChildId();

        for (TArtifactTemplate artifactTemplate : entry.getDefinitions().getArtifactTemplates()) {
            final TPolicy signingPolicy = artifactTemplate.getSigningPolicy();
            final TPolicy encPolicy = artifactTemplate.getEncryptionPolicy();

            DirectoryId fileDir = new ArtifactTemplateFilesDirectoryId(aTempId);
            Set<RepositoryFileReference> files = RepositoryFactory.getRepository().getContainedFiles(fileDir);

            if (Objects.nonNull(signingPolicy) && !signingPolicy.getIsApplied()) {
                prepareSecurityPolicyForExport(signingPolicy);
                TArtifactTemplate.ArtifactReferences refs = artifactTemplate.getArtifactReferences();
                if (Objects.nonNull(refs) && refs.getArtifactReference().size() > 0) {
                    String signingKeyAlias = signingPolicy.getPolicyRef().getLocalPart();
                    try {
                        Key signingKey = this.keystoreManager.loadKey(signingKeyAlias);
                        signFilesOfArtifactTemplate(aTempId, artifactTemplate, signingKey, files, SecureCSARConstants.ARTIFACT_SIGN_MODE_PLAIN);

                        if (Objects.nonNull(encPolicy) && !encPolicy.getIsApplied()) {
                            prepareSecurityPolicyForExport(encPolicy);
                            String ecnryptionKeyAlias = encPolicy.getPolicyRef().getLocalPart();
                            Key encryptionKey = this.keystoreManager.loadKey(ecnryptionKeyAlias);
                            encryptFilesOfArtifactTemplate(artifactTemplate, encryptionKey, files);
                            // Sign encrypted artifact as well
                            signFilesOfArtifactTemplate(aTempId, artifactTemplate, signingKey, files, SecureCSARConstants.ARTIFACT_SIGN_MODE_ENCRYPTED);
                        }
                    } catch (GenericKeystoreManagerException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                if (Objects.nonNull(encPolicy) && !encPolicy.getIsApplied()) {
                    prepareSecurityPolicyForExport(encPolicy);
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
    }

    private void signFilesOfArtifactTemplate(ArtifactTemplateId tcId, TArtifactTemplate artifactTemplate, Key signingKey, Set<RepositoryFileReference> files, String mode) {
        try {
            for (RepositoryFileReference fileRef : files) {
                if (!fileRef.getFileName().contains(SecureCSARConstants.ARTIFACT_SIGN_MODE_PLAIN) || !fileRef.getFileName().contains(SecureCSARConstants.ARTIFACT_SIGN_MODE_ENCRYPTED)) {
                    byte[] blockSignatureFileContent;
                    String blockSignatureFilePath;
                    String fileHash;

                    if (mode.equals(SecureCSARConstants.ARTIFACT_SIGN_MODE_PLAIN)) {
                        fileHash = HashingUtil.getHashForFile(((FilebasedRepository) repository).ref2AbsolutePath(fileRef).toString(), TOSCAMetaFileAttributes.HASH);
                    } else {
                        fileHash = encryptedFilesDigests.get(fileRef);
                    }

                    if (Objects.nonNull(fileHash)) {
                        blockSignatureFileContent = this.securityProcessor.signBytes(signingKey, fileHash.getBytes());
                        // generate signature block file                
                        String blockSignatureFileName = fileRef.getFileName().concat(mode).concat(SecureCSARConstants.ARTIFACT_SIGNEXTENSION);
                        blockSignatureFilePath = BackendUtils.addFileRefToArtifactTemplate(tcId, artifactTemplate, blockSignatureFileName);

                        MetaFileEntry blockSigEntry = new MetaFileEntry(blockSignatureFilePath, MimeTypes.MIMETYPE_OCTET_STREAM);
                        addedReferences.put(blockSigEntry, new BytesBasedCsarEntry(blockSignatureFileContent));
                    } else {
                        // file's hash cannot be calculated
                        // TODO this is an exception, abort export
                    }
                }
            }
            artifactTemplate.getSigningPolicy().setIsApplied(true);
        } catch (GenericSecurityProcessorException e) {
            e.printStackTrace();
        }
    }

    private void encryptFilesOfArtifactTemplate(TArtifactTemplate artifactTemplate, Key encryptionKey, Set<RepositoryFileReference> files) {
        try {
            for (RepositoryFileReference fileRef : files) {
                if (!fileRef.getFileName().contains(SecureCSARConstants.ARTIFACT_SIGN_MODE_PLAIN) || !fileRef.getFileName().contains(SecureCSARConstants.ARTIFACT_SIGN_MODE_ENCRYPTED)) {
                    Path absolutePath = ((FilebasedRepository) repository).ref2AbsolutePath(fileRef);
                    byte[] fileBytes = Files.readAllBytes(absolutePath);
                    byte[] encryptedFileBytes = this.securityProcessor.encryptBytes(encryptionKey, fileBytes);

                    String pathInRepo = BackendUtils.getPathInsideRepo(fileRef);

                    MetaFileEntry blockSigEntry = new MetaFileEntry(pathInRepo, MimeTypes.MIMETYPE_OCTET_STREAM);
                    addedReferences.put(blockSigEntry, new BytesBasedCsarEntry(encryptedFileBytes));
                    encryptedFilesDigests.put(fileRef, HashingUtil.getChecksum(new ByteArrayInputStream(encryptedFileBytes), TOSCAMetaFileAttributes.HASH));
                }
            }
            artifactTemplate.getEncryptionPolicy().setIsApplied(true);
        } catch (IOException | GenericSecurityProcessorException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static Response decryptFilesOfArtifactTemplate(TArtifactTemplate artifactTemplate, Key secretKey, Set<RepositoryFileReference> files) {
        try {
            SecurityProcessor securityProcessor = new BCSecurityProcessor();
            for (RepositoryFileReference fileRef : files) {
                if (!(fileRef.getFileName().contains(SecureCSARConstants.ARTIFACT_SIGN_MODE_PLAIN) || fileRef.getFileName().contains(SecureCSARConstants.ARTIFACT_SIGN_MODE_ENCRYPTED))) {
                    Path absolutePath = ((FilebasedRepository) RepositoryFactory.getRepository()).ref2AbsolutePath(fileRef);
                    byte[] fileBytes = Files.readAllBytes(absolutePath);
                    byte[] decrypted = securityProcessor.decryptBytes(secretKey, fileBytes);
                    Files.write(absolutePath, decrypted);
                }
            }
            artifactTemplate.getEncryptionPolicy().setIsApplied(false);
            return Response.ok()
                .entity("Artifact Template files were successfully decrypted")
                .build();
        } catch (IOException | GenericSecurityProcessorException e) {
            e.printStackTrace();
            return Response.serverError()
                .entity(e.getMessage())
                .build();
        }
    }

    private Map<String, String> calculatePropDigests(List<String> propertyNames, Map<String, String> artifactTemplateKVProperties) {
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

    private String generateSignedPropsManifestContent(Map<String, String> propertiesDigestsAfterEncryption, Map<String, String> overlappingPropsDigestsBeforeEncryption) {
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
     * @param secPolicy the TPolicy attached to TOSCA entity
     * @return a PolicyTemplateId of the given TPolicy
     */
    private PolicyTemplateId prepareSecurityPolicyForExport(TPolicy secPolicy) {
        PolicyTypeId secPolicyTypeId = BackendUtils.getDefinitionsChildId(PolicyTypeId.class, secPolicy.getPolicyType());
        PolicyTemplateId secPolicyTemplateId = BackendUtils.getDefinitionsChildId(PolicyTemplateId.class, secPolicy.getPolicyRef());
        this.addedIdsForImports.put(secPolicyTypeId.getQName().toString(), secPolicyTypeId);
        this.addedIdsForImports.put(secPolicyTemplateId.getQName().toString(), secPolicyTemplateId);

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

    private class EntitySecurityState {
        private boolean hasEncPolicy;
        private boolean encryptionRequired;
        private boolean hasSignPolicy;
        private boolean signingRequired;
        private boolean secPolicySpecIsCorrupt;

        EntitySecurityState(TNodeTemplate nodeTemplate) {

            final TEntityType nodeType = repository.getTypeForTemplate(nodeTemplate);
            if (Objects.nonNull(nodeType.getPolicies())) {
                TPolicy encTypeLevelPolicy = nodeType.getPolicyByQName(QNames.WINERY_ENCRYPTION_POLICY_TYPE);
                TPolicy encTypeLevelPropsPolicy = nodeType.getPolicyByQName(QNames.WINERY_ENCRYPTEDPROP_POLICY_TYPE);
                TPolicy signingTypeLevelPolicy = nodeType.getPolicyByQName(QNames.WINERY_SIGNING_POLICY_TYPE);
                TPolicy signingTypeLevelPropsPolicy = nodeType.getPolicyByQName(QNames.WINERY_SIGNEDPROP_POLICY_TYPE);

                if (Objects.nonNull(encTypeLevelPolicy) || Objects.nonNull(encTypeLevelPropsPolicy)) {
                    this.hasEncPolicy = true;
                    // only combination of these policies can be used
                    this.secPolicySpecIsCorrupt = !(Objects.nonNull(encTypeLevelPolicy) && Objects.nonNull(encTypeLevelPropsPolicy));
                    // encryption is required if template level policy is not present or is not applied
                    TPolicy encTemplateLevelPolicy = nodeTemplate.getPolicyByQName(QNames.WINERY_ENCRYPTION_POLICY_TYPE);
                    this.encryptionRequired = Objects.isNull(encTemplateLevelPolicy) || (!encTemplateLevelPolicy.getIsApplied());
                }

                if (Objects.nonNull(signingTypeLevelPolicy) || Objects.nonNull(signingTypeLevelPropsPolicy)) {
                    this.hasSignPolicy = true;
                    // only combination of these policies can be used
                    this.secPolicySpecIsCorrupt = !(Objects.nonNull(signingTypeLevelPolicy) && Objects.nonNull(signingTypeLevelPropsPolicy));
                    // signing is required if template level policy is not present or is not applied
                    TPolicy signingTemplateLevelPolicy = nodeTemplate.getPolicyByQName(QNames.WINERY_SIGNING_POLICY_TYPE);
                    this.signingRequired = Objects.isNull(signingTemplateLevelPolicy) || (!signingTemplateLevelPolicy.getIsApplied());
                }
            }
        }
    }
}
