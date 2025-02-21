package it.fabiodezuani.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeSpec;
import it.fabiodezuani.utils.GeneratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.lang.reflect.Field;

public class DtoGenerator {
    private static final Logger logger = LoggerFactory.getLogger(DtoGenerator.class);

    private GeneratorUtil utils;

    public DtoGenerator(GeneratorUtil utils) {
        this.utils = utils;
    }

    public void generate(String packageName, Class<?> entityClass, String entityName, boolean skipDto) throws IOException {

        if(skipDto) {
            logger.info("\uD83E\uDD20 DTOs skipped!");
            return;
        }

        TypeSpec.Builder dtoBuilder = TypeSpec.classBuilder(utils.getDtoPackage(packageName, entityName))
                .addAnnotation(ClassName.get("lombok", "RequiredArgsConstructor"))
                .addAnnotation(ClassName.get("lombok", "Data"))
                .addModifiers(Modifier.PUBLIC);

        for (Field field : entityClass.getDeclaredFields()) {
            if (field.getType().getPackageName().startsWith("java")) {
                dtoBuilder.addField(FieldSpec.builder(field.getType(), field.getName(), Modifier.PRIVATE).build());
            } else {
                // Nested DTO
                dtoBuilder.addField(FieldSpec.builder(ClassName.get(packageName, field.getType().getSimpleName() + "DTO"), field.getName(), Modifier.PRIVATE)
                        .addAnnotation(ClassName.get("com.fasterxml.jackson.annotation", "JsonIgnore")) // Avoid recursion
                        .build());
            }
        }

        utils.saveJavaFile(packageName + ".dto", dtoBuilder.build());
    }

}
