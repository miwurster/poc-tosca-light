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
package org.eclipse.winery.accountability.exceptions;

public class ParticipantPublicKeyNotSetException extends EthereumException {
    public ParticipantPublicKeyNotSetException() {
        super("The official public key of the participant is not set");
    }

    public ParticipantPublicKeyNotSetException(String message) {
        super(message);
    }

    public ParticipantPublicKeyNotSetException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParticipantPublicKeyNotSetException(Throwable cause) {
        super(cause);
    }
}
