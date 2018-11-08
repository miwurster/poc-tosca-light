/*******************************************************************************
 * Copyright (c) 2012-2018 Contributors to the Eclipse Foundation
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.SortedSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.winery.accountability.AccountabilityManager;
import org.eclipse.winery.accountability.AccountabilityManagerFactory;
import org.eclipse.winery.accountability.exceptions.AccountabilityException;
import org.eclipse.winery.common.HashingUtil;
import org.eclipse.winery.common.RepositoryFileReference;
import org.eclipse.winery.common.Util;
import org.eclipse.winery.common.constants.MimeTypes;
import org.eclipse.winery.common.ids.GenericId;
import org.eclipse.winery.common.ids.IdNames;
import org.eclipse.winery.common.ids.admin.NamespacesId;
import org.eclipse.winery.common.ids.definitions.ArtifactTemplateId;
import org.eclipse.winery.common.ids.definitions.DefinitionsChildId;
import org.eclipse.winery.common.ids.definitions.ServiceTemplateId;
import org.eclipse.winery.common.version.VersionUtils;
import org.eclipse.winery.model.csar.toscametafile.TOSCAMetaFileAttributes;
import org.eclipse.winery.model.selfservice.Application;
import org.eclipse.winery.model.selfservice.Application.Options;
import org.eclipse.winery.model.selfservice.ApplicationOption;
import org.eclipse.winery.model.tosca.TArtifactReference;
import org.eclipse.winery.model.tosca.TArtifactTemplate;
import org.eclipse.winery.repository.Constants;
import org.eclipse.winery.repository.GitInfo;
import org.eclipse.winery.repository.backend.BackendUtils;
import org.eclipse.winery.repository.backend.IGenericRepository;
import org.eclipse.winery.repository.backend.IRepository;
import org.eclipse.winery.repository.backend.RepositoryFactory;
import org.eclipse.winery.repository.backend.SelfServiceMetaDataUtils;
import org.eclipse.winery.repository.backend.constants.MediaTypes;
import org.eclipse.winery.repository.configuration.Environment;
import org.eclipse.winery.repository.datatypes.ids.elements.DirectoryId;
import org.eclipse.winery.repository.datatypes.ids.elements.SelfServiceMetaDataId;
import org.eclipse.winery.repository.datatypes.ids.elements.ServiceTemplateSelfServiceFilesDirectoryId;
import org.eclipse.winery.repository.exceptions.RepositoryCorruptException;
import org.eclipse.winery.repository.export.entries.BytesBasedCsarEntry;
import org.eclipse.winery.repository.export.entries.CsarEntry;
import org.eclipse.winery.repository.export.entries.RepositoryRefBasedCsarEntry;
import org.eclipse.winery.repository.export.entries.StringBasedCsarEntry;
import org.eclipse.winery.repository.security.csar.BCSecurityProcessor;
import org.eclipse.winery.repository.security.csar.JCEKSKeystoreManager;
import org.eclipse.winery.repository.security.csar.KeystoreManager;
import org.eclipse.winery.repository.security.csar.SecureCSARConstants;
import org.eclipse.winery.repository.security.csar.SecurityPolicyEnforcer;
import org.eclipse.winery.repository.security.csar.SecurityProcessor;
import org.eclipse.winery.repository.security.csar.exceptions.GenericKeystoreManagerException;
import org.eclipse.winery.repository.security.csar.exceptions.GenericSecurityProcessorException;

import com.google.common.base.Charsets;
import de.danielbechler.util.Strings;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.winery.model.csar.toscametafile.TOSCAMetaFileAttributes.CREATED_BY;
import static org.eclipse.winery.model.csar.toscametafile.TOSCAMetaFileAttributes.CREATOR_NAME;
import static org.eclipse.winery.model.csar.toscametafile.TOSCAMetaFileAttributes.CSAR_VERSION;
import static org.eclipse.winery.model.csar.toscametafile.TOSCAMetaFileAttributes.ENTRY_DEFINITIONS;
import static org.eclipse.winery.model.csar.toscametafile.TOSCAMetaFileAttributes.HASH;
import static org.eclipse.winery.model.csar.toscametafile.TOSCAMetaFileAttributes.LOCATION_IN_CSAR;
import static org.eclipse.winery.model.csar.toscametafile.TOSCAMetaFileAttributes.TOSCA_META_CERT_PATH;
import static org.eclipse.winery.model.csar.toscametafile.TOSCAMetaFileAttributes.TOSCA_META_SIGN_BLOCK_FILE_PATH;
import static org.eclipse.winery.model.csar.toscametafile.TOSCAMetaFileAttributes.TOSCA_META_SIGN_FILE_PATH;
import static org.eclipse.winery.model.csar.toscametafile.TOSCAMetaFileAttributes.TOSCA_META_VERSION;
import static org.eclipse.winery.model.csar.toscametafile.TOSCAMetaFileAttributes.TOSCA_SIGNATURE_VERSION;
import static org.eclipse.winery.model.csar.toscametafile.TOSCAMetaFileAttributes.TOSCA_SIGNATURE_VERSION_VALUE;
import static org.eclipse.winery.repository.export.CsarExportConfiguration.APPLY_SECURITY_POLICIES;
import static org.eclipse.winery.repository.export.CsarExportConfiguration.INCLUDE_HASHES;
import static org.eclipse.winery.repository.export.CsarExportConfiguration.STORE_FINGERPRINT_IN_ACCOUNTABILITY;
import static org.eclipse.winery.repository.export.CsarExportConfiguration.STORE_IMMUTABLY;

/**
 * This class exports a CSAR crawling from the the given GenericId. Currently, only ServiceTemplates are supported.
 * commons-compress is used as an output stream should be provided. An alternative implementation is to use Java7's Zip
 * File System Provider
 */
public class CsarExporter {

    public static final String PATH_TO_NAMESPACES_PROPERTIES = "winery/Namespaces.properties";
    public static final String PATH_TO_NAMESPACES_JSON = "winery/Namespaces.json";

    private static final Logger LOGGER = LoggerFactory.getLogger(CsarExporter.class);

    private static final String DEFINITONS_PATH_PREFIX = "Definitions/";
    private static final String WINERY_TEMP_DIR_PREFIX = "winerytmp";

    /**
     * Returns a unique name for the given definitions to be used as filename
     */
    private static String getDefinitionsName(IGenericRepository repository, DefinitionsChildId id) {
        // the prefix is globally unique and the id locally in a namespace
        // therefore a concatenation of both is also unique
        return repository.getNamespaceManager().getPrefix(id.getNamespace()) + "__" + id.getXmlId().getEncoded();
    }

    public static String getDefinitionsFileName(IGenericRepository repository, DefinitionsChildId id) {
        return CsarExporter.getDefinitionsName(repository, id) + Constants.SUFFIX_TOSCA_DEFINITIONS;
    }

    public static String getDefinitionsPathInsideCSAR(IGenericRepository repository, DefinitionsChildId id) {
        return CsarExporter.DEFINITONS_PATH_PREFIX + CsarExporter.getDefinitionsFileName(repository, id);
    }

    public CompletableFuture<String> writeCsarAndSaveManifestInProvenanceLayer(IRepository repository, DefinitionsChildId entryId, OutputStream out)
        throws IOException, RepositoryCorruptException, AccountabilityException, InterruptedException, ExecutionException {
        LocalDateTime start = LocalDateTime.now();
        Properties props = repository.getAccountabilityConfigurationManager().properties;
        AccountabilityManager accountabilityManager = AccountabilityManagerFactory.getAccountabilityManager(props);

        EnumSet<CsarExportConfiguration> exportConfiguration =
            EnumSet.of(INCLUDE_HASHES, STORE_IMMUTABLY);

        /*String manifestString = this.writeCsar(repository, entryId, out, exportConfiguration);*/
        String qNameWithComponentVersionOnly = VersionUtils.getQNameWithComponentVersionOnly(entryId);
        LOGGER.debug("Preparing CSAR export (provenance) lasted {}", Duration.between(LocalDateTime.now(), start).toString());

        return accountabilityManager.storeFingerprint(qNameWithComponentVersionOnly, "");
    }

    /**
     * Writes a complete CSAR containing all necessary things reachable from the given service template
     *
     * @param entryId the id of the service template to export
     * @param out     the output stream to write to
     * @return the TOSCA meta file for the generated Csar
     */
    public void writeCsar(IRepository repository, DefinitionsChildId entryId, OutputStream out, EnumSet<CsarExportConfiguration> exportConfiguration)
        throws IOException, RepositoryCorruptException, InterruptedException, AccountabilityException, ExecutionException {
        CsarExporter.LOGGER.trace("Starting CSAR export with {}", entryId.toString());

        Map<MetaFileEntry, CsarEntry> refMap = new HashMap<>();

        this.fetchReferencedDefinitions(repository, entryId, refMap);
        this.addNamespacePrefixes(repository, refMap);

        if (exportConfiguration.contains(APPLY_SECURITY_POLICIES)) {
            this.enforceSecurityPolicies(repository, refMap);
        }

        /* now, refMap contains all files to be added to the CSAR */
        if (exportConfiguration.contains(INCLUDE_HASHES) || exportConfiguration.contains(APPLY_SECURITY_POLICIES)) {
            LOGGER.trace("Calculating checksum for {} files.", refMap.size());
            calculateFileHashes(refMap);
        }

        if (exportConfiguration.contains(STORE_IMMUTABLY)) {
            try {
                LOGGER.trace("Storing {} files in the immutable file storage", refMap.size());
                immutablyStoreRefFiles(refMap, repository);
            } catch (InterruptedException | ExecutionException | AccountabilityException e) {
                LOGGER.error("Failed to store files in immutable storage. Reason: {}", e.getMessage());
                throw e;
            }
        }

        MetaFileEntry metaFileEntry = this.generateToscaMetaFile(refMap, entryId);

        if (exportConfiguration.contains(APPLY_SECURITY_POLICIES)) {
            try {
                this.generateSecuredManifest(refMap, metaFileEntry);
            } catch (GenericSecurityProcessorException | GenericKeystoreManagerException | NoSuchAlgorithmException e) {
                LOGGER.error("Specified digest algorithm is not found. Message: {}", e.getMessage());
            }
        }

        if (exportConfiguration.contains(STORE_FINGERPRINT_IN_ACCOUNTABILITY)) {
            StringBasedCsarEntry entry = (StringBasedCsarEntry) refMap.get(metaFileEntry);
            this.storeFingerprintInAccountability(repository, entryId, entry.getContent());
        }

        // Archive creation
        try (final ZipOutputStream zos = new ZipOutputStream(out)) {
            // write all referenced files
            for (Map.Entry<MetaFileEntry, CsarEntry> entry : refMap.entrySet()) {
                MetaFileEntry fileProperties = entry.getKey();
                CsarEntry ref = entry.getValue();
                CsarExporter.LOGGER.trace("Creating {}", fileProperties.getPathInsideCsar());

                if (ref instanceof RepositoryRefBasedCsarEntry && ((RepositoryRefBasedCsarEntry) ref).getReference().getParent() instanceof DirectoryId) {
                    addArtifactTemplateToZipFile(zos, (RepositoryRefBasedCsarEntry) ref, fileProperties);
                } else {
                    addCsarEntryToArchive(zos, ref, fileProperties);
                }
            }
        }
    }

    private void enforceSecurityPolicies(IRepository repository, Map<MetaFileEntry, CsarEntry> refMap) {
        SecurityPolicyEnforcer enforcer = new SecurityPolicyEnforcer(repository);
        enforcer.enforceSecurityPolicies(refMap);
        /*
        // calculate digests of a definition
        // this is required for subsequent generation of the manifest's signature files 
        Map.Entry<String, String> entry = enforcer.calculateDefinitionDigest(entryDefinitions, tcId);
        definitionsDigests.put(entry.getKey(), entry.getValue());       
        */
    }

    private MetaFileEntry generateToscaMetaFile(Map<MetaFileEntry, CsarEntry> refMap, DefinitionsChildId entryId) {
        StringBuilder builder = new StringBuilder();
        builder.append(TOSCA_META_VERSION).append(": 1.0").append(System.lineSeparator());
        builder.append(CSAR_VERSION).append(": 1.0").append(System.lineSeparator());
        builder.append(CREATED_BY).append(": Winery ").append(Environment.getVersion()).append(System.lineSeparator());

        // Winery currently is unaware of tDefinitions, therefore, we use the
        // name of the service template
        builder.append(ENTRY_DEFINITIONS).append(": ").append(getDefinitionsPathInsideCSAR(RepositoryFactory.getRepository(), entryId)).append(System.lineSeparator());
        builder.append(System.lineSeparator());

        refMap.forEach((metaFileEntry, csarEntry)
            -> builder.append(metaFileEntry.getMetaFileEntryString()).append(System.lineSeparator())
        );

        MetaFileEntry metaFileEntry = new MetaFileEntry(LOCATION_IN_CSAR, MimeTypes.MIMETYPE_MANIFEST);
        refMap.put(metaFileEntry, new StringBasedCsarEntry(builder.toString()));

        return metaFileEntry;
    }

    private void generateSecuredManifest(Map<MetaFileEntry, CsarEntry> refMap, MetaFileEntry manifest) throws IOException, NoSuchAlgorithmException, GenericKeystoreManagerException, GenericSecurityProcessorException {
        StringBuilder builder = new StringBuilder();
        StringBasedCsarEntry manifestEntry = (StringBasedCsarEntry) refMap.get(manifest);
        String manifestDigest = HashingUtil.getChecksum(manifestEntry.getInputStream(), HASH);

        ToscaMetaFirstBlockEntry firstBlock = new ToscaMetaFirstBlockEntry.Builder(TOSCA_SIGNATURE_VERSION, TOSCA_SIGNATURE_VERSION_VALUE)
            .createdBy(CREATED_BY)
            .creatorName(CREATOR_NAME)
            .creatorVersion(Environment.getVersion())
            .manifestDigestWithDefaultAlgorithm(manifestDigest)
            .build();
        builder.append(firstBlock.toString());

        for (MetaFileEntry m : refMap.keySet()) {
            if (Strings.hasText(m.getFileHash())) {
                builder.append(
                    new ToscaMetaEntry.Builder(m.getPathInsideCsar())
                        .contentType(m.getMimeType())
                        .digestValueUsingDefaultAlgorithm(HashingUtil.getChecksum(m.getFileHash(), HASH))
                        .build()
                        .toString()
                );
            }
        }

        MetaFileEntry metaFileEntry = new MetaFileEntry(TOSCA_META_SIGN_FILE_PATH, MimeTypes.MIMETYPE_MANIFEST);
        refMap.put(metaFileEntry, new StringBasedCsarEntry(builder.toString()));

        // add ToscaMetaFile's signature block file and certificate
        SecurityProcessor securityProcessor = new BCSecurityProcessor();
        KeystoreManager keystoreManager = new JCEKSKeystoreManager();
        Key signingKey = keystoreManager.loadKey(SecureCSARConstants.MASTER_SIGNING_KEYNAME);
        // TODO: notify a user if no master key is set
        if (Objects.nonNull(signingKey)) {
            String blockSignatureFileHash = HashingUtil.getChecksum(builder.toString(), HASH);
            byte[] blockSignatureFileContent = securityProcessor.signBytes(signingKey, blockSignatureFileHash.getBytes());
            MetaFileEntry blockSignatureFileEntry = new MetaFileEntry(TOSCA_META_SIGN_BLOCK_FILE_PATH, MimeTypes.MIMETYPE_OCTET_STREAM);
            refMap.put(blockSignatureFileEntry, new BytesBasedCsarEntry(blockSignatureFileContent));

            byte[] cert = keystoreManager.getCertificateEncoded(SecureCSARConstants.MASTER_SIGNING_KEYNAME);
            MetaFileEntry certFileEntry = new MetaFileEntry(TOSCA_META_CERT_PATH, MimeTypes.MIMETYPE_OCTET_STREAM);
            refMap.put(certFileEntry, new BytesBasedCsarEntry(cert));
        }
    }

    private void fetchReferencedDefinitions(IRepository repository, DefinitionsChildId entryId, Map<MetaFileEntry, CsarEntry> refMap)
        throws IOException, RepositoryCorruptException {
        ToscaExportUtil exporter = new ToscaExportUtil();
        Collection<DefinitionsChildId> referencedIds;
        ExportedState exportedState = new ExportedState();
        DefinitionsChildId currentId = entryId;

        do {
            String definitionsPathInsideCSAR = CsarExporter.getDefinitionsPathInsideCSAR(repository, currentId);
            MetaFileEntry definitionsFileProperties = new MetaFileEntry(definitionsPathInsideCSAR, MimeTypes.MIMETYPE_TOSCA_DEFINITIONS);
            referencedIds = exporter.processTOSCA(currentId, definitionsFileProperties, refMap);

            // for each entryId add license and readme files (if they exist) to the refMap
            addLicenseAndReadmeFiles(repository, currentId, refMap);

            exportedState.flagAsExported(currentId);
            exportedState.flagAsExportRequired(referencedIds);

            currentId = exportedState.pop();
        } while (currentId != null);

        // if we export a ServiceTemplate, data for the self-service portal might exist
        if (entryId instanceof ServiceTemplateId) {
            ServiceTemplateId serviceTemplateId = (ServiceTemplateId) entryId;
            this.addSelfServiceMetaData(repository, serviceTemplateId, refMap);
            this.addSelfServiceFiles(repository, serviceTemplateId, refMap);
        }
    }

    private void storeFingerprintInAccountability(IRepository repository, DefinitionsChildId entryId, String manifestString) throws AccountabilityException, ExecutionException, InterruptedException {
        Properties props = repository.getAccountabilityConfigurationManager().properties;
        AccountabilityManager accountabilityManager = AccountabilityManagerFactory.getAccountabilityManager(props);
        String qNameWithComponentVersionOnly = VersionUtils.getQNameWithComponentVersionOnly(entryId);
        accountabilityManager.storeFingerprint(qNameWithComponentVersionOnly, manifestString)
            .get();
    }

    private void calculateFileHashes(Map<MetaFileEntry, CsarEntry> files) {
        files.forEach((properties, entry) -> {
            try (InputStream is = entry.getInputStream()) {
                properties.setFileHash(HashingUtil.getChecksum(is, HASH));
            } catch (IOException | NoSuchAlgorithmException e) {
                LOGGER.error("Failed to calculate hash for {}. Reason: {}.", properties.getPathInsideCsar(), e.getMessage());
            }
        });
    }

    /**
     * Stores all files listed in the map in the immutable file storage, and updates the MetaFileEntry of each
     * file to contain its address in the aforementioned storage.
     *
     * @param filesToStore a map of the MetaFileEntry of all files to be stored in the CSAR and their contents.
     */
    private void immutablyStoreRefFiles(Map<MetaFileEntry, CsarEntry> filesToStore, IRepository repository)
        throws AccountabilityException, ExecutionException, InterruptedException, IOException {
        Properties props = repository.getAccountabilityConfigurationManager().properties;
        AccountabilityManager manager = AccountabilityManagerFactory.getAccountabilityManager(props);
        Map<String, InputStream> filesMap = new HashMap<>();

        for (Map.Entry<MetaFileEntry, CsarEntry> entry : filesToStore.entrySet()) {
            filesMap.put(entry.getKey().getPathInsideCsar(), entry.getValue().getInputStream());
        }

        // store all files in immutable storage (already stored files will get their same old address)
        Map<String, String> addressMap = manager
            .storeState(filesMap)
            .get();

        filesToStore.keySet().forEach((MetaFileEntry properties) -> {
            properties.setImmutableAddress(addressMap.get(properties.getPathInsideCsar()));
        });
    }

    /**
     * Special handling for artifact template directories source and files
     *
     * @param zos            Output stream for the archive that should contain the file
     * @param csarEntry      Reference to the file that should be added to the archive
     * @param fileProperties Describing the path to the file inside the archive
     * @throws IOException thrown when the temporary directory can not be created
     */
    private void addArtifactTemplateToZipFile(ZipOutputStream zos, RepositoryRefBasedCsarEntry csarEntry,
                                              MetaFileEntry fileProperties) throws IOException {
        GitInfo gitInfo = BackendUtils.getGitInformation((DirectoryId) csarEntry.getReference().getParent());

        if (gitInfo == null) {
            addCsarEntryToArchive(zos, csarEntry, fileProperties);
            return;
        }

        // TODO: This is not quite correct. The files should reside checked out at "source/"
        // TODO: Hash all these git files (to be included in the provenance)
        Path tempDir = Files.createTempDirectory(WINERY_TEMP_DIR_PREFIX);
        try {
            Git git = Git
                .cloneRepository()
                .setURI(gitInfo.URL)
                .setDirectory(tempDir.toFile())
                .call();
            git.checkout().setName(gitInfo.BRANCH).call();
            String path = "artifacttemplates/"
                + Util.URLencode(((ArtifactTemplateId) csarEntry.getReference().getParent().getParent()).getQName().getNamespaceURI())
                + "/"
                + ((ArtifactTemplateId) csarEntry.getReference().getParent().getParent()).getQName().getLocalPart()
                + "/files/";
            TArtifactTemplate template = BackendUtils.getTArtifactTemplate((DirectoryId) csarEntry.getReference().getParent());
            addWorkingTreeToArchive(zos, template, tempDir, path);
        } catch (GitAPIException e) {
            CsarExporter.LOGGER.error(String.format("Error while cloning repo: %s / %s", gitInfo.URL, gitInfo.BRANCH), e);
        } finally {
            deleteDirectory(tempDir);
        }
    }

    /**
     * Adds a file to an archive
     *
     * @param zos           Output stream of the archive
     * @param csarEntry     Reference to the file that should be added to the archive
     * @param metaFileEntry Describing the path inside the archive to the file
     */
    private void addCsarEntryToArchive(ZipOutputStream zos, CsarEntry csarEntry,
                                       MetaFileEntry metaFileEntry) {
        try {
            zos.putNextEntry(new ZipEntry(metaFileEntry.getPathInsideCsar()));
            csarEntry.writeToOutputStream(zos);
            zos.closeEntry();
        } catch (IOException e) {
            CsarExporter.LOGGER.error("Could not copy file content to ZIP outputstream", e);
        }
    }

    /**
     * Deletes a directory recursively
     *
     * @param path Path to the directory that should be deleted
     */
    private void deleteDirectory(Path path) {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> s = Files.newDirectoryStream(path)) {
                for (Path p : s) {
                    deleteDirectory(p);
                }
                Files.delete(path);
            } catch (IOException e) {
                CsarExporter.LOGGER.error("Error iterating directory " + path.toAbsolutePath(), e);
            }
        } else {
            try {
                Files.delete(path);
            } catch (IOException e) {
                CsarExporter.LOGGER.error("Error deleting file " + path.toAbsolutePath(), e);
            }
        }
    }

    /**
     * Adds a working tree to an archive
     *
     * @param zos         Output stream of the archive
     * @param template    Template of the artifact
     * @param rootDir     The root of the working tree
     * @param archivePath The path inside the archive to the working tree
     */
    private void addWorkingTreeToArchive(ZipOutputStream zos, TArtifactTemplate template, Path rootDir, String archivePath) {
        addWorkingTreeToArchive(rootDir.toFile(), zos, template, rootDir, archivePath);
    }

    /**
     * Adds a working tree to an archive
     *
     * @param file        The current directory to add
     * @param zos         Output stream of the archive
     * @param template    Template of the artifact
     * @param rootDir     The root of the working tree
     * @param archivePath The path inside the archive to the working tree
     */
    private void addWorkingTreeToArchive(File file, ZipOutputStream zos, TArtifactTemplate template, Path rootDir, String archivePath) {
        // TODO: this is NOT CORRECT. The files MUST also be referenced in the manifest file!
        if (file.isDirectory()) {
            if (file.getName().equals(".git")) {
                return;
            }
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    addWorkingTreeToArchive(f, zos, template, rootDir, archivePath);
                }
            }
        } else {
            boolean foundInclude = false;
            boolean included = false;
            boolean excluded = false;
            for (TArtifactReference artifactReference : template.getArtifactReferences().getArtifactReference()) {
                for (Object includeOrExclude : artifactReference.getIncludeOrExclude()) {
                    if (includeOrExclude instanceof TArtifactReference.Include) {
                        foundInclude = true;
                        TArtifactReference.Include include = (TArtifactReference.Include) includeOrExclude;
                        String reference = artifactReference.getReference();
                        if (reference.endsWith("/")) {
                            reference += include.getPattern();
                        } else {
                            reference += "/" + include.getPattern();
                        }
                        reference = reference.substring(1);
                        included |= BackendUtils.isGlobMatch(reference, rootDir.relativize(file.toPath()));
                    } else if (includeOrExclude instanceof TArtifactReference.Exclude) {
                        TArtifactReference.Exclude exclude = (TArtifactReference.Exclude) includeOrExclude;
                        String reference = artifactReference.getReference();
                        if (reference.endsWith("/")) {
                            reference += exclude.getPattern();
                        } else {
                            reference += "/" + exclude.getPattern();
                        }
                        reference = reference.substring(1);
                        excluded |= BackendUtils.isGlobMatch(reference, rootDir.relativize(file.toPath()));
                    }
                }
            }

            if ((!foundInclude || included) && !excluded) {
                try (InputStream is = new FileInputStream(file)) {
                    ZipEntry archiveEntry = new ZipEntry(archivePath + rootDir.relativize(Paths.get(file.getAbsolutePath())));
                    zos.putNextEntry(archiveEntry);
                    IOUtils.copy(is, zos);
                    zos.closeEntry();
                } catch (Exception e) {
                    CsarExporter.LOGGER.error("Could not copy file to ZIP outputstream", e);
                }
            }
        }
    }

    /**
     * Writes the configured mapping namespaceprefix -> namespace to the archive
     * <p>
     * This is kind of a quick hack. TODO: during the import, the prefixes should be extracted using JAXB and stored in
     * the NamespacesResource
     */
    private void addNamespacePrefixes(IRepository repository, Map<MetaFileEntry, CsarEntry> refMap) throws IOException {
        // ensure that the namespaces are saved as json
        SortedSet<RepositoryFileReference> references = repository.getContainedFiles(new NamespacesId());

        for (RepositoryFileReference ref : references) {
            if (ref.getFileName().toLowerCase().endsWith(Constants.SUFFIX_JSON)) {
                MetaFileEntry metaFileEntry = new MetaFileEntry(CsarExporter.PATH_TO_NAMESPACES_JSON, repository.getMimeType(ref));
                refMap.put(metaFileEntry, new RepositoryRefBasedCsarEntry(ref));
            }
        }
    }

    /**
     * Adds all self service meta data to the targetDir
     *
     * @param repository the repository to work from
     * @param entryId    the service template to export for
     * @param targetDir  the directory in the CSAR where to put the content to
     * @param refMap     is used later to create the CSAR
     */
    private void addSelfServiceMetaData(IRepository repository, ServiceTemplateId entryId, String targetDir, Map<MetaFileEntry, CsarEntry> refMap) throws IOException {
        final SelfServiceMetaDataId selfServiceMetaDataId = new SelfServiceMetaDataId(entryId);

        // This method is also called if the directory SELFSERVICE-Metadata exists without content and even if the directory does not exist at all,
        // but the ServiceTemplate itself exists.
        // The current assumption is that this is enough for an existence.
        // Thus, we have to take care of the case of an empty directory and add a default data.xml
        SelfServiceMetaDataUtils.ensureDataXmlExists(selfServiceMetaDataId);

        MetaFileEntry metaFileEntry = new MetaFileEntry(targetDir + "data.xml", MimeTypes.MIMETYPE_XSD);
        refMap.put(metaFileEntry, new RepositoryRefBasedCsarEntry(SelfServiceMetaDataUtils.getDataXmlRef(selfServiceMetaDataId)));

        // The schema says that the images have to exist
        // However, at a quick modeling, there might be no images
        // Therefore, we check for existence
        final RepositoryFileReference iconJpgRef = SelfServiceMetaDataUtils.getIconJpgRef(selfServiceMetaDataId);
        if (repository.exists(iconJpgRef)) {
            metaFileEntry = new MetaFileEntry(targetDir + "icon.jpg", repository.getMimeType(iconJpgRef));
            refMap.put(metaFileEntry, new RepositoryRefBasedCsarEntry(iconJpgRef));
        }
        final RepositoryFileReference imageJpgRef = SelfServiceMetaDataUtils.getImageJpgRef(selfServiceMetaDataId);
        if (repository.exists(imageJpgRef)) {
            metaFileEntry = new MetaFileEntry(targetDir + "image.jpg", repository.getMimeType(iconJpgRef));
            refMap.put(metaFileEntry, new RepositoryRefBasedCsarEntry(imageJpgRef));
        }

        Application application = SelfServiceMetaDataUtils.getApplication(selfServiceMetaDataId);

        // clear CSAR name as this may change.
        application.setCsarName(null);

        // hack for the OpenTOSCA container to display something
        application.setVersion("1.0");
        List<String> authors = application.getAuthors();
        if (authors.isEmpty()) {
            authors.add("Winery");
        }

        // make the patches to data.xml permanent
        try {
            BackendUtils.persist(application, SelfServiceMetaDataUtils.getDataXmlRef(selfServiceMetaDataId), MediaTypes.MEDIATYPE_TEXT_XML);
        } catch (IOException e) {
            LOGGER.error("Could not persist patches to data.xml", e);
        }

        Options options = application.getOptions();
        if (options != null) {
            SelfServiceMetaDataId id = new SelfServiceMetaDataId(entryId);
            for (ApplicationOption option : options.getOption()) {
                String url = option.getIconUrl();
                if (Util.isRelativeURI(url)) {
                    putRefIntoRefMap(targetDir, refMap, repository, id, url);
                }
                url = option.getPlanInputMessageUrl();
                if (Util.isRelativeURI(url)) {
                    putRefIntoRefMap(targetDir, refMap, repository, id, url);
                }
            }
        }
    }

    private void putRefIntoRefMap(String targetDir, Map<MetaFileEntry, CsarEntry> refMap, IRepository repository, GenericId id, String fileName) throws IOException {
        RepositoryFileReference ref = new RepositoryFileReference(id, fileName);
        if (repository.exists(ref)) {
            MetaFileEntry metaFileEntry = new MetaFileEntry(targetDir + fileName, repository.getMimeType(ref));
            refMap.put(metaFileEntry, new RepositoryRefBasedCsarEntry(ref));
        } else {
            CsarExporter.LOGGER.error("Data corrupt: pointing to non-existent file " + ref);
        }
    }

    private void addLicenseAndReadmeFiles(IRepository repository, DefinitionsChildId entryId, Map<MetaFileEntry, CsarEntry> refMap) throws IOException {
        final RepositoryFileReference licenseRef = new RepositoryFileReference(entryId, Constants.LICENSE_FILE_NAME);
        if (repository.exists(licenseRef)) {
            refMap.put(new MetaFileEntry(BackendUtils.getPathInsideRepo(licenseRef), repository.getMimeType(licenseRef)),
                new RepositoryRefBasedCsarEntry(licenseRef));
        }

        final RepositoryFileReference readmeRef = new RepositoryFileReference(entryId, Constants.README_FILE_NAME);
        if (repository.exists(readmeRef)) {
            refMap.put(new MetaFileEntry(BackendUtils.getPathInsideRepo(readmeRef), repository.getMimeType(readmeRef)),
                new RepositoryRefBasedCsarEntry(readmeRef));
        }
    }

    private void addSelfServiceMetaData(IRepository repository, ServiceTemplateId serviceTemplateId, Map<MetaFileEntry, CsarEntry> refMap) throws IOException {
        SelfServiceMetaDataId id = new SelfServiceMetaDataId(serviceTemplateId);
        // We add the self-service information regardless of the existence. - i.e., no "if (repository.exists(id)) {"
        // This ensures that the name of the application is
        // add everything in the root of the CSAR
        String targetDir = Constants.DIRNAME_SELF_SERVICE_METADATA + "/";
        addSelfServiceMetaData(repository, serviceTemplateId, targetDir, refMap);
    }

    private void addSelfServiceFiles(IRepository repository, ServiceTemplateId serviceTemplateId, Map<MetaFileEntry, CsarEntry> refMap) throws IOException {
        ServiceTemplateSelfServiceFilesDirectoryId selfServiceFilesDirectoryId = new ServiceTemplateSelfServiceFilesDirectoryId(serviceTemplateId);
        for (RepositoryFileReference ref : repository.getContainedFiles(selfServiceFilesDirectoryId)) {
            String file = IdNames.SELF_SERVICE_PORTAL_FILES + "/" + BackendUtils.getFilenameAndSubDirectory(ref);
            MetaFileEntry metaFileEntry = new MetaFileEntry(file, repository.getMimeType(ref));
            refMap.put(metaFileEntry, new RepositoryRefBasedCsarEntry(ref));
        }
    }
}
