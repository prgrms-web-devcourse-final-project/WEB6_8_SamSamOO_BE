package com.ai.lawyer.global.qdrant.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QdrantService {

    private final VectorStore vectorStore;

    public List<Document> searchDocument(String query, String key, String value, int topK) {

        SearchRequest caseSearchRequest = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .filterExpression(new Filter.Expression(Filter.ExpressionType.EQ, new Filter.Key(key), new Filter.Value(value)))
                .build();
        List<Document> similarCaseDocuments = vectorStore.similaritySearch(caseSearchRequest);

        if (caseSearchRequest == null) {
            return Collections.singletonList(
                    Document.builder()
                            .text("더미")
                            .metadata(key, value)
                            .score(0.0)
                            .build()
            );
        }

        return similarCaseDocuments;
    }

}