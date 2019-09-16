package org.eclipse.winery.repository.backend.filebased;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.namespace.QName;

import org.eclipse.winery.model.tosca.TAppliesTo;
import org.eclipse.winery.model.tosca.TArtifactReference;
import org.eclipse.winery.model.tosca.TArtifactTemplate;
import org.eclipse.winery.model.tosca.TArtifactType;
import org.eclipse.winery.model.tosca.TCapabilityType;
import org.eclipse.winery.model.tosca.TDeploymentArtifact;
import org.eclipse.winery.model.tosca.TDeploymentArtifacts;
import org.eclipse.winery.model.tosca.TEntityTemplate;
import org.eclipse.winery.model.tosca.TEntityType;
import org.eclipse.winery.model.tosca.TImplementationArtifacts;
import org.eclipse.winery.model.tosca.TInterface;
import org.eclipse.winery.model.tosca.TNodeType;
import org.eclipse.winery.model.tosca.TNodeTypeImplementation;
import org.eclipse.winery.model.tosca.TOperation;
import org.eclipse.winery.model.tosca.TParameter;
import org.eclipse.winery.model.tosca.TPolicyType;
import org.eclipse.winery.model.tosca.TRelationshipType;
import org.eclipse.winery.model.tosca.TRelationshipTypeImplementation;
import org.eclipse.winery.model.tosca.TRequirement;
import org.eclipse.winery.model.tosca.TRequirementDefinition;
import org.eclipse.winery.model.tosca.TRequirementType;
import org.eclipse.winery.model.tosca.TTag;
import org.eclipse.winery.model.tosca.TTags;
import org.eclipse.winery.model.tosca.yaml.TArtifactDefinition;
import org.eclipse.winery.model.tosca.yaml.TAttributeDefinition;
import org.eclipse.winery.model.tosca.yaml.TImportDefinition;
import org.eclipse.winery.model.tosca.yaml.TInterfaceDefinition;
import org.eclipse.winery.model.tosca.yaml.TInterfaceType;
import org.eclipse.winery.model.tosca.yaml.TOperationDefinition;
import org.eclipse.winery.model.tosca.yaml.TPolicyDefinition;
import org.eclipse.winery.model.tosca.yaml.TPropertyAssignment;
import org.eclipse.winery.model.tosca.yaml.TPropertyAssignmentOrDefinition;
import org.eclipse.winery.model.tosca.yaml.TPropertyDefinition;
import org.eclipse.winery.model.tosca.yaml.TRequirementAssignment;
import org.eclipse.winery.model.tosca.yaml.support.Metadata;
import org.eclipse.winery.repository.backend.filebased.converter.support.yaml.TypeConverter;
import org.eclipse.winery.repository.backend.filebased.converter.support.yaml.extension.TImplementationArtifactDefinition;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

public class Y2XConverter {
    
    public static TNodeType convert(Map.Entry<String, org.eclipse.winery.model.tosca.yaml.TNodeType> node) {
        if (Objects.isNull(node)) return null;
        TNodeType.Builder builder = convert(node.getValue(), new TNodeType.Builder(node.getKey()))
            .addRequirementDefinitions(convert(node.getValue().getRequirements()))
            .addCapabilityDefinitions(convert(node.getValue().getCapabilities()))
            .addInterfaces(convert(refactor(node.getValue().getInterfaces(), node.getValue())));
        return builder.build();
    }

    /**
     * Inserts operation output definitions defined in attributes "{ get_operation_output: [ SELF, interfaceName,
     * operationName, propertyName ] }" into interfaceDefinitions
     */
    private static Map<String, TInterfaceDefinition> refactor(Map<String, TInterfaceDefinition> map, org.eclipse.winery.model.tosca.yaml.TNodeType node) {
        if (Objects.isNull(map) || map.isEmpty() || node.getAttributes().isEmpty()) return map;

        // Extract Outputs from Attributes and attach them to the Operations (if possible)
        // Template: attribute.default: { get_operation_output: [ SELF, interfaceName, operationName, propertyName ] }
        for (Map.Entry<String, TAttributeDefinition> entry : node.getAttributes().entrySet()) {
            TAttributeDefinition attr = entry.getValue();
            if (attr.getDefault() != null && attr.getDefault() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> aDefault = (Map<String, Object>) attr.getDefault();
                if (aDefault != null && aDefault.containsKey("get_operation_output")) {
                    @SuppressWarnings("unchecked")
                    List<String> values = (List<String>) aDefault.get("get_operation_output");
                    if (values.size() == 4 &&
                        values.get(0).equals("SELF") &&
                        map.containsKey(values.get(1)) &&
                        map.get(values.get(1)).getOperations().containsKey(values.get(2)) &&
                        !map.get(values.get(1)).getOperations().get(values.get(2)).getOutputs().containsKey(values.get(3))
                    ) {
                        TPropertyDefinition.Builder pBuilder = new TPropertyDefinition.Builder(attr.getType());
                        map.get(values.get(1)).getOperations().get(values.get(2)).getOutputs().put(values.get(3), pBuilder.build());
                    }
                }
            }
        }

        return map;
    }
    
    public static List<TNodeTypeImplementation> convertNodeTypeImplementaions(Map.Entry<String, org.eclipse.winery.model.tosca.yaml.TNodeType> node, String targetNamespace) {
        //TODO refractor
        return convertNodeTypeImplementation(
            node.getValue().getArtifacts(),
            node.getValue().getArtifacts(),
            node.getKey(),
            targetNamespace
        );
    }
    

    /**
     * Converts a TOSCA YAML ArtifactDefinition to a TOSCA XML ArtifactTemplate
     *
     * @param node TOSCA YAML ArtifactDefinition
     * @return TOSCA XML ArtifactTemplate
     */
    @NonNull
    public static List<TArtifactTemplate> convertArtifactTemplate(TArtifactDefinition node, String id) {
        List<TArtifactTemplate> output = new ArrayList<>();
        TArtifactTemplate.Builder artifactBuilder = new TArtifactTemplate.Builder(id, node.getType())
            .setProperties(convertPropertyAssignments(node.getProperties(), getPropertyTypeName(node.getType())));
        if (node.getFiles() != null) {
            artifactBuilder.addArtifactReferences(node.getFiles().stream()
                .filter(Objects::nonNull)
                // TODO change filepath
                .map(file -> new TArtifactReference.Builder(file).build())
                .collect(Collectors.toList())
            );
        }
        output.add(artifactBuilder.build());
        return output;
    }

    /**
     * Converts a map of TOSCA YAML PropertyAssignment to TOSCA XML EntityTemplate.Properties
     */
    private static TEntityTemplate.Properties convertPropertyAssignments(Map<String, TPropertyAssignment> propertyMap, QName type) {
        if (Objects.isNull(propertyMap)) return null;
        TEntityTemplate.Properties properties = new TEntityTemplate.Properties();
//        properties.setAny(assignmentBuilder.getAssignment(propertyMap, type));
        return properties;
    }

    /**
     * Constructs the the name of the PropertyType for a given type
     */
    private static QName getPropertyTypeName(QName type) {
        return new QName(type.getNamespaceURI(), type.getLocalPart() + "_Properties");
    }

    /**
     * Converts TOSCA YAML InterfaceDefinitions to TOSCA XML Interface Additional TOSCA YAML element input with
     * PropertyAssignment or PropertyDefinition is not converted
     *
     * @param node TOSCA YAML InterfaceDefinition
     * @return TOSCA XML Interface
     */
    private static TInterface convert(TInterfaceDefinition node, String id) {
        List<TOperation> operation = new ArrayList<>();
//        if (this.interfaceTypes.containsKey(node.getType())) {
//            operation.addAll(convert(this.interfaceTypes.get(node.getType()).getOperations()));
//        }

        operation.addAll(convert(node.getOperations()));

        TInterface.Builder builder = new TInterface.Builder(id, operation);

        return builder.build();
    }

    /**
     * Converts TOSCA YAML ArtifactTypes to TOSCA XML ArtifactTypes. Both objects have a super type EntityType.
     * Additional elements mime_type and file_ext from TOSCA YAML are moved to tags in TOSCA XML
     *
     * @param node the YAML ArtifactType
     * @return TOSCA XML ArtifactType
     */
    public static TArtifactType convert(org.eclipse.winery.model.tosca.yaml.TArtifactType node, String id) {
        if (node == null) return null;
        TArtifactType.Builder builder = new TArtifactType.Builder(id);
        convert(node, builder);
        if (node.getFileExt() != null) {
            builder.addTags("file_ext", "[" + node.getFileExt().stream().map(Object::toString)
                .collect(Collectors.joining(",")) + "]");
        }
        if (node.getMimeType() != null) {
            builder.addTags("mime_type", node.getMimeType());
        }
        return builder.build();
    }
    

    /**
     * Converts TOSCA YAML ArtifactDefinitions to TOSCA XML NodeTypeImplementations and ArtifactTemplates
     */
    private static List<TNodeTypeImplementation> convertNodeTypeImplementation(
        Map<String, TArtifactDefinition> implArtifacts,
        Map<String, TArtifactDefinition> deplArtifacts, String type, String targetNamespace) {
        List<TNodeTypeImplementation> output = new ArrayList<>();
        output.add(new TNodeTypeImplementation.Builder(type + "-impl", new QName(targetNamespace, type))
            .setTargetNamespace(targetNamespace)
            .setDeploymentArtifacts(convertDeploymentArtifacts(deplArtifacts))
            .setImplementationArtifacts(convertImplementationArtifact(implArtifacts))
            .build()
        );
        return output;
    }

    /**
     * Converts TOSCA YAML ArtifactDefinitions to TOSCA XML ImplementationArtifacts
     *
     * @param artifactDefinitionMap map of TOSCA YAML ArtifactDefinitions
     * @return TOSCA XML ImplementationArtifacts
     */
    private static TImplementationArtifacts convertImplementationArtifact(@NonNull Map<String, TArtifactDefinition> artifactDefinitionMap) {
        if (artifactDefinitionMap.isEmpty()) return null;
        return new TImplementationArtifacts.Builder(artifactDefinitionMap.entrySet().stream()
            .filter(entry -> Objects.nonNull(entry) && Objects.nonNull(entry.getValue()))
            .map(entry -> {
                return new TImplementationArtifacts.ImplementationArtifact.Builder(entry.getValue().getType())
                    .setName(entry.getKey())
                    .setArtifactRef(new QName(entry.getKey()))
                    .setInterfaceName(convertInterfaceName(entry.getValue()))
                    .setOperationName(convertOperationName(entry.getValue()))
                    .build();
            })
            .collect(Collectors.toList()))
            .build();
    }

    @Nullable
    public static String convertInterfaceName(@NonNull TArtifactDefinition node) {
        if (node instanceof TImplementationArtifactDefinition)
            return ((TImplementationArtifactDefinition) node).getInterfaceName();
        return null;
    }

    @Nullable
    public static String convertOperationName(@NonNull TArtifactDefinition node) {
        if (node instanceof TImplementationArtifactDefinition)
            return ((TImplementationArtifactDefinition) node).getOperationName();
        return null;
    }


    /**
     * Converts TOSCA YAML RelationshipTypes to TOSCA XML RelationshipTypes Additional element valid_target_types
     * (specifying Capability Types) is not converted
     *
     * @param node TOSCA YAML RelationshipType
     * @return TOSCA XML RelationshipType
     */
    public static TRelationshipType convert(org.eclipse.winery.model.tosca.yaml.TRelationshipType node, String id) {
        if (Objects.isNull(node)) return null;
        return convert(node, new TRelationshipType.Builder(id))
            // TODO source or target interface?
            .addSourceInterfaces(convert(node.getInterfaces()))
            .build();
    }
    
    
    public static TRelationshipTypeImplementation convertRelationshipTypeImplementation(org.eclipse.winery.model.tosca.yaml.TRelationshipType node, String id) {
//        if (Objects.isNull(node)) return null;
//        TRelationshipTypeImplementation relationshipTypeImplementation = new TRelationshipTypeImplementation();
//        relationshipTypeImplementation.set
        return null;
    }

    /**
     * Converts TOSCA YAML PolicyTypes to TOSCA XML  PolicyTypes Additional TOSCA YAML element triggers is not
     * converted
     *
     * @param node TOSCA YAML PolicyType
     * @return TOSCA XML PolicyType
     */
    public static TPolicyType convert(org.eclipse.winery.model.tosca.yaml.TPolicyType node, String id) {
        if (node == null) {
            return null;
        }

        TPolicyType.Builder builder = new TPolicyType.Builder(id);
        convert(node, builder);
        builder.setAppliesTo(convertTargets(node.getTargets()));

        return builder.build();
    }

    /**
     * Convert A list of TOSCA YAML PolicyType targets to TOSCA XML PolicyType AppliesTo
     *
     * @param targetList list of TOSCA YAML PolicyType targets
     * @return TOSCA XML PolicyType AppliesTo
     */
    private static TAppliesTo convertTargets(List<QName> targetList) {
        if (targetList == null || targetList.size() == 0) {
            return null;
        }

        List<TAppliesTo.NodeTypeReference> references = new ArrayList<>();
        for (QName nodeRef : targetList) {
            TAppliesTo.NodeTypeReference ref = new TAppliesTo.NodeTypeReference();
            ref.setTypeRef(nodeRef);
            references.add(ref);
        }

        TAppliesTo appliesTo = new TAppliesTo();
        appliesTo.getNodeTypeReference().addAll(references);
        return appliesTo;
    }

    /**
     * Converts TOSCA YAML ArtifactDefinitions to TOSCA XML DeploymentArtifacts
     *
     * @param artifactDefinitionMap map of TOSCA YAML ArtifactDefinitions
     * @return TOSCA XML DeploymentArtifacts
     */
    private static TDeploymentArtifacts convertDeploymentArtifacts(@NonNull Map<String, TArtifactDefinition> artifactDefinitionMap) {
        if (artifactDefinitionMap.isEmpty()) return null;
        return new TDeploymentArtifacts.Builder(artifactDefinitionMap.entrySet().stream()
            .filter(Objects::nonNull)
            .map(entry -> {
                return new TDeploymentArtifact.Builder(entry.getKey(), entry.getValue().getType())
                    .setArtifactRef(new QName(entry.getKey()))
                    .build();
            })
            .collect(Collectors.toList()))
            .build();
    }
    


    /**
     * Converts TOSCA YAML EntityTypes to TOSCA XML EntityTypes
     * <p>
     * Additional element version added to tag. Missing elements abstract, final will not be set. Missing element
     * targetNamespace is searched in metadata
     *
     * @param node TOSCA YAML EntityType
     * @return TOSCA XML EntityType
     */
    private static  <T extends TEntityType.Builder<T>> T convert(org.eclipse.winery.model.tosca.yaml.TEntityType node, T builder) {
        builder.addDocumentation(node.getDescription())
            .setDerivedFrom(node.getDerivedFrom())
            .addTags(convertMetadata(node.getMetadata()))
            .setTargetNamespace(node.getMetadata().get("targetNamespace"));

        if (node.getVersion() != null) {
            TTag tag = new TTag();
            tag.setName("version");
            tag.setValue(node.getVersion().getVersion());
            builder.addTags(tag);
        }

//        if (!node.getProperties().isEmpty()) {
//            builder.setPropertiesDefinition(convertPropertyDefinition(builder.build().getIdFromIdOrNameField() + "_Properties"));
//        }

        return builder;
    }

    private static  <V, T> List<T> convert(List<? extends Map<String, V>> node) {
        return node.stream()
            .flatMap(map -> map.entrySet().stream())
            .map((Map.Entry<String, V> entry) -> {
//                if (entry.getValue() instanceof TImportDefinition) {
//                    return (T) convert((TImportDefinition) entry.getValue(), entry.getKey());
//                } else 
                    if (entry.getValue() instanceof org.eclipse.winery.model.tosca.yaml.TRequirementDefinition) {
                    return (T) convert((org.eclipse.winery.model.tosca.yaml.TRequirementDefinition) entry.getValue(), entry.getKey());
                } else if (entry.getValue() instanceof TRequirementAssignment) {
                    return (T) convert((TRequirementAssignment) entry.getValue(), entry.getKey());
                } else {
                    V v = entry.getValue();
                    assert (v instanceof TImportDefinition ||
                        v instanceof org.eclipse.winery.model.tosca.yaml.TRequirementDefinition ||
                        v instanceof TRequirementAssignment);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * Converts TOSCA YAML RequirementDefinition to TOSCA XML RequirementDefinition
     *
     * @param node TOSCA YAML RequirementDefinition
     * @return TOSCA XML RequirementDefinition
     */
    private static TRequirementDefinition convert(org.eclipse.winery.model.tosca.yaml.TRequirementDefinition node, String id) {
        if (Objects.isNull(node)) return null;
        // TOSCA YAML does not have RequirementTypes:
        // * construct TOSCA XML RequirementType from TOSCA YAML Requirement Definition	
        return new TRequirementDefinition.Builder(id,
            convertRequirementDefinition(
                node,
                getRequirementTypeName(node.getCapability(), id)
            ))
            .setLowerBound(node.getLowerBound())
            .setUpperBound(node.getUpperBound())
            .build();
    }

    /**
     * Convert TOSCA YAML RequirementDefinition to TOSCA XML RequirementType
     *
     * @param node TOSCA YAML RequirementDefinition
     * @param id   with name of the TRequirementType
     * @return QName of the TOSCA XML RequirementType
     */
    private static QName convertRequirementDefinition(org.eclipse.winery.model.tosca.yaml.TRequirementDefinition node, String id) {
        if (node == null) return null;
        String namespace = Optional.ofNullable(node.getCapability()).map(QName::getNamespaceURI).orElse("NAMESPACE");
        TRequirementType result = new TRequirementType.Builder(id)
            .setRequiredCapabilityType(node.getCapability())
            .setTargetNamespace(namespace)
            .build();
        return new QName(namespace, result.getName());
    }

    /**
     * Converts TOSCA YAML RequirementAssignments to TOSCA XML Requirements Additional TOSCA YAML elements node_filter
     * and occurrences are not converted
     *
     * @param node TOSCA YAML RequirementAssignments
     * @return return List of TOSCA XML Requirements
     */
    private static TRequirement convert(TRequirementAssignment node, String id) {
        if (Objects.isNull(node)) return null;
        // Skip requirement if it only the field node is set
        if (Objects.nonNull(node.getNode())
            && Objects.isNull(node.getCapability())
            && Objects.isNull(node.getNodeFilter())
            && node.getOccurrences().isEmpty()
            && Objects.isNull(node.getRelationship())) return null;

        return new TRequirement.Builder(id, new QName(
            Optional.ofNullable(node.getCapability()).map(QName::getNamespaceURI).orElse("NAMESPACE"),
            getRequirementTypeName(node.getCapability(), id)
        ))
            .build();
    }

    private static String getRequirementTypeName(QName capability, String id) {
        if (Objects.isNull(capability)) return id.concat("Type");
        return "Req".concat(capability.getLocalPart());
    }

    /**
     * Converts TOSCA YAML metadata to TOSCA XML Tags
     *
     * @param metadata map of strings
     * @return TOSCA XML Tags
     */
    @NonNull
    private static TTags convertMetadata(Metadata metadata) {
        return new TTags.Builder()
            .addTag(
                metadata.entrySet().stream()
                    .filter(Objects::nonNull)
                    .map(entry -> new TTag.Builder().setName(entry.getKey()).setValue(entry.getValue()).build())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList())
            )
            .build();
    }

    /**
     * Converts TOSCA YAML CapabilityTypes to TOSCA XML CapabilityTypes
     *
     * @param node TOSCA YAML CapabilityType
     * @return TOSCA XML CapabilityType
     */
    public static TCapabilityType convert(org.eclipse.winery.model.tosca.yaml.TCapabilityType node, String id) {
        if (Objects.isNull(node)) return null;
        return convert(node, new TCapabilityType.Builder(id))
            .addTags(convertValidSourceTypes(node.getValidSourceTypes()))
            .build();
    }

    private static TTag convertValidSourceTypes(@NonNull List<QName> node) {
        if (node.isEmpty()) return null;
        return new TTag.Builder()
            .setName("valid_source_types")
            .setValue("[" + node.stream().map(QName::toString).collect(Collectors.joining(",")) + "]")
            .build();
    }

    private static TOperation convert(TOperationDefinition node, String id) {
        return new TOperation.Builder(id)
            .addDocumentation(node.getDescription())
            .addInputParameters(convertParameters(node.getInputs()))
            .addOutputParameters(convertParameters(node.getOutputs()))
            .build();
    }

    private static List<TParameter> convertParameters(Map<String, TPropertyAssignmentOrDefinition> node) {
        return node.entrySet().stream()
            .map(entry -> {
                if (entry.getValue() instanceof TPropertyDefinition) {
                    return convertParameter((TPropertyDefinition) entry.getValue(), entry.getKey());
                } else {
                    return null;
                }
            }).filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private static TParameter convertParameter(TPropertyDefinition node, String id) {
        return new TParameter.Builder(
            id,
            TypeConverter.INSTANCE.convert(node.getType()).getLocalPart(),
            node.getRequired()
        ).build();
    }

    @SuppressWarnings( {"unchecked"})
    private static  <V, T> List<T> convert(@NonNull Map<String, V> map) {
        return map.entrySet().stream()
            .map((Map.Entry<String, V> entry) -> {
//                if (entry.getValue() == null) {
//                    return null;
//                } else if (entry.getValue() instanceof org.eclipse.winery.model.tosca.yaml.TRelationshipType) {
//                    return convert((org.eclipse.winery.model.tosca.yaml.TRelationshipType) entry.getValue(), entry.getKey());
//                } else if (entry.getValue() instanceof org.eclipse.winery.model.tosca.yaml.TRelationshipTemplate) {
//                    return convert((org.eclipse.winery.model.tosca.yaml.TRelationshipTemplate) entry.getValue(), entry.getKey());
//                } else if (entry.getValue() instanceof org.eclipse.winery.model.tosca.yaml.TArtifactType) {
//                    return convert((org.eclipse.winery.model.tosca.yaml.TArtifactType) entry.getValue(), entry.getKey());
//                } else if (entry.getValue() instanceof TArtifactDefinition) {
//                    return convert((TArtifactDefinition) entry.getValue(), entry.getKey());
//                } else 
                if (entry.getValue() instanceof org.eclipse.winery.model.tosca.yaml.TCapabilityType) {
                    return convert((org.eclipse.winery.model.tosca.yaml.TCapabilityType) entry.getValue(), entry.getKey());
                } 
//                else if (entry.getValue() instanceof org.eclipse.winery.model.tosca.yaml.TCapabilityDefinition) {
//                    return convert((org.eclipse.winery.model.tosca.yaml.TCapabilityDefinition) entry.getValue(), entry.getKey());
//                } else if (entry.getValue() instanceof org.eclipse.winery.model.tosca.yaml.TPolicyType) {
//                    return convert((org.eclipse.winery.model.tosca.yaml.TPolicyType) entry.getValue(), entry.getKey());
//                } else if (entry.getValue() instanceof org.eclipse.winery.model.tosca.yaml.TRequirementDefinition) {
//                    return convert((org.eclipse.winery.model.tosca.yaml.TRequirementDefinition) entry.getValue(), entry.getKey());
//                } else if (entry.getValue() instanceof TInterfaceType) {
//                    assert (!interfaceTypes.containsKey(new QName(entry.getKey())));
//                    this.interfaceTypes.put(new QName(entry.getKey()), (TInterfaceType) entry.getValue());
//                    return null;
                 else if (entry.getValue() instanceof TInterfaceDefinition) {
                    return convert((TInterfaceDefinition) entry.getValue(), entry.getKey());
                } 
                else if (entry.getValue() instanceof TOperationDefinition) {
                    return convert((TOperationDefinition) entry.getValue(), entry.getKey());
                } 
//                else if (entry.getValue() instanceof org.eclipse.winery.model.tosca.yaml.TNodeTemplate) {
//                    return convert((org.eclipse.winery.model.tosca.yaml.TNodeTemplate) entry.getValue(), entry.getKey());
//                } else if (entry.getValue() instanceof org.eclipse.winery.model.tosca.yaml.TDataType) {
//                    return convert((org.eclipse.winery.model.tosca.yaml.TDataType) entry.getValue(), entry.getKey());
//                } else if (entry.getValue() instanceof org.eclipse.winery.model.tosca.yaml.TGroupType) {
//                    return convert((org.eclipse.winery.model.tosca.yaml.TGroupType) entry.getValue(), entry.getKey());
//                } else if (entry.getValue() instanceof org.eclipse.winery.model.tosca.yaml.TNodeType) {
//                    return convert((org.eclipse.winery.model.tosca.yaml.TNodeType) entry.getValue(), entry.getKey());
//                } else if (entry.getValue() instanceof TImportDefinition) {
//                    return convert((TImportDefinition) entry.getValue(), entry.getKey());
//                } else if (entry.getValue() instanceof org.eclipse.winery.model.tosca.yaml.TPolicyType) {
//                    return convert((org.eclipse.winery.model.tosca.yaml.TPolicyType) entry.getValue(), entry.getKey());
//                } else {
//                    V v = entry.getValue();
//                    System.err.println(v);
//                    assert (v instanceof org.eclipse.winery.model.tosca.yaml.TRelationshipType ||
//                        v instanceof org.eclipse.winery.model.tosca.yaml.TRelationshipTemplate ||
//                        v instanceof org.eclipse.winery.model.tosca.yaml.TArtifactType ||
//                        v instanceof TArtifactDefinition ||
//                        v instanceof org.eclipse.winery.model.tosca.yaml.TCapabilityType ||
//                        v instanceof org.eclipse.winery.model.tosca.yaml.TCapabilityDefinition ||
//                        v instanceof org.eclipse.winery.model.tosca.yaml.TPolicyType ||
//                        v instanceof org.eclipse.winery.model.tosca.yaml.TRequirementDefinition ||
//                        v instanceof TInterfaceType ||
//                        v instanceof TInterfaceDefinition ||
//                        v instanceof TOperationDefinition ||
//                        v instanceof org.eclipse.winery.model.tosca.yaml.TNodeTemplate ||
//                        v instanceof org.eclipse.winery.model.tosca.yaml.TDataType ||
//                        v instanceof org.eclipse.winery.model.tosca.yaml.TGroupType ||
//                        v instanceof org.eclipse.winery.model.tosca.yaml.TNodeType ||
//                        v instanceof TImportDefinition ||
//                        v instanceof TPolicyDefinition
//                    );
//                    return null;
//                }
                else {
                    System.out.println(entry.getValue());
                    return null;
                }
            })
            .flatMap(entry -> {
                if (entry instanceof List) {
                    return ((List<T>) entry).stream();
                } else {
                    return (Stream<T>) Stream.of(entry);
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
