/*******************************************************************************
 * Copyright (c) 2017-2018 Contributors to the Eclipse Foundation
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.winery.common.HashingUtil;
import org.eclipse.winery.common.ids.definitions.ArtifactTemplateId;
import org.eclipse.winery.common.ids.definitions.DefinitionsChildId;
import org.eclipse.winery.common.ids.definitions.ServiceTemplateId;
import org.eclipse.winery.model.csar.toscametafile.TOSCAMetaFileAttributes;
import org.eclipse.winery.repository.TestWithGitBackedRepository;
import org.eclipse.winery.repository.backend.RepositoryFactory;

import org.apache.commons.io.IOUtils;
import org.eclipse.virgo.util.parser.manifest.ManifestContents;
import org.eclipse.virgo.util.parser.manifest.RecoveringManifestParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import static org.eclipse.winery.repository.export.CsarExportConfiguration.INCLUDE_HASHES;
import static org.eclipse.winery.repository.export.CsarExportConfiguration.STORE_FINGERPRINT_IN_ACCOUNTABILITY;
import static org.eclipse.winery.repository.export.CsarExportConfiguration.STORE_IMMUTABLY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CsarExporterTest extends TestWithGitBackedRepository {

    private ByteArrayInputStream createOutputAndInputStream(String commitId, DefinitionsChildId id, EnumSet<CsarExportConfiguration> exportConfiguration) throws Exception {
        setRevisionTo(commitId);
        CsarExporter exporter = new CsarExporter();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        exporter.writeCsar(RepositoryFactory.getRepository(), id, os, exportConfiguration);
        return new ByteArrayInputStream(os.toByteArray());
    }

    private ManifestContents parseManifest(ZipInputStream zis) throws IOException {
        ZipEntry entry;
        ManifestContents manifestContents = null;

        while ((entry = zis.getNextEntry()) != null) {
            if ("TOSCA-Metadata/TOSCA.meta".equals(entry.getName())) {
                byte[] bytes = IOUtils.toByteArray(zis);
                String s = new String(bytes, StandardCharsets.UTF_8);

                manifestContents = new RecoveringManifestParser().parse(s);
            }
        }

        return manifestContents;
    }

    @Test
    public void csarIsValidZipForArtifactTemplateWithFilesAndSources() throws Exception {
        EnumSet<CsarExportConfiguration> exportConfiguration = EnumSet.of(INCLUDE_HASHES);
        try (InputStream inputStream = this.createOutputAndInputStream("origin/plain", new ArtifactTemplateId("http://plain.winery.opentosca.org/artifacttemplates", "ArtifactTemplateWithFilesAndSources-ArtifactTypeWithoutProperties", false), exportConfiguration);
             ZipInputStream zis = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                assertNotNull(name);
                assertFalse(name.contains("\\"), "name contains backslashes");
            }
        }
    }

    @Test
    public void metafileDoesNotContainUnnecessaryFileAttributes() throws Exception {
        // create an empty configuration object
        EnumSet<CsarExportConfiguration> exportConfiguration = EnumSet.noneOf(CsarExportConfiguration.class);

        try (InputStream inputStream = this.createOutputAndInputStream("origin/plain", new ArtifactTemplateId("http://plain.winery.opentosca.org/artifacttemplates", "ArtifactTemplateWithFilesAndSources-ArtifactTypeWithoutProperties", false), exportConfiguration); ZipInputStream zis = new ZipInputStream(inputStream)) {
            ManifestContents manifestContents = parseManifest(zis);

            assertNotNull(manifestContents);

            for (String section : manifestContents.getSectionNames()) {
                assertNull(manifestContents.getAttributesForSection(section).get(TOSCAMetaFileAttributes.HASH));
                assertNull(manifestContents.getAttributesForSection(section).get(TOSCAMetaFileAttributes.IMMUTABLE_ADDRESS));
            }
        }
    }

    @Test
    public void testCsarFilesAreMentionedInTheManifest() throws Exception {
        EnumSet<CsarExportConfiguration> exportConfiguration = EnumSet.of(INCLUDE_HASHES);

        try (InputStream inputStream = this.createOutputAndInputStream("origin/plain", new ServiceTemplateId("http://plain.winery.opentosca.org/servicetemplates", "ServiceTemplateWithAllReqCapVariants", false), exportConfiguration); ZipInputStream zis = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            List<String> elementsList = new ArrayList<>();
            ManifestContents manifestContents = null;

            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                elementsList.add(name);
                assertNotNull(name);
                assertFalse(name.contains("\\"), "name contains backslashes");

                if ("TOSCA-Metadata/TOSCA.meta".equals(name)) {
                    byte[] bytes = IOUtils.toByteArray(zis);
                    String s = new String(bytes, StandardCharsets.UTF_8);

                    manifestContents = new RecoveringManifestParser().parse(s);
                }
            }

            assertNotNull(manifestContents);

            for (String section : manifestContents.getSectionNames()) {
                // ensures that the file is contained in the archive
                assertTrue(elementsList.remove(section), "Contains element " + section);
            }

            // ensures that the manifest was part of the archive
            assertTrue(elementsList.remove("TOSCA-Metadata/TOSCA.meta"));
            // ensures that every file in the archive is listed in the manifest
            assertEquals(0, elementsList.size());
        }
    }

    @Test
    public void testHashesForEachFile() throws Exception {
        EnumSet<CsarExportConfiguration> exportConfiguration = EnumSet.of(INCLUDE_HASHES);

        try (InputStream inputStream = this.createOutputAndInputStream(
            // quick fix - should work if eclipse/winery#305 is merged
            "7c8d8c7057403a07fde90dec1f44f0190ae65ae2",
            new ServiceTemplateId("http://plain.winery.opentosca.org/servicetemplates", "ServiceTemplateWithAllReqCapVariants", false),
            exportConfiguration);
             ZipInputStream zis = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            List<MetaFileEntry> elementsList = new ArrayList<>();
            ManifestContents manifestContents = null;

            while ((entry = zis.getNextEntry()) != null) {
                MetaFileEntry fileProperties = new MetaFileEntry(entry.getName(), null);
                elementsList.add(fileProperties);
                byte[] array = IOUtils.toByteArray(zis);
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(array);
                fileProperties.setFileHash(HashingUtil.getChecksum(byteArrayInputStream, TOSCAMetaFileAttributes.HASH));

                if ("TOSCA-Metadata/TOSCA.meta".equals(fileProperties.getPathInsideCsar())) {
                    String s = new String(array, StandardCharsets.UTF_8);
                    manifestContents = new RecoveringManifestParser().parse(s);
                }
            }

            assertNotNull(manifestContents);

            for (MetaFileEntry fileProperties : elementsList) {
                Map<String, String> attributes = manifestContents.getAttributesForSection(fileProperties.getPathInsideCsar());

                if (!"TOSCA-Metadata/TOSCA.meta".equals(fileProperties.getPathInsideCsar())) {
                    // ensure each file has a hash
                    assertTrue(attributes.containsKey(TOSCAMetaFileAttributes.HASH));
                    // ensure that the hashes match
                    assertEquals(fileProperties.getFileHash(), attributes.get(TOSCAMetaFileAttributes.HASH));
                }
            }

            // ensures that every file in the archive has a hash in the manifest
            assertEquals(elementsList.size() - 1, manifestContents.getSectionNames().size());
        }
    }

    @Test
    @EnabledIf("(new java.io.File(\"C:/Ethereum/keystore/UTC--2018-03-05T15-33-22.456000000Z--e4b51a3d4e77d2ce2a9d9ce107ec8ec7cff5571d.json\")).exists()")
    public void csarFilesHaveImmutableStorageAddresses() throws Exception {
        EnumSet<CsarExportConfiguration> exportConfiguration = EnumSet.of(STORE_IMMUTABLY);

        try (InputStream inputStream = this.createOutputAndInputStream(
            "origin/plain",
            new ServiceTemplateId("http://plain.winery.opentosca.org/servicetemplates", "ServiceTemplateWithAllReqCapVariants", false),
            exportConfiguration);
             ZipInputStream zis = new ZipInputStream(inputStream)) {

            ManifestContents manifestContents = parseManifest(zis);

            assertNotNull(manifestContents);

            for (String section : manifestContents.getSectionNames()) {
                assertNotNull(manifestContents.getAttributesForSection(section).get(TOSCAMetaFileAttributes.IMMUTABLE_ADDRESS));
            }
        }
    }

    @Test
    @EnabledIf("(new java.io.File(\"C:/Ethereum/keystore/UTC--2018-03-05T15-33-22.456000000Z--e4b51a3d4e77d2ce2a9d9ce107ec8ec7cff5571d.json\")).exists()")
    public void testPutCsarInBlockchainAndImmutableStorage() throws Exception {
        setRevisionTo("origin/plain");
        CsarExporter exporter = new CsarExporter();
        DefinitionsChildId id = new ServiceTemplateId("http://plain.winery.opentosca.org/servicetemplates", "ServiceTemplateWithAllReqCapVariants", false);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        EnumSet<CsarExportConfiguration> exportConfiguration = 
            EnumSet.of(STORE_IMMUTABLY, STORE_FINGERPRINT_IN_ACCOUNTABILITY, INCLUDE_HASHES);
        exporter.writeCsar(RepositoryFactory.getRepository(), id, os, exportConfiguration);

        try (InputStream inputStream = new ByteArrayInputStream(os.toByteArray());
             ZipInputStream zis = new ZipInputStream(inputStream)) {
            ManifestContents manifestContents = parseManifest(zis);

            assertNotNull(manifestContents);

            for (String section : manifestContents.getSectionNames()) {
                assertNotNull(manifestContents.getAttributesForSection(section).get(TOSCAMetaFileAttributes.IMMUTABLE_ADDRESS));
            }
        }
    }
}
