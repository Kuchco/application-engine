package com.netgrif.workflow.petrinet.domain.dataset

import org.springframework.data.mongodb.core.mapping.Document

@Document
public class TextField extends Field<String> {

    public static final String SIMPLE_SUBTYPE = "simple";
    public static final String AREA_SUBTYPE = "area";

    private String subType;

    public TextField() {
        super();
    }

    public TextField(String[] values) {
        this();
        this.subType = values != null ? values[0] : SIMPLE_SUBTYPE;
    }

    String getSubType() {
        return subType
    }
}