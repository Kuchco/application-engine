package com.netgrif.application.engine.workflow.web.responsebodies;

import com.netgrif.application.engine.petrinet.domain.I18nString;
import com.netgrif.application.engine.petrinet.domain.dataset.ChoiceField;
import com.netgrif.application.engine.petrinet.domain.dataset.FieldType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class LocalisedChoiceField extends LocalisedCollectionField {

    protected List<Serializable> choices;

    public LocalisedChoiceField(ChoiceField<?> field, Locale locale) {
        super(field, locale);
        this.choices = new LinkedList<>();
        List<Serializable> computedChoices = new ArrayList<>();
        if (FieldType.I18N.equals(collectionType)) {
            computedChoices.addAll(field.getChoices().stream()
                    .map(choice -> ((I18nString) choice).getTranslation(locale))
                    .collect(Collectors.toList()));
        }
        if (!computedChoices.isEmpty()) {
            this.choices.addAll(computedChoices);
        } else {
            //TODO getTraslation value
            this.choices.addAll(field.getChoices());
        }
    }
}
