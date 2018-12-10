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
package org.eclipse.winery.repository.export.entries.decorators;

import java.io.IOException;
import java.io.InputStream;

import javax.crypto.SecretKey;

import org.eclipse.winery.security.algorithm.SymmetricEncryptionAlgorithm;

public class EncryptionDecorator extends CsarEntryDecorator {
    private SymmetricEncryptionAlgorithm algorithm;
    private SecretKey key;

    public EncryptionDecorator(SymmetricEncryptionAlgorithm algorithm, SecretKey key) {
        this.algorithm = algorithm;
        this.key = key;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        InputStream beforeDecoration = this.toDecorate.getInputStream();

        return algorithm.encryptStream(key, beforeDecoration);
    }
}
