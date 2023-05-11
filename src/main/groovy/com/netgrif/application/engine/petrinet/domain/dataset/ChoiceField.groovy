package com.netgrif.application.engine.petrinet.domain.dataset

import com.netgrif.application.engine.petrinet.domain.I18nString
import com.netgrif.application.engine.petrinet.domain.dataset.logic.action.runner.Expression
import java.util.stream.Collectors

abstract class ChoiceField<T> extends CollectionField<T> {

    protected Set<Serializable> choices
    protected Expression choicesExpression

    ChoiceField() {
        super()
        choices = new LinkedHashSet<Serializable>()
    }

    ChoiceField(String collectionType) {
        super(collectionType)
        choices = new LinkedHashSet<Serializable>()
    }

    ChoiceField(List<Serializable> choices, String collectionType) {
        this(collectionType)
        if (choices != null)
            this.choices.addAll(choices)
    }

    ChoiceField(Expression expression, String collectionType) {
        this(collectionType)
        this.choicesExpression = expression
    }

    Set<Serializable> getChoices() {
        return choices
    }

    Set<String> getStringChoices() {
        return choices.stream().map(choice -> choice.toString()).collect(Collectors.toSet())
    }

    void setChoices(Set<Serializable> choices) {
        this.choices = choices
    }

    Expression getExpression() {
        return choicesExpression
    }

    void setExpression(Expression expression) {
        this.choicesExpression = expression
    }

    void setChoicesFromStrings(Collection<String> choices) {
        this.choices = new LinkedHashSet<>()
        choices.each {
            this.choices.add(new I18nString(it))
        }
    }

    boolean isDynamic() {
        return this.choicesExpression != null
    }
}