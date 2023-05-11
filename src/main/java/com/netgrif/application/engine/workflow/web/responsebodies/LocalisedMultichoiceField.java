package com.netgrif.application.engine.workflow.web.responsebodies;

import com.netgrif.application.engine.petrinet.domain.I18nString;
import com.netgrif.application.engine.petrinet.domain.dataset.FieldType;
import com.netgrif.application.engine.petrinet.domain.dataset.MultichoiceField;
import lombok.Data;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

@Data
public class LocalisedMultichoiceField extends LocalisedChoiceField {

    public LocalisedMultichoiceField(MultichoiceField field, Locale locale) {
        super(field, locale);
        this.setValue(new LinkedList<Serializable>());
        Collection<Serializable> values = field.getValue();
        if (values != null) {
            for (Serializable value : values) {
                if (FieldType.I18N.equals(this.collectionType)) {
                    ((List) this.getValue()).add(((I18nString) value).getTranslation(locale));
                } else {
                    ((List) this.getValue()).add(value);
                }
            }
        }
    }
}
