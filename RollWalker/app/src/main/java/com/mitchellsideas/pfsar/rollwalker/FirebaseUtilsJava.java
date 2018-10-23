package com.mitchellsideas.pfsar.rollwalker;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.GenericTypeIndicator;

import java.util.Map;

// SOURCE: https://gist.github.com/epool/131531644952f47072c2f6d8792ddcaa
public class FirebaseUtilsJava {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final JavaType JAVA_TYPE = OBJECT_MAPPER.getTypeFactory().constructMapType(Map.class, String.class, Object.class);

    static {
        OBJECT_MAPPER.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    public static Map<String, Object> serialize(Object o) {
        // Convert to a Map<String,Object> using Jackson and then pass that to Firebase
        return OBJECT_MAPPER.convertValue(o, JAVA_TYPE);
    }

    public static <T> T deserialize(DataSnapshot dataSnapshot, Class<T> tClass) {
        // Use Firebase to convert to a Map<String,Object>
        GenericTypeIndicator<Map<String, Object>> genericTypeIndicator = new GenericTypeIndicator<Map<String, Object>>() {
        };
        Map<String, Object> map = dataSnapshot.getValue(genericTypeIndicator);
        // Use Jackson to convert from a Map to an Office object
        return OBJECT_MAPPER.convertValue(map, tClass);
    }

}
