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

package org.eclipse.winery.security.support;

import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DigestHelperTest {
    private static final String MESSAGE = "my super content of any file which will be hashed using a SHA-256 hash.";
    private static final String MESSAGE_SHA256_DIGEST = "c0af55785d21197a9fe4c5e9435fa77bb763f386810909e97f646eba7c827df7";
    private static final String FILE_SHA256_DIGEST = "226fe4b0d3fe5d95cdf6a1bfb1d3a93842c97f76418b51d6f3e3752b4a20e05f";
    private static final String FILE_NAME = "largeFile.bmp";

    @Test
    public void testGetChecksum() throws Exception {
        assertEquals(MESSAGE_SHA256_DIGEST,
            DigestHelper.getChecksum(IOUtils.toInputStream(MESSAGE), "SHA-256"));
    }

    @Test
    public void testGetChecksumOfFile() throws Exception {
        final String filePath = Objects.requireNonNull(getClass().getClassLoader().getResource(FILE_NAME)).getPath();
        assertEquals(FILE_SHA256_DIGEST,
            DigestHelper.getChecksumForFile(filePath, "SHA-256"));
    }

}
