package com.example.difyfilter.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.web.util.UriUtils;

public final class JsonParamUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonParamUtils() {
    }

    public static Map<String, List<String>> decodeFilters(String filtersJson) {
        if (filtersJson == null || filtersJson.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            String decoded = UriUtils.decode(filtersJson, java.nio.charset.StandardCharsets.UTF_8);
            Map<String, Object> map = MAPPER.readValue(decoded, new TypeReference<Map<String, Object>>() {
            });
            return map.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> toList(e.getValue())));
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private static List<String> toList(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(Collectors.toList());
        }
        return List.of(String.valueOf(value));
    }
}
