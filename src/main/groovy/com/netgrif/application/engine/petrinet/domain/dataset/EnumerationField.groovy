package com.netgrif.application.engine.petrinet.domain.dataset


import com.netgrif.application.engine.petrinet.domain.I18nString
import org.springframework.data.mongodb.core.mapping.Document

@Document
class EnumerationField extends ChoiceField<Serializable> {

    EnumerationField() {
        super()
    }

    EnumerationField(String collectionType) {
        super(collectionType)
    }

    EnumerationField(List<Serializable> values, String collectionType) {
        super(values, collectionType)
    }

    @Override
    FieldType getType() {
        return FieldType.ENUMERATION
    }

    String getTranslatedValue(Locale locale) {
        if (value instanceof I18nString) {
            return value?.getTranslation(locale)
        }
        return defaultValue
    }

    @Override
    Field clone() {
        EnumerationField clone = new EnumerationField(this.collectionDataType)
        super.clone(clone)
        clone.choices = this.choices
        clone.choicesExpression = this.choicesExpression
        return clone
    }
}