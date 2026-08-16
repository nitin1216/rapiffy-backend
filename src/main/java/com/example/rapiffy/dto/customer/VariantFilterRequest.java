package com.example.rapiffy.dto.customer;

import java.util.Map;

public class VariantFilterRequest {
    private Map<String, String> attributes;

    public Map<String, String> getAttributes() { return attributes; }
    public void setAttributes(Map<String, String> attributes) { this.attributes = attributes; }
}
