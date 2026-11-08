package com.chatapp.chat_backend.service.message;

import com.chatapp.chat_backend.exception.BusinessException;
import com.chatapp.chat_backend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
public class LocalStorageService implements StorageService {

    private final Path storageDirectory;

    public LocalStorageService(@Value("${app.storage.local-dir:./uploads/attachments}") String uploadDir) {
        this.storageDirectory = Paths.get(uploadDir);
        try {
            Files.createDirectories(this.storageDirectory);
        } catch (IOException e) {
            log.error("Could not initialize storage directory", e);
        }
    }

    @Override
    public Mono<String> store(FilePart filePart) {
        String originalFilename = filePart.filename();
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex);
        }

        String storedFilename = UUID.randomUUID() + extension;
        Path destination = this.storageDirectory.resolve(storedFilename);

        return filePart.transferTo(destination)
                .thenReturn("/api/v1/attachments/files/" + storedFilename)
                .onErrorMap(e -> new BusinessException(ErrorCode.FILE_STORAGE_ERROR));
    }

    @Override
    public Mono<byte[]> load(String filename) {
        return Mono.fromCallable(() -> {
            Path file = this.storageDirectory.resolve(filename);
            if (!Files.exists(file)) {
                throw new BusinessException(ErrorCode.ENDPOINT_NOT_FOUND);
            }
            return Files.readAllBytes(file);
        });
    }
}

