package com.netgrif.application.engine.workflow.web.responsebodies;

import com.netgrif.application.engine.petrinet.domain.dataset.EnumerationField;
import com.netgrif.application.engine.petrinet.domain.dataset.FieldType;
import lombok.Data;
import java.util.Locale;

@Data
public class LocalisedEnumerationField extends LocalisedChoiceField {

    public LocalisedEnumerationField(EnumerationField field, Locale locale) {
        super(field, locale);
        if (FieldType.I18N.equals(collectionType)) {
            this.setValue(field.getTranslatedValue(locale));
        } else {
            this.setValue(field.getValue());
        }
    }
}
