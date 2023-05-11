package com.netgrif.application.engine.petrinet.domain.dataset

import com.netgrif.application.engine.petrinet.domain.I18nString
import org.springframework.data.mongodb.core.mapping.Document

@Document
class MultichoiceField extends ChoiceField<Set<Serializable>> {

    MultichoiceField() {
        super()
        super.setValue(new HashSet<Serializable>())
        super.setDefaultValue(new HashSet<Serializable>())
    }

    MultichoiceField(String collectionType) {
        super(collectionType)
        super.setValue(new HashSet<Serializable>())
        super.setDefaultValue(new HashSet<Serializable>())
    }

    MultichoiceField(List<Serializable> choices, String collectionType) {
        super(choices, collectionType)
        super.setValue(new HashSet<Serializable>())
        super.setDefaultValue(new HashSet<Serializable>())
    }

    @Override
    FieldType getType() {
        return FieldType.MULTICHOICE
    }

    void setDefaultValues(List<Serializable> inits) {
        if (inits == null || inits.isEmpty()) {
            this.defaultValue = null
        } else {
            Set<Serializable> defaults = new HashSet<>()
            inits.forEach { initValue ->
                defaults << choices.find { choice ->
                    {
                        if (choice.value instanceof I18nString && initValue.value instanceof I18nString) {
                            ((I18nString) choice.value).defaultValue == ((I18nString) initValue.value).defaultValue
                        } else {
                            choice == initValue
                        }
                    }
                }
            }
            super.setDefaultValue(defaults)
        }
    }

    @Override
    Field clone() {
        MultichoiceField clone = new MultichoiceField(this.collectionDataType)
        super.clone(clone)
        clone.choices = this.choices
        clone.choicesExpression = this.choicesExpression
        return clone
    }
}