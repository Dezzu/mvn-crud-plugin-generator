package it.fabiodezuani.generator;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
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

    public void generate(String packageName, String entityName, boolean skipMapper) throws IOException {

        if(skipMapper) {
            logger.info("\uD83E\uDD20 Mapper skipped!");
            return;
        }

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

}
