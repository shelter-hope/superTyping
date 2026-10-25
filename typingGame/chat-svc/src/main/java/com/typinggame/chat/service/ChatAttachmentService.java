package com.typinggame.chat.service;

import com.typinggame.chat.repository.ChatAttachmentRepository;
import com.typinggame.chat.repository.FriendChatMessageRepository;
import com.typinggame.common.exception.BusinessException;
import com.typinggame.dto.message.ChatAttachmentDTO;
import com.typinggame.entity.message.ChatAttachment;
import com.typinggame.chat.im.MessagePolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

@Service
public class ChatAttachmentService {
    static final long IMAGE_LIMIT = 5L * 1024 * 1024;
    static final long FILE_LIMIT = 10L * 1024 * 1024;

    private final ChatAttachmentRepository repository;
    private final FriendChatMessageRepository messages;
    private final Path directory;

    public ChatAttachmentService(ChatAttachmentRepository repository,
                                 FriendChatMessageRepository messages,
                                 @Value("${chat.files.dir:data/chat-files}") String directory) {
        this.repository = repository;
        this.messages = messages;
        this.directory = Path.of(directory);
    }

    public ChatAttachmentDTO store(Integer ownerId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择文件");
        }
        String originalName = safeName(file.getOriginalFilename());
        String extension = extensionOf(originalName);
        StoredKind kind = classify(extension, file.getContentType());
        long size = file.getSize();
        long limit = kind.kind() == MessagePolicy.TYPE_IMAGE ? IMAGE_LIMIT : FILE_LIMIT;
        if (size <= 0 || size > limit) {
            throw new IllegalArgumentException(kind.kind() == MessagePolicy.TYPE_IMAGE ? "图片不能超过 5MB" : "文件不能超过 10MB");
        }
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
        ChatAttachment attachment = new ChatAttachment();
        attachment.setOwnerId(ownerId);
        attachment.setOriginalName(originalName);
        attachment.setStoredName(storedName);
        attachment.setMediaType(kind.mediaType());
        attachment.setSizeBytes(size);
        attachment.setKind(kind.kind());
        repository.save(attachment);
        return toDto(attachment);
    }

    public ChatAttachment requireForSend(Integer ownerId, String content, int kind) {
        int id = parseId(content);
        ChatAttachment attachment = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("附件不存在"));
        if (!ownerId.equals(attachment.getOwnerId())) {
            throw new IllegalArgumentException("不能发送别人的附件");
        }
        if (!Integer.valueOf(kind).equals(attachment.getKind())) {
            throw new IllegalArgumentException("附件类型不匹配");
        }
        return attachment;
    }

    public Download loadFor(Integer userId, Integer attachmentId) {
        ChatAttachment attachment = repository.findById(attachmentId)
                .orElseThrow(() -> BusinessException.notFound("附件不存在"));
        if (!canRead(userId, attachment)) {
            throw BusinessException.forbidden("无权下载该附件");
        }
        Path root = directory.toAbsolutePath().normalize();
        Path path = root.resolve(attachment.getStoredName()).normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path)) {
            throw BusinessException.notFound("附件文件不存在");
        }
        return new Download(attachment, path);
    }

    private boolean canRead(Integer userId, ChatAttachment attachment) {
        if (userId.equals(attachment.getOwnerId())) {
            return true;
        }
        String content = String.valueOf(attachment.getId());
        return messages.existsByMessageContentAndMessageTypeAndSenderId(content, attachment.getKind(), userId)
                || messages.existsByMessageContentAndMessageTypeAndReceiverId(content, attachment.getKind(), userId);
    }

    private static int parseId(String content) {
        try {
            return Integer.parseInt(content.trim());
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("附件无效");
        }
    }

    private static ChatAttachmentDTO toDto(ChatAttachment attachment) {
        ChatAttachmentDTO dto = new ChatAttachmentDTO();
        dto.setId(attachment.getId());
        dto.setOriginalName(attachment.getOriginalName());
        dto.setMediaType(attachment.getMediaType());
        dto.setSizeBytes(attachment.getSizeBytes());
        dto.setKind(attachment.getKind());
        return dto;
    }

    static String safeName(String original) {
        String name = original == null ? "" : Path.of(original).getFileName().toString();
        name = name.replaceAll("[\\r\\n\\\\\"']", "_").trim();
        if (name.length() > 200) {
            name = name.substring(name.length() - 200);
        }
        return name.isBlank() ? "file" : name;
    }

    private static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static StoredKind classify(String extension, String contentType) {
        String media = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        return switch (extension) {
            case "jpg", "jpeg" -> image("image/jpeg", media);
            case "png" -> image("image/png", media);
            case "gif" -> image("image/gif", media);
            case "webp" -> image("image/webp", media);
            case "pdf" -> file("application/pdf", media);
            case "txt" -> file("text/plain", media);
            case "zip" -> file("application/zip", media);
            default -> throw new IllegalArgumentException("只支持 jpg、png、gif、webp、pdf、txt、zip");
        };
    }

    private static StoredKind image(String mediaType, String provided) {
        if (!provided.isBlank() && !provided.startsWith("image/") && !"application/octet-stream".equals(provided)) {
            throw new IllegalArgumentException("图片类型不匹配");
        }
        return new StoredKind(MessagePolicy.TYPE_IMAGE, mediaType);
    }

    private static StoredKind file(String mediaType, String provided) {
        if (!provided.isBlank()
                && !"application/octet-stream".equals(provided)
                && !provided.equals(mediaType)
                && !(mediaType.equals("application/zip") && provided.equals("application/x-zip-compressed"))
                && !(mediaType.equals("text/plain") && provided.startsWith("text/"))) {
            throw new IllegalArgumentException("文件类型不匹配");
        }
        return new StoredKind(MessagePolicy.TYPE_FILE, mediaType);
    }

    private record StoredKind(int kind, String mediaType) {
    }

    public record Download(ChatAttachment attachment, Path path) {
    }
}
