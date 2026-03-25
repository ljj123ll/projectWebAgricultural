package com.agricultural.assistplatform.service.common;

import com.agricultural.assistplatform.vo.common.RegionNodeVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegionService {

    private final ObjectMapper objectMapper;
    private volatile List<RegionNodeVO> cachedRegionTree;

    public List<RegionNodeVO> getRegionTree() {
        if (cachedRegionTree != null) return cachedRegionTree;
        synchronized (this) {
            if (cachedRegionTree != null) return cachedRegionTree;
            try {
                ClassPathResource resource = new ClassPathResource("regions/china-region-tree.json");
                try (InputStream in = resource.getInputStream()) {
                    List<RegionNodeVO> tree = objectMapper.readValue(in, new TypeReference<List<RegionNodeVO>>() {});
                    cachedRegionTree = tree == null ? new ArrayList<>() : tree;
                }
            } catch (Exception e) {
                cachedRegionTree = new ArrayList<>();
            }
        }
        return cachedRegionTree;
    }
}

