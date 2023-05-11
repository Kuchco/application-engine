package com.netgrif.application.engine.petrinet.domain.dataset


import org.springframework.data.mongodb.core.mapping.Document

@Document
class ListField extends CollectionField<List<Serializable>>{

    ListField() {
    }

    ListField(String collectionType) {
        super(collectionType)
    }

    @Override
    FieldType getType() {
        return FieldType.LIST;
    }

    @Override
    Field clone() {
        ListField clone = new ListField(this.collectionDataType)
        super.clone(clone)
        return clone
    }
}
