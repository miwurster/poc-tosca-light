/*******************************************************************************
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

import javax.xml.bind.JAXBException;

import org.eclipse.winery.model.tosca.Definitions;
import org.eclipse.winery.model.tosca.yaml.TServiceTemplate;
import org.eclipse.winery.repository.JAXBSupport;
import org.eclipse.winery.repository.backend.IRepository;
import org.eclipse.winery.repository.backend.RepositoryFactory;
import org.eclipse.winery.repository.backend.filebased.GitBasedRepository;
import org.eclipse.winery.repository.backend.filebased.YamlRepository;
import org.eclipse.winery.repository.converter.X2YConverter;
import org.eclipse.winery.repository.converter.support.writer.YamlWriter;
import org.eclipse.winery.repository.exceptions.WineryRepositoryException;

public class YAMLDefinitionsBasedCsarEntry implements CsarEntry {
    private TServiceTemplate definitions;

    public YAMLDefinitionsBasedCsarEntry(Definitions definitions) {
        assert (definitions != null);
        try {
            IRepository repo = RepositoryFactory.getRepository();
            X2YConverter c;
            if (repo instanceof GitBasedRepository) {
                GitBasedRepository wrapper = (GitBasedRepository) RepositoryFactory.getRepository();
                c = new X2YConverter((YamlRepository) wrapper.getRepository());
            } else if (repo instanceof YamlRepository) {
                c = new X2YConverter((YamlRepository) repo);
            } else {
                throw new WineryRepositoryException("The chosen repository mode is incompatible with YAML-based export");
            }
            this.definitions = c.convert(definitions, true);
        } catch (WineryRepositoryException e) {
            e.printStackTrace();
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        YamlWriter writer = new YamlWriter();
        InputStream is = writer.writeToInputStream(definitions);
        if (Objects.nonNull(is)) {
            return is;
        }
        throw new IOException("Failed to convert XML to YAML");
    }

    @Override
    public void writeToOutputStream(OutputStream outputStream) throws IOException {
        try {
            // TODO
            JAXBSupport.createMarshaller(true).marshal(definitions, outputStream);
        } catch (JAXBException e) {
            throw new IOException(e);
        }
    }
}
