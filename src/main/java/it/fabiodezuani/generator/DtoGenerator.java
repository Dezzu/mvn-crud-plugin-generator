package it.fabiodezuani.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import it.fabiodezuani.utils.GeneratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public class DtoGenerator {
    private static final Logger logger = LoggerFactory.getLogger(DtoGenerator.class);

    private GeneratorUtil utils;

    public DtoGenerator(GeneratorUtil utils) {
        this.utils = utils;
    }

    public void generate(String packageName, Class<?> entityClass, String entityName, boolean skipDto) throws IOException {

        if (skipDto) {
            logger.info("\uD83E\uDD20 DTOs skipped!");
            return;
        }

        generateDto(packageName, entityClass, entityName);
    }

    private void generateDto(String packageName, Class<?> entityClass, String entityName) throws IOException {
        // Keep track of processed entities to prevent infinite recursion
        generateDtoInternal(packageName, entityClass, entityName, new java.util.HashSet<>());
    }

    private void generateDtoInternal(String packageName, Class<?> entityClass, String entityName,
                                    java.util.Set<Class<?>> processedEntities) throws IOException {
        if (processedEntities.contains(entityClass)) {
            return;
        }
        processedEntities.add(entityClass);

        TypeSpec.Builder dtoBuilder = TypeSpec.classBuilder(entityName + "Dto")
                .addAnnotation(ClassName.get("lombok", "RequiredArgsConstructor"))
                .addAnnotation(ClassName.get("lombok", "Data"))
                .addModifiers(Modifier.PUBLIC);

        for (Field field : entityClass.getDeclaredFields()) {
            java.lang.reflect.Type type = field.getGenericType();
            if (type instanceof java.lang.reflect.ParameterizedType paramType) {
                java.lang.reflect.Type[] typeArgs = paramType.getActualTypeArguments();
                Class<?> rawType = (Class<?>) paramType.getRawType();

                if (List.class.isAssignableFrom(rawType)) {
                    Class<?> genericType = (Class<?>) typeArgs[0];
                    if (!genericType.getPackageName().startsWith("java")) {
                        generateDtoInternal(packageName, genericType, genericType.getSimpleName(), processedEntities);
                        ClassName dtoType = utils.getDtoPackage(packageName, genericType.getSimpleName());
                        dtoBuilder.addField(FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(List.class), dtoType),
                                field.getName(),
                                Modifier.PRIVATE
                        ).build());
                    } else {
                        dtoBuilder.addField(FieldSpec.builder(field.getType(), field.getName(), Modifier.PRIVATE).build());
                    }
                } else if (Map.class.isAssignableFrom(rawType)) {
                    Class<?> keyType = (Class<?>) typeArgs[0];
                    Class<?> valueType = (Class<?>) typeArgs[1];
                    if (!valueType.getPackageName().startsWith("java")) {
                        generateDtoInternal(packageName, valueType, valueType.getSimpleName(), processedEntities);
                        ClassName valueDtoType = utils.getDtoPackage(packageName, valueType.getSimpleName());
                        dtoBuilder.addField(FieldSpec.builder(
                                ParameterizedTypeName.get(
                                        ClassName.get(Map.class),
                                        ClassName.get(keyType),
                                        valueDtoType
                                ),
                                field.getName(),
                                Modifier.PRIVATE
                        ).build());
                    } else {
                        dtoBuilder.addField(FieldSpec.builder(field.getType(), field.getName(), Modifier.PRIVATE).build());
                    }
                }
            } else if (field.getType().getPackageName().startsWith("java")) {
                dtoBuilder.addField(FieldSpec.builder(field.getType(), field.getName(), Modifier.PRIVATE).build());
            } else {
                String nestedEntityName = field.getType().getSimpleName();
                generateDtoInternal(packageName, field.getType(), nestedEntityName, processedEntities);
                ClassName dtoType = utils.getDtoPackage(packageName, nestedEntityName);
                dtoBuilder.addField(FieldSpec.builder(dtoType, field.getName(), Modifier.PRIVATE)
                        .addAnnotation(ClassName.get("com.fasterxml.jackson.annotation", "JsonIgnore"))
                        .build());
            }
        }

        utils.saveJavaFile(packageName + ".dto", dtoBuilder.build());
    }
}