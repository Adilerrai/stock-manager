package com.ceramique.persistent.dto;

import java.util.List;
import java.util.Map;

public class ImageMetadata {
    private List<String> supportedFormats;
    private Long maxFileSize;
    private Integer maxWidth;
    private Integer maxHeight;
    private Integer maxImagesPerProduct;
    private Map<String, Object> compressionSettings;
    private List<String> requiredFields;
    
    // Constructors, getters, setters
}