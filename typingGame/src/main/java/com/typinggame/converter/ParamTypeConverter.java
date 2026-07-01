package com.typinggame.converter;

import com.typinggame.enums.ParamType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ParamTypeConverter implements AttributeConverter<ParamType, String> {

    @Override
    public String convertToDatabaseColumn(ParamType attribute) {
        if (attribute == null) {
            return null;
        }
        // Handle special cases
        if (attribute == ParamType._boolean) {
            return "boolean";
        }
        if (attribute == ParamType._enum) {
            return "enum";
        }
        return attribute.name();
    }

    @Override
    public ParamType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        switch (dbData) {
            case "boolean":
                return ParamType._boolean;
            case "enum":
                return ParamType._enum;
            default:
                try {
                    return ParamType.valueOf(dbData);
                } catch (IllegalArgumentException e) {
                    return null; // or throw exception
                }
        }
    }
}
