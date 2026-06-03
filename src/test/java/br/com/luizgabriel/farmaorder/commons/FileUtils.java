package br.com.luizgabriel.farmaorder.commons;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.nio.file.Files;

@Component
public class FileUtils {

    @Autowired
    private ResourceLoader resourceLoader;

    @SneakyThrows
    public String readResourceFile(String fileName) {
        var file = resourceLoader.getResource("classpath:%s".formatted(fileName)).getFile();
        return new String(Files.readAllBytes(file.toPath()));
    }
}
