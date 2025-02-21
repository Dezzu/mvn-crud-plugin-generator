package it.fabiodezuani.generator;

import com.squareup.javapoet.*;
import it.fabiodezuani.utils.GeneratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.List;

public class ControllerGenerator {
    private static final Logger logger = LoggerFactory.getLogger(ControllerGenerator.class);

    private GeneratorUtil utils;

    public ControllerGenerator(GeneratorUtil utils) {
        this.utils = utils;
    }

    public void generate(String packageName, String entityName, boolean skipController) throws IOException {

        if(skipController) {
            logger.info("\uD83E\uDD20 Controllers skipped!");
            return;
        }

        // Controller class
        TypeSpec controller = TypeSpec.classBuilder(entityName + "Controller")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get("org.springframework.web.bind.annotation", "RestController"))
                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "RequestMapping"))
                        .addMember("value", "$S", "/api/" + entityName.toLowerCase())
                        .build())
                .addAnnotation(ClassName.get("lombok", "RequiredArgsConstructor")) // Lombok annotation for constructor
                .addField(utils.getServicePackage(packageName, entityName), "service", Modifier.PRIVATE, Modifier.FINAL)

                // Get by ID
                .addMethod(MethodSpec.methodBuilder("getById")
                        .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "GetMapping"))
                                .addMember("value", "$S", "/{id}")
                                .build())
                        .addModifiers(Modifier.PUBLIC)
                        .returns(utils.getDtoPackage(packageName, entityName))
                        .addParameter(ParameterSpec.builder(
                                        Long.class, "id"
                                )
                                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "PathVariable"))
                                        .addMember("value", "$S", "id")
                                        .build())
                                .build())
                        .addStatement("return service.findById(id)")
                        .build())


                // Get all
                .addMethod(MethodSpec.methodBuilder("getAll")
                        .addAnnotation(ClassName.get("org.springframework.web.bind.annotation", "GetMapping"))
                        .addModifiers(Modifier.PUBLIC)
                        .returns(ParameterizedTypeName.get(ClassName.get(List.class), utils.getDtoPackage(packageName, entityName)))
                        .addStatement("return service.findAll()")
                        .build())

                // Create (POST)
                .addMethod(MethodSpec.methodBuilder("create")
                        .addAnnotation(ClassName.get("org.springframework.web.bind.annotation", "PostMapping"))
                        .addModifiers(Modifier.PUBLIC)
                        .returns(utils.getDtoPackage(packageName, entityName))
                        .addParameter(ParameterSpec.builder(utils.getDtoPackage(packageName, entityName), "dto")
                                .addAnnotation(ClassName.get("org.springframework.web.bind.annotation", "RequestBody"))
                                .build())
                        .addStatement("return service.save(dto)")
                        .build())


                // Update (PUT)
                .addMethod(MethodSpec.methodBuilder("update")
                        .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "PutMapping"))
                                .addMember("value", "$S", "/{id}")
                                .build()).addModifiers(Modifier.PUBLIC)
                        .returns(utils.getDtoPackage(packageName, entityName))
                        .addParameter(ParameterSpec.builder(
                                        Long.class, "id"
                                )
                                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "PathVariable"))
                                        .addMember("value", "$S", "id")
                                        .build()).build())
                        .addParameter(ParameterSpec.builder(utils.getDtoPackage(packageName, entityName), "dto")
                                .addAnnotation(ClassName.get("org.springframework.web.bind.annotation", "RequestBody"))
                                .build())
                        .addStatement("return service.update(id, dto)")
                        .build())


                // Delete (DELETE)
                .addMethod(MethodSpec.methodBuilder("delete")
                        .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "DeleteMapping"))
                                .addMember("value", "$S", "/{id}")
                                .build())
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(ParameterSpec.builder(
                                        Long.class, "id"
                                )
                                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "PathVariable"))
                                        .addMember("value", "$S", "id")
                                        .build()).build()).addStatement("service.deleteById(id)")
                        .build())


                .build();

        utils.saveJavaFile(packageName + ".controller", controller);
    }

}
