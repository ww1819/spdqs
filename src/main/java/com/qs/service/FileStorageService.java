package com.qs.service;

import com.qs.config.UploadProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final DateTimeFormatter DATE_DIR = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final Path rootPath;

    public FileStorageService(UploadProperties properties) throws IOException {
        this.rootPath = Paths.get(properties.getDir()).toAbsolutePath().normalize();
        Files.createDirectories(this.rootPath);
    }

    public String store(String ticketId, MultipartFile file) throws IOException {
        return storeUnder("tickets", ticketId, file);
    }

    public String storeDelivery(String deliveryId, MultipartFile file) throws IOException {
        return storeUnder("archives", deliveryId, file);
    }

    private String storeUnder(String category, String ownerId, MultipartFile file) throws IOException {
        String ext = extractExtension(file.getOriginalFilename());
        String storedName = UUID.randomUUID().toString().replace("-", "") + ext;
        String dateDir = LocalDate.now().format(DATE_DIR);
        String relativePath = category + "/" + dateDir + "/" + ownerId + "/" + storedName;
        Path target = rootPath.resolve(relativePath);
        Files.createDirectories(target.getParent());
        file.transferTo(target);
        return relativePath;
    }

    public Path load(String relativePath) {
        Path file = rootPath.resolve(relativePath).normalize();
        if (!file.startsWith(rootPath)) {
            throw new IllegalArgumentException("非法文件路径");
        }
        return file;
    }

    public void delete(String relativePath) throws IOException {
        Path file = load(relativePath);
        Files.deleteIfExists(file);
    }

    public void deleteTicketDir(String ticketId) throws IOException {
        deleteOwnerDir("tickets", ticketId);
    }

    public void deleteDeliveryDir(String deliveryId) throws IOException {
        deleteOwnerDir("archives", deliveryId);
    }

    private void deleteOwnerDir(String category, String ownerId) throws IOException {
        Path categoryDir = rootPath.resolve(category);
        if (!Files.exists(categoryDir)) {
            return;
        }
        // 新路径：{category}/{日期}/{ownerId}/
        try (var stream = Files.list(categoryDir)) {
            for (Path dateDir : stream.filter(Files::isDirectory).toList()) {
                Path ownerDir = dateDir.resolve(ownerId);
                if (Files.exists(ownerDir)) {
                    deleteDirRecursive(ownerDir);
                }
            }
        }
        // 兼容旧路径：{category}/{ownerId}/...
        Path legacyDir = categoryDir.resolve(ownerId);
        if (Files.exists(legacyDir)) {
            deleteDirRecursive(legacyDir);
        }
    }

    private void deleteDirRecursive(Path dir) throws IOException {
        try (var stream = Files.walk(dir)) {
            stream.sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.')).toLowerCase();
    }
}
