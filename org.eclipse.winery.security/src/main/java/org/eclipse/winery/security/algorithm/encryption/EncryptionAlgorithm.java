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
package org.eclipse.winery.security.algorithm.encryption;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.Key;

public interface EncryptionAlgorithm {
    InputStream encryptStream(Key key, InputStream plainText) throws InvalidKeyException;

    byte[] encryptBytes(Key key, byte[] plainText) throws InvalidKeyException;

    InputStream decryptStream(Key key, InputStream cipherText) throws IOException, InvalidKeyException;

    byte[] decryptBytes(Key key, byte[] cipherText) throws InvalidKeyException;
}
