package com.gestion.mapper;

import com.gestion.persistent.dto.UniteMesureOption;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UniteMesureOptionMapper {

    default UniteMesureOption stringToOption(String value) {
        if (value == null) return null;
        return new UniteMesureOption(value, getLabelForValue(value));
    }

    default String optionToString(UniteMesureOption option) {
        return option != null ? option.getValue() : null;
    }

    default String getLabelForValue(String value) {
        if (value == null) return null;
        try {
            return com.gestion.persistent.enums.UniteMesure.valueOf(value.trim().toUpperCase()).getLabel();
        } catch (IllegalArgumentException e) {
            return value;
        }
    }
}
