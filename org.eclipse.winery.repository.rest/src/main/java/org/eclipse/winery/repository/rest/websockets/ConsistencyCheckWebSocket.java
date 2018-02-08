/********************************************************************************
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
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
 ********************************************************************************/
package org.eclipse.winery.repository.rest.websockets;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.winery.repository.backend.consistencycheck.ConsistencyChecker;
import org.eclipse.winery.repository.backend.consistencycheck.ConsistencyCheckerConfiguration;
import org.eclipse.winery.repository.backend.consistencycheck.ConsistencyCheckerProgressListener;
import org.eclipse.winery.repository.backend.consistencycheck.ConsistencyErrorLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

@ServerEndpoint("/checkconsistency")
public class ConsistencyCheckWebSocket implements ConsistencyCheckerProgressListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsistencyCheckWebSocket.class);
    private Session session;

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        LOGGER.info("Opened consistency check web-socket with id: " + session.getId());
    }

    @OnClose
    public void onClose(Session session) throws IOException {
        LOGGER.info("Closing session " + session.getId());
        this.session.close();
        this.session = null;
    }

    @OnError
    public void onError(Throwable t) throws Throwable {
        LOGGER.trace("Error in session " + session.getId(), t);
    }

    @OnMessage
    public void onMessage(String message, Session session) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        ConsistencyCheckerConfiguration config = mapper.readValue(message, ConsistencyCheckerConfiguration.class);

        ConsistencyErrorLogger errorList = ConsistencyChecker.checkCorruptionUsingCsarExport(config, this);

        // Transform object to JSON and send it.
        this.session.getBasicRemote().sendText(mapper.writeValueAsString(errorList));

        // Close the connection after the check has passed.
        onClose(session);
    }

    /**
     * Publishes the current checking progress to the client in JSON format.
     */
    @Override
    public void updateCheckerProgress(float progress) {
        this.session.getAsyncRemote().sendText("{\"progress\":" + progress + "}");
    }

    /**
     * Publishes the detailed checking progress to the customer in JSON format.
     */
    @Override
    public void detailedCheckerProgress(float progress, String checkingDefinition) {
        this.session.getAsyncRemote().sendText("{"
            + "\"progress\":" + progress + ","
            + "\"currentlyChecking\":\"" + checkingDefinition + "\""
            + "}"
        );
    }
}
