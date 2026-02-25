package org.dwtech.common.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.BaseVector;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.RequiredArgsConstructor;
import org.dwtech.common.config.properties.MilvusProperties;
import org.springframework.stereotype.Service;

import java.util.*;
/**
 * MilvusService
 *
 * @author steve12311
 * @since 2025-11-18
 */

@Service
@RequiredArgsConstructor
public class MilvusService {
    private final MilvusClientV2 milvusClient;
    private final MilvusProperties milvusProperties;

    // 插入向量数据
    public void insertVectors(String json) {
        Gson gson = new Gson();
        List<JsonObject> data = Collections.singletonList(gson.fromJson(json, JsonObject.class));
        InsertReq insertReq = InsertReq.builder()
                .collectionName(milvusProperties.getCollectionName())
                .data(data)
                .build();

        milvusClient.insert(insertReq);
    }

    // 查询向量数据
    public Set<String> searchVectors(List<float[]> vectors) {
        List<BaseVector> floatVecs = new ArrayList<>();
        for (float[] vector : vectors) {
            FloatVec floatVec = new FloatVec(vector);
            floatVecs.add(floatVec);
        }

        SearchReq searchReq = SearchReq.builder()
                .collectionName(milvusProperties.getCollectionName())
                .data(floatVecs)
                .annsField("vector")
                .topK(1)
                .build();

        SearchResp searchResp = milvusClient.search(searchReq);
        List<List<SearchResp.SearchResult>> searchResults = searchResp.getSearchResults();
        Set<String> isbns = new HashSet<>();
        for (List<SearchResp.SearchResult> results : searchResults) {
            for (SearchResp.SearchResult result : results) {
                isbns.add(result.getId().toString());
            }
        }
        return isbns;
    }
}
