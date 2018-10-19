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

public class ToscaMetaEntry {
    
    private String name;
    private String contentType;
    private String digestAlgorithm;
    private String digestValue;
    private String digestValueUsingDefaultAlgorithm;
    private String digestOfEncryptedProperty;
        
    private ToscaMetaEntry(Builder builder) {
        this.name = builder.name;
        this.contentType = builder.contentType;
        this.digestAlgorithm = builder.digestAlgorithm;
        this.digestValue = builder.digestValue;
        this.digestValueUsingDefaultAlgorithm = builder.digestValueUsingDefaultAlgorithm;
        this.digestOfEncryptedProperty = builder.digestOfEncryptedProperty;
    }

    public static class Builder {
        // Required parameters
        private final String name;
        
        private String contentType;
        private String digestAlgorithm;
        private String digestValue;
        private String digestValueUsingDefaultAlgorithm;
        private String digestOfEncryptedProperty;

        public Builder(String name) {
            this.name = name;
        }
        
        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }
        
        public Builder digestAlgorithm(String digestAlgorithm) {
            this.digestAlgorithm = digestAlgorithm;
            return this;
        }

        public Builder digestValue(String digestValue) {
            this.digestValue = digestValue;
            return this;
        }
        
        public Builder digestValueUsingDefaultAlgorithm(String digestValueUsingDefaultAlgorithm) {
            this.digestValueUsingDefaultAlgorithm = digestValueUsingDefaultAlgorithm;
            return this;
        }

        public Builder digestOfEncryptedProperty(String digestOfEncryptedProperty) {
            this.digestOfEncryptedProperty = digestOfEncryptedProperty;
            return this;
        }

        public ToscaMetaEntry build() {
            return new ToscaMetaEntry(this);
        }
    }
    
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(System.lineSeparator());
        sb.append(TOSCAMetaFileAttributes.NAME);
        sb.append(TOSCAMetaFileAttributes.NAME_VALUE_SEPARATOR);
        sb.append(name);
        sb.append(System.lineSeparator());
        if (Objects.nonNull(contentType)) {
            sb.append(TOSCAMetaFileAttributes.CONTENT_TYPE);
            sb.append(TOSCAMetaFileAttributes.NAME_VALUE_SEPARATOR);
            sb.append(contentType);
            sb.append(System.lineSeparator());
        }
        if (Objects.nonNull(digestAlgorithm)) {
            sb.append(TOSCAMetaFileAttributes.DIGEST_ALGORITHM);
            sb.append(TOSCAMetaFileAttributes.NAME_VALUE_SEPARATOR);
            sb.append(digestAlgorithm);
            sb.append(System.lineSeparator());
        }
        if (Objects.nonNull(digestValue)) {
            sb.append(TOSCAMetaFileAttributes.DIGEST);
            sb.append(TOSCAMetaFileAttributes.NAME_VALUE_SEPARATOR);
            sb.append(digestValue);
            sb.append(System.lineSeparator());
        }
        if (Objects.nonNull(digestValueUsingDefaultAlgorithm)) {
            sb.append(TOSCAMetaFileAttributes.HASH);
            sb.append(TOSCAMetaFileAttributes.NAME_VALUE_SEPARATOR);
            sb.append(digestValueUsingDefaultAlgorithm);
            sb.append(System.lineSeparator());
        }
        if (Objects.nonNull(digestOfEncryptedProperty)) {
            sb.append(TOSCAMetaFileAttributes.DIGEST_PROP_ENCRYPTED);
            sb.append(TOSCAMetaFileAttributes.NAME_VALUE_SEPARATOR);
            sb.append(digestOfEncryptedProperty);
            sb.append(System.lineSeparator());
        }
        
        return sb.toString();
    }
    
    public void setDigestValue(String value) {
        this.digestValue = value;
    }

    public String getDigestValue() {
        return this.digestValue;
    }

    public void setDigestOfEncryptedProperty(String value) {
        this.digestOfEncryptedProperty = value;
    }

    public String getDigestOfEncryptedProperty() {
        return this.digestOfEncryptedProperty;
    }
    
}
