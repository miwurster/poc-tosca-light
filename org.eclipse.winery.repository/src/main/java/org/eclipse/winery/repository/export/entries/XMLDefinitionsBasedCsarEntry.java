/********************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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
package org.eclipse.winery.repository.export.entries;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.bind.JAXBException;

import org.eclipse.winery.model.tosca.Definitions;
import org.eclipse.winery.repository.JAXBSupport;

/**
 * Provides access to an entry that represents a TOSCA definition.
 */
public class XMLDefinitionsBasedCsarEntry implements CsarEntry {
    private Definitions definitions;

    public XMLDefinitionsBasedCsarEntry(Definitions definitions) {
        assert (definitions != null);
        this.definitions = definitions;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            JAXBSupport.createMarshaller(true).marshal(definitions, byteArrayOutputStream);
            return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        } catch (JAXBException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void writeToOutputStream(OutputStream outputStream) throws IOException {
        try {
            JAXBSupport.createMarshaller(true).marshal(definitions, outputStream);
        } catch (JAXBException e) {
            throw new IOException(e);
        }
    }
}
