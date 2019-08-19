package com.netgrif.workflow.elastic.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NumberField extends DataField<Double> {

    public NumberField(Double value) {
        super(value);
    }
}