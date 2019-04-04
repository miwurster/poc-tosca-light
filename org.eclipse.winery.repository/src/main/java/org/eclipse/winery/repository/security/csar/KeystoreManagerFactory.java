/********************************************************************************
 * Copyright (c) 2018-2019 Contributors to the Eclipse Foundation
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

import org.eclipse.winery.common.RepositoryFileReference;
import org.eclipse.winery.common.ids.admin.KeystoreId;
import org.eclipse.winery.repository.backend.RepositoryFactory;
import org.eclipse.winery.repository.backend.filebased.FilebasedRepository;
import org.eclipse.winery.security.JCEKSKeystoreManager;
import org.eclipse.winery.security.KeystoreManager;
import org.eclipse.winery.security.SecurityProcessorFactory;

public class KeystoreManagerFactory {
    public static KeystoreManager getInstance() {
        SecurityProcessorFactory.allowUnlimitedEncryption();
        RepositoryFileReference keystoreRef = new RepositoryFileReference(new KeystoreId(), JCEKSKeystoreManager.KEYSTORE_NAME);
        FilebasedRepository fr = (FilebasedRepository) RepositoryFactory.getRepository();
        fr.flagAsExisting(keystoreRef.getParent());
        String keystorePath = fr.ref2AbsolutePath(keystoreRef).toString();
        return new JCEKSKeystoreManager(keystorePath);
    }
}
