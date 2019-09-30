package org.eclipse.winery.repository.backend.filebased;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.plaf.multi.MultiInternalFrameUI;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import org.eclipse.winery.common.RepositoryFileReference;
import org.eclipse.winery.common.Util;
import org.eclipse.winery.common.ids.GenericId;
import org.eclipse.winery.common.ids.Namespace;
import org.eclipse.winery.common.ids.XmlId;
import org.eclipse.winery.common.ids.definitions.ArtifactTemplateId;
import org.eclipse.winery.common.ids.definitions.DefinitionsChildId;
import org.eclipse.winery.common.ids.definitions.NodeTypeId;
import org.eclipse.winery.common.ids.definitions.NodeTypeImplementationId;
import org.eclipse.winery.common.ids.definitions.RelationshipTypeId;
import org.eclipse.winery.common.ids.definitions.RelationshipTypeImplementationId;
import org.eclipse.winery.common.version.VersionUtils;
import org.eclipse.winery.common.version.WineryVersion;
import org.eclipse.winery.model.tosca.Definitions;
import org.eclipse.winery.model.tosca.TArtifactTemplate;
import org.eclipse.winery.model.tosca.yaml.TArtifactDefinition;
import org.eclipse.winery.model.tosca.yaml.TImportDefinition;
import org.eclipse.winery.model.tosca.yaml.TInterfaceDefinition;
import org.eclipse.winery.model.tosca.yaml.TNodeType;
import org.eclipse.winery.model.tosca.yaml.TOperationDefinition;
import org.eclipse.winery.model.tosca.yaml.TServiceTemplate;
import org.eclipse.winery.model.tosca.yaml.support.Defaults;
import org.eclipse.winery.model.tosca.yaml.support.TMapImportDefinition;
import org.eclipse.winery.repository.Constants;
import org.eclipse.winery.repository.backend.BackendUtils;
import org.eclipse.winery.repository.backend.filebased.converter.Y2XConverter;
import org.eclipse.winery.repository.backend.filebased.converter.X2YConverter;
import org.eclipse.winery.repository.backend.filebased.converter.support.exception.MultiException;
import org.eclipse.winery.repository.backend.filebased.converter.support.reader.yaml.Reader;
import org.eclipse.winery.repository.backend.filebased.converter.support.writer.yaml.Writer;
import org.eclipse.winery.repository.configuration.FileBasedRepositoryConfiguration;
import org.eclipse.winery.repository.targetallocation.criteria.minexternalconnections.ConnectsToGraph;

import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import com.sun.org.omg.CORBA.OperationDescription;
import jdk.internal.util.xml.impl.Input;
import org.apache.tika.mime.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YamlBasedRepository extends FilebasedRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(YamlBasedRepository.class);
    private final String nameRegex;
    private final Pattern namePattern;
    
    public YamlBasedRepository(FileBasedRepositoryConfiguration fileBasedRepositoryConfiguration) {
        super(Objects.requireNonNull(fileBasedRepositoryConfiguration));
        this.nameRegex = "(.*)@(.*)@(.*)";
        this.namePattern = Pattern.compile(nameRegex);
    }

    @Override
    public Path ref2AbsolutePath(RepositoryFileReference ref) {
        Path resultPath = id2AbsolutePath(ref.getParent());
        Optional<Path> subDirectory = ref.getSubDirectory();
        if (subDirectory.isPresent()) {
            resultPath = resultPath.resolve(subDirectory.get());
        }
        GenericId convertedId = convertGenericId(ref.getParent());
        if (convertedId != null) {
            if (convertedId instanceof DefinitionsChildId) {
               String convertedFilename = BackendUtils.getFileNameOfDefinitions((DefinitionsChildId) convertedId);
               return resultPath.resolve(convertedFilename);
            }
        }
        return resultPath.resolve(ref.getFileName());
    }

    @Override
    public Path id2AbsolutePath(GenericId id) {
        GenericId convertedId = convertGenericId(id);
        if (convertedId != null) {
            return super.id2AbsolutePath(convertedId);
        } else {
            return super.id2AbsolutePath(id);
        }
    }

    private GenericId convertGenericId(GenericId id) {
        if (id instanceof NodeTypeImplementationId) {
            return new NodeTypeId(((NodeTypeImplementationId) id).getQName());
        } else if (id instanceof RelationshipTypeImplementationId) {
            return new RelationshipTypeId(((RelationshipTypeImplementationId) id).getQName());
        } else if (id instanceof ArtifactTemplateId) {
            QName qName = ((ArtifactTemplateId) id).getQName();
            Matcher nameMatcher = namePattern.matcher(qName.getLocalPart());
            if (nameMatcher.matches()) {
                String typeName = nameMatcher.group(2);
                if (nameMatcher.group(3).equalsIgnoreCase("nodetypes")) {
                    return new NodeTypeId(new QName(qName.getNamespaceURI(), typeName));
                } else {
                    return new RelationshipTypeId(new QName(qName.getNamespaceURI(), typeName));
                }
                
            } else {
                return new NodeTypeId(new QName(qName.getNamespaceURI(), "Cache"));
            }
        }
        return null;
    }
    
    private <T extends DefinitionsChildId> List<Class<T>> convertDefinitionsChildIdIfNeeded(List<Class<T>> idClasses) {
        List<Class<T>> output = new ArrayList<>();
        if (idClasses.size() == 1) {
            Class<T> idClass = idClasses.get(0);
            if (NodeTypeImplementationId.class.isAssignableFrom(idClass)) {
                output.add((Class<T>) NodeTypeId.class);
                return output;
            } else if (RelationshipTypeImplementationId.class.isAssignableFrom(idClass)) {
                output.add((Class<T>) RelationshipTypeId.class);
                return output;
            } else if (ArtifactTemplateId.class.isAssignableFrom(idClass)) {
                output.add((Class<T>) NodeTypeId.class);
                output.add((Class<T>) RelationshipTypeId.class);
                return output;
            }
        }
        return idClasses;
    }

    @Override
    public boolean exists(GenericId id) {
        Path targetPath = id2AbsolutePath(id);
        if (id instanceof ArtifactTemplateId) {
            GenericId convertedId = convertGenericId(id);
            if (convertedId != null) {
                String convertedFilename = BackendUtils.getFileNameOfDefinitions((DefinitionsChildId) convertedId);
                targetPath = targetPath.resolve(convertedFilename);
                return chechIfArtifactTemplateExists(targetPath, ((ArtifactTemplateId) id).getQName());
            }
        }
        return Files.exists(targetPath);
    }

    @Override
    public boolean exists(RepositoryFileReference ref) {
        Path targetPath = this.ref2AbsolutePath(ref);
        if (ref.getParent() instanceof ArtifactTemplateId) {
            if (Files.exists(targetPath)) {
                return chechIfArtifactTemplateExists(targetPath, ((ArtifactTemplateId) ref.getParent()).getQName());
            }
        }
        return Files.exists(targetPath);
    }
    
    private String getNameOfArtifactFromArtifactName(String name) {
        Matcher nameMatcher = namePattern.matcher(name);
        if (nameMatcher.matches()) {
            return nameMatcher.group(1);    
        }
        return name;
    }
    
    private String getNameOfTypeFromArtifactName(String name) {
        Matcher nameMatcher = namePattern.matcher(name);
        if (nameMatcher.matches()) {
            return nameMatcher.group(2);
        }
        return "Cache";
    }

    @Override
    public Definitions definitionsFromRef(RepositoryFileReference ref) throws IOException {
        Path targetPath = this.ref2AbsolutePath(ref);
        if (ref.getParent() instanceof DefinitionsChildId) {
            try {
                QName name = ((DefinitionsChildId) ref.getParent()).getQName();
                Definitions definitions = convertToDefinitions(targetPath, name.getLocalPart(), name.getNamespaceURI());
                return getRequestedDefinition((DefinitionsChildId) ref.getParent(), definitions);
            } catch (MultiException e) {
                LOGGER.debug("Internal error", e);
            }
        }
        return null;
    }
    
    
    private Definitions getRequestedDefinition(DefinitionsChildId id, Definitions definitions) {
        if (id instanceof NodeTypeId) {
            Definitions.Builder requestedDefinitions = getEmptyDefinition(definitions);
            requestedDefinitions.addNodeTypes(definitions.getNodeTypes());
            return requestedDefinitions.build();
        } else if (id instanceof RelationshipTypeId) {
            Definitions.Builder requestedDefinitions = getEmptyDefinition(definitions);
            requestedDefinitions.addRelationshipTypes(definitions.getRelationshipTypes());
            Definitions output =  requestedDefinitions.build();
            return output;
        } else if (id instanceof NodeTypeImplementationId) {
            Definitions.Builder requestedDefinitions = getEmptyDefinition(definitions);
            requestedDefinitions.addNodeTypeImplementations(definitions.getNodeTypeImplementations());
            return requestedDefinitions.build();
        } else if (id instanceof RelationshipTypeImplementationId) {
            Definitions.Builder requestedDefinitions = getEmptyDefinition(definitions);
            requestedDefinitions.addRelationshipTypeImplementations(definitions.getRelationshipTypeImplementations());
            return requestedDefinitions.build();
        } else if (id instanceof ArtifactTemplateId) {
            String artifactName = getNameOfArtifactFromArtifactName(id.getQName().getLocalPart());
            List<TArtifactTemplate> artifactTemplates = definitions.getArtifactTemplates();
            List<TArtifactTemplate> requestedArtifactTemplates = new ArrayList<>();
            if (artifactTemplates != null) {
                for (TArtifactTemplate artifactTemplate : artifactTemplates) {
                    if (artifactTemplate.getId().equalsIgnoreCase(artifactName)) {
                        requestedArtifactTemplates.add(artifactTemplate);
                        Definitions.Builder requestedDefinitions = getEmptyDefinition(definitions);
                        requestedDefinitions.addArtifactTemplates(requestedArtifactTemplates);
                        return requestedDefinitions.build();
                    }
                }
            }
        }
        return definitions;
    }
    
    private Definitions.Builder getEmptyDefinition(Definitions definitions) {
        return (new Definitions.Builder(definitions.getId(), definitions.getTargetNamespace()
        ));
    }

    private boolean chechIfArtifactTemplateExists(Path targetPath, QName qName) {
        try {
            Definitions xmlDefinitions = convertToDefinitions(targetPath, getNameOfTypeFromArtifactName(qName.getLocalPart()), qName.getNamespaceURI());
            List<TArtifactTemplate> artifacts = xmlDefinitions.getArtifactTemplates();
            if (artifacts != null) {
                for (TArtifactTemplate artifact : artifacts) {
                    if (artifact.getId().equalsIgnoreCase(getNameOfArtifactFromArtifactName(qName.getLocalPart()))) {
                        return true;
                    }
                }
            }
            
        } catch (IOException | MultiException e) {
            LOGGER.debug("Internal error", e);
        }
        return false;
        
    }
    
    private Definitions convertToDefinitions(Path targetPath, String id, String targetNamespace) throws IOException, MultiException{
        TServiceTemplate serviceTemplate = readServiceTemplate(targetPath);
        Y2XConverter converter = new Y2XConverter();
        return converter.convert(serviceTemplate, id, targetNamespace);
    }
    
    private TServiceTemplate readServiceTemplate(Path targetPath) throws IOException, MultiException {
        InputStream in = newInputStream(targetPath);
        return Reader.getReader().parse(in);
    }
    private TServiceTemplate readServiceTemplate(RepositoryFileReference ref) throws IOException, MultiException {
        Path targetPath = ref2AbsolutePath(ref);
        InputStream in = newInputStream(targetPath);
        return Reader.getReader().parse(in);
    }
    
    private Definitions readInputStream(InputStream inputStream) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(Definitions.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        return (Definitions) unmarshaller.unmarshal(inputStream);
    }
    
    private <T extends DefinitionsChildId> List<String> getAllArtifactNamesFromType(Path target, Class<T> idClass, String targetNamespace) {
        List<String> output = new ArrayList<>();
        try {
            String fileName = BackendUtils.getFileNameOfDefinitions(idClass);
            String id = target.getFileName().toString();
            target = target.resolve(fileName);
            Definitions definitions = convertToDefinitions(target, id, targetNamespace);
            List<TArtifactTemplate> artifactTemplates = definitions.getArtifactTemplates();
            if (artifactTemplates != null) {
                for (TArtifactTemplate artifactTemplate : artifactTemplates) {
                    output.add(artifactTemplate.getId() + "@" + id);
                }
            }
        } catch (MultiException | IOException e) {
            LOGGER.debug("Internal error", e);
        } 
        return output;
    }

    @Override
    public void putContentToFile(RepositoryFileReference ref, InputStream inputStream, MediaType mediaType) throws IOException {
//        if (mediaType == null) {
//            // quick hack for storing mime type called this method
//            assert (ref.getFileName().endsWith(Constants.SUFFIX_MIMETYPE));
//            // we do not need to store the mime type of the file containing the mime type information
//        } else {
//            this.setMimeType(ref, mediaType);
//        }
        Path targetPath = this.ref2AbsolutePath(ref);
        inputStream = convertToServiceTemplate(ref, inputStream);
        writeInputStreamToPath(targetPath, inputStream);
        if (ref.getParent() instanceof NodeTypeImplementationId) {
            clearCache();
        }
    }
    
    
    private InputStream convertToServiceTemplate(RepositoryFileReference ref, InputStream inputStream) {
        ByteArrayOutputStream outputStream = convertInputStream(inputStream);
        try {
            Definitions definitions = readInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
            X2YConverter converter = new X2YConverter(this);
            TServiceTemplate serviceTemplate;
            if (ref.getParent() instanceof NodeTypeImplementationId) {
                serviceTemplate = readServiceTemplate(ref);
                serviceTemplate = converter.convertNodeTypeImplementation(serviceTemplate, definitions.getNodeTypeImplementations().get(0));
            } else if (ref.getParent() instanceof NodeTypeId) {
                serviceTemplate = converter.convert(definitions);
                if (exists(ref)) { 
                    TServiceTemplate oldServiceTemplate = readServiceTemplate(ref);
                    serviceTemplate = replaceOldWithNewData(serviceTemplate, oldServiceTemplate);
                }
            } else if (ref.getParent() instanceof ArtifactTemplateId){
                TArtifactTemplate artifactTemplate = definitions.getArtifactTemplates().get(0);
                TArtifactDefinition artifact = converter.convertArtifactTemplate(artifactTemplate);
                List<TMapImportDefinition> imports = converter.convertImports();
                Path targetPath = ref2AbsolutePath(ref);
                if (Files.exists(targetPath)) {
                    serviceTemplate = readServiceTemplate(targetPath);
                    if (serviceTemplate == null) {
                        serviceTemplate = createNewCacheNodeTypeWithArtifact(ref,artifactTemplate,artifact, imports);
                    } else {
                        TNodeType nodeType = serviceTemplate.getNodeTypes().entrySet().iterator().next().getValue();
                        Map<String, TArtifactDefinition> artifacts = nodeType.getArtifacts();
                        if (artifacts.containsKey(artifactTemplate.getIdFromIdOrNameField())) {
                            artifacts.replace(artifactTemplate.getIdFromIdOrNameField(), artifact);
                        } else {
                            artifacts.put(artifactTemplate.getIdFromIdOrNameField(), artifact);
                        }
                        nodeType.setArtifacts(artifacts);
                        serviceTemplate.getNodeTypes().entrySet().iterator().next().setValue(nodeType);
                        serviceTemplate.setImports(addImports(serviceTemplate.getImports(), imports));
                    }
                } else {
                    serviceTemplate = createNewCacheNodeTypeWithArtifact(ref,artifactTemplate,artifact, imports);
                }
            } else {
                serviceTemplate = converter.convert(definitions);
            }
            Writer writer = new Writer();
            InputStream output = writer.writeToInputStream(serviceTemplate);
            return output;
        } catch (MultiException | IOException e) {
            LOGGER.debug("Internal error", e);
        } catch (JAXBException e) {
            return new ByteArrayInputStream(outputStream.toByteArray());
        }
        return null;
    }
    
    private List<TMapImportDefinition> addImports(List<TMapImportDefinition> oldImports, List<TMapImportDefinition> newImport) {
        if (newImport.isEmpty()) {return oldImports;}
        if (newImport.get(0).isEmpty()) {return oldImports;}
        Map.Entry<String, TImportDefinition> targetImport = newImport.get(0).entrySet().iterator().next();
        for (TMapImportDefinition tMapImportDefinition : oldImports) {
            for(Map.Entry<String, TImportDefinition> tImportDefinitionEntry : tMapImportDefinition.entrySet()) {
                if (tImportDefinitionEntry.getKey().equalsIgnoreCase(targetImport.getKey())) {
                    if (tImportDefinitionEntry.getValue().equals(targetImport.getValue())) {
                        return oldImports;
                    }
                }
            }
        }
        oldImports.get(0).put(targetImport.getKey(), targetImport.getValue());
        return oldImports;
    }
    
    private TServiceTemplate createNewCacheNodeTypeWithArtifact(RepositoryFileReference ref, TArtifactTemplate artifactTemplate, TArtifactDefinition artifact, List<TMapImportDefinition> imports) {
        TServiceTemplate serviceTemplate = createEmptyCacheNodeType(((ArtifactTemplateId) ref.getParent()).getQName().getNamespaceURI());
        Map<String, TArtifactDefinition> artifacts = new LinkedHashMap<>();
        artifacts.put(artifactTemplate.getIdFromIdOrNameField(), artifact);
        serviceTemplate.getNodeTypes().entrySet().iterator().next().getValue().setArtifacts(artifacts);
        serviceTemplate.setImports(imports);
        return serviceTemplate;
    }
    
    private TServiceTemplate createEmptyCacheNodeType(String targetNamespace) {
        return new TServiceTemplate.Builder(Defaults.TOSCA_DEFINITIONS_VERSION)
                .setNodeType("Cache", (new TNodeType.Builder().addMetadata("targetNamespace", targetNamespace).build()))
            .build();
    }
    
    private void clearCache() {
        SortedSet<ArtifactTemplateId> artifacts = getAllDefinitionsChildIds(ArtifactTemplateId.class);
        for (ArtifactTemplateId artifact : artifacts) {
            if (getNameOfTypeFromArtifactName(artifact.getQName().getLocalPart()).equalsIgnoreCase("cache")) {
                for (ArtifactTemplateId otherArtifact : artifacts) {
                    if (otherArtifact.getQName().getNamespaceURI().equalsIgnoreCase(artifact.getQName().getNamespaceURI())
                        && getNameOfArtifactFromArtifactName(otherArtifact.getQName().getLocalPart()).equalsIgnoreCase(getNameOfArtifactFromArtifactName(artifact.getQName().getLocalPart()))
                        && !getNameOfTypeFromArtifactName(otherArtifact.getQName().getLocalPart()).equalsIgnoreCase("cache")){
                        forceDelete(artifact);
                    }
                }
            }
        }
    }
    
    private TServiceTemplate replaceOldWithNewData(TServiceTemplate newData, TServiceTemplate oldData) {
        TNodeType oldNodeType = oldData.getNodeTypes().entrySet().iterator().next().getValue();
        TNodeType newNodeType = newData.getNodeTypes().entrySet().iterator().next().getValue();
        oldNodeType.setMetadata(newNodeType.getMetadata());
        oldNodeType.setProperties(newNodeType.getProperties());
        oldNodeType.setDerivedFrom(newNodeType.getDerivedFrom());
        oldNodeType.setDescription(newNodeType.getDescription());
        oldNodeType.setRequirements(newNodeType.getRequirements());
        oldNodeType.setCapabilities(newNodeType.getCapabilities());
        Map<String, TInterfaceDefinition> oldInterfaces = oldNodeType.getInterfaces();
        Map<String, TInterfaceDefinition> newInterfaces = newNodeType.getInterfaces();
        for (Map.Entry<String, TInterfaceDefinition> oldInterface : oldInterfaces.entrySet()) {
            TInterfaceDefinition newInterfaceDefinition = newInterfaces.get(oldInterface.getKey());
            if (newInterfaceDefinition != null) {
                Map<String, TOperationDefinition> oldOperationDefinitions = oldInterface.getValue().getOperations();
                Map<String, TOperationDefinition> newOperationDefinitions = newInterfaceDefinition.getOperations();
                for (Map.Entry<String, TOperationDefinition> oldOperationDefinition : oldOperationDefinitions.entrySet()) {
                    TOperationDefinition newOperationDefinition = newOperationDefinitions.get(oldOperationDefinition.getKey());
                    if (newOperationDefinition != null) {
                        newOperationDefinition.setImplementation(oldOperationDefinition.getValue().getImplementation());
                        newOperationDefinitions.remove(oldOperationDefinition.getKey());
                        newOperationDefinitions.put(oldOperationDefinition.getKey(), newOperationDefinition);
                    }
                }
                newInterfaceDefinition.setOperations(newOperationDefinitions);
            }
            newInterfaces.remove(oldInterface.getKey());
            newInterfaces.put(oldInterface.getKey(), newInterfaceDefinition);
        }
        oldNodeType.setInterfaces(newInterfaces);
        oldData.getNodeTypes().entrySet().iterator().next().setValue(oldNodeType);
        return oldData;
    }

    @Override
    public <T extends DefinitionsChildId> SortedSet<T> getDefinitionsChildIds(Class<T> inputIdClass, boolean omitDevelopmentVersions) {
        SortedSet<T> res = new TreeSet<>();
        List<Class<T>> idClasses = new ArrayList<>();
        idClasses.add(inputIdClass);
        idClasses = convertDefinitionsChildIdIfNeeded(idClasses);
        for (Class<T> idClass : idClasses) {
            String rootPathFragment = Util.getRootPathFragment(idClass);
            Path dir = this.repositoryRoot.resolve(rootPathFragment);
            if (!Files.exists(dir)) {
                // return empty list if no ids are available
                return res;
            }
            assert (Files.isDirectory(dir));

            final OnlyNonHiddenDirectories onhdf = new OnlyNonHiddenDirectories();

            // list all directories contained in this directory
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, onhdf)) {
                for (Path nsP : ds) {
                    // the current path is the namespace
                    Namespace ns = new Namespace(nsP.getFileName().toString(), true);
                    try (DirectoryStream<Path> idDS = Files.newDirectoryStream(nsP, onhdf)) {
                        for (Path idP : idDS) {
                            
                            
                            List<XmlId> xmlIds = new ArrayList<>();
                            if (ArtifactTemplateId.class.isAssignableFrom(inputIdClass)) {
                                List<String> artifactNames = getAllArtifactNamesFromType(idP, idClass, ns.getDecoded());
                                for (String artifactName : artifactNames) {
                                    xmlIds.add(new XmlId(artifactName + "@" + Util.getFolderName(idClass), true));
                                }
                            } else {
                                xmlIds.add(new XmlId(idP.getFileName().toString(), true));
                            }
                            
                            
                            
                            for (XmlId xmlId : xmlIds) {
                                if (omitDevelopmentVersions) {
                                    WineryVersion version = VersionUtils.getVersion(xmlId.getDecoded());

                                    if (version.toString().length() > 0 && version.getWorkInProgressVersion() > 0) {
                                        continue;
                                    }
                                }
                                Constructor<T> constructor;
                                try {
                                    constructor = inputIdClass.getConstructor(Namespace.class, XmlId.class);
                                } catch (Exception e) {
                                    LOGGER.debug("Internal error at determining id constructor", e);
                                    // abort everything, return invalid result
                                    return res;
                                }
                                T id;
                                try {
                                    id = constructor.newInstance(ns, xmlId);
                                } catch (InstantiationException
                                    | IllegalAccessException
                                    | IllegalArgumentException
                                    | InvocationTargetException e) {
                                    LOGGER.debug("Internal error at invocation of id constructor", e);
                                    // abort everything, return invalid result
                                    return res;
                                }
                                res.add(id);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.debug("Cannot close ds", e);
            }
        }
        return res;
    }

    private ByteArrayOutputStream convertInputStream(InputStream inputStream) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) > -1) {
                byteArrayOutputStream.write(buffer, 0, len);
            }
            byteArrayOutputStream.flush();
            return byteArrayOutputStream;
        } catch (IOException e) {
            return null;
        }
    }
}
