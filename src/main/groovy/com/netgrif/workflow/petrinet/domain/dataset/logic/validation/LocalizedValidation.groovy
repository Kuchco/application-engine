package com.netgrif.workflow.petrinet.domain.dataset.logic.validation

class LocalizedValidation {

    private String validationRule

    private String validationMessage

    LocalizedValidation(String validationRule, String validationMessage) {
        this.validationRule = validationRule
        this.validationMessage = validationMessage
    }

    LocalizedValidation() {
    }

    String getValidationMessage() {
        return validationMessage
    }

    void setValidationMessage(String valMessage) {
        this.validationMessage = valMessage
    }

    String getValidationRule() {
        return validationRule
    }

    void setValidationRule(String validationRule) {
        this.validationRule = validationRule
    }
}
