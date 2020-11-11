package com.netgrif.workflow.pdf.generator.service.fieldbuilder;

import com.netgrif.workflow.pdf.generator.config.PdfResource;
import com.netgrif.workflow.pdf.generator.domain.PdfField;
import com.netgrif.workflow.pdf.generator.domain.PdfMultiChoiceField;
import com.netgrif.workflow.petrinet.domain.DataGroup;
import com.netgrif.workflow.workflow.web.responsebodies.LocalisedMultichoiceField;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MultiChoiceFieldBuilder extends SelectionFieldBuilder {

    public MultiChoiceFieldBuilder(PdfResource resource) {
        super(resource);
    }

    public PdfField buildField(DataGroup dataGroup, LocalisedMultichoiceField field, int lastX, int lastY){
        List<String> choices;
        List<String> values = new ArrayList<>();
        this.lastX = lastX;
        this.lastY = lastY;

        choices = field.getChoices();
        if (field.getValue() != null) {
            values.addAll((Collection<? extends String>) field.getValue());
        }

        String translatedTitle = field.getName();
        PdfMultiChoiceField pdfField = new PdfMultiChoiceField(field.getStringId(), dataGroup, field.getType(), translatedTitle, values, choices, resource);
        setFieldParams(dataGroup, field, pdfField);
        setFieldPositions(pdfField, resource.getFontLabelSize());
        return pdfField;
    }
}