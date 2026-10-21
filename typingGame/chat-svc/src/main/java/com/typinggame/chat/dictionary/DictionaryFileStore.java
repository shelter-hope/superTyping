package com.typinggame.chat.dictionary;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Component
public class DictionaryFileStore {
    private final Path root;

    public DictionaryFileStore(@Value("${typing.storage.dir:data/dictionary-files}") String directory) {
        this.root = Path.of(directory).toAbsolutePath().normalize();
    }

    public Stored save(String originalName, byte[] bytes) {
        String extension = DictionaryTextParser.extension(originalName);
        if (bytes == null || bytes.length == 0 || bytes.length > DictionaryTextParser.MAX_BYTES) {
            throw new IllegalArgumentException("词库文件不能超过 1MB");
        }
        String storedName = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(root);
            Files.write(root.resolve(storedName), bytes);
        } catch (IOException exception) {
            throw new IllegalStateException("词库文件保存失败", exception);
        }
        return new Stored(storedName, "/api/v1/dictionaries/files/" + storedName);
    }

    public Path resolve(String name) {
        if (name == null || name.isBlank() || name.contains("/") || name.contains("\\") || name.contains("..")) {
            throw new IllegalArgumentException("文件名不合法");
        }
        Path path = root.resolve(name).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("文件名不合法");
        }
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("文件不存在");
        }
        return path;
    }

    public record Stored(String fileName, String fileUrl) {
    }
}
