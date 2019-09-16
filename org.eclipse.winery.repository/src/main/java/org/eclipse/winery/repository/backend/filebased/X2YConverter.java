package org.eclipse.winery.repository.backend.filebased;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.namespace.QName;

import org.eclipse.winery.common.ids.definitions.ArtifactTemplateId;
import org.eclipse.winery.common.ids.definitions.ArtifactTypeId;
import org.eclipse.winery.common.ids.definitions.CapabilityTypeId;
import org.eclipse.winery.common.ids.definitions.DefinitionsChildId;
import org.eclipse.winery.common.ids.definitions.NodeTypeId;
import org.eclipse.winery.common.ids.definitions.PolicyTypeId;
import org.eclipse.winery.common.ids.definitions.RelationshipTypeId;
import org.eclipse.winery.common.ids.definitions.RequirementTypeId;
import org.eclipse.winery.model.tosca.TArtifactReference;
import org.eclipse.winery.model.tosca.TArtifactTemplate;
import org.eclipse.winery.model.tosca.TBoolean;
import org.eclipse.winery.model.tosca.TBoundaryDefinitions;
import org.eclipse.winery.model.tosca.TCapability;
import org.eclipse.winery.model.tosca.TDeploymentArtifacts;
import org.eclipse.winery.model.tosca.TDocumentation;
import org.eclipse.winery.model.tosca.TEntityTemplate;
import org.eclipse.winery.model.tosca.TEntityType;
import org.eclipse.winery.model.tosca.TImplementationArtifact;
import org.eclipse.winery.model.tosca.TImplementationArtifacts;
import org.eclipse.winery.model.tosca.TInterface;
import org.eclipse.winery.model.tosca.TInterfaces;
import org.eclipse.winery.model.tosca.TNodeTypeImplementation;
import org.eclipse.winery.model.tosca.TOperation;
import org.eclipse.winery.model.tosca.TParameter;
import org.eclipse.winery.model.tosca.TRelationshipTypeImplementation;
import org.eclipse.winery.model.tosca.TRequirement;
import org.eclipse.winery.model.tosca.TRequirementType;
import org.eclipse.winery.model.tosca.TTag;
import org.eclipse.winery.model.tosca.TTags;
import org.eclipse.winery.model.tosca.TTopologyTemplate;
import org.eclipse.winery.model.tosca.kvproperties.PropertyDefinitionKV;
import org.eclipse.winery.model.tosca.kvproperties.WinerysPropertiesDefinition;
import org.eclipse.winery.model.tosca.utils.ModelUtilities;
import org.eclipse.winery.model.tosca.yaml.TArtifactDefinition;
import org.eclipse.winery.model.tosca.yaml.TArtifactType;
import org.eclipse.winery.model.tosca.yaml.TCapabilityAssignment;
import org.eclipse.winery.model.tosca.yaml.TCapabilityDefinition;
import org.eclipse.winery.model.tosca.yaml.TCapabilityType;
import org.eclipse.winery.model.tosca.yaml.TImplementation;
import org.eclipse.winery.model.tosca.yaml.TInterfaceDefinition;
import org.eclipse.winery.model.tosca.yaml.TNodeTemplate;
import org.eclipse.winery.model.tosca.yaml.TOperationDefinition;
import org.eclipse.winery.model.tosca.yaml.TPolicyType;
import org.eclipse.winery.model.tosca.yaml.TPropertyAssignment;
import org.eclipse.winery.model.tosca.yaml.TPropertyAssignmentOrDefinition;
import org.eclipse.winery.model.tosca.yaml.TPropertyDefinition;
import org.eclipse.winery.model.tosca.yaml.TRelationshipType;
import org.eclipse.winery.model.tosca.yaml.TRequirementAssignment;
import org.eclipse.winery.model.tosca.yaml.TTopologyTemplateDefinition;
import org.eclipse.winery.model.tosca.yaml.support.Metadata;
import org.eclipse.winery.model.tosca.yaml.support.TMapRequirementAssignment;
import org.eclipse.winery.model.tosca.yaml.support.TMapRequirementDefinition;
import org.eclipse.winery.repository.backend.filebased.converter.support.ValueConverter;
import org.eclipse.winery.repository.backend.filebased.converter.support.xml.TypeConverter;
import org.eclipse.winery.repository.datatypes.ids.elements.ArtifactTemplateFilesDirectoryId;

import org.eclipse.jdt.annotation.NonNull;

import static org.eclipse.winery.repository.backend.filebased.converter.support.reader.yaml.Builder.LOGGER;

public class X2YConverter {
    
    public static FilebasedRepository repository;

    public static String convertDocumentation(@NonNull List<TDocumentation> doc) {
        return doc.stream()
            .map(TDocumentation::getContent)
            .flatMap(List::stream)
            .map(Object::toString)
            .collect(Collectors.joining("\n"));
    }

    public static  <T, K> Map<String, K> convert(List<T> nodes) {
        return nodes.stream()
            .filter(Objects::nonNull)
            .flatMap(node -> {
                if (node instanceof org.eclipse.winery.model.tosca.TNodeType) {
                    return convert((org.eclipse.winery.model.tosca.TNodeType) node).entrySet().stream();
                }
                throw new AssertionError();
            })
            .peek(entry -> LOGGER.debug("entry: {}", entry))
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> (K) entry.getValue()));
    }

    public  static Map<String, org.eclipse.winery.model.tosca.yaml.TNodeType> convert(org.eclipse.winery.model.tosca.TNodeType node) {
        if (Objects.isNull(node)) return null;
        return Collections.singletonMap(
            node.getIdFromIdOrNameField(),
            convert(node, new org.eclipse.winery.model.tosca.yaml.TNodeType.Builder(), org.eclipse.winery.model.tosca.TNodeType.class)
                .setCapabilities(convert(node.getCapabilityDefinitions()))
                .setInterfaces(convert(node.getInterfaces()))
                .build()
        );
    }
    
    public static Map<String, TInterfaceDefinition> convert(TInterfaces node) {
        if (Objects.isNull(node)) return null;
        return node.getInterface().stream()
            .filter(Objects::nonNull)
            .map(entry -> convert(entry)
            )
            .flatMap(entry -> entry.entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    
    public static Map<String, TInterfaceDefinition> convert(TInterface node) {
        if (Objects.isNull(node)) return new LinkedHashMap<>();
        return Collections.singletonMap(
            node.getName(),
            new TInterfaceDefinition.Builder()
                .setOperations(convertOperations(node.getOperation()))
                .build()
        );
    }

    public static Map<String, TOperationDefinition> convertOperations(List<TOperation> nodes) {
        if (Objects.isNull(nodes)) return null;
        return nodes.stream()
            .filter(Objects::nonNull)
            .flatMap(node -> convert(node)
            .entrySet().stream())
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static boolean convert(TBoolean node) {
        return Objects.nonNull(node) && node.equals(TBoolean.YES);
    }
    
    public static Map<String, TInterfaceDefinition> addImplementationsToInterfaceDefinition(Map<String, TInterfaceDefinition> currentTInterfaceDefinition, TNodeTypeImplementation node) {
        if (Objects.isNull(node)) return null;
        if(node.getImplementationArtifacts() != null) {
            for (TImplementationArtifact implementationArtifact : node.getImplementationArtifacts().getImplementationArtifact()) {
                String artifactName = new ArtifactTemplateId(implementationArtifact.getArtifactRef()).getXmlId().toString();
                TInterfaceDefinition interfaceDefinition = currentTInterfaceDefinition.get(implementationArtifact.getInterfaceName());
                if (interfaceDefinition != null) {
                    TOperationDefinition operationDefinition = interfaceDefinition.getOperations().get(implementationArtifact.getOperationName());
                    if (operationDefinition != null) {
                        operationDefinition.setImplementation(addArtifact(operationDefinition.getImplementation(), artifactName, node.getTargetNamespace()));
                    }
                }
            }
        }
        
        return currentTInterfaceDefinition;
    }

    public static Map<String, TRelationshipType> convert(org.eclipse.winery.model.tosca.TRelationshipType node) {
        if (Objects.isNull(node)) return null;
        return Collections.singletonMap(
            node.getIdFromIdOrNameField(),
            convert(node, new TRelationshipType.Builder(), org.eclipse.winery.model.tosca.TRelationshipType.class)
                .addInterfaces(convert(node.getInterfaces()))
                .addInterfaces(convert(node.getSourceInterfaces()))
                .addInterfaces(convert(node.getTargetInterfaces()))
                .addProperties(convert(node.getWinerysPropertiesDefinition()))
                .build()
        );
    }

//    public static TTopologyTemplateDefinition convert(org.eclipse.winery.model.tosca.TServiceTemplate node) {
//        if (Objects.isNull(node)) return null;
//        return convert(node.getTopologyTemplate(), node.getBoundaryDefinitions());
//    }
//
//    public static TTopologyTemplateDefinition convert(TTopologyTemplate node, TBoundaryDefinitions boundary) {
//        if (Objects.isNull(node)) return null;
//        return new TTopologyTemplateDefinition.Builder()
//            .setDescription(convertDocumentation(node.getDocumentation()))
//            .setNodeTemplates(convert(node.getNodeTemplates(), node.getRelationshipTemplates()))
//            .setRelationshipTemplates(convert(node.getRelationshipTemplates()))
//            .setPolicies(convert(
//                node.getNodeTemplates().stream()
//                    .filter(Objects::nonNull)
//                    .map(org.eclipse.winery.model.tosca.TNodeTemplate::getPolicies)
//                    .filter(Objects::nonNull)
//                    .flatMap(p -> p.getPolicy().stream())
//                    .filter(Objects::nonNull)
//                    .collect(Collectors.toList())
//            ))
//            .setSubstitutionMappings(convert(boundary))
//            .build();
//    }

//    public static Map<String, TNodeTemplate> convert(List<org.eclipse.winery.model.tosca.TNodeTemplate> nodes, List<org.eclipse.winery.model.tosca.TRelationshipTemplate> rTs) {
//        if (Objects.isNull(nodes)) return null;
//        return nodes.stream()
//            .filter(Objects::nonNull)
//            .flatMap(entry -> convert(entry, Optional.ofNullable(rTs).orElse(new ArrayList<>())).entrySet().stream())
//            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
//    }

//    @NonNull
//    public static Map<String, TNodeTemplate> convert(org.eclipse.winery.model.tosca.TNodeTemplate node, @NonNull List<org.eclipse.winery.model.tosca.TRelationshipTemplate> rTs) {
//        if (Objects.isNull(node)) return new LinkedHashMap<>();
//        return Collections.singletonMap(
//            node.getIdFromIdOrNameField(),
//            new TNodeTemplate.Builder(
//                convert(
//                    node.getType(),
//                    new NodeTypeId(node.getType())
//                ))
//                .setProperties(convert(node, node.getProperties()))
//                .setRequirements(convert(node.getRequirements()))
//                .addRequirements(rTs.stream()
//                    .filter(entry -> Objects.nonNull(entry.getSourceElement()) && entry.getSourceElement().getRef().equals(node))
//                    .map(entry -> new LinkedHashMap.SimpleEntry<>(
//                        Optional.ofNullable(entry.getName()).orElse(entry.getId()),
//                        new TRequirementAssignment.Builder()
//                            .setNode(new QName(entry.getTargetElement().getRef().getId()))
//                            .build()
//                    ))
//                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
//                )
//                .setCapabilities(convert(node.getCapabilities()))
//                .setArtifacts(convert(node.getDeploymentArtifacts()))
//                .build()
//        );
//    }

//    public TArtifactDefinition convertArtifactReference(QName ref) {
//        if (Objects.isNull(ref)) return null;
//        return convert(new ArtifactTemplateId(ref));
//    }
//
//    public static Map<String, TArtifactDefinition> convert(TDeploymentArtifacts node) {
//        if (Objects.isNull(node)) return null;
//        return node.getDeploymentArtifact().stream()
//            .filter(Objects::nonNull)
//            .map(ia -> new LinkedHashMap.SimpleEntry<>(ia.getName(), convertArtifactReference(ia.getArtifactRef())))
//            .filter(Objects::nonNull)
//            .filter(entry -> Objects.nonNull(entry.getValue()))
//            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
//    }

    public static Map<String, TCapabilityAssignment> convert(org.eclipse.winery.model.tosca.TNodeTemplate.Capabilities node) {
        if (Objects.isNull(node)) return null;
        return node.getCapability().stream()
            .filter(Objects::nonNull)
            .map(X2YConverter::convert)
            .filter(Objects::nonNull)
            .flatMap(map -> map.entrySet().stream())
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static Map<String, TCapabilityAssignment> convert(TCapability node) {
        if (Objects.isNull(node)) return null;
        return Collections.singletonMap(
            node.getName(),
            new TCapabilityAssignment.Builder()
                .setProperties(convert(node, node.getProperties()))
                .build()
        );
    }

    public static List<TMapRequirementAssignment> convert(org.eclipse.winery.model.tosca.TNodeTemplate.Requirements node) {
        if (Objects.isNull(node)) return null;
        return node.getRequirement().stream()
            .filter(Objects::nonNull)
            .map(X2YConverter::convert)
            .collect(Collectors.toList());
    }

    public static TMapRequirementAssignment convert(TRequirement node) {
        if (Objects.isNull(node)) return null;
        return new TMapRequirementAssignment().setMap(Collections.singletonMap(
            node.getName(),
            new TRequirementAssignment.Builder()
//                .setCapability(convert(repository.getElement(new RequirementTypeId(node.getType()))))
                .build()
        ));
    }

    public static Map<String, TPropertyAssignment> convert(TEntityTemplate tEntityTemplate, TEntityTemplate.Properties node) {
        if (Objects.isNull(node)) return null;
        Map<String, String> propertiesKV = ModelUtilities.getPropertiesKV(tEntityTemplate);
        if (Objects.isNull(propertiesKV)) return null;
        return propertiesKV.entrySet().stream()
            .map(entry ->
                new LinkedHashMap.SimpleEntry<>(
                    String.valueOf(entry.getKey()),
                    new TPropertyAssignment.Builder()
                        .setValue("\"" + ValueConverter.INSTANCE.convert(entry.getValue()) + "\"")
                        .build()
                )
            )
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue
            ));
    }

    @NonNull
    public static Map<String, TPolicyType> convert(org.eclipse.winery.model.tosca.TPolicyType node) {
        if (Objects.isNull(node)) return new LinkedHashMap<>();
        return Collections.singletonMap(
            node.getName(),
            convert(node, new TPolicyType.Builder(), org.eclipse.winery.model.tosca.TPolicyType.class)
                .build()
        );
    }
    
    private static Map<String, TPropertyDefinition> convert(WinerysPropertiesDefinition properties) {
        if (Objects.isNull(properties) ||
            Objects.isNull(properties.getPropertyDefinitionKVList()) ||
            properties.getPropertyDefinitionKVList().isEmpty()) return null;
        return properties.getPropertyDefinitionKVList().stream()
            .collect(Collectors.toMap(
                PropertyDefinitionKV::getKey,
                entry -> new TPropertyDefinition.Builder(convertType(entry.getType()))
                    .setRequired(false)
                    .build()
            ));
    }

    public static Map<String, TInterfaceDefinition> convert(TInterfaces node, TRelationshipTypeImplementation implementation) {
        if (Objects.isNull(node)) return null;
        return node.getInterface().stream()
            .filter(Objects::nonNull)
            .map(entry -> convert(
                entry,
                Optional.ofNullable(implementation.getImplementationArtifacts()).orElse(new TImplementationArtifacts())
                    .getImplementationArtifact().stream()
                    .filter(impl -> Objects.nonNull(impl) && impl.getInterfaceName().equals(entry.getName()))
                    .collect(Collectors.toList())
                )
            )
            .flatMap(entry -> entry.entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @NonNull
    public static Map<String, TInterfaceDefinition> convert(TInterface node, @NonNull List<TImplementationArtifact> impl) {
        if (Objects.isNull(node)) return new LinkedHashMap<>();
        return Collections.singletonMap(
            node.getName(),
            new TInterfaceDefinition.Builder()
                .setOperations(convertOperations(node.getOperation(), impl))
                .build()
        );
    }

    public static Map<String, TOperationDefinition> convertOperations(List<TOperation> nodes, @NonNull List<TImplementationArtifact> impl) {
        if (Objects.isNull(nodes)) return null;
        return nodes.stream()
            .filter(Objects::nonNull)
            .flatMap(node -> convert(
                node,
                impl.stream()
                    .filter(entry -> Objects.nonNull(entry)
                        && Objects.nonNull(entry.getOperationName())
                        && entry.getOperationName().equals(node.getName()))
                    .collect(Collectors.toList())
            ).entrySet().stream())
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @NonNull
    public static Map<String, TOperationDefinition> convert(TOperation node, List<TImplementationArtifact> impl) {
        if (Objects.isNull(node)) return new LinkedHashMap<>();
        return Collections.singletonMap(
            node.getName(),
            new TOperationDefinition.Builder()
                .setInputs(convert(node.getInputParameters()))
                .setOutputs(convert(node.getOutputParameters()))
                .setImplementation(convertImplementation(impl))
                .build()
        );
    }

    public static TImplementation convertImplementation(List<TImplementationArtifact> node) {
        if (Objects.isNull(node) || node.isEmpty()) return null;
        List<TImplementationArtifact> tmp = new ArrayList<>(node);
        return new TImplementation.Builder(new QName(tmp.remove(0).getName()))
            .setDependencies(tmp.stream()
                .filter(artifact -> Objects.nonNull(artifact) && Objects.nonNull(artifact.getName()))
                .map(artifact -> new QName(artifact.getName()))
                .collect(Collectors.toList())
            )
            .build();
    }


    public static TImplementation addArtifact(TImplementation currentImplementation, String node, String targetNameSpace) {
        QName newImplementation = new QName(targetNameSpace, node);
        if (currentImplementation == null) {
            return new TImplementation(newImplementation);
        }
        if (currentImplementation.getPrimary() == null) {
            currentImplementation.setPrimary(newImplementation);
        } else {
            if (currentImplementation.getDependencies() != null) {
                if (!currentImplementation.getDependencies().contains(newImplementation)) {
                    List<QName> currentDependencies = currentImplementation.getDependencies();
                    currentDependencies.add(newImplementation);
                    currentImplementation.setDependencies(currentDependencies);
                }
            } else {
                List<QName> newDependencies = new ArrayList<>();
                newDependencies.add(newImplementation);
                currentImplementation.setDependencies(newDependencies);
            }
        }
        return currentImplementation;
    }

    @NonNull
    public static Map<String, TOperationDefinition> convert(TOperation node) {
        if (Objects.isNull(node)) return new LinkedHashMap<>();
        return Collections.singletonMap(
            node.getName(),
            new TOperationDefinition.Builder()
                .setInputs(convert(node.getInputParameters()))
                .setOutputs(convert(node.getOutputParameters()))
                .build()
        );
    }

    public static Map<String, TPropertyAssignmentOrDefinition> convert(TOperation.OutputParameters node) {
        if (Objects.isNull(node)) return null;
        return node.getOutputParameter().stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(
                TParameter::getName,
                entry -> new TPropertyDefinition.Builder(convertType(entry.getType()))
                    .setRequired(convert(entry.getRequired()))
                    .build()
            ));
    }
    
    public static Map<String, TPropertyAssignmentOrDefinition> convert(TOperation.InputParameters node) {
        if (Objects.isNull(node)) return null;
        return node.getInputParameter().stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(
                TParameter::getName,
                entry -> new TPropertyDefinition.Builder(convertType(entry.getType()))
                    .setRequired(convert(entry.getRequired()))
                    .build()
            ));
    }
    
    
    public static TArtifactDefinition convertArtifactTemplate(TArtifactTemplate node, List<String> files) {
        if (node.getType() != null) {
            return new TArtifactDefinition.Builder(getQName(
                new ArtifactTypeId(node.getType()),
                node.getType().getNamespaceURI(),
                node.getType().getLocalPart()
            ), files)
                .build();
        } else {
            return new TArtifactDefinition.Builder(new QName("null"), files)
                .build();
        }
    }
    public static Map<String, TArtifactType> convert(org.eclipse.winery.model.tosca.TArtifactType node) {
        return Collections.singletonMap(
            node.getIdFromIdOrNameField(),
            convert(node, new TArtifactType.Builder(), org.eclipse.winery.model.tosca.TArtifactType.class).build()
        );
    }

    @NonNull
    public static Map<String, TCapabilityType> convert(org.eclipse.winery.model.tosca.TCapabilityType node) {
        if (Objects.isNull(node)) return new LinkedHashMap<>();
        return Collections.singletonMap(
            node.getName(),
            convert(node, new TCapabilityType.Builder(), org.eclipse.winery.model.tosca.TCapabilityType.class)
                .build()
        );
    }

    public static  <T extends org.eclipse.winery.model.tosca.yaml.TEntityType.Builder<T>> T convert(TEntityType node, T builder, Class<? extends TEntityType> clazz) {
        return builder.setDescription(convertDocumentation(node.getDocumentation()))
            .setDerivedFrom(convert(node.getDerivedFrom(), clazz))
            .setMetadata(convert(node.getTags()))
            .addMetadata("targetNamespace", node.getTargetNamespace())
            .addMetadata("abstract", node.getAbstract().value())
            .addMetadata("final", node.getFinal().value())
            .setProperties(convert(node, node.getPropertiesDefinition()));
    }

    public static QName convert(TEntityType.DerivedFrom node, Class<? extends TEntityType> clazz) {
        if (Objects.isNull(node)) return null;
        DefinitionsChildId id;
        if (clazz.equals(org.eclipse.winery.model.tosca.TNodeType.class)) {
            id = new NodeTypeId(node.getTypeRef());
        } else if (clazz.equals(org.eclipse.winery.model.tosca.TRelationshipType.class)) {
            id = new RelationshipTypeId(node.getTypeRef());
        } else if (clazz.equals(TRequirementType.class)) {
            id = new RequirementTypeId(node.getTypeRef());
        } else if (clazz.equals(org.eclipse.winery.model.tosca.TCapabilityType.class)) {
            id = new CapabilityTypeId(node.getTypeRef());
        } else if (clazz.equals(org.eclipse.winery.model.tosca.TArtifactType.class)) {
            id = new ArtifactTypeId(node.getTypeRef());
        } else {
            id = new PolicyTypeId(node.getTypeRef());
        }
        return getQName(
            id,
            node.getTypeRef().getNamespaceURI(),
            node.getTypeRef().getLocalPart());
    }

    private static Metadata convert(TTags node) {
        if (Objects.isNull(node)) return null;
        return node.getTag().stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(
                TTag::getName,
                TTag::getValue,
                (a, b) -> a + "|" + b,
                Metadata::new));
    }

    private static Map<String, TPropertyDefinition> convert(TEntityType type, TEntityType.PropertiesDefinition node) {
        // TODO convert properties beside simple winery properties
        WinerysPropertiesDefinition properties = type.getWinerysPropertiesDefinition();
        if (Objects.isNull(properties) ||
            Objects.isNull(properties.getPropertyDefinitionKVList()) ||
            properties.getPropertyDefinitionKVList().isEmpty()) return null;
        return properties.getPropertyDefinitionKVList().stream()
            .collect(Collectors.toMap(
                PropertyDefinitionKV::getKey,
                entry -> new TPropertyDefinition.Builder(convertType(entry.getType()))
                    .setRequired(false)
                    .build()
            ));
    }

    private static QName convertType(String type) {
        return TypeConverter.INSTANCE.convert(type);
    }

    private static Map<String, TCapabilityDefinition> convert(org.eclipse.winery.model.tosca.TNodeType.CapabilityDefinitions node) {
        if (Objects.isNull(node) || node.getCapabilityDefinition().isEmpty()) return null;
        return node.getCapabilityDefinition().stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(
                org.eclipse.winery.model.tosca.TCapabilityDefinition::getName,
                X2YConverter::convert
            ));
    }

    private static TCapabilityDefinition convert(org.eclipse.winery.model.tosca.TCapabilityDefinition node) {
        return new TCapabilityDefinition.Builder(
            convert(
                node.getCapabilityType(),
                new CapabilityTypeId(node.getCapabilityType())
            ))
            .setDescription(convertDocumentation(node.getDocumentation()))
            .setOccurrences(node.getLowerBound(), node.getUpperBound())
            .build();
    }

    private static QName convert(QName node, DefinitionsChildId id) {
        if (Objects.isNull(node)) return null;
        return getQName(
            id,
            node.getNamespaceURI(),
            node.getLocalPart()
        );
    }

    private static QName getQName(DefinitionsChildId id, String namespaceURI, String localPart) {
        return new QName(
            namespaceURI,
            localPart,
            ""
        );
    }
    
}
