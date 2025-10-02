package com.ai.lawyer.global.qdrant.initializer;

import io.qdrant.client.QdrantClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Slf4j
@Component
@RequiredArgsConstructor
public class QdrantInitializer {

    private final QdrantClient qdrantClient;

    @Value("${spring.ai.vectorstore.qdrant.collection-name}")
    private String collectionName;

    @Value("${spring.ai.vectorstore.qdrant.vector-size}")
    private Long vectorSize;

    @PostConstruct
    private void existQdrantCollection() throws InterruptedException, ExecutionException {
        var collections = qdrantClient.listCollectionsAsync().get();
        boolean collectionExists = collections.stream()
                .anyMatch(collection -> collection.equals(collectionName));

        if (!collectionExists) {
            log.info("'{}' 컬렉션이 존재하지 않아 새로 생성 중", collectionName);
            qdrantClient.createCollectionAsync(
                    collectionName,
                    io.qdrant.client.grpc.Collections.VectorParams.newBuilder()
                            .setSize(vectorSize.intValue())
                            .setDistance(io.qdrant.client.grpc.Collections.Distance.Cosine)
                            .build()
            ).get();
            log.info("'{}' 컬렉션 생성 완료", collectionName);
        } else {
            log.info("'{}' 컬렉션이 이미 존재합니다.", collectionName);
        }
    }

}