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

package org.eclipse.winery.repository.export;

import org.eclipse.winery.model.csar.toscametafile.TOSCAMetaFileAttributes;

import java.util.Objects;

public class ToscaMetaFirstBlockEntry {
    // required
    private String versionName;
    private String versionValue;
    // optional 
    private String csarVersionName;
    private String csarVersionValue;
    private String createdBy;
    private String creatorName;
    private String creatorVersion;
    private String entryDefinitionsReference;
    private String digestAlgorithm;
    private String digestManifest;


    private ToscaMetaFirstBlockEntry(Builder builder) {
        this.versionName = builder.versionName;
        this.versionValue = builder.versionValue;
        this.csarVersionName = builder.csarVersionName;
        this.csarVersionValue = builder.csarVersionValue;
        this.createdBy = builder.createdBy;
        this.creatorName = builder.creatorName;
        this.creatorVersion = builder.creatorVersion;
        this.entryDefinitionsReference = builder.entryDefinitionsReference;
        this.digestAlgorithm = builder.digestAlgorithm;
        this.digestManifest = builder.digestManifest;
    }

    public static class Builder {
        // Required parameters
        private final String versionName;
        private final String versionValue;
        // optional
        private String csarVersionName;
        private String csarVersionValue;
        private String createdBy;
        private String creatorName;
        private String creatorVersion;
        private String entryDefinitionsReference;
        private String digestAlgorithm;
        private String digestManifest;

        public Builder(String versionName, String versionValue) {
            this.versionName = versionName;
            this.versionValue = versionValue;
        }

        public Builder csarVersionName(String csarVersionName) {
            this.csarVersionName = csarVersionName;
            return this;
        }

        public Builder csarVersionValue(String csarVersionValue) {
            this.csarVersionValue = csarVersionValue;
            return this;
        }

        public Builder createdBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Builder creatorName(String creatorName) {
            this.creatorName = creatorName;
            return this;
        }

        public Builder creatorVersion(String creatorVersion) {
            this.creatorVersion = creatorVersion;
            return this;
        }

        public Builder entryDefinitionsReference(String entryDefinitionsReference) {
            this.entryDefinitionsReference = entryDefinitionsReference;
            return this;
        }

        public Builder digestAlgorithm(String digestAlgorithm) {
            this.digestAlgorithm = digestAlgorithm;
            return this;
        }

        public Builder digestManifest(String digestManifest) {
            this.digestManifest = digestManifest;
            return this;
        }

        public ToscaMetaFirstBlockEntry build() {
            return new ToscaMetaFirstBlockEntry(this);
        }
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(versionName);
        sb.append(TOSCAMetaFileAttributes.NAME_VALUE_SEPARATOR);
        sb.append(versionValue);
        sb.append(System.lineSeparator());
        if (Objects.nonNull(csarVersionName) && Objects.nonNull(csarVersionValue)) {
            sb.append(csarVersionName);
            sb.append(TOSCAMetaFileAttributes.NAME_VALUE_SEPARATOR);
            sb.append(csarVersionValue);
            sb.append(System.lineSeparator());
        }
        if (Objects.nonNull(createdBy) && Objects.nonNull(creatorName) && Objects.nonNull(creatorVersion)) {
            sb.append(createdBy);
            sb.append(TOSCAMetaFileAttributes.NAME_VALUE_SEPARATOR);
            sb.append(creatorName);
            sb.append(TOSCAMetaFileAttributes.NAME_VERSION_SEPARATOR);
            sb.append(creatorVersion);
            sb.append(System.lineSeparator());
        }
        if (Objects.nonNull(entryDefinitionsReference)) {
            sb.append(TOSCAMetaFileAttributes.ENTRY_DEFINITIONS);
            sb.append(TOSCAMetaFileAttributes.NAME_VALUE_SEPARATOR);
            sb.append(entryDefinitionsReference);
            sb.append(System.lineSeparator());
        }
        if (Objects.nonNull(digestAlgorithm)) {
            sb.append(TOSCAMetaFileAttributes.DIGEST_ALGORITHM);
            sb.append(TOSCAMetaFileAttributes.NAME_VALUE_SEPARATOR);
            sb.append(digestAlgorithm);
            sb.append(System.lineSeparator());
        }
        if (Objects.nonNull(digestManifest)) {
            sb.append(TOSCAMetaFileAttributes.DIGEST_MANIFEST);
            sb.append(TOSCAMetaFileAttributes.NAME_VALUE_SEPARATOR);
            sb.append(digestManifest);
            sb.append(System.lineSeparator());
        }

        return sb.toString();
    }

}
