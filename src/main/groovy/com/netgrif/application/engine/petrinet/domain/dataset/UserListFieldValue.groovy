package com.netgrif.application.engine.petrinet.domain.dataset;

/**
 * This class is no longer used in lists with users.
 * Class is only kept because of unit tests.
 */
//TODO Safe delete class
@Deprecated
class UserListFieldValue {

    private List<UserFieldValue> userValues;

    UserListFieldValue(List<UserFieldValue> userValues) {
        this.userValues = userValues;
    }

    List<UserFieldValue> getUserValues() {
        return userValues;
    }

    void setUserValues(List<UserFieldValue> userValues) {
        this.userValues = userValues;
    }

    @Override
    String toString() {
        return userValues.toString();
    }
}
