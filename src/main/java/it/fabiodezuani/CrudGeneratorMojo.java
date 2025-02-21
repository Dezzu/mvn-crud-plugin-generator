package it.fabiodezuani;

import com.squareup.javapoet.*;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Maven Plugin to generate CRUD services with DTOs and MapStruct.
 */
@Mojo(name = "generate-crud")
public class CrudGeneratorMojo extends AbstractMojo {

    private static final Logger logger = LoggerFactory.getLogger(CrudGeneratorMojo.class);

    @Parameter(property = "modelClass", required = true)
    private String modelClass;

    @Parameter(property = "rootPackage", required = true)
    private String rootPackage;

    @Parameter(defaultValue = "${project.basedir}/src/main/java")
    private String outputDir;
    
    @Parameter(property = "onlyDto")
    private boolean onlyDto;
    
    @Parameter(property = "onlyRepository")
    private boolean onlyRepository;
    
    @Parameter(property = "onlyService")
    private boolean onlyService;
    
    @Parameter(property = "onlyController")
    private boolean onlyController;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    public void execute() throws MojoExecutionException {
        logger.info("🚀 Starting CRUD generation for entity: {}", modelClass);

        try {
            logger.info("🔍 Attempting to load the entity class...");
            ClassLoader projectClassLoader = getClassLoader();
            Class<?> entityClass = projectClassLoader.loadClass(rootPackage + "." + modelClass);

            logger.info("✅ Model class successfully loaded: {}", entityClass.getName());

            String packageName = entityClass.getPackage().getName();
            String entityName = entityClass.getSimpleName();

            logger.info("📌 Root package: {}", rootPackage);
            logger.info("📌 Package name: {}", packageName);
            logger.info("📌 Entity name: {}", entityName);

            logger.info("📌 Generating DTOs...");
            generateDTO(rootPackage, entityClass, entityName);
            logger.info("📌 Generating Mapper...");
            generateMapper(rootPackage, entityName);
            logger.info("📌 Generating Repository...");
            generateRepository(rootPackage, entityName);
            logger.info("📌 Generating Services...");
            generateService(rootPackage, entityName);
            logger.info("📌 Generating Controllers...");
            generateController(rootPackage, entityName);

            logger.info("🎉 CRUD generation completed successfully!");

        } catch (ClassNotFoundException e) {
            logger.error("❌ Model class not found: {}", modelClass, e);
            throw new MojoExecutionException("Class not found: " + modelClass, e);
        } catch (Exception e) {
            logger.error("❌ Unexpected error during CRUD generation!", e);
            throw new MojoExecutionException("Unexpected error during CRUD generation", e);
        }
    }

    private ClassLoader getClassLoader() throws MojoExecutionException {
        try {
            List<URL> urls = new ArrayList<>();

            // Load compiled classes from target/classes
            File classesDir = new File(project.getBuild().getOutputDirectory());
            if (classesDir.exists()) {
                urls.add(classesDir.toURI().toURL());
                logger.info("✅ Added project compiled classes directory: {}", classesDir.getAbsolutePath());
            } else {
                logger.warn("⚠️ Compiled classes directory not found: {}", classesDir.getAbsolutePath());
            }

            // Load dependencies
            project.getArtifacts().forEach(artifact -> {
                try {
                    urls.add(artifact.getFile().toURI().toURL());
                    logger.info("✅ Added dependency: {}", artifact.getFile().getAbsolutePath());
                } catch (MalformedURLException e) {
                    logger.warn("⚠️ Skipping malformed URL for dependency: {}", artifact.getFile().getAbsolutePath());
                }
            });

            return new URLClassLoader(urls.toArray(new URL[0]), Thread.currentThread().getContextClassLoader());
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to build classpath for the project", e);
        }
    }

    private void generateDTO(String packageName, Class<?> entityClass, String entityName) throws IOException {
        TypeSpec.Builder dtoBuilder = TypeSpec.classBuilder(getDtoPackage(packageName, entityName))
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

        saveJavaFile(packageName + ".dto", dtoBuilder.build());
    }

    private void generateMapper(String packageName, String entityName) throws IOException {
        TypeSpec mapper = TypeSpec.interfaceBuilder(entityName + "Mapper")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.mapstruct", "Mapper"))
                        .addMember("componentModel", "$S", "spring")
                        .build())
                .addMethod(MethodSpec.methodBuilder("toDTO")
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .returns(ClassName.get(packageName + ".dto", entityName + "Dto"))
                        .addParameter(getModelPackage(packageName, entityName), "entity")
                        .build())
                .addMethod(MethodSpec.methodBuilder("toEntity")
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .returns(getModelPackage(packageName, entityName))
                        .addParameter(ClassName.get(packageName + ".dto", entityName + "Dto"), "dto")
                        .build())
                .build();

        saveJavaFile(packageName + ".mapper", mapper);
    }

    private void generateRepository(String packageName, String entityName) throws IOException {
        TypeSpec repository = TypeSpec.interfaceBuilder(entityName + "Repository")
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ParameterizedTypeName.get(
                        ClassName.get("org.springframework.data.jpa.repository", "JpaRepository"),
                        ClassName.get(packageName + ".model", entityName),
                        ClassName.get(Long.class)
                ))
                .build();

        saveJavaFile(packageName + ".repository", repository);
    }

    private void generateService(String packageName, String entityName) throws IOException {
        ClassName repository = getRepositoryPackage(packageName, entityName);
        ClassName mapper = getMapperPackage(packageName, entityName);
        ClassName dto = getDtoPackage(packageName, entityName);

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
                        .returns(ParameterizedTypeName.get(ClassName.get(List.class), dto))
                        .addStatement("log.debug(\"Executing findAll() method\")")
                        .addStatement("return repository.findAll().stream().map(mapper::toDTO).toList()")
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

        saveJavaFile(packageName + ".service", service);
    }


    private void generateController(String packageName, String entityName) throws IOException {

        // Controller class
        TypeSpec controller = TypeSpec.classBuilder(entityName + "Controller")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get("org.springframework.web.bind.annotation", "RestController"))
                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "RequestMapping"))
                        .addMember("value", "$S", "/api/" + entityName.toLowerCase())
                        .build())
                .addAnnotation(ClassName.get("lombok", "RequiredArgsConstructor")) // Lombok annotation for constructor
                .addField(getServicePackage(packageName, entityName), "service", Modifier.PRIVATE, Modifier.FINAL)

                // Get by ID
                .addMethod(MethodSpec.methodBuilder("getById")
                        .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "GetMapping"))
                                .addMember("value", "$S", "/{id}")
                                .build())
                        .addModifiers(Modifier.PUBLIC)
                        .returns(getDtoPackage(packageName, entityName))
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
                        .returns(ParameterizedTypeName.get(ClassName.get(List.class), getDtoPackage(packageName, entityName)))
                        .addStatement("return service.findAll()")
                        .build())

                // Create (POST)
                .addMethod(MethodSpec.methodBuilder("create")
                        .addAnnotation(ClassName.get("org.springframework.web.bind.annotation", "PostMapping"))
                        .addModifiers(Modifier.PUBLIC)
                        .returns(getDtoPackage(packageName, entityName))
                        .addParameter(ParameterSpec.builder(getDtoPackage(packageName, entityName), "dto")
                                .addAnnotation(ClassName.get("org.springframework.web.bind.annotation", "RequestBody"))
                                .build())
                        .addStatement("return service.save(dto)")
                        .build())


                // Update (PUT)
                .addMethod(MethodSpec.methodBuilder("update")
                        .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "PutMapping"))
                                .addMember("value", "$S", "/{id}")
                                .build()).addModifiers(Modifier.PUBLIC)
                        .returns(getDtoPackage(packageName, entityName))
                        .addParameter(ParameterSpec.builder(
                                        Long.class, "id"
                                )
                                .addAnnotation(AnnotationSpec.builder(ClassName.get("org.springframework.web.bind.annotation", "PathVariable"))
                                        .addMember("value", "$S", "id")
                                        .build()).build())
                        .addParameter(ParameterSpec.builder(getDtoPackage(packageName, entityName), "dto")
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

        saveJavaFile(packageName + ".controller", controller);
    }


    public ClassName getServicePackage(String packageName, String entityName) throws IOException {
        return ClassName.get(packageName + ".service", entityName + "Service");
    }

    public ClassName getModelPackage(String packageName, String entityName) throws IOException {
        return ClassName.get(packageName + ".model", entityName);
    }

    public ClassName getDtoPackage(String packageName, String entityName) throws IOException {
        return ClassName.get(packageName + ".dto", entityName + "Dto");
    }

    public ClassName getRepositoryPackage(String packageName, String entityName) throws IOException {
        return ClassName.get(packageName + ".repository", entityName + "Repository");
    }

    public ClassName getMapperPackage(String packageName, String entityName) throws IOException {
        return ClassName.get(packageName + ".mapper", entityName + "Mapper");
    }

    private void saveJavaFile(String packageName, TypeSpec typeSpec) throws IOException {
        JavaFile javaFile = JavaFile.builder(packageName, typeSpec).build();
        String packagePath = packageName.replace(".", "/");  // Correctly format the package path
        String fullPath = outputDir + "/" + packagePath;

        Files.createDirectories(Paths.get(fullPath));  // Ensure directories exist
        javaFile.writeTo(new File(outputDir));
    }
}
