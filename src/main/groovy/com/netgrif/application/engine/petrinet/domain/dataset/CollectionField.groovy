package com.netgrif.application.engine.petrinet.domain.dataset

abstract class CollectionField<T> extends Field<T> {

    protected String collectionDataType

    CollectionField() {
    }

    CollectionField(String collectionDataType) {
        super()
        this.collectionDataType = collectionDataType
    }

    @Override
    FieldType getType() {
        return FieldType.COLLECTION
    }

    String getCollectionDataType() {
        return collectionDataType
    }

    void setCollectionDataType(String collectionDataType) {
        this.collectionDataType = collectionDataType
    }
}
