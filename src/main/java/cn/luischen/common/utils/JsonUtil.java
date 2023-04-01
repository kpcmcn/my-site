package cn.luischen.common.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.SneakyThrows;

/**
 * @author andy
 * @date 2021/8/4 15:02
 */
public class JsonUtil {
    private static final ObjectMapper JSON = new ObjectMapper();

    static {
        JSON.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        JSON.configure(SerializationFeature.INDENT_OUTPUT, Boolean.TRUE);
    }

    public static ObjectMapper getJSON() {
        return JSON;
    }

    public static String of(Object o) {
        if (o instanceof String){
            return (String) o;
        }
        try {
            return JSON.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
