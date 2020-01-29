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

package org.eclipse.winery.model.adaptation.qcdeployment;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.namespace.QName;

import org.eclipse.winery.common.RepositoryFileReference;
import org.eclipse.winery.common.ids.definitions.ArtifactTemplateId;
import org.eclipse.winery.model.tosca.TNodeTemplate;
import org.eclipse.winery.model.tosca.TParameter;
import org.eclipse.winery.repository.backend.IRepository;
import org.eclipse.winery.repository.backend.RepositoryFactory;
import org.eclipse.winery.repository.datatypes.ids.elements.ArtifactTemplateFilesDirectoryId;

import com.amazonaws.util.StringInputStream;
import org.apache.commons.io.IOUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.slf4j.LoggerFactory;

public class FlaskPythonAPIGenerator extends Generator {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(FlaskPythonAPIGenerator.class);
    private static final IRepository repo = RepositoryFactory.getRepository();
    private String algoName;

    public FlaskPythonAPIGenerator(Extractor extractor) {
        super(extractor);
    }

    @Override
    public void generateApi(TNodeTemplate node, QName selectedArtifact, OutputStream output) {
        algoName = extractor.getAlgorithmName(node);
        if (algoName.contains("_")) {
            algoName = algoName.substring(0, algoName.indexOf("_"));
        }

        if (node == null) {
            LOGGER.error("Cannot create API for empty node. Cancelling...");
            return;
        }
        if (selectedArtifact == null) {
            LOGGER.error("Cannot create API without a selected artifact. Cancelling...");
            return;
        }
        LOGGER.debug("Generate API implementation for {} using artifact {}", node.getId(), selectedArtifact);

        try (final ZipOutputStream zos = new ZipOutputStream(output)) {
//            addFileToZip("flasktemplate/requirements.txt", algoName + "/requirements.txt", zos);
            addFileToZip("flasktemplate/templates/api.html", algoName + "/templates/api.html", zos);
            addFileToZip("flasktemplate/templates/base.html", algoName + "/templates/base.html", zos);
            addFileToZip("flasktemplate/templates/service.html", algoName + "/templates/service.html", zos);
            addFileToZip("flasktemplate/startup.sh", algoName + "/startup.sh", zos);

            String fileName = "main.py";
            for (RepositoryFileReference file : repo.getContainedFiles(new ArtifactTemplateFilesDirectoryId(new ArtifactTemplateId(selectedArtifact)))) {
                InputStream inputStream = repo.newInputStream(file);
                if ("requirements.txt".equals(file.getFileName())) {
                    String requirements = combineRequirements(inputStream, this.getClass().getResourceAsStream("flasktemplate/requirements.txt"));
                    InputStream stringInputStream = new StringInputStream(requirements);
                    addFileToZip(stringInputStream, algoName + "/requirements.txt", zos);
                } else {
                    // TODO find a more appropriate way to get the correct filename (Problem with multiple files)
                    fileName = file.getFileName();
                    addFileToZip(inputStream, algoName + "/" + file.getFileName(), zos);
                }
            }

            addFileToZip(fillTemplate(node, fileName), algoName + "/app.py", zos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String combineRequirements(InputStream inputStream, InputStream resourceAsStream) throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(inputStream, writer, StandardCharsets.UTF_8);
        writer.append("\n");
        IOUtils.copy(resourceAsStream, writer, StandardCharsets.UTF_8);

        String[] lines = writer.toString().split("\n");
        StringBuilder result = new StringBuilder();
        Set<String> alreadyPresent = new HashSet<>();
        for (String line : lines) {
            if (!line.isEmpty() && !alreadyPresent.contains(line)) {
                result.append(line).append("\n");
                alreadyPresent.add(line);
            }
        }
        return result.toString();
    }

    private void addFileToZip(InputStream is, String targetPath, ZipOutputStream zos) throws IOException {
        zos.putNextEntry(new ZipEntry(targetPath));
        IOUtils.copy(is, zos);
        zos.closeEntry();
    }

    private void addFileToZip(String sourcePath, String targetPath, ZipOutputStream zos) throws IOException {
        addFileToZip(this.getClass().getResourceAsStream(sourcePath), targetPath, zos);
    }

    private InputStream fillTemplate(TNodeTemplate node, String fileName) {
        @NonNull List<TParameter> inputParameters = extractor.getAllInputParameters(node);
        StringBuilder prameterStringBuilder = new StringBuilder("[");
        StringBuilder mainParameterStringBuilder = new StringBuilder("main_")
            .append(algoName)
            .append("(");

        for (int i = 0; i < inputParameters.size(); i++) {
            TParameter inputParameter = inputParameters.get(0);
            LOGGER.debug("Add input parameter: {} ({})", inputParameter.getName(), inputParameter.getType());
            if (i != 0) {
                prameterStringBuilder.append(", ");
                mainParameterStringBuilder.append(", ");
            }

            mainParameterStringBuilder.append(inputParameter.getName())
                .append("=params.get(\"")
                .append(inputParameter.getName())
                .append("\"))");

            prameterStringBuilder.append("{'name': '")
                .append(inputParameter.getName())
                .append("', 'description': '', 'type': '")
                .append(getPythonType(inputParameter.getType()))
                .append("'}");
        }
        prameterStringBuilder.append("]");

        String content = "";
        try (InputStream inputStream = this.getClass().getResourceAsStream("flasktemplate/app.py")) {
            StringWriter writer = new StringWriter();
            IOUtils.copy(inputStream, writer, StandardCharsets.UTF_8);
            content = writer.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }

        content = content.replace("$PARAMETERS$", prameterStringBuilder.toString());
        content = content.replace("$SERVICE_NAME$", "\"" + algoName + "\"");
        content = content.replace("$IMPLEMENTATIONS$", "[{'id': 1, 'name': 'Shor on Qiskit Aqua', 'fw': 'Qiskit'}]"); // TODO remove from Template
        content = content.replace("$MODULE_IMPORT$", "from " + fileName.substring(0, fileName.indexOf(".")) + " import main as main_" + algoName);
        content = content.replace("$MAIN$", mainParameterStringBuilder.toString());

        StringInputStream stringInputStream = null;
        try {
            stringInputStream = new StringInputStream(content);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return stringInputStream;
    }

    private String getPythonType(String type) {
//        return type.substring("xsd:".length())); // TODO maybe integer must be number
        return "number";
    }

    private String getArtifact() {
        // TODO implement
        return "Shor.py";
    }
}
