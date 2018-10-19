/*******************************************************************************
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

import java.util.Objects;

import org.eclipse.winery.model.csar.toscametafile.TOSCAMetaFileAttributes;

public class MetaFileEntry {
    
    private String pathInsideCsar;
    private String mimeType;
    private String fileHash;

    /**
     * Address of the file in the immutable file storage
     */
    private String immutableAddress;

    public MetaFileEntry(String pathInsideCsar, String mimeType) {
        this.pathInsideCsar = pathInsideCsar;
        this.mimeType = mimeType;
    }

    public String getPathInsideCsar() {
        return pathInsideCsar;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public String getImmutableAddress() {
        return immutableAddress;
    }

    public void setImmutableAddress(String immutableAddress) {
        this.immutableAddress = immutableAddress;
    }
    
    @Override
    public boolean equals(Object other) {
        return other instanceof MetaFileEntry && 
                this.pathInsideCsar != null &&
                this.pathInsideCsar.equals(((MetaFileEntry) other).getPathInsideCsar());
    }
    
    @Override
    public int hashCode() {
        if (this.pathInsideCsar == null)
            return 0;
        
        return this.pathInsideCsar.hashCode();
    }
    
    public String getMetaFileEntryString() {
        StringBuilder entry = new StringBuilder();
        
        entry.append(TOSCAMetaFileAttributes.NAME).append(": ").append(pathInsideCsar).append("\n");
        entry.append(TOSCAMetaFileAttributes.CONTENT_TYPE).append(": ").append(mimeType).append("\n");
        
        if (Objects.nonNull(fileHash)) {
            entry.append(TOSCAMetaFileAttributes.HASH).append(": ").append(fileHash).append("\n");
        }
        
        // todo: add everything else here
        
        return entry.toString();
    }

    public String getMimeType() {
        return mimeType;
    }
}
