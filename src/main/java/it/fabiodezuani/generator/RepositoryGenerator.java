package it.fabiodezuani.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import it.fabiodezuani.utils.GeneratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Modifier;
import java.io.IOException;

public class RepositoryGenerator {
    private static final Logger logger = LoggerFactory.getLogger(RepositoryGenerator.class);

    private GeneratorUtil utils;

    public RepositoryGenerator(GeneratorUtil utils) {
        this.utils = utils;
    }

    public void generate(String packageName, String entityName, boolean skipRepository) throws IOException {

        if(skipRepository) {
            logger.info("\uD83E\uDD20 Repository skipped!");
            return;
        }

        TypeSpec repository = TypeSpec.interfaceBuilder(entityName + "Repository")
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ParameterizedTypeName.get(
                        ClassName.get("org.springframework.data.repository", "PagingAndSortingRepository"),
                        utils.getModelPackage(packageName, entityName),
                        ClassName.get(Long.class)
                ))
                .addSuperinterface(ParameterizedTypeName.get(
                        ClassName.get("org.springframework.data.jpa.repository", "JpaRepository"),
                        utils.getModelPackage(packageName, entityName),
                        ClassName.get(Long.class)
                ))
                .build();

        utils.saveJavaFile(packageName + ".repository", repository);
    }
}
