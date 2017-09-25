/*******************************************************************************
 * Copyright (c) 2017 University of Stuttgart.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and the Apache License 2.0 which both accompany this distribution,
 * and are available at http://www.eclipse.org/legal/epl-v20.html
 * and http://www.apache.org/licenses/LICENSE-2.0
 *******************************************************************************/
package org.eclipse.winery.cli;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.eclipse.winery.common.RepositoryFileReference;
import org.eclipse.winery.common.ToscaDocumentBuilderFactory;
import org.eclipse.winery.common.ids.Namespace;
import org.eclipse.winery.common.ids.definitions.DefinitionsChildId;
import org.eclipse.winery.common.ids.definitions.EntityTemplateId;
import org.eclipse.winery.common.ids.definitions.EntityTypeId;
import org.eclipse.winery.model.tosca.TEntityTemplate;
import org.eclipse.winery.model.tosca.TEntityType;
import org.eclipse.winery.repository.backend.BackendUtils;
import org.eclipse.winery.repository.backend.IRepository;
import org.eclipse.winery.repository.backend.RepositoryFactory;
import org.eclipse.winery.repository.backend.filebased.FilebasedRepository;
import org.eclipse.winery.repository.exceptions.RepositoryCorruptException;
import org.eclipse.winery.repository.export.CsarExporter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class WineryCli {

	private static final Logger LOGGER = LoggerFactory.getLogger(WineryCli.class);
	private static final String ARTEFACT_BE = "artefact";

	private enum Verbosity {
		OUTPUT_NUMBER_OF_TOSCA_COMPONENTS,
		OUTPUT_CURRENT_TOSCA_COMPONENT_ID,
		OUTPUT_ERROS
	}

	public static void main(String[] args) throws ParseException {
		Option repositoryPathOption = new Option("p", "path", true, "use given path as repository path");
		Option verboseOption = new Option("v", "verbose", false, "be verbose: Output the checked elements");
		Option helpOption = new Option("h", "help", false, "prints this help");

		Options options = new Options();
		options.addOption(repositoryPathOption);
		options.addOption(verboseOption);
		options.addOption(helpOption);
		CommandLineParser parser = new DefaultParser();
		CommandLine line = parser.parse(options, args);

		if (line.hasOption("h")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("winery", options);
			System.exit(0);
		}

		IRepository repository;
		if (line.hasOption("p")) {
			repository = RepositoryFactory.getRepository(Paths.get(line.getOptionValue("p")));
		} else {
			repository = RepositoryFactory.getRepository();
		}
		System.out.println("Using repository path " + ((FilebasedRepository) repository).getRepositoryRoot() + "...");

		EnumSet<Verbosity> verbosity;
		if (line.hasOption("v")) {
			verbosity = EnumSet.of(Verbosity.OUTPUT_NUMBER_OF_TOSCA_COMPONENTS, Verbosity.OUTPUT_CURRENT_TOSCA_COMPONENT_ID, Verbosity.OUTPUT_ERROS);
		} else {
			verbosity = EnumSet.of(Verbosity.OUTPUT_NUMBER_OF_TOSCA_COMPONENTS);
		}

		List<String> errors = checkCorruptionUsingCsarExport(repository, verbosity);

		System.out.println();
		if (errors.isEmpty()) {
			System.out.println("No errors exist.");
		} else {
			System.out.println("Errors in repository found:");
			System.out.println();
			for (String error : errors) {
				System.out.println(error);
			}
			System.exit(1);
		}
	}

	private static List<String> checkCorruptionUsingCsarExport(IRepository repository, EnumSet<Verbosity> verbosity) {
		List<String> res = new ArrayList<>();
		SortedSet<DefinitionsChildId> allDefintionsChildIds = repository.getAllDefinitionsChildIds();
		if (verbosity.contains(Verbosity.OUTPUT_NUMBER_OF_TOSCA_COMPONENTS)) {
			System.out.format("Number of TOSCA definitions to check: %d\n", allDefintionsChildIds.size());
		}
		if (!verbosity.contains(Verbosity.OUTPUT_CURRENT_TOSCA_COMPONENT_ID)) {
			System.out.print("Checking ");
		}

		final Path tempCsar;
		try {
			tempCsar = Files.createTempFile("Export", ".csar");
		} catch (IOException e) {
			LOGGER.debug("Could not create temp CSAR file", e);
			res.add("Could not create temp CSAR file");
			return res;
		}

		for (DefinitionsChildId id : allDefintionsChildIds) {
			if (verbosity.contains(Verbosity.OUTPUT_CURRENT_TOSCA_COMPONENT_ID)) {
				System.out.format("Checking %s...\n", id.toReadableString());
			} else {
				System.out.print(".");
			}

			checkId(res, verbosity, id);
			checkXmlSchemaValidation(repository, res, verbosity, id);
			checkQNames(repository, res, verbosity, id);
			checkPropertiesXmlValidation(repository, res, verbosity, id);
			checkCsar(res, verbosity, id, tempCsar);
		}

		// some console output cleanup
		if (verbosity.contains(Verbosity.OUTPUT_ERROS)) {
			if (!verbosity.contains(Verbosity.OUTPUT_CURRENT_TOSCA_COMPONENT_ID)) {
				System.out.println();
			}
		}

		return res;
	}

	private static void checkQNames(IRepository repository, List<String> res, EnumSet<Verbosity> verbosity, DefinitionsChildId id) {
		if (id instanceof EntityTypeId) {
			final TEntityType entityType = (TEntityType) repository.getDefinitions(id).getElement();
			final TEntityType.PropertiesDefinition propertiesDefinition = entityType.getPropertiesDefinition();
			if (propertiesDefinition != null) {
				@Nullable final QName element = propertiesDefinition.getElement();
				if (element != null) {
					if (StringUtils.isEmpty(element.getNamespaceURI())) {
						printAndAddError(res, verbosity, id, "Referenced element is not a full QName");
					}
				}
			}
		}
	}

	private static void checkXmlSchemaValidation(IRepository repository, List<String> res, EnumSet<Verbosity> verbosity, DefinitionsChildId id) {
		try (InputStream inputStream = repository.newInputStream(BackendUtils.getRefOfDefinitions(id))) {
			DocumentBuilder documentBuilder = ToscaDocumentBuilderFactory.INSTANCE.getSchemaAwareToscaDocumentBuilder();
			StringBuilder errorStringBuilder = new StringBuilder();
			documentBuilder.setErrorHandler(BackendUtils.getErrorHandler(errorStringBuilder));
			documentBuilder.parse(inputStream);
			String errors = errorStringBuilder.toString();
			if (!errors.isEmpty()) {
				printAndAddError(res, verbosity, id, errors);
			}
		} catch (IOException e) {
			printAndAddError(res, verbosity, id, "I/O error during XML validation " + e.getMessage());
		} catch (SAXException e) {
			printAndAddError(res, verbosity, id, "SAX error during XML validation: " + e.getMessage());
		}
	}

	private static void validate(RepositoryFileReference xmlSchemaFileReference, @Nullable Object any, IRepository repository, List<String> res, EnumSet<Verbosity> verbosity, DefinitionsChildId id) {
		if (!(any instanceof Element)) {
			printAndAddError(res, verbosity, id, "any is not instance of Document, but " + any.getClass());
			return;
		}
		Element element = (Element) any;
		SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		InputStream inputStream = null;
		try {
			inputStream = repository.newInputStream(xmlSchemaFileReference);
			Source schemaFile = new StreamSource(inputStream);
			Schema schema = factory.newSchema(schemaFile);
			Validator validator = schema.newValidator();
			validator.validate(new DOMSource(element.getOwnerDocument()));
		} catch (Exception e) {
			printAndAddError(res, verbosity, id, "error during validating XML schema " + e.getMessage());
			try {
				inputStream.close();
			} catch (IOException e1) {
				return;
			}
		}
	}

	public static void checkPropertiesXmlValidation(IRepository repository, List<String> res, EnumSet<Verbosity> verbosity, DefinitionsChildId id) {
		if (id instanceof EntityTemplateId) {
			TEntityTemplate entityTemplate = (TEntityTemplate) repository.getDefinitions(id).getElement();
			final TEntityType entityType = repository.getTypeForTemplate(entityTemplate);
			final TEntityType.PropertiesDefinition propertiesDefinition = entityType.getPropertiesDefinition();
			if (propertiesDefinition != null) {
				final TEntityTemplate.Properties properties = entityTemplate.getProperties();
				if (properties == null) {
					printAndAddError(res, verbosity, id, "Properties required, but no properties defined");
					return;
				}

				@Nullable final Object any = properties.getAny();
				if (any == null) {
					printAndAddError(res, verbosity, id, "Properties required, but no properties defined (any case)");
					return;
				}

				@Nullable final QName element = propertiesDefinition.getElement();
				if (element != null) {
					final Map<String, RepositoryFileReference> mapFromLocalNameToXSD = repository.getXsdImportManager().getMapFromLocalNameToXSD(new Namespace(element.getNamespaceURI(), false), false);
					final RepositoryFileReference repositoryFileReference = mapFromLocalNameToXSD.get(element.getLocalPart());
					if (repositoryFileReference == null) {
						printAndAddError(res, verbosity, id, "No Xml Schema definition found for " + element);
						return;
					}
					validate(repositoryFileReference, any, repository, res, verbosity, id);
				}
			}
		}
	}

	private static void checkId(List<String> res, EnumSet<Verbosity> verbosity, DefinitionsChildId id) {
		checkUri(res, verbosity, id, id.getNamespace().getDecoded());
		checkNcname(res, verbosity, id, id.getXmlId().getDecoded());
	}

	private static void checkNcname(List<String> res, EnumSet<Verbosity> verbosity, DefinitionsChildId id, String ncname) {
		if (!ncname.trim().equals(ncname)) {
			printAndAddError(res, verbosity, id, "local name starts or ends with white spaces");
		}
		if (ncname.contains(ARTEFACT_BE)) {
			printAndAddError(res, verbosity, id, "artifact is spelled with i in American English, not artefact as in British English");
		}
	}

	private static void checkUri(List<String> res, EnumSet<Verbosity> verbosity, DefinitionsChildId id, String uriStr) {
		Objects.requireNonNull(res);
		Objects.requireNonNull(verbosity);
		Objects.requireNonNull(id);
		Objects.requireNonNull(uriStr);
		if (!uriStr.trim().equals(uriStr)) {
			printAndAddError(res, verbosity, id, "Namespace starts or ends with white spaces");
		}
		URI uri;
		try {
			uri = new URI(uriStr);
		} catch (URISyntaxException e) {
			LOGGER.debug("Invalid URI", e);
			printAndAddError(res, verbosity, id, "Invalid URI: " + e.getMessage());
			return;
		}
		if (!uri.isAbsolute()) {
			printAndAddError(res, verbosity, id, "URI is relative");
		}
		if ((uriStr.startsWith("http://www.opentosca.org/") && (!uriStr.toLowerCase().equals(uriStr)))) {
			printAndAddError(res, verbosity, id, "opentosca URI is not lowercase");
		}
		if (uriStr.endsWith("/")) {
			printAndAddError(res, verbosity, id, "URI ends with a slash");
		}
		if (uriStr.contains(ARTEFACT_BE)) {
			printAndAddError(res, verbosity, id, "artifact is spelled with i in American English, not artefact as in British English");
		}
	}

	private static void checkCsar(List<String> res, EnumSet<Verbosity> verbosity, DefinitionsChildId id, Path tempCsar) {
		CsarExporter exporter = new CsarExporter();
		final OutputStream outputStream;
		try {
			outputStream = Files.newOutputStream(tempCsar, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			final String error = "Could not write to temp CSAR file";
			LOGGER.debug(error, e);
			printAndAddError(res, verbosity, id, error);
			return;
		}
		try {
			exporter.writeCsar(RepositoryFactory.getRepository(), id, outputStream);
			try (InputStream inputStream = Files.newInputStream(tempCsar);
				 ZipInputStream zis = new ZipInputStream(inputStream)) {
				ZipEntry entry;
				while ((entry = zis.getNextEntry()) != null) {
					if (entry.getName() == null) {
						printAndAddError(res, verbosity, id, "Empty filename in zip file");
					}
				}
			}
		} catch (ArchiveException | JAXBException | IOException e) {
			LOGGER.debug("Error during checking ZIP", e);
			printAndAddError(res, verbosity, id, "Invalid zip file");
		} catch (RepositoryCorruptException e) {
			LOGGER.debug("Repository is corrupt", e);
			printAndAddError(res, verbosity, id, "Corrupt: " + e.getMessage());
		}
	}

	public static void printAndAddError(List<String> res, EnumSet<Verbosity> verbosity, DefinitionsChildId id, String error) {
		if (verbosity.contains(Verbosity.OUTPUT_ERROS)) {
			if (!verbosity.contains(Verbosity.OUTPUT_CURRENT_TOSCA_COMPONENT_ID)) {
				System.out.println();
			}
			System.out.println(error);
		}
		res.add(id.toReadableString() + ": " + error);
	}
}
