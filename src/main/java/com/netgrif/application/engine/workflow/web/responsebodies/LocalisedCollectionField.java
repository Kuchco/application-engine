package com.netgrif.application.engine.workflow.web.responsebodies;

import com.netgrif.application.engine.petrinet.domain.dataset.CollectionField;
import com.netgrif.application.engine.petrinet.domain.dataset.FieldType;
import lombok.Data;

import java.util.Locale;

@Data
public class LocalisedCollectionField extends LocalisedField {

    protected FieldType collectionType;

    public LocalisedCollectionField() {
        super();
    }

    public LocalisedCollectionField(CollectionField<?> field, Locale locale) {
        super(field, locale);
        this.collectionType = FieldType.fromString(field.getCollectionDataType());
    }
}
