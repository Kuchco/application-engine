package com.netgrif.application.engine.petrinet.domain.dataset

/**
 * This class is no longer used in lists with files.
 * Class is only kept because of unit tests.
 */
//TODO Safe delete class
@Deprecated()
class FileListFieldValue {

    private HashSet<FileFieldValue> namesPaths

    FileListFieldValue() {
        this.namesPaths = new HashSet<>()
    }

    HashSet<FileFieldValue> getNamesPaths() {
        return this.namesPaths
    }

    @Override
    String toString() {
        return namesPaths.toString()
    }
}
