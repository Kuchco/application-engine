package com.netgrif.workflow.workflow.service;

import com.netgrif.workflow.petrinet.domain.I18nString;
import com.netgrif.workflow.petrinet.domain.dataset.ChoiceField;
import com.netgrif.workflow.petrinet.domain.dataset.Field;
import com.netgrif.workflow.petrinet.domain.dataset.MapOptionsField;
import com.netgrif.workflow.petrinet.domain.dataset.logic.action.runner.CaseFieldsExpressionRunner;
import com.netgrif.workflow.petrinet.domain.dataset.logic.action.runner.Expression;
import com.netgrif.workflow.workflow.domain.Case;
import com.netgrif.workflow.workflow.service.interfaces.IInitValueExpressionEvaluator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class InitValueExpressionEvaluator implements IInitValueExpressionEvaluator {

    @Autowired
    private CaseFieldsExpressionRunner runner;

    @Override
    public <T> T evaluate(Case useCase, Field<T> defaultField) {
        return (T) evaluate(useCase, defaultField.getInitExpression());
    }

    @Override
    public Map<String, I18nString> evaluateOptions(Case useCase, MapOptionsField<I18nString, ?> field) {
        Object result = evaluate(useCase, field.getExpression());
        if (!(result instanceof Map)) {
            throw new IllegalArgumentException("[" + useCase.getStringId() + "] Dynamic options not an instance of Map: " + field.getImportId());
        }
        Map<String, Object> map = (Map) result;
        if (map.values().stream().anyMatch(it -> !(it instanceof I18nString))) {
            return map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, it -> new I18nString(it.getValue().toString())));
        } else {
            return (Map<String, I18nString>) result;
        }
    }

    @Override
    public Set<I18nString> evaluateChoices(Case useCase, ChoiceField<?> field) {
        Object result = evaluate(useCase, field.getExpression());
        if (!(result instanceof Collection)) {
            throw new IllegalArgumentException("[" + useCase.getStringId() + "] Dynamic choices not an instance of Collection: " + field.getImportId());
        }
        Collection<Object> collection = (Collection) result;
        return collection.stream().map(it -> (it instanceof I18nString) ? (I18nString) it : new I18nString(it.toString())).collect(Collectors.toSet());
    }

    @Override
    public I18nString evaluateCaseName(Case useCase, Expression expression) {
        Object result = evaluate(useCase, expression);
        if (result instanceof I18nString) {
            return (I18nString) result;
        } else {
            return new I18nString(result.toString());
        }
    }

    @Override
    public Object evaluate(Case useCase, Expression expression) {
        return runner.run(useCase, expression);
    }
}
