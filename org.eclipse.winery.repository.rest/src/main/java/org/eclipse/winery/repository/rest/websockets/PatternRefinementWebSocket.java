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

package org.eclipse.winery.repository.rest.websockets;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.eclipse.winery.common.ids.definitions.ServiceTemplateId;
import org.eclipse.winery.model.substitution.refinement.AbstractRefinement;
import org.eclipse.winery.model.substitution.refinement.RefinementCandidate;
import org.eclipse.winery.model.substitution.refinement.RefinementChooser;
import org.eclipse.winery.model.substitution.refinement.patterns.PatternRefinement;
import org.eclipse.winery.model.substitution.refinement.tests.TestRefinement;
import org.eclipse.winery.model.tosca.TTopologyTemplate;
import org.eclipse.winery.repository.rest.resources.apiData.RefinementElementApiData;
import org.eclipse.winery.repository.rest.resources.apiData.RefinementWebSocketApiData;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServerEndpoint(value = "/refinetopology")
public class PatternRefinementWebSocket implements RefinementChooser {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsistencyCheckWebSocket.class);

    private Session session;
    private ObjectMapper mapper = new ObjectMapper();
    private AbstractRefinement patternRefinement;
    private CompletableFuture<Integer> future;
    private boolean running = false;
    private ServiceTemplateId refinementServiceTemplate;

    @OnOpen
    public void onOpen(Session session) throws IOException {
        this.session = session;
        Map<String, List<String>> requestParameterMap = session.getRequestParameterMap();

        List<String> refinementType = requestParameterMap.get("type");
        if (Objects.nonNull(refinementType)) {
            String type = refinementType.get(0);
            if (type.equals("patterns")) {
                this.patternRefinement = new PatternRefinement(this);
            } else if (type.equals("tests")) {
                this.patternRefinement = new TestRefinement(this);
            }
            if (Objects.nonNull(this.patternRefinement)) {
                LOGGER.info("Opened consistency check web-socket with id: " + session.getId());
                return;
            }
        }

        this.onClose(this.session);
        LOGGER.debug("Closed session due to missing or incompatible refinement type!");
    }

    @OnClose
    public void onClose(Session session) throws IOException {
        LOGGER.info("Closing session " + session.getId());
        this.session.close();
        this.session = null;
    }

    @OnError
    public void onError(Throwable t) throws Throwable {
        LOGGER.error("Error in session " + session.getId(), t);
    }

    @OnMessage
    public void onMessage(String message) throws IOException {
        RefinementWebSocketApiData data = this.mapper.readValue(message, RefinementWebSocketApiData.class);

        switch (data.task) {
            case START:
                if (!running) {
                    Thread thread = new Thread(() -> {
                        RefinementElementApiData element = new RefinementElementApiData();
                        element.serviceTemplateContainingRefinements = patternRefinement.refineServiceTemplate(new ServiceTemplateId(data.serviceTemplate));
                        try {
                            this.send(element);
                            session.close();
                            session = null;
                        } catch (JsonProcessingException e) {
                            LOGGER.error("Error while sending refinement result", e);
                        } catch (IOException e) {
                            LOGGER.error("Error while closing the session", e);
                        }
                        running = false;
                    });
                    this.running = true;
                    thread.start();
                }
                break;
            case REFINE_WITH:
                this.future.complete(data.refineWith);
                break;
            case STOP:
                this.future.complete(-1);
                this.send(new RefinementElementApiData(null, this.refinementServiceTemplate, null));
                this.onClose(this.session);
                break;
        }
    }

    @Override
    public RefinementCandidate chooseRefinement(List<RefinementCandidate> candidates,
                                                ServiceTemplateId refinementServiceTemplate, TTopologyTemplate topology) {
        this.refinementServiceTemplate = refinementServiceTemplate;
        try {
            this.future = new CompletableFuture<>();

            RefinementElementApiData element = new RefinementElementApiData(candidates, refinementServiceTemplate, topology);
            this.send(element);

            int id = future.get();

            if (id >= 0) {
                return candidates.stream()
                    .filter(candidate -> candidate.getId() == id)
                    .findFirst()
                    .orElseThrow(NotFoundException::new);
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Error while retrieving ");
        } catch (JsonProcessingException e) {
            LOGGER.error("Error while creating JSON request", e);
        }

        return null;
    }

    private void send(RefinementElementApiData element) throws JsonProcessingException {
        this.session.getAsyncRemote().sendText(mapper.writeValueAsString(element));
    }
}
