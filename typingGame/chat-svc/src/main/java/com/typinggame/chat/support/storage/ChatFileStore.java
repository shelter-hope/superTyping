package com.typinggame.chat.support.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** 聊天附件磁盘存储。业务元数据仍由 ChatAttachmentService 管理。 */
@Component
public class ChatFileStore {
    private final Path directory;

    public ChatFileStore(@Value("${chat.files.dir:data/chat-files}") String directory) {
        this.directory = Path.of(directory);
    }

    public String save(MultipartFile file, String extension) {
        String storedName = UUID.randomUUID() + "." + extension;
        Path root = directory.toAbsolutePath().normalize();
        Path target = root.resolve(storedName).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("附件路径无效");
        }
        try {
            Files.createDirectories(root);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("附件保存失败", exception);
        }
        return storedName;
    }

    public Path resolveExisting(String storedName) {
        Path root = directory.toAbsolutePath().normalize();
        Path path = root.resolve(storedName).normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path)) {
            return null;
        }
        return path;
    }
}
