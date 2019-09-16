package org.eclipse.winery.repository.backend.filebased;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import org.eclipse.winery.common.RepositoryFileReference;
import org.eclipse.winery.common.Util;
import org.eclipse.winery.common.ids.GenericId;
import org.eclipse.winery.common.ids.IdUtil;
import org.eclipse.winery.common.ids.Namespace;
import org.eclipse.winery.common.ids.XmlId;
import org.eclipse.winery.common.ids.definitions.ArtifactTemplateId;
import org.eclipse.winery.common.ids.definitions.DefinitionsChildId;
import org.eclipse.winery.common.ids.definitions.NodeTypeId;
import org.eclipse.winery.model.tosca.Definitions;
import org.eclipse.winery.model.tosca.TArtifactReference;
import org.eclipse.winery.model.tosca.TArtifactTemplate;
import org.eclipse.winery.model.tosca.TArtifactType;
import org.eclipse.winery.model.tosca.TCapability;
import org.eclipse.winery.model.tosca.TCapabilityType;
import org.eclipse.winery.model.tosca.TDeploymentArtifact;
import org.eclipse.winery.model.tosca.TImplementationArtifact;
import org.eclipse.winery.model.tosca.TNodeTypeImplementation;
import org.eclipse.winery.model.tosca.TPolicyType;
import org.eclipse.winery.model.tosca.TRelationshipType;
import org.eclipse.winery.model.tosca.yaml.TArtifactDefinition;
import org.eclipse.winery.model.tosca.yaml.TNodeType;
import org.eclipse.winery.model.tosca.yaml.TServiceTemplate;
import org.eclipse.winery.model.tosca.yaml.support.Defaults;
import org.eclipse.winery.repository.backend.BackendUtils;
import org.eclipse.winery.repository.backend.filebased.converter.support.exception.MultiException;
import org.eclipse.winery.repository.backend.filebased.converter.support.reader.yaml.Reader;
import org.eclipse.winery.repository.backend.filebased.converter.support.writer.yaml.Writer;

import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import com.jcraft.jsch.Logger;
import javafx.util.Pair;
import jdk.internal.util.xml.impl.Input;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.internal.storage.file.GC;

public class YamlManager {
    
    private FilebasedRepository repository;
    
    private final String idRegex;
    private final Pattern idPattern;
    private final String nameRegex;
    private final Pattern namePattern;
    
    
    YamlManager(FilebasedRepository repository) {
        this.repository = repository;
        this.idRegex = "(.*\\/)(.*)@(.*)";
        this.idPattern = Pattern.compile(idRegex);
        this.nameRegex = "(.*)@(.*)";
        this.namePattern = Pattern.compile(nameRegex);
    }
    
    public Definitions definitionsFromRef(RepositoryFileReference ref) {
        switch (getClassName(ref)) {
            case "nodetype":
                System.out.println("readNodeType");
                return parseNodeType(ref);
            case "nodetypeimplementation":
                System.out.println("readNodeTypeImplementation");
                return parseNodeTypeImplementation(ref);
            case "artifacttemplate":
                System.out.println("readArtifactTemplate");
                return parseArtifactTemplate(ref);
            case "artifacttype":
                System.out.println("readArtifactType");
                return parseArtifactType(ref);
            case "capabilitytype":
                System.out.println("readCapabilityType");
                return parseCapabilityType(ref);
            case "relationshiptype":
                System.out.println("readRelationshipType");
                return parseRelationshipType(ref);
            case "policytype":
                System.out.println("readPolicyType");
                return parsePolicyType(ref);
//            case "servicetemplate":
//                System.out.println("readServiceTemplate");
//                return parseServiceTemplate(ref);
            default:
                try {
                    return parseXML(repository.newInputStream(ref));
                } catch (Exception e) {
                    return null;
                }
        }
    }
    
    
    public Pair<Path, InputStream> putContentToFile(RepositoryFileReference ref, InputStream in) {
        X2YConverter.repository = repository;
        switch (getClassName(ref)) {
            case "nodetype":
                System.out.println("writeNodeType");
                return parseNodeType(ref,in);
            case "nodetypeimplementation":
                System.out.println("writeNodeTypeImplementation");
                return parseNodeTypeImplementation(ref, in);
            case "artifacttemplate":
                System.out.println("writeArtifactTemplate");
                return parseArtifactTemplate(ref, in);
            case "artifacttype":
                System.out.println("writeArtifactType");
                return parseArtifactType(ref, in);
            case "capabilitytype":
                System.out.println("writeCapabilityType");
                return parseCapabilityType(ref, in);
            case "relationshiptype":
                System.out.println("writeRelationshipType");
                return parseRelationshipType(ref, in);
            case "policytype":
                System.out.println("writePolicyType");
                return parsePolicyType(ref, in);
//            case "servicetemplate":
//                System.out.println("writeServiceTemplate");
//                return parseServiceTemplate(ref, in);
            default:
                return new Pair<>(repository.ref2AbsolutePath(ref), in);
        }
        
    }
    
    
//    private Pair<Path, InputStream> parseServiceTemplate(RepositoryFileReference ref, InputStream in) {
//        Path targetPath = repository.ref2AbsolutePath(ref);
//        try {
//            Definitions serviceTemplateDef = parseXML(in);
//            List<org.eclipse.winery.model.tosca.TServiceTemplate> serviceTemplates = serviceTemplateDef.getServiceTemplates();
//        }
//    }
    
    private Definitions parsePolicyType(RepositoryFileReference ref) {
        Path targetPath = repository.ref2AbsolutePath(ref);
        if (Files.exists(targetPath)) {
            try {
                TServiceTemplate policyTypeST = Reader.getReader().parse(repository.newInputStream(targetPath));
                String policyTypeId = policyTypeST.getPolicyTypes().entrySet().iterator().next().getKey();
                org.eclipse.winery.model.tosca.yaml.TPolicyType policyType = policyTypeST.getPolicyTypes().entrySet().iterator().next().getValue();
                TPolicyType xmlPolicyType = Y2XConverter.convert(policyType, policyTypeId);
                if (xmlPolicyType != null) {
                    List<TPolicyType> policyTypes = new ArrayList<>();
                    policyTypes.add(xmlPolicyType);
                    Definitions definitions = new Definitions.Builder(policyTypeId, policyType.getMetadata().get("targetNamespace"))
                        .setPolicyTypes(policyTypes)
                        .build();
                    return definitions;
                }
            } catch (MultiException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    
    
    private Pair<Path, InputStream> parsePolicyType(RepositoryFileReference ref, InputStream in) {
        Path targetPath = repository.ref2AbsolutePath(ref);
        try {
            Definitions policyTypeDef = parseXML(in);
            List<TPolicyType> policyTypes = policyTypeDef.getPolicyTypes();
            if (policyTypes != null) {
                if (!policyTypes.isEmpty()) {
                    TPolicyType policyType = policyTypes.get(0);
                    TServiceTemplate.Builder builder = new TServiceTemplate.Builder(Defaults.TOSCA_DEFINITIONS_VERSION)
                        .setDescription(X2YConverter.convertDocumentation(policyType.getDocumentation()))
                        .setPolicyTypes(X2YConverter.convert(policyType));
                    Writer writer = new Writer();
                    InputStream output = writer.writeToInputStream(builder.build());
                    return new Pair<>(targetPath, output);
                }
            }
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    
    private Definitions parseRelationshipType(RepositoryFileReference ref) {
        Path targetPath = repository.ref2AbsolutePath(ref);
        if (Files.exists(targetPath)) {
            try {
                TServiceTemplate relationshipTypeST = Reader.getReader().parse(repository.newInputStream(targetPath));
                String relationshipTypeId = relationshipTypeST.getRelationshipTypes().entrySet().iterator().next().getKey();
                org.eclipse.winery.model.tosca.yaml.TRelationshipType relationshipType = relationshipTypeST.getRelationshipTypes().entrySet().iterator().next().getValue();
                TRelationshipType xmlRelationshipType = Y2XConverter.convert(relationshipType, relationshipTypeId);
                if (xmlRelationshipType != null) {
                    List<TRelationshipType> relationshipTypes = new ArrayList<>();
                    relationshipTypes.add(xmlRelationshipType);
                    Definitions definitions = new Definitions.Builder(relationshipTypeId, relationshipType.getMetadata().get("targetNamespace"))
                        .setRelationshipTypes(relationshipTypes)
                        .build();
                    return definitions;
                }
            } catch (MultiException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    
    private Pair<Path, InputStream> parseRelationshipType(RepositoryFileReference ref, InputStream in) {
        ByteArrayOutputStream outputStream = repository.convertInputStream(in);
        printInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
        in = new ByteArrayInputStream(outputStream.toByteArray());
        Path targetPath = repository.ref2AbsolutePath(ref);
        try {
            Definitions relationshipTypeDef = parseXML(in);
            List<TRelationshipType> relationshipTypes = relationshipTypeDef.getRelationshipTypes();
            if (relationshipTypes != null) {
                if (!relationshipTypes.isEmpty()) {
                    TRelationshipType relationshipType = relationshipTypes.get(0);
                    TServiceTemplate.Builder builder = new TServiceTemplate.Builder(Defaults.TOSCA_DEFINITIONS_VERSION)
                        .setDescription(X2YConverter.convertDocumentation(relationshipType.getDocumentation()))
                        .setRelationshipTypes(X2YConverter.convert(relationshipType));
                    Writer writer = new Writer();
                    InputStream output = writer.writeToInputStream(builder.build());
                    return new Pair<>(targetPath, output);
                }
            }
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    
    
    private Definitions parseCapabilityType(RepositoryFileReference ref) {
        Path targetPath = repository.ref2AbsolutePath(ref);
        if (Files.exists(targetPath)) {
            try {
                TServiceTemplate capabilitytypeST = Reader.getReader().parse(repository.newInputStream(targetPath));
                String capabilitytypeId = capabilitytypeST.getCapabilityTypes().entrySet().iterator().next().getKey();
                org.eclipse.winery.model.tosca.yaml.TCapabilityType capabilityType = capabilitytypeST.getCapabilityTypes().entrySet().iterator().next().getValue();
                TCapabilityType xmlCapabilityType= Y2XConverter.convert(capabilityType, capabilitytypeId);
                if (xmlCapabilityType != null) {
                    List<TCapabilityType> capabilityTypes = new ArrayList<>();
                    capabilityTypes.add(xmlCapabilityType);
                    Definitions definitions = new Definitions.Builder(capabilitytypeId, capabilityType.getMetadata().get("targetNamespace"))
                        .setCapabilityTypes(capabilityTypes)
                        .build();
                    return definitions;
                }
            } catch (MultiException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    
    
    private Pair<Path, InputStream> parseCapabilityType(RepositoryFileReference ref, InputStream in) {
        Path targetPath = repository.ref2AbsolutePath(ref);
        try {
            Definitions capabilityTypeDef = parseXML(in);
            List<TCapabilityType> capabilityTypes = capabilityTypeDef.getCapabilityTypes();
            if (capabilityTypes != null) {
                if (!capabilityTypes.isEmpty()) {
                    TCapabilityType capabilityType = capabilityTypes.get(0);
                    TServiceTemplate.Builder builder = new TServiceTemplate.Builder(Defaults.TOSCA_DEFINITIONS_VERSION)
                        .setDescription(X2YConverter.convertDocumentation(capabilityType.getDocumentation()))
                        .setCapabilityTypes(X2YConverter.convert(capabilityType));
                    Writer writer = new Writer();
                    InputStream output = writer.writeToInputStream(builder.build());
                    return new Pair<>(targetPath, output);
                }
            }
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private Definitions parseArtifactType(RepositoryFileReference ref) {
        Path targetPath = repository.ref2AbsolutePath(ref);
        if (Files.exists(targetPath)) {
            try {
                TServiceTemplate artifactTypeST = Reader.getReader().parse(repository.newInputStream(targetPath));
                String artifactTypeId = artifactTypeST.getArtifactTypes().entrySet().iterator().next().getKey();
                org.eclipse.winery.model.tosca.yaml.TArtifactType artifactType = artifactTypeST.getArtifactTypes().entrySet().iterator().next().getValue();
                TArtifactType xmlArtifactType = Y2XConverter.convert(artifactType, artifactTypeId);
                if (xmlArtifactType != null) {
                    List<TArtifactType> artifactTypes = new ArrayList<>();
                    artifactTypes.add(xmlArtifactType);
                    Definitions definitions = new Definitions.Builder(artifactTypeId, artifactType.getMetadata().get("targetNamespace"))
                        .setArtifactTypes(artifactTypes)
                        .build();
                    return definitions;
                }
                
            } catch (MultiException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    
    
    private Pair<Path, InputStream> parseArtifactType(RepositoryFileReference ref, InputStream in) {
        Path targetPath = repository.ref2AbsolutePath(ref);
        try {
            Definitions artifactType = parseXML(in);
            List<TArtifactType> artifactTypes = artifactType.getArtifactTypes();
            if (artifactTypes != null) {
                if (!artifactTypes.isEmpty()) {
                    TServiceTemplate.Builder builder = new TServiceTemplate.Builder(Defaults.TOSCA_DEFINITIONS_VERSION)
                        .setDescription(X2YConverter.convertDocumentation(artifactType.getDocumentation()))
                        .setArtifactTypes(X2YConverter.convert(artifactTypes.get(0)));
                    Writer writer = new Writer();
                    InputStream output = writer.writeToInputStream(builder.build());
                    return new Pair<>(targetPath, output);
                }
            }
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private String getClassName(RepositoryFileReference ref) {
        try {
            return Util.getEverythingBetweenTheLastDotAndBeforeId(((DefinitionsChildId)ref.getParent()).getClass()).toLowerCase();
        } catch (Exception e) {
            return "";
        }
    }
    
    private Definitions parseArtifactTemplate(RepositoryFileReference ref) {
        Path absolutPath = convertPathFromRef(ref, "artifacttemplates", "nodetypes", "NodeType.tosca");
        Matcher matcher = namePattern.matcher(ref.getParent().getXmlId().getDecoded());
        if (matcher.matches()) {
            String artifactName = matcher.group(1);
            if (checkIfArtifactTemplateExists(ref)) {
                return getArtifactTemplateDefinitions(artifactName, absolutPath);
            } else {
                return getArtifactTemplateDefinitions(artifactName, redirectArtifactTemplateRequest(artifactName));
            }
        } else {
            String artifactName = ref.getParent().getXmlId().getDecoded();
            if ((absolutPath = getCachePathFromRef(ref)) != null) {
                return getArtifactTemplateDefinitions(artifactName, absolutPath);
                
            }
        }
        return null;
    }
    
    private Path redirectArtifactTemplateRequest(String artifactName) {
        SortedSet<ArtifactTemplateId> allArtifacts = repository.getAllDefinitionsChildIds(ArtifactTemplateId.class);
        for (ArtifactTemplateId artifactTemplate : allArtifacts) {
            Matcher nameMatcher = namePattern.matcher(artifactTemplate.getXmlId().toString());
            if (nameMatcher.matches()) {
                if (nameMatcher.group(1).equalsIgnoreCase(artifactName)) {
                    return convertPathFromId(artifactTemplate, "artifacttemplates", "nodetypes", "NodeType.tosca");
                }
            }
        }
        return null;
    }
    
    
    private Path getCachePathFromRef(RepositoryFileReference ref) {
        String originalFilePath = repository.fileSystem.getPath(Util.getPathInsideRepo(ref.getParent())).toString();
        String parentNameRegex = "(.*\\/)(.*)";
        Pattern parentNamePattern = Pattern.compile(parentNameRegex);
        Matcher partenNameMatcher = parentNamePattern.matcher(originalFilePath);
        if (partenNameMatcher.matches()) {
            String pathString = partenNameMatcher.group(1) + "Cache";
            return convertPathFromPath(pathString, "artifacttemplates", "nodetypes", "NodeType.tosca");
        }
        return null;
    }
    
    
    
    public Boolean exists(RepositoryFileReference ref, Path path) {
        switch (getClassName(ref)) {
            case "nodetypeimplementation":
                Path nodeTypeImplementationPath = convertPathFromRef(ref, "nodetypeimplementations", "nodetypes", "NodeType.tosca");
                return Files.exists(nodeTypeImplementationPath);
            case "artifacttemplate":
                return checkIfArtifactTemplateExists(ref);
            default:
                return Files.exists(path);
        }
        
    }
    
    private Boolean checkIfArtifactTemplateExists(RepositoryFileReference ref) {
        Path absolutPath = convertPathFromRef(ref, "artifacttemplates", "nodetypes", "NodeType.tosca");
        Matcher matcher = namePattern.matcher(ref.getParent().getXmlId().getDecoded());
        if (matcher.matches()) {
            String artifactName = matcher.group(1);
            return checkIfNodeTypeContainsArtifactTemplate(absolutPath, artifactName);
        } else {
            String artifactName = ref.getParent().getXmlId().getDecoded();
            if ((absolutPath = getCachePathFromRef(ref)) != null) {
                return checkIfNodeTypeContainsArtifactTemplate(absolutPath, artifactName);
            }
        }
        return false;
    }
    
    private Boolean checkIfNodeTypeContainsArtifactTemplate(Path path, String artifactName) {
        if (Files.exists(path)) {
            try {
                TServiceTemplate nodeType = Reader.getReader().parse(repository.newInputStream(path));
                Map<String, TArtifactDefinition> artifacts = nodeType.getNodeTypes().entrySet().iterator().next().getValue().getArtifacts();
                TArtifactDefinition foundArtifact = null;
                if ((foundArtifact = artifacts.get(artifactName)) != null ) {
                    return true;
                }
            } catch (MultiException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
    
    private Definitions getArtifactTemplateDefinitions(String artifactName, Path absolutPath) {
        try {
            if (Files.exists(absolutPath)) {
                InputStream nodeTypeInputStream = repository.newInputStream(absolutPath);
                TServiceTemplate yamlNodeType = Reader.getReader().parse(nodeTypeInputStream);
                TArtifactDefinition artifact = yamlNodeType.getNodeTypes().entrySet().iterator().next().getValue().getArtifacts().get(artifactName);
                if (artifact != null) {
                        Definitions definitions = new Definitions.Builder(artifactName, yamlNodeType.getNodeTypes().entrySet().iterator().next().getValue().getMetadata().get("targetNamespace"))
                            .addArtifactTemplates(Y2XConverter.convertArtifactTemplate(artifact, artifactName))
                            .setName(artifactName)
                            .build();
                        return definitions;
                    
                }
            }
        } catch (MultiException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private Pair<Path, InputStream> parseArtifactTemplate(RepositoryFileReference ref, InputStream in) {
        try {
            Definitions xmlArtifactTemplate = parseXML(in);
            List<TArtifactTemplate> templates = xmlArtifactTemplate.getArtifactTemplates();
            if (!templates.isEmpty()) {
                TArtifactTemplate artifactTemplate = templates.get(0);
                if (artifactTemplate.getType() == null) {return null;}
                Matcher matcher = namePattern.matcher(ref.getParent().getXmlId().getDecoded());
                if (matcher.matches()) {
                    Path absolutPath = convertPathFromRef(ref, "artifacttemplates", "nodetypes", "NodeType.tosca");
                    String artifactName = matcher.group(1);
                    TServiceTemplate nodeType = Reader.getReader().parse(repository.newInputStream(absolutPath));
                    searchForExistingArtifactOrAdd(nodeType, artifactTemplate, artifactName);
                    Writer writer = new Writer();
                    InputStream output = writer.writeToInputStream(nodeType);
                    return new Pair<>(absolutPath, output);
                } else {
                    String artifactName = ref.getParent().getXmlId().getDecoded();
                    Path cachePath = getCachePathFromRef(ref);
                    if (Files.exists(cachePath)) {
                        TServiceTemplate cacheNodeType = Reader.getReader().parse(repository.newInputStream(cachePath));
                        searchForExistingArtifactOrAdd(cacheNodeType, artifactTemplate, artifactName);
                        Writer writer = new Writer();
                        InputStream output = writer.writeToInputStream(cacheNodeType);
                        return new Pair<>(cachePath, output);
                    } else {
                        TServiceTemplate newCacheNodeType = new TServiceTemplate.Builder(Defaults.TOSCA_DEFINITIONS_VERSION)
                            .setNodeType("CacheNodeType", createCacheNodeTypeWithArtifact(artifactName, X2YConverter.convertArtifactTemplate(artifactTemplate, new ArrayList<>()), xmlArtifactTemplate.getTargetNamespace()))
                            .build();
                        Writer writer = new Writer();
                        InputStream output = writer.writeToInputStream(newCacheNodeType);
                        return new Pair<>(cachePath, output);
                    }
                }
            }

        } catch (JAXBException | NullPointerException e) {
            e.printStackTrace();
        } catch (MultiException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
        
    }
    
    private void searchForExistingArtifactOrAdd(TServiceTemplate nodeType, TArtifactTemplate artifactTemplate, String artifactName) {
        Map<String, TArtifactDefinition> cachedArtifacts = nodeType.getNodeTypes().entrySet().iterator().next().getValue().getArtifacts();
        for (Map.Entry<String, TArtifactDefinition> artifact : cachedArtifacts.entrySet()) {
            if (artifact.getKey().equalsIgnoreCase(artifactName)) {
                artifact.setValue(X2YConverter.convertArtifactTemplate(artifactTemplate, new ArrayList<>()));
                return;
            }
        }
        cachedArtifacts.put(artifactName, X2YConverter.convertArtifactTemplate(artifactTemplate, new ArrayList<>()));
    }
    
    private TNodeType createCacheNodeTypeWithArtifact(String artifactName, TArtifactDefinition artifact, String targetNamespace) {
        return new TNodeType.Builder()
            .addArtifacts(artifactName, artifact)
            .addMetadata("targetNamespace", targetNamespace)
            .build();
    }
    
    private void printInputStream(InputStream in) {
        try {
            StringWriter writer = new StringWriter();
            IOUtils.copy(in, writer);
            System.out.println(writer.toString());
        }catch (IOException e) {
        }
    }
    
    public  <T extends DefinitionsChildId>SortedSet<T> getDefinitionsChildIds(Namespace ns, XmlId xmlId, Path path, String originalRootFragment, Constructor<T> constructor) {
        SortedSet<T> res = new TreeSet<>();
        try {
            if (originalRootFragment == null) {
                if (!xmlId.getDecoded().equalsIgnoreCase("Cache")) {
                    T id = constructor.newInstance(ns, xmlId);
                    res.add(id);
                }
            } else {
                switch (originalRootFragment) {
                    case "nodetypeimplementations/":
                        if (!xmlId.getDecoded().equalsIgnoreCase("Cache")) {
//                        XmlId nodeTypeImplementationXMLId = new XmlId(xmlId.toString() + "-impl" + "@" + xmlId.toString(), true);
                            T nodeTypeImplementationId = constructor.newInstance(ns, xmlId);
                            res.add(nodeTypeImplementationId);
                        }
                        break;
                    case "artifacttemplates/":
                        List<String> artifactTemplates = getAllArtifactTemplatesFromNode(path);
                        for (String artifact : artifactTemplates) {
                            XmlId artifactTemplateXMLId = new XmlId(artifact + "@" + xmlId.toString(), true);
                            T artifactTemplateId = constructor.newInstance(ns, artifactTemplateXMLId);
                            res.add(artifactTemplateId);
                        }
                        break;
                }
                
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return res;
    }
    
    private List<String> getAllArtifactTemplatesFromNode(Path path) {
        List<String> artifactNames = new ArrayList<>();
        path = path.resolve("NodeType.tosca");
        try {
            TServiceTemplate yamlNodeType = Reader.getReader().parse(repository.newInputStream(path));
            for (Map.Entry<String, TArtifactDefinition> artifact : yamlNodeType.getNodeTypes().entrySet().iterator().next().getValue().getArtifacts().entrySet()) {
                artifactNames.add(artifact.getKey());
            }
        } catch (MultiException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return artifactNames;
    }
    
    private Definitions parseNodeType(RepositoryFileReference ref) {
        try {
            TServiceTemplate yamlNodeType = Reader.getReader().parse(repository.newInputStream(ref));
            Map.Entry<String, TNodeType> nodeType = yamlNodeType.getNodeTypes().entrySet().iterator().next();
            Definitions definitions = new Definitions.Builder(nodeType.getKey() + "_Definitions", nodeType.getValue().getMetadata().get("targetNamespace"))
                .addNodeTypes(Y2XConverter.convert(nodeType))
                .setName(nodeType.getKey())
                .build();
            return definitions;
        } catch (MultiException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private Pair<Path, InputStream> parseNodeTypeImplementation(RepositoryFileReference ref, InputStream in) {
        Path nodeTypePath;
        try {
            Definitions xmlNodeTypeImplementaion = parseXML(in);
            List<TNodeTypeImplementation> implementations = xmlNodeTypeImplementaion.getNodeTypeImplementations();
            if (!implementations.isEmpty()) {
                TNodeTypeImplementation implementation = implementations.get(0);
                QName nodeType = implementation.getNodeType();
                if (nodeType != null) {
                    RepositoryFileReference nodeTypeRef = BackendUtils.getRefOfDefinitions(new NodeTypeId(nodeType));
                    nodeTypePath = repository.ref2AbsolutePath(nodeTypeRef);
                    InputStream nodeTypeInputStream = repository.newInputStream(nodeTypeRef);
                    TServiceTemplate yamlNodeType = Reader.getReader().parse(nodeTypeInputStream);
                    Map<String, TArtifactDefinition> artifacts = getArtifactDefinitionsFromImplementation(implementation);
                    yamlNodeType.getNodeTypes().entrySet().iterator().next().getValue().setArtifacts(artifacts);
                    yamlNodeType.getNodeTypes().entrySet().iterator().next().getValue().setInterfaces(X2YConverter.addImplementationsToInterfaceDefinition(yamlNodeType.getNodeTypes().entrySet().iterator().next().getValue().getInterfaces(), implementation));
                    Writer writer = new Writer();
                    InputStream outputStream = writer.writeToInputStream(yamlNodeType);
                    return new Pair<>(nodeTypePath, outputStream);
                }
            }
            
            
        } catch (JAXBException | IOException | MultiException | NullPointerException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private Map<String, TArtifactDefinition> getArtifactDefinitionsFromImplementation(TNodeTypeImplementation implementation) {
        Map<String, TArtifactDefinition> output = new LinkedHashMap<>();
        if (implementation.getImplementationArtifacts() != null) {
            for (TImplementationArtifact implementationArtifact : implementation.getImplementationArtifacts().getImplementationArtifact()) {
                Pair<String, TArtifactDefinition> artifact = getArtifactDefinition(implementationArtifact.getArtifactRef());
                output.put(artifact.getKey(), artifact.getValue());
            }
        }
        if (implementation.getDeploymentArtifacts() != null) {
            for (TDeploymentArtifact deploymentArtifact : implementation.getDeploymentArtifacts().getDeploymentArtifact()) {
                Pair<String, TArtifactDefinition> artifact = getArtifactDefinition(deploymentArtifact.getArtifactRef());
                output.put(artifact.getKey(), artifact.getValue());
            }
        }
        return output;
    }
    
    private Pair<String, TArtifactDefinition> getArtifactDefinition(QName ref) {
        ArtifactTemplateId id = new ArtifactTemplateId(ref);
        TArtifactTemplate artifact = repository.getElement(id);
        Matcher nameMatcher = namePattern.matcher(artifact.getId()) ;
        String artifactName;
        if (nameMatcher.matches()) {
            artifactName = nameMatcher.group(1);
        } else {
            artifactName = artifact.getId();
        }
        List<String> files = new ArrayList<>();
        if (artifact.getArtifactReferences() != null) {
            for (TArtifactReference reference : artifact.getArtifactReferences().getArtifactReference()) {
                //TODO copy file to nodeTypeFolder
                files.add(reference.getReference());
            }
        }
        TArtifactDefinition artifactDefinition = X2YConverter.convertArtifactTemplate(artifact, files);
        return new Pair<>(artifactName, artifactDefinition);
    }
    
    public void clearCache() {
        SortedSet<ArtifactTemplateId> allArtifacts = repository.getAllDefinitionsChildIds(ArtifactTemplateId.class);
        List<ArtifactTemplateId> cachedArtifacts = new ArrayList<>();
        for (ArtifactTemplateId artifactTemplateId : allArtifacts) {
            Matcher nameMatcher = namePattern.matcher(artifactTemplateId.getXmlId().toString());
            if (nameMatcher.matches()) {
                if (nameMatcher.group(2).equalsIgnoreCase("Cache")) {
                    cachedArtifacts.add(artifactTemplateId);
                }
            }
        }
        
        for (ArtifactTemplateId cachedArtifactTemplateId: cachedArtifacts) {
            for (ArtifactTemplateId artifactTemplateId : allArtifacts) {
                if (!cachedArtifactTemplateId.equals(artifactTemplateId)) {
                    Matcher cachedNameMatcher = namePattern.matcher(cachedArtifactTemplateId.getXmlId().toString());
                    Matcher nameMatcher = namePattern.matcher(artifactTemplateId.getXmlId().toString());
                    if (cachedNameMatcher.matches() && nameMatcher.matches()) {
                        if (cachedArtifactTemplateId.getQName().getNamespaceURI().equalsIgnoreCase(artifactTemplateId.getQName().getNamespaceURI())) {
                            if (cachedNameMatcher.group(1).equalsIgnoreCase(nameMatcher.group(1))) {
                                deleteArtifact(cachedArtifactTemplateId);
                            }
                        }
                    }
                }
            }
        }
        
    }
    
    private void deleteArtifact(ArtifactTemplateId id) {
        Path targetPath = convertPathFromId(id, "artifacttemplates", "nodetypes", "NodeType.tosca");
        if (Files.exists(targetPath)) {
            try {
                TServiceTemplate nodeType = Reader.getReader().parse(repository.newInputStream(targetPath));
                Map<String, TArtifactDefinition> artifacts = nodeType.getNodeTypes().entrySet().iterator().next().getValue().getArtifacts();
                Matcher nameMatcher = namePattern.matcher(id.getXmlId().toString());
                if (nameMatcher.matches()) {
                    String targetArtifactName = nameMatcher.group(1);
                    artifacts.remove(targetArtifactName);
                    nodeType.getNodeTypes().entrySet().iterator().next().getValue().setArtifacts(artifacts);
                    Writer writer = new Writer();
                    InputStream output = writer.writeToInputStream(nodeType);
                    repository.putInputStreamToFile(targetPath, output);
                }
            } catch (MultiException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private Path convertPathFromId(GenericId id, String target, String replaceBy, String fileName) {
        System.out.println(IdUtil.getPathFragment(id));
        String originalRelativePathString = IdUtil.getPathFragment(id);
        return convertPathFromPath(originalRelativePathString, target, replaceBy, fileName);
    }
    
    
    private Path convertPathFromRef(RepositoryFileReference ref, String target, String replaceBy, String fileName) {
        String originalRelativePathString = repository.fileSystem.getPath(Util.getPathInsideRepo(ref.getParent())).toString();
        return convertPathFromPath(originalRelativePathString, target, replaceBy, fileName);
    }
    
    
    private Path convertPathFromPath(String path, String target, String replaceBy, String fileName) {
        Path absolutPath = repository.fileSystem.getPath(path);
        if (path.startsWith(target)) {
            String convertedRelativePathString = path.replaceFirst(target, replaceBy);
            Matcher matcher = idPattern.matcher(convertedRelativePathString);
            if (matcher.matches()) {
                convertedRelativePathString = matcher.group(1) + matcher.group(3);
            }
            Path convertedRelativePath = repository.fileSystem.getPath(convertedRelativePathString);
            absolutPath = repository.makeAbsolute(convertedRelativePath);
        }
        return absolutPath.resolve(fileName);
    }
    
    private Definitions parseNodeTypeImplementation(RepositoryFileReference ref){
        try {
            Path absolutPath;
            if ((absolutPath = searchForNodeTypeImplementationPath(ref)) != null) {
                InputStream nodeTypeInputStream = repository.newInputStream(absolutPath);
                TServiceTemplate yamlNodeType = Reader.getReader().parse(nodeTypeInputStream);
                if (yamlNodeType.getNodeTypes() != null) {
                    Map.Entry<String, TNodeType> nodeType = yamlNodeType.getNodeTypes().entrySet().iterator().next();
                    Definitions definitions = new Definitions.Builder(nodeType.getKey() + "-impl", nodeType.getValue().getMetadata().get("targetNamespace"))
                        .addNodeTypeImplementations(Y2XConverter.convertNodeTypeImplementaions(nodeType, nodeType.getValue().getMetadata().get("targetNamespace")))
                        .build();
                    return definitions;
                } else if (yamlNodeType.getRelationshipTypes() != null){
//                    Map.Entry<String, org.eclipse.winery.model.tosca.yaml.TRelationshipType> relationshipType = yamlNodeType.getRelationshipTypes().entrySet().iterator().next();
//                    Definitions definitions = new Definitions.Builder(relationshipType.getKey() + "-impl", relationshipType.getValue().getMetadata().get("targetNamespace"))
//                        .addRelationshipTypeImplementations(Y2XConverter.convertRelationshipTypeImplementation(relationshipType.getValue(), relationshipType.getKey()))
//                        .build();
//                    return definitions;
                }
            }
        }catch (MultiException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
    
    private Path searchForNodeTypeImplementationPath(RepositoryFileReference ref) {
        Path absolutePath;
        if ((absolutePath = convertPathFromRef(ref, "nodetypeimplementations", "nodetypes", "NodeType.tosca")) != null) {
            return absolutePath;
        } else if ((absolutePath = convertPathFromRef(ref, "nodetypeimplementations", "relationshiptypes", "RelationshipType.tosca")) != null) {
            return absolutePath;
        } else {
            return absolutePath;
       }
        
    }
    
    
    private Pair<Path, InputStream> parseNodeType(RepositoryFileReference ref, InputStream in) {
        Path nodeTypePath = repository.ref2AbsolutePath(ref);
        try {
            Definitions xmlNodeType = parseXML(in);
            TServiceTemplate newNodeType = convertNodeType(xmlNodeType);
            if (repository.exists(ref)) {
                InputStream existingData = repository.newInputStream(nodeTypePath);
                Reader reader = Reader.getReader();
                TServiceTemplate existingNodeType = reader.parse(existingData);
                existingNodeType.getNodeTypes().entrySet().iterator().next().getValue().setInterfaces(newNodeType.getNodeTypes().entrySet().iterator().next().getValue().getInterfaces());
                existingNodeType.getNodeTypes().entrySet().iterator().next().getValue().setCapabilities(newNodeType.getNodeTypes().entrySet().iterator().next().getValue().getCapabilities());
                Writer writer = new Writer();
                return new Pair<>(nodeTypePath, writer.writeToInputStream(existingNodeType));
            }
            Writer writer = new Writer();
            InputStream output = writer.writeToInputStream(newNodeType);
            return new Pair<>(nodeTypePath, output);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MultiException e) {
            e.printStackTrace();
        } catch (JAXBException e) {
            e.printStackTrace();
        }

        return new Pair<>(nodeTypePath, in);
    }
    
    
    private Definitions parseXML(InputStream inputStream) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(Definitions.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        return (Definitions) unmarshaller.unmarshal(inputStream);
    }
    
    
    private TServiceTemplate convertNodeType(Definitions node) {
        TServiceTemplate.Builder builder = new TServiceTemplate.Builder(Defaults.TOSCA_DEFINITIONS_VERSION)
            .setDescription(X2YConverter.convertDocumentation(node.getDocumentation()))
            .setNodeTypes(X2YConverter.convert(node.getNodeTypes()));
        
        return builder.build();
    }
    
}
