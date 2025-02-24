package it.fabiodezuani.utils;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class GeneratorUtil {

    private String outputDir;

    public GeneratorUtil(String outputDir) {
        this.outputDir = outputDir;
    }

    public ClassName getServicePackage(String packageName, String entityName) throws IOException {
        return ClassName.get(packageName + ".service", entityName + "Service");
    }

    public ClassName getModelPackage(String packageName, String entityName) throws IOException {
        return ClassName.get(packageName + ".model", entityName);
    }

    public ClassName getDtoClassName(String packageName, String simpleClassName) throws IOException {
        return ClassName.get(packageName + ".dto", simpleClassName);
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

    public void saveJavaFile(String packageName, TypeSpec typeSpec) throws IOException {
        JavaFile javaFile = JavaFile.builder(packageName, typeSpec).build();
        String packagePath = packageName.replace(".", "/");  // Correctly format the package path
        String fullPath = outputDir + "/" + packagePath;

        Files.createDirectories(Paths.get(fullPath));  // Ensure directories exist
        javaFile.writeTo(new File(outputDir));
    }

}
