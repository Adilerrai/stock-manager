package com.ceramique.mapper;

import com.ceramique.persistent.dto.UniteMesureOption;
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
        return switch (value) {
            case "M2" -> "Mètre carré (m²)";
            case "PIECE" -> "Pièce";
            case "KG" -> "Kilogramme";
            default -> value;
        };
    }
}