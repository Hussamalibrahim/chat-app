package com.chatapp.chat_backend.service.message;

import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

public interface StorageService {
    Mono<String> store(FilePart filePart);
    Mono<byte[]> load(String filename);
}

