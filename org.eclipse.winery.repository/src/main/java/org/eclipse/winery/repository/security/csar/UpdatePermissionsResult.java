/********************************************************************************
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
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

import java.io.Serializable;

public class UpdatePermissionsResult implements Serializable {
    private static final long serialVersionUID = -2243360914248376611L;
    private int addedKeysCount;
    private int badGiversCount;

    UpdatePermissionsResult() {

    }

    UpdatePermissionsResult(int addedKeysCount, int badGiversCount) {
        this.addedKeysCount = addedKeysCount;
        this.badGiversCount = badGiversCount;
    }

    public int getBadGiversCount() {
        return badGiversCount;
    }

    public void setBadGiversCount(int badGiversCount) {
        this.badGiversCount = badGiversCount;
    }

    public int getAddedKeysCount() {
        return addedKeysCount;
    }

    public void setAddedKeysCount(int addedKeysCount) {
        this.addedKeysCount = addedKeysCount;
    }
}
