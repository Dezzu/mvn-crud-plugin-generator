package it.fabiodezuani.generator;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import it.fabiodezuani.model.MapperEnum;
import it.fabiodezuani.utils.GeneratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Modifier;
import java.io.IOException;

public class MapperGenerator {
    private static final Logger logger = LoggerFactory.getLogger(MapperGenerator.class);

    private GeneratorUtil utils;

    public MapperGenerator(GeneratorUtil utils) {
        this.utils = utils;
    }

    public void generate(String packageName, String entityName, boolean skipMapper, MapperEnum mapper) throws IOException {
        if (skipMapper) {
            logger.info("\uD83E\uDD20 Mapper skipped!");
            return;
        }

        switch (mapper) {
            case MAPSTRUCT:
                generateMapStructMapper(packageName, entityName);
                break;
            case OBJECT_MAPPER:
                generateObjectMapperMapper(packageName, entityName);
                break;
            default:
                throw new IllegalArgumentException("Unsupported mapper type: " + mapper);
        }
    }

    private void generateMapStructMapper(String packageName, String entityName) throws IOException {
        TypeSpec mapper = TypeSpec.interfaceBuilder(entityName + "Mapper")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.mapstruct", "Mapper"))
                        .addMember("componentModel", "$S", "spring")
                        .build())
                .addMethod(MethodSpec.methodBuilder("toDTO")
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .returns(ClassName.get(packageName + ".dto", entityName + "Dto"))
                        .addParameter(utils.getModelPackage(packageName, entityName), "entity")
                        .build())
                .addMethod(MethodSpec.methodBuilder("toEntity")
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .returns(utils.getModelPackage(packageName, entityName))
                        .addParameter(ClassName.get(packageName + ".dto", entityName + "Dto"), "dto")
                        .build())
                .build();

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
                        .returns(ClassName.get(packageName + ".dto", entityName + "Dto"))
                        .addParameter(utils.getModelPackage(packageName, entityName), "entity")
                        .beginControlFlow("try")
                        .addStatement("return objectMapper.readValue(objectMapper.writeValueAsString(entity), $T.class)",
                                ClassName.get(packageName + ".dto", entityName + "Dto"))
                        .nextControlFlow("catch ($T e)", ClassName.get("com.fasterxml.jackson.core", "JsonProcessingException"))
                        .addStatement("log.error(\"Error converting entity to DTO\", e)")
                        .addStatement("return null")
                        .endControlFlow()
                        .build())
                .addMethod(MethodSpec.methodBuilder("toEntity")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(utils.getModelPackage(packageName, entityName))
                        .addParameter(ClassName.get(packageName + ".dto", entityName + "Dto"), "dto")
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
