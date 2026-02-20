package com.rollout.io.server.controlplaneservice.logic;

import com.rollout.io.server.controlplaneservice.entity.Flag;
import com.rollout.io.server.controlplaneservice.entity.FlagType;
import com.rollout.io.server.controlplaneservice.exceptions.RolloutError;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FlagHelperLogic {

    private final ObjectMapper objectMapper;

    public void validateFlagValue(Flag flag) {
        FlagType type = flag.getType();
        Object value = flag.getValue();

        if (type == null) {
            throw new RolloutError("Flag type cannot be null", HttpStatus.BAD_REQUEST);
        }

        if (value == null) {
            // For core flags, maybe null is executed as "false" or "empty"? 
            // But usually core flags should have a default value.
            throw new RolloutError("Flag value cannot be null for Core flags", HttpStatus.BAD_REQUEST);
        }

        try {
            switch (type) {
                case BOOLEAN:
                    if (!(value instanceof Boolean)) {
                        throw new IllegalArgumentException();
                    }
                    break;
                case INTEGER:
                    if (value instanceof Number) {
                        long longVal = ((Number) value).longValue();
                        if (longVal < Integer.MIN_VALUE || longVal > Integer.MAX_VALUE) {
                            throw new IllegalArgumentException("Integer value out of bounds");
                        }
                        flag.setValue((int) longVal);
                    } else if (value instanceof String) {
                        flag.setValue(Integer.parseInt((String) value));
                    } else {
                        throw new IllegalArgumentException();
                    }
                    break;
                case DOUBLE:
                    if (value instanceof Number) {
                        flag.setValue(((Number) value).doubleValue());
                    } else if (value instanceof String) {
                        flag.setValue(Double.parseDouble((String) value));
                    } else {
                        throw new IllegalArgumentException();
                    }
                    break;
                case STRING:
                    if (!(value instanceof String)) {
                        throw new IllegalArgumentException("Value must be a strictly formatted string");
                    }
                    break;
                case JSON:
                    if (value instanceof java.util.Map || value instanceof java.util.List) {
                        // Already valid map/list, map is fine
                        break;
                    } else if (value instanceof String) {
                        // Attempt to parse string as JSON and convert to standard Java objects
                        try {
                            JsonNode node = objectMapper.readTree((String) value);
                            if (node == null || node.isNull()) {
                                throw new IllegalArgumentException("Top level JSON cannot be null");
                            }
                            flag.setValue(objectMapper.convertValue(node, Object.class));
                        } catch (Exception e) {
                            throw new IllegalArgumentException("String value is not a valid JSON structure");
                        }
                    } else {
                        throw new IllegalArgumentException("JSON flag value must be a valid JSON object or array");
                    }
                    break;
            }
        } catch (Exception e) {
            throw new RolloutError("Invalid value for flag type " + type, HttpStatus.BAD_REQUEST);
        }
        
    }
}
