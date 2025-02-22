package it.fabiodezuani;

import com.squareup.javapoet.*;
import it.fabiodezuani.generator.*;
import it.fabiodezuani.model.MapperEnum;
import it.fabiodezuani.utils.GeneratorUtil;
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
import java.util.Map;

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

    @Parameter(property = "mapper", defaultValue = "MAPSTRUCT")
    private MapperEnum mapper;
    
    @Parameter(property = "skipDto")
    private boolean skipDto;
    @Parameter(property = "skipRepository")
    private boolean skipRepository  ;
    @Parameter(property = "skipService")
    private boolean skipService;
    @Parameter(property = "skipController")
    private boolean skipController;
    @Parameter(property = "skipMapper")
    private boolean skipMapper;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    public void execute() throws MojoExecutionException {

        GeneratorUtil generatorUtil = new GeneratorUtil(outputDir);
        DtoGenerator dtoGenerator = new DtoGenerator(generatorUtil);
        RepositoryGenerator repositoryGenerator = new RepositoryGenerator(generatorUtil);
        ServiceGenerator serviceGenerator = new ServiceGenerator(generatorUtil);
        ControllerGenerator controllerGenerator = new ControllerGenerator(generatorUtil);
        MapperGenerator mapperGenerator = new MapperGenerator(generatorUtil);

        logger.info("🚀 Starting CRUD generation for entity: {}", modelClass);

        try {
            logger.info("🔍 Attempting to load the entity class...");
            ClassLoader projectClassLoader = getClassLoader();
            Class<?> entityClass = projectClassLoader.loadClass(rootPackage + "." + modelClass);
            List<Class<?>> joinedEntities = new ArrayList<>();

            logger.info("✅ Model class successfully loaded: {}", entityClass.getName());

            // Extract joined entities from fields
            for (Field field : entityClass.getDeclaredFields()) {
                if (!field.getType().getPackageName().startsWith("java")) {
                    // Direct entity reference
                    joinedEntities.add(field.getType());
                } else if (field.getGenericType() instanceof java.lang.reflect.ParameterizedType paramType) {
                    // Handle collections and maps
                    if (List.class.isAssignableFrom((Class<?>) paramType.getRawType())) {
                        Class<?> listType = (Class<?>) paramType.getActualTypeArguments()[0];
                        if (!listType.getPackageName().startsWith("java")) {
                            joinedEntities.add(listType);
                        }
                    } else if (Map.class.isAssignableFrom((Class<?>) paramType.getRawType())) {
                        Class<?> valueType = (Class<?>) paramType.getActualTypeArguments()[1];
                        if (!valueType.getPackageName().startsWith("java")) {
                            joinedEntities.add(valueType);
                        }
                    }
                }
            }

            String packageName = entityClass.getPackage().getName();
            String entityName = entityClass.getSimpleName();

            logger.info("📌 Root package: {}", rootPackage);
            logger.info("📌 Package name: {}", packageName);
            logger.info("📌 Entity name: {}", entityName);
            logger.info("📌 Found {} joined entities", joinedEntities.size());

            logger.info("📌 Generating DTOs...");
            dtoGenerator.generate(rootPackage, entityClass, entityName, skipDto);
            logger.info("📌 Generating Mapper...");
            mapperGenerator.generate(rootPackage, entityName, joinedEntities, skipMapper, mapper);
            logger.info("📌 Generating Repository...");
            repositoryGenerator.generate(rootPackage, entityName, skipRepository);
            logger.info("📌 Generating Services...");
            serviceGenerator.generate(rootPackage, entityName, skipService);
            logger.info("📌 Generating Controllers...");
            controllerGenerator.generate(rootPackage, entityName, skipController);

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

}
