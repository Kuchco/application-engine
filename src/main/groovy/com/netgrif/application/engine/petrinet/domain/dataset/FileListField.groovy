package com.netgrif.application.engine.petrinet.domain.dataset

import com.netgrif.application.engine.configuration.ApplicationContextProvider
import com.netgrif.application.engine.workflow.domain.FileStorageConfiguration

class FileListField extends CollectionField<HashSet<FileFieldValue>> {
    private Boolean remote

    FileListField() {
        super()
    }

    FileListField(String collectionDataType) {
        super(collectionDataType)
    }

    void setValue(Collection<FileFieldValue> value) {
        if (value instanceof String)
            this.setValue((String) value)
        else
            super.setValue(new HashSet(value))
    }

    void setValue(String value) {
        this.setValue(fromString(value))
    }

    @Override
    FieldType getType() {
        return FieldType.LIST
    }

    @Override
    void setDefaultValue(HashSet<FileFieldValue> defaultValue) {
        if (value instanceof String)
            this.setDefaultValue((String) value)
        else
            super.setDefaultValue(defaultValue)
    }

    void setDefaultValue(String defaultValue) {
        this.setDefaultValue(fromString(defaultValue))
    }

    void setDefaultValue(List<String> defaultValues) {
        this.setDefaultValue(fromList(defaultValues))
    }

    void addValue(String fileName, String path) {
        if (this.getValue() == null) {
            this.setValue(new HashSet<FileFieldValue>())
        }
        this.getValue().add(new FileFieldValue(fileName, path))
    }

    /**
     * Get complete file path to the file
     * Path is generated as follow:
     * - if file is remote, path is field value / remote URI
     * - if file is local
     *    - saved file path consists of Case id, slash field import id, slash original file name
     * @param caseId
     * @param name
     * @return path to the saved file
     */
    String getFilePath(String caseId, String name) {
        if (this.remote) {
            FileFieldValue first = this.getValue().find({ namePath -> ((FileFieldValue) namePath.value).name == name }).value as FileFieldValue
            return first != null ? first.path : null
        }
        return getPath(caseId, getStringId(), name)
    }

    static HashSet<FileFieldValue> fromString(String value) {
        if (value == null) value = ""
        return buildValueFromParts(Arrays.asList(value.split(",")))
    }

    static HashSet<FileFieldValue> fromList(List<String> value) {
        return buildValueFromParts(value)
    }

    private static HashSet<FileFieldValue> buildValueFromParts(List<String> parts) {
        HashSet<FileFieldValue> newVal = new HashSet<FileFieldValue>()
        for (String part : parts) {
            if (!part.contains(":"))
                newVal.add(new FileFieldValue(part, null))
            else {
                String[] filePart = part.split(":", 2)
                newVal.add(new FileFieldValue(filePart[0], filePart[1]))
            }
        }
        return newVal
    }

    static String getPath(String caseId, String fieldId, String name) {
        FileStorageConfiguration fileStorageConfiguration = ApplicationContextProvider.getBean("fileStorageConfiguration")
        return "${fileStorageConfiguration.getStoragePath()}/${caseId}/${fieldId}/${name}"
    }

    boolean isRemote() {
        return this.remote
    }

    void setRemote(boolean remote) {
        this.remote = remote
    }

    @Override
    Field clone() {
        FileListField clone = new FileListField()
        super.clone(clone)
        clone.remote = this.remote
        clone.collectionDataType = this.collectionDataType
        return clone
    }
}
