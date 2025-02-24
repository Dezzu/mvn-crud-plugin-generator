package it.fabiodezuani.generator;

import com.squareup.javapoet.*;
import it.fabiodezuani.utils.GeneratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.List;

public class ServiceGenerator {
    private static final Logger logger = LoggerFactory.getLogger(ServiceGenerator.class);

    private GeneratorUtil utils;

    public ServiceGenerator(GeneratorUtil utils) {
        this.utils = utils;
    }

    public void generate(String packageName, String entityName, boolean skipService) throws IOException {

        if(skipService) {
            logger.info("\uD83E\uDD20 Service skipped!");
            return;
        }

        ClassName repository = utils.getRepositoryPackage(packageName, entityName);
        ClassName mapper = utils.getMapperPackage(packageName, entityName);
        ClassName dto = utils.getDtoPackage(packageName, entityName);

        TypeSpec service = TypeSpec.classBuilder(entityName + "Service")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get("org.springframework.stereotype", "Service"))
                .addAnnotation(ClassName.get("lombok", "RequiredArgsConstructor"))
                .addAnnotation(ClassName.get("lombok.extern.slf4j", "Slf4j")) // Add Slf4j annotation for logging
                .addField(repository, "repository", Modifier.PRIVATE, Modifier.FINAL)
                .addField(mapper, "mapper", Modifier.PRIVATE, Modifier.FINAL)

                // Find all
                .addMethod(MethodSpec.methodBuilder("findAll")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(ParameterizedTypeName.get(ClassName.get("org.springframework.data.domain", "Page"), dto))
                        .addParameter(ParameterSpec.builder(utils.getDtoClassName(packageName, "PaginationRequestDto"), "pageRequest").build())
                        .addStatement("log.debug(\"Executing findAll() method\")")
                        .addStatement("return repository.findAll(pageRequest.toPageRequest()).map(mapper::toDTO)")
                        .build())

                // Find by ID
                .addMethod(MethodSpec.methodBuilder("findById")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(dto)
                        .addParameter(Long.class, "id")
                        .addStatement("log.debug(\"Executing findById() method with id: {}\", id)")
                        .addStatement("return repository.findById(id).map(mapper::toDTO).orElse(null)")
                        .build())

                // Save (Create)
                .addMethod(MethodSpec.methodBuilder("save")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(dto)
                        .addParameter(dto, "dto")
                        .addStatement("log.debug(\"Executing save() method with DTO: {}\", dto)")
                        .addStatement("dto = mapper.toDTO(repository.save(mapper.toEntity(dto)))")
                        .addStatement("log.info(\"Entity created and saved successfully: {}\", dto)")
                        .addStatement("return dto")
                        .build())

                // Delete by ID
                .addMethod(MethodSpec.methodBuilder("deleteById")
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(Long.class, "id")
                        .addStatement("log.debug(\"Executing deleteById() method with id: {}\", id)")
                        .addStatement("repository.deleteById(id)")
                        .addStatement("log.info(\"Entity with id {} deleted successfully\", id)")
                        .build())

                // Update method
                .addMethod(MethodSpec.methodBuilder("update")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(dto)
                        .addParameter(Long.class, "id")
                        .addParameter(dto, "dto")
                        .addStatement("dto.setId(id)")
                        .addStatement("log.debug(\"Executing update() method with id: {} and DTO: {}\", id, dto)")
                        .beginControlFlow("if (repository.existsById(id))")  // Check if the entity exists
                        .addStatement("log.info(\"Entity with id {} found, proceeding with update\", id)")
                        .addStatement("dto = mapper.toDTO(repository.save(mapper.toEntity(dto)))") // Update and save
                        .addStatement("log.info(\"Entity with id {} updated successfully: {}\", id, dto)")
                        .addStatement("return dto")
                        .endControlFlow()
                        .addStatement("log.warn(\"Entity with id {} not found, cannot update\", id)") // Log warning if entity is not found
                        .addStatement("return null") // Return null if entity doesn't exist
                        .build())

                .build();

        utils.saveJavaFile(packageName + ".service", service);
    }

}
