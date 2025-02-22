package it.fabiodezuani.generator;

import com.squareup.javapoet.*;
import it.fabiodezuani.model.MapperEnum;
import it.fabiodezuani.utils.GeneratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.List;

public class MapperGenerator {
    private static final Logger logger = LoggerFactory.getLogger(MapperGenerator.class);

    private GeneratorUtil utils;

    public MapperGenerator(GeneratorUtil utils) {
        this.utils = utils;
    }

    public void generate(String packageName, String entityName, List<Class<?>> joinedEntities, boolean skipMapper, MapperEnum mapper) throws IOException {
        if (skipMapper) {
            logger.info("\uD83E\uDD20 Mapper skipped!");
            return;
        }
        List<String> entitylist = new java.util.ArrayList<>(List.copyOf(joinedEntities.stream().map(Class::getSimpleName).toList()));
        entitylist.add(entityName);

        for(String entity : entitylist){
            switch (mapper) {
                case MAPSTRUCT:
                    generateMapStructMapper(packageName, entity, joinedEntities.stream().map(Class::getSimpleName).toList());
                    break;
                case OBJECT_MAPPER:
                    generateObjectMapperMapper(packageName, entity);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported mapper type: " + mapper);
            }
        }
    }

    private void generateMapStructMapper(String packageName, String entityName, List<String> joinedEntities) throws IOException {
        TypeSpec.Builder mapperBuilder = TypeSpec.interfaceBuilder(entityName + "Mapper")
                .addModifiers(Modifier.PUBLIC);

        // Create the base mapper annotation with uses clause for dependencies
        AnnotationSpec.Builder mapperAnnotation = AnnotationSpec.builder(ClassName.get("org.mapstruct", "Mapper"))
                .addMember("componentModel", "$S", "spring");

        // Add dependencies to the uses clause if there are any joined entities
        if (!joinedEntities.isEmpty()) {
            StringBuilder usesClause = new StringBuilder();
            for (String dependency : joinedEntities) {
                if (!dependency.equals(entityName)) {
                    if (!usesClause.isEmpty()) usesClause.append(", ");
                    usesClause.append(dependency).append("Mapper.class");
                }
            }
            if (!usesClause.isEmpty()) {
                mapperAnnotation.addMember("uses", "{$L}", usesClause.toString());
            }
        }

        mapperBuilder.addAnnotation(mapperAnnotation.build());

        // Add the basic mapping methods
        mapperBuilder.addMethod(MethodSpec.methodBuilder("toDTO")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(utils.getDtoPackage(packageName, entityName))
                .addParameter(utils.getModelPackage(packageName, entityName), "entity")
                .build());

        mapperBuilder.addMethod(MethodSpec.methodBuilder("toEntity")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(utils.getModelPackage(packageName, entityName))
                .addParameter(utils.getDtoPackage(packageName, entityName), "dto")
                .build());

        // Add @AfterMapping method for each dependency
        for (String dependency : joinedEntities) {
            if (!dependency.equals(entityName)) {
                String fieldName = dependency.toLowerCase();
                MethodSpec afterMapping = MethodSpec.methodBuilder("set" + entityName + "In" + dependency)
                        .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                        .addAnnotation(ClassName.get("org.mapstruct", "AfterMapping"))
                        .addParameter(ParameterSpec.builder(utils.getModelPackage(packageName, entityName), entityName.toLowerCase())
                                .addAnnotation(ClassName.get("org.mapstruct", "MappingTarget"))
                                .build())
                        .beginControlFlow("if ($L.get$L() != null)", entityName.toLowerCase(), dependency)
                        .addStatement("$L.get$L().forEach($L -> $L.set$L($L))",
                                entityName.toLowerCase(), dependency,
                                fieldName.substring(0, 1),
                                fieldName.substring(0, 1),
                                entityName,
                                entityName.toLowerCase())
                        .endControlFlow()
                        .build();

                mapperBuilder.addMethod(afterMapping);
            }
        }

        TypeSpec mapper = mapperBuilder.build();
        utils.saveJavaFile(packageName + ".mapper", mapper);
    }

    private void generateObjectMapperMapper(String packageName, String entityName) throws IOException {
        TypeSpec mapper = TypeSpec.classBuilder(entityName + "Mapper")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get("org.springframework.stereotype", "Component"))
                .addAnnotation(ClassName.get("lombok.extern.slf4j", "Slf4j")) // Add Slf4j annotation for logging
                .addField(ClassName.get("com.fasterxml.jackson.databind", "ObjectMapper"), "objectMapper", Modifier.PRIVATE, Modifier.FINAL)
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addStatement("this.objectMapper = new ObjectMapper()")
                        .build())
                .addMethod(MethodSpec.methodBuilder("toDTO")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(utils.getDtoPackage(packageName, entityName))
                        .addParameter(utils.getModelPackage(packageName, entityName), "entity")
                        .beginControlFlow("try")
                        .addStatement("return objectMapper.readValue(objectMapper.writeValueAsString(entity), $T.class)",
                                utils.getDtoPackage(packageName, entityName))
                        .nextControlFlow("catch ($T e)", ClassName.get("com.fasterxml.jackson.core", "JsonProcessingException"))
                        .addStatement("log.error(\"Error converting entity to DTO\", e)")
                        .addStatement("return null")
                        .endControlFlow()
                        .build())
                .addMethod(MethodSpec.methodBuilder("toEntity")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(utils.getModelPackage(packageName, entityName))
                        .addParameter(utils.getDtoPackage(packageName, entityName), "dto")
                        .beginControlFlow("try")
                        .addStatement("return objectMapper.readValue(objectMapper.writeValueAsString(dto), $T.class)",
                                utils.getModelPackage(packageName, entityName))
                        .nextControlFlow("catch ($T e)", ClassName.get("com.fasterxml.jackson.core", "JsonProcessingException"))
                        .addStatement("log.error(\"Error converting DTO to entity\", e)")
                        .addStatement("return null")
                        .endControlFlow()
                        .build())
                .build();


        utils.saveJavaFile(packageName + ".mapper", mapper);
    }

}
