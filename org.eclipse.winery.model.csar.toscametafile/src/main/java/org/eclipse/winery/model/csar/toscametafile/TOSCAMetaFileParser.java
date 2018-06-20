/*******************************************************************************
 * Copyright (c) 2013-2017 Contributors to the Eclipse Foundation
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
package org.eclipse.winery.model.csar.toscametafile;

import org.eclipse.virgo.util.parser.manifest.ManifestContents;
import org.eclipse.virgo.util.parser.manifest.ManifestParser;
import org.eclipse.virgo.util.parser.manifest.ManifestProblem;
import org.eclipse.virgo.util.parser.manifest.RecoveringManifestParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Parses and validates a TOSCA meta file.
 */
public class TOSCAMetaFileParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(TOSCAMetaFileParser.class);

    /**
     * Parses and validates the <code>toscaMetaFile</code>.
     *
     * @param toscaMetaFile to process
     * @return <code>TOSCAMetaFile</code> that gives access to the content of
     * the TOSCA meta file. If the given file doesn't exist or is
     * invalid <code>null</code>.
     */
    public TOSCAMetaFile parse(Path toscaMetaFile) {
        // counts the errors during parsing
        int numErrors = 0;

        FileReader reader = null;
        ManifestParser parser;
        ManifestContents manifestContent;
        TOSCAMetaFile toscaMetaFileContent = null;

        try {
            parser = new RecoveringManifestParser();
            reader = new FileReader(toscaMetaFile.toFile());
            TOSCAMetaFileParser.LOGGER.debug("Parsing TOSCA meta file \"{}\"...", toscaMetaFile.getFileName().toString());
            manifestContent = parser.parse(reader);
            reader.close();

            for (ManifestProblem problem : parser.getProblems()) {
                this.logManifestProblem(problem);
                numErrors++;
            }

            Map<String, String> mainAttr = manifestContent.getMainAttributes();

            // signature file block 0 validation
            if (mainAttr.containsKey(TOSCAMetaFileAttributes.TOSCA_SIGNATURE_VERSION)) {
                numErrors += this.validateSignatureBlock0(manifestContent);
            }
            // properties manifest file block 0 validation
            else if (mainAttr.containsKey(TOSCAMetaFileAttributes.TOSCA_PROPS_META_VERSION)) {
                numErrors += this.validatePropsManifestBlock0(manifestContent);
            }
            // properties signature file block 0 validation
            else if (mainAttr.containsKey(TOSCAMetaFileAttributes.TOSCA_PROPSSIGNATURE_VERSION)) {
                numErrors += this.validatePropsSignatureBlock0(manifestContent);
            }
            // standard block 0 validation
            else {
                numErrors += this.validateBlock0(manifestContent);
            }
            
            if (mainAttr.containsKey(TOSCAMetaFileAttributes.TOSCA_PROPS_META_VERSION) || mainAttr.containsKey(TOSCAMetaFileAttributes.TOSCA_PROPSSIGNATURE_VERSION)) {
                numErrors += this.validatePropsManifestFileBlocks(manifestContent);
            }
            else {
                numErrors += this.validateFileBlocks(manifestContent);
            }
            
            if (numErrors == 0) {
                TOSCAMetaFileParser.LOGGER.debug("Parsing TOSCA meta file \"{}\" completed without errors. TOSCA meta file is valid.", toscaMetaFile.getFileName().toString());
                toscaMetaFileContent = new TOSCAMetaFile(manifestContent);
            } else {
                TOSCAMetaFileParser.LOGGER.error("Parsing TOSCA meta file \"{}\" failed - {} error(s) occured. TOSCA meta file is invalid.", toscaMetaFile.getFileName().toString(), numErrors);
            }
        } catch (FileNotFoundException exc) {
            TOSCAMetaFileParser.LOGGER.error("\"{}\" doesn't exist or is not a file.", toscaMetaFile, exc);
        } catch (IOException exc) {
            TOSCAMetaFileParser.LOGGER.error("An IO Exception occured.", exc);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException exc) {
                    TOSCAMetaFileParser.LOGGER.warn("An IOException occured.", exc);
                }
            }
        }

        return toscaMetaFileContent;
    }

    /**
     * Validates block 0 of the TOSCA meta file.
     * <p>
     * Required attributes in block 0:
     * <ul>
     * <li><code>TOSCA-Meta-Version</code> (value must be <code>1.0</code>)</li>
     * <li><code>CSAR-Version</code> (value must be <code>1.0</code>)</li>
     * <li><code>Created-By</code></li>
     * </ul>
     * Optional attributes in block 0:
     * <ul>
     * <li><code>Entry-Definitions</code></li>
     * <li><code>Description</code></li>
     * <li><code>Topology</code></li>
     * </ul>
     * <p>
     * Further, arbitrary attributes are also allowed.
     *
     * @param mf to validate
     * @return Number of errors occurred during validation.
     */
    private int validateBlock0(ManifestContents mf) {
        int numErrors = 0;

        String metaFileVersion;
        String csarVersion;
        String createdBy;
        String entryDefinitions;
        String description;
        String topology;

        Map<String, String> mainAttr = mf.getMainAttributes();

        metaFileVersion = mainAttr.get(TOSCAMetaFileAttributes.TOSCA_META_VERSION);

        if (metaFileVersion == null) {
            this.logAttrMissing(TOSCAMetaFileAttributes.TOSCA_META_VERSION, 0);
            numErrors++;
        } else if (!(metaFileVersion.trim()).equals(TOSCAMetaFileAttributes.TOSCA_META_VERSION_VALUE)) {
            this.logAttrWrongVal(TOSCAMetaFileAttributes.TOSCA_META_VERSION, 0, TOSCAMetaFileAttributes.TOSCA_META_VERSION_VALUE);
            numErrors++;
        }

        csarVersion = mainAttr.get(TOSCAMetaFileAttributes.CSAR_VERSION);

        if (csarVersion == null) {
            this.logAttrMissing(TOSCAMetaFileAttributes.CSAR_VERSION, 0);
            numErrors++;
        } else if (!(csarVersion.trim()).equals(TOSCAMetaFileAttributes.TOSCA_META_VERSION_VALUE)) {
            this.logAttrWrongVal(TOSCAMetaFileAttributes.CSAR_VERSION, 0, TOSCAMetaFileAttributes.CSAR_VERSION_VALUE);
            numErrors++;
        }

        createdBy = mainAttr.get(TOSCAMetaFileAttributes.CREATED_BY);

        if (createdBy == null) {
            this.logAttrMissing(TOSCAMetaFileAttributes.CREATED_BY, 0);
            numErrors++;
        } else if ((createdBy.trim()).isEmpty()) {
            this.logAttrValEmpty(TOSCAMetaFileAttributes.CREATED_BY, 0);
            numErrors++;
        }

        entryDefinitions = mainAttr.get(TOSCAMetaFileAttributes.ENTRY_DEFINITIONS);

        if ((entryDefinitions != null) && entryDefinitions.trim().isEmpty()) {
            this.logAttrValEmpty(TOSCAMetaFileAttributes.ENTRY_DEFINITIONS, 0);
            numErrors++;
        }

        description = mainAttr.get(TOSCAMetaFileAttributes.DESCRIPTION);

        if ((description != null) && description.trim().isEmpty()) {
            this.logAttrValEmpty(TOSCAMetaFileAttributes.DESCRIPTION, 0);
            numErrors++;
        }

        topology = mainAttr.get(TOSCAMetaFileAttributes.TOPOLOGY);

        if ((topology != null) && topology.trim().isEmpty()) {
            this.logAttrValEmpty(TOSCAMetaFileAttributes.TOPOLOGY, 0);
            numErrors++;
        }

        return numErrors;
    }

    /**
     * Validates block 0 of the signature of TOSCA meta file.
     * <p>
     * Required attributes in block 0:
     * <ul>
     * <li><code>Signature-Version</code> (value must be <code>1.0</code>)</li>
     * <li><code>Created-By</code></li>
     * <li><code>Entry-Definitions</code></li>
     * <li><code>Digest-Algorithm</code></li>
     * <li><code>Digest-Manifest</code></li>
     * </ul>
     * <p>
     * Further, arbitrary attributes are also allowed.
     *
     * @param manifestContent to validate
     * @return Number of errors occurred during validation.
     */
    private int validateSignatureBlock0(ManifestContents manifestContent) {
        int numErrors = 0;
        Map<String, String> attributes = manifestContent.getMainAttributes();
        
        // Validate signature header attributes
        numErrors += validateAttributeValue(attributes, TOSCAMetaFileAttributes.TOSCA_SIGNATURE_VERSION, TOSCAMetaFileAttributes.TOSCA_SIGNATURE_VERSION_VALUE, 0);
        numErrors += validateAttributeValue(attributes, TOSCAMetaFileAttributes.CREATED_BY, 0);
        numErrors += validateAttributeValue(attributes, TOSCAMetaFileAttributes.ENTRY_DEFINITIONS, 0);
        numErrors += validateAttributeValue(attributes, TOSCAMetaFileAttributes.DIGEST_ALGORITHM, 0);
        numErrors += validateAttributeValue(attributes, TOSCAMetaFileAttributes.DIGEST_MANIFEST, 0);

        return numErrors;
    }

    private int validatePropsManifestBlock0(ManifestContents manifestContent) {
        int numErrors = 0;
        Map<String, String> attributes = manifestContent.getMainAttributes();

        // Validate signature header attributes
        numErrors += validateAttributeValue(attributes, TOSCAMetaFileAttributes.TOSCA_PROPS_META_VERSION, TOSCAMetaFileAttributes.TOSCA_PROPS_META_VERSION_VALUE, 0);
        numErrors += validateAttributeValue(attributes, TOSCAMetaFileAttributes.CREATED_BY, 0);

        return numErrors;
    }
    
    private int validatePropsSignatureBlock0(ManifestContents manifestContent) {
        int numErrors = 0;
        Map<String, String> attributes = manifestContent.getMainAttributes();

        // Validate signature header attributes
        numErrors += validateAttributeValue(attributes, TOSCAMetaFileAttributes.TOSCA_PROPSSIGNATURE_VERSION, TOSCAMetaFileAttributes.TOSCA_PROPSSIGNATURE_VERSION_VALUE, 0);
        numErrors += validateAttributeValue(attributes, TOSCAMetaFileAttributes.CREATED_BY, 0);

        return numErrors;
    }

    private int validatePropsManifestFileBlocks(ManifestContents manifestContent) {
        int blockNr = 0;
        int numErrors = 0;

        for (String name : manifestContent.getSectionNames()) {
            blockNr++;
            if ((name != null) && name.trim().isEmpty()) {
                this.logAttrValEmpty(name, blockNr);
                numErrors++;
            }
            Map<String, String> attributes = manifestContent.getAttributesForSection(name);
            numErrors += validateAttributeValue(attributes, TOSCAMetaFileAttributes.DIGEST_ALGORITHM, blockNr);
            numErrors += validateAttributeValue(attributes, TOSCAMetaFileAttributes.DIGEST, blockNr);
            // digest of encrypted property is not always the case
            // => no validation for TOSCAMetaFileAttributes.DIGEST_PROP_ENCRYPTED
        }

        return numErrors;
    }

    private int validateAttributeValue(Map<String, String> mainAttr, String attrName, int blockNr) {
        String attribute = mainAttr.get(attrName);
        if (Objects.isNull(attribute)) {
            this.logAttrMissing(attrName, blockNr);
            return 1;
        }
        else if (attribute.trim().isEmpty()) {
            this.logAttrValEmpty(attrName, blockNr);
            return 1;
        }
        return 0;
    }

    private int validateAttributeValue(Map<String, String> mainAttr, String attrName, String correctValue, int blockNr) {
        String attribute = mainAttr.get(attrName);
        if (Objects.isNull(attribute)) {
            this.logAttrMissing(attrName, blockNr);
            return 1;
        }
        else if (attribute.trim().isEmpty()) {
            this.logAttrValEmpty(attrName, blockNr);
            return 1;
        }
        else if (!attribute.trim().equals(correctValue)) {
            this.logAttrWrongVal(attrName, blockNr, correctValue);
            return 1;
        }
        return 0;
    }

    /**
     * Validates the file blocks (block 1 to last block) of the TOSCA meta file.
     * <p>
     * Each file block has the following required attributes:
     * <ul>
     * <li><code>Name</code></li>
     * <li><code>Content-Type</code> (will be checked for correct syntax)</li>
     * </ul>
     * <p>
     * Further, arbitrary attributes are also allowed in a file block.
     *
     * @param mf to validate.
     * @return Number of errors occurred during validation.
     */
    private int validateFileBlocks(ManifestContents mf) {
        int blockNr = 0;
        int numErrors = 0;

        String contentType;

        List<String> names = mf.getSectionNames();

        for (String name : names) {

            blockNr++;

            if ((name != null) && name.trim().isEmpty()) {
                this.logAttrValEmpty(name, blockNr);
                numErrors++;
            }

            Map<String, String> attr = mf.getAttributesForSection(name);
            contentType = attr.get(TOSCAMetaFileAttributes.CONTENT_TYPE);

            if (contentType == null) {
                this.logAttrMissing(TOSCAMetaFileAttributes.CONTENT_TYPE, blockNr);
                numErrors++;
            } else if (!contentType.trim().matches("^[-\\w\\+\\.]+/[-\\w\\+\\.]+$")) {
                this.logAttrWrongVal(TOSCAMetaFileAttributes.CONTENT_TYPE, blockNr);
                numErrors++;
            }

        }

        return numErrors;

    }

    /**
     * Logs that attribute <code>attributeName</code> in block
     * <code>blockNr</code> is missing.
     *
     * @param attributeName missing attribute's name.
     * @param blockNr       block number.
     */
    private void logAttrMissing(String attributeName, int blockNr) {
        TOSCAMetaFileParser.LOGGER.warn("Required attribute {} in block {} is missing.", attributeName, blockNr);
    }

    /**
     * Logs that attribute <code>attributeName</code> in block
     * <code>blockNr</code> has an invalid value. Correct is
     * <code>correctValue</code>.
     *
     * @param attributeName attribute's name.
     * @param blockNr       block number.
     * @param correctValue  correct value.
     */
    private void logAttrWrongVal(String attributeName, int blockNr, String correctValue) {
        TOSCAMetaFileParser.LOGGER.warn("Attribute {} in block {} has an invalid value. Must be {}.", attributeName, blockNr, correctValue);
    }

    /**
     * Logs that attribute <code>attributeName</code> in block
     * <code>blockNr</code> has an invalid value.
     *
     * @param attributeName
     * @param blockNr
     */
    private void logAttrWrongVal(String attributeName, int blockNr) {
        TOSCAMetaFileParser.LOGGER.warn("Attribute {} in block {} has an invalid value.", attributeName, blockNr);
    }

    /**
     * Logs that attribute <code>attributeName</code> in block
     * <code>blockNr</code> has an empty value.
     *
     * @param attributeName
     * @param blockNr
     */
    private void logAttrValEmpty(String attributeName, int blockNr) {
        TOSCAMetaFileParser.LOGGER.warn("Attribute {} in block {} has a empty value.", attributeName, blockNr);
    }

    /**
     * Logs the ManifestProblem <code>problem</code>.
     *
     * @param problem
     */
    private void logManifestProblem(ManifestProblem problem) {
        TOSCAMetaFileParser.LOGGER.warn(problem.toString());
    }

}
