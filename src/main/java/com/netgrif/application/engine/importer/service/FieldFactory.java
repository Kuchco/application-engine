package com.netgrif.application.engine.importer.service;

import com.netgrif.application.engine.auth.domain.IUser;
import com.netgrif.application.engine.auth.service.interfaces.IUserService;
import com.netgrif.application.engine.importer.model.*;
import com.netgrif.application.engine.importer.model.CollectionType;
import com.netgrif.application.engine.importer.service.throwable.MissingIconKeyException;
import com.netgrif.application.engine.petrinet.domain.Component;
import com.netgrif.application.engine.petrinet.domain.Format;
import com.netgrif.application.engine.petrinet.domain.I18nString;
import com.netgrif.application.engine.petrinet.domain.dataset.*;
import com.netgrif.application.engine.petrinet.domain.dataset.logic.action.runner.Expression;
import com.netgrif.application.engine.petrinet.domain.dataset.logic.validation.DynamicValidation;
import com.netgrif.application.engine.petrinet.domain.views.View;
import com.netgrif.application.engine.workflow.domain.Case;
import com.netgrif.application.engine.workflow.domain.DataField;
import com.netgrif.application.engine.workflow.service.interfaces.IDataValidationExpressionEvaluator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@org.springframework.stereotype.Component
@Slf4j
public final class FieldFactory {

    @Autowired
    private FormatFactory formatFactory;

    @Autowired
    private ViewFactory viewFactory;

    @Autowired
    private ComponentFactory componentFactory;

    @Autowired
    private IDataValidator dataValidator;

    @Autowired
    private IUserService userService;

    @Autowired
    private IDataValidationExpressionEvaluator dataValidationExpressionEvaluator;

    // TODO: refactor this shit
    Field getField(Data data, Importer importer) throws IllegalArgumentException, MissingIconKeyException {
        Field field;
        if (data.getType() == null) {
            throw new IllegalArgumentException(String.format("Data type was not specified for field: %s", data.getId()));
        }
        switch (data.getType()) {
            case COLLECTION:
                field = buildCollectionField(data, importer);
                break;
            case TEXT:
                field = buildTextField(data);
                break;
            case BOOLEAN:
                field = buildBooleanField(data);
                break;
            case DATE:
                field = buildDateField(data);
                break;
            case FILE:
                field = buildFileField(data);
                break;
            case NUMBER:
                field = buildNumberField(data);
                break;
            case USER:
                field = buildUserField(data, importer);
                break;
            case CASE_REF:
                field = buildCaseField(data);
                break;
            case DATE_TIME:
                field = buildDateTimeField(data);
                break;
            case BUTTON:
                field = buildButtonField(data);
                break;
            case TASK_REF:
                field = buildTaskField(data, importer.getDocument().getTransition());
                break;
            case ENUMERATION_MAP:
                field = buildEnumerationMapField(data, importer);
                break;
            case MULTICHOICE_MAP:
                field = buildMultichoiceMapField(data, importer);
                break;
            case FILTER:
                field = buildFilterField(data);
                break;
            case I_18_N:
                field = buildI18nField(data, importer);
                break;
            default:
                throw new IllegalArgumentException(data.getType() + " is not a valid Field type");
        }

        field.setName(importer.toI18NString(data.getTitle()));
        field.setImportId(data.getId());
        field.setImmediate(data.isImmediate());
        if (data.getLength() != null) {
            field.setLength(data.getLength());
        }
        if (data.getDesc() != null)
            field.setDescription(importer.toI18NString(data.getDesc()));

        if (data.getPlaceholder() != null)
            field.setPlaceholder(importer.toI18NString(data.getPlaceholder()));

        if (data.getValid() != null) {
            List<Valid> list = data.getValid();
            for (Valid item : list) {
                field.addValidation(makeValidation(item.getValue(), null, item.isDynamic()));
            }
        }
        if (data.getValidations() != null) {
            List<com.netgrif.application.engine.importer.model.Validation> list = data.getValidations().getValidation();
            for (com.netgrif.application.engine.importer.model.Validation item : list) {
                field.addValidation(makeValidation(item.getExpression().getValue(), importer.toI18NString(item.getMessage()), item.getExpression().isDynamic()));
            }
        }

        if (data.getFormat() != null) {
            Format format = formatFactory.buildFormat(data.getFormat());
            field.setFormat(format);
        }
        if (data.getView() != null) {
            log.warn("Data attribute [view] in field [" + field.getImportId()  + "] is deprecated.");
            View view = viewFactory.buildView(data);
            field.setComponent(new Component(view.getValue()));
        }

        if (data.getComponent() != null) {
            Component component = componentFactory.buildComponent(data.getComponent(), importer, data);
            field.setComponent(component);
        }

        setActions(field, data);
        setEncryption(field, data);

        dataValidator.checkDeprecatedAttributes(data);
        return field;
    }

    private com.netgrif.application.engine.petrinet.domain.dataset.logic.validation.Validation makeValidation(String rule, I18nString message, boolean dynamic) {
        return dynamic ? new DynamicValidation(rule, message) : new com.netgrif.application.engine.petrinet.domain.dataset.logic.validation.Validation(rule, message);
    }

    private TaskField buildTaskField(Data data, List<Transition> transitions) {
        TaskField field = new TaskField();
        setDefaultValues(field, data, defaultValues -> {
            if (defaultValues != null && !defaultValues.isEmpty()) {
                List<String> defaults = new ArrayList<>();
                defaultValues.forEach(s -> {
                    if (transitions.stream().noneMatch(t -> t.getId().equals(s)))
                        log.warn("There is no transition with id [" + s + "]");
                    defaults.add(s);
                });
                field.setDefaultValue(defaults);
            }
        });
        return field;
    }

    private MultichoiceField buildMultichoiceField(Data data, Importer importer) {
        MultichoiceField field;
        String collectionDataType = resolveSelectingCollectionDataType(data);
        field = new MultichoiceField(collectionDataType);
        if (data.getOptions() != null) {
            setFieldOptions(field, data, importer);
        }
        setDefaultValues(field, data, inits -> {
            if (inits != null && !inits.isEmpty()) {
                field.setDefaultValues(inits.stream()
                        .map(init -> resolveCollectionValue(init, collectionDataType))
                        .collect(Collectors.toList()));
            }
        });
        return field;
    }

    private CollectionField<?> buildCollectionField(Data data, Importer importer) {
        if (data.getCollectionType() == null) {
            throw new IllegalArgumentException("Collection type for " + data.getType() + " was not specified.");
        }
        CollectionType collectionType = data.getCollectionType();
        switch (collectionType) {
            case ENUMERATION:
                return buildEnumerationField(data, importer);
            case MULTICHOICE:
                return buildMultichoiceField(data, importer);
            case LIST:
                return buildListField(data);
            default:
                throw new IllegalArgumentException("Collection type " + data.getCollectionType() + " is not a valid value.");
        }
    }

    private CollectionField<? extends Collection<? extends Serializable>> buildListField(Data data) {
        if (data.getCollectionDataType().equals(DataType.FILE)) {
            FileListField fileListField = buildFileListField(data);
            fileListField.setDefaultValue(new HashSet<>());
            fileListField.setCollectionDataType(data.getCollectionDataType().value());
            return fileListField;
        }
        ListField listField = new ListField(data.getCollectionDataType().value());
        setDefaultValues(listField, data, inits -> {
            listField.setDefaultValue(inits.stream()
                    .map(init -> resolveCollectionValue(init, listField.getCollectionDataType()))
                    .collect(Collectors.toList()));
        });
        return listField;
    }

    private EnumerationField buildEnumerationField(Data data, Importer importer) {
        String collectionDataType;
        collectionDataType = resolveSelectingCollectionDataType(data);
        EnumerationField field = new EnumerationField(collectionDataType);
        if (data.getOptions() != null) {
            setFieldOptions(field, data, importer);
        }
        setDefaultValue(field, data, init -> {
            if (init != null && !init.equals("")) {
                field.setDefaultValue(resolveCollectionValue(init, collectionDataType));
            }
        });
        return field;
    }

    private static String resolveSelectingCollectionDataType(Data data) {
        String collectionDataType;
        if (data.getCollectionDataType() != null) {
            collectionDataType = data.getCollectionDataType().value();
            log.warn(String.format("Data type: %s is not supported for %s in the current version.",
                    data.getCollectionDataType().value(), data.getCollectionType()));
        } else {
            collectionDataType = FieldType.I18N.name();
        }
        return collectionDataType;
    }

//    private void setFieldChoices(ChoiceField<?> field, Data data, Importer importer) {
//        if (data.getValues() != null && !data.getValues().isEmpty() && data.getValues().get(0).isDynamic()) {
//            field.setExpression(new Expression(data.getValues().get(0).getValue()));
//
//        } else if (data.getValues() != null) {
//            List<CollectionValue> choices = data.getValues().stream()
//                    .map(option -> resolveLol1(option, importer, field.getCollectionType()))
//                    .collect(Collectors.toList());
//            field.getChoices().addAll(choices);
//        }
//    }

    private MultichoiceMapField buildMultichoiceMapField(Data data, Importer importer) {
        MultichoiceMapField field = new MultichoiceMapField();
        setFieldOptions(field, data, importer);
        setDefaultValues(field, data, init -> {
            if (init != null && !init.isEmpty()) {
                field.setDefaultValue(new HashSet<>(init));
            }
        });
        return field;
    }

    private EnumerationMapField buildEnumerationMapField(Data data, Importer importer) {
        EnumerationMapField field = new EnumerationMapField();
        setFieldOptions(field, data, importer);
        setDefaultValue(field, data, init -> {
            if (init != null && !init.isEmpty()) {
                field.setDefaultValue(init);
            }
        });
        return field;
    }

    private void setFieldOptions(ChoiceField<?> field, Data data, Importer importer) {
        if (data.getOptions() != null && data.getOptions().getInit() != null) {
            field.setExpression(new Expression(data.getOptions().getInit().getValue()));
            return;
        }

        List<Serializable> options = (data.getOptions() == null) ? new ArrayList<>() : data.getOptions().getOption().stream()
                .map(option -> resolveFieldOption(option, importer, field.getCollectionDataType()))
                .collect(Collectors.toList());
        field.getChoices().addAll(options);
    }

    public Serializable resolveCollectionValue(String value, String fieldType) {
        FieldType collectionDataType = FieldType.fromString(fieldType);
        switch (collectionDataType) {
            case DATE:
                return parseDate(value);
            case NUMBER:
                return Double.parseDouble(value);
            case TEXT:
                return value;
            case FILE:
                return FileFieldValue.fromString(value);
            case DATETIME:
                return parseDateTime(value);
            case I18N:
                return new I18nString(value.trim());
            case USER:
                IUser user = userService.resolveById(value, true);
                return new UserFieldValue(user);
            default:
                return null;
        }
    }

    private Serializable resolveFieldOption(I18NStringType option, Importer importer, String collectionDataType) {
        FieldType collectionDataTypeParsed = FieldType.fromString(collectionDataType);
        if (FieldType.I18N.equals(collectionDataTypeParsed)) {
            return importer.toI18NString(option);
        }
        return resolveCollectionValue(option.getValue(), collectionDataType);
    }

    private void setFieldOptions(MapOptionsField<I18nString, ?> field, Data data, Importer importer) {
        if (data.getOptions() != null && data.getOptions().getInit() != null) {
            field.setExpression(new Expression(data.getOptions().getInit().getValue()));
            return;
        }

        Map<String, I18nString> choices = (data.getOptions() == null) ? new LinkedHashMap<>() : data.getOptions().getOption().stream()
                .collect(Collectors.toMap(Option::getKey, importer::toI18NString, (o1, o2) -> o1, LinkedHashMap::new));
        field.setOptions(choices);
    }

    private TextField buildTextField(Data data) {
        String value = null;
        List<I18NStringTypeWithExpression> values = data.getValues();
        if (values != null && !values.isEmpty())
            value = values.get(0).getValue();

        TextField field = new TextField(value);
        setDefaultValue(field, data, field::setDefaultValue);
        return field;
    }

    private BooleanField buildBooleanField(Data data) {
        BooleanField field = new BooleanField();
        setDefaultValue(field, data, defaultValue -> {
            if (defaultValue != null) {
                field.setDefaultValue(Boolean.valueOf(defaultValue));
            }
        });
        return field;
    }

    private DateField buildDateField(Data data) {
        DateField field = new DateField();
        setDefaultValue(field, data, defaultValue -> {
            if (defaultValue != null) {
                field.setDefaultValue(parseDate(defaultValue));
            }
        });
        return field;
    }

    private NumberField buildNumberField(Data data) {
        NumberField field = new NumberField();
        setDefaultValue(field, data, defaultValue -> {
            if (defaultValue != null) {
                field.setDefaultValue(Double.parseDouble(defaultValue));
            }
        });
        return field;
    }

    private ButtonField buildButtonField(Data data) {
        ButtonField field = new ButtonField();
        setDefaultValue(field, data, defaultValue -> {
            if (defaultValue != null) {
                field.setDefaultValue(Integer.parseInt(defaultValue));
            }
        });
        return field;
    }

    private DateTimeField buildDateTimeField(Data data) {
        DateTimeField field = new DateTimeField();
        setDefaultValue(field, data, defaultValue -> field.setDefaultValue(parseDateTime(defaultValue)));
        return field;
    }

    private CaseField buildCaseField(Data data) {
        AllowedNets nets = data.getAllowedNets();
        CaseField field;
        if (nets == null) {
            field = new CaseField();
        } else {
            field = new CaseField(new ArrayList<>(nets.getAllowedNet()));
        }
        setDefaultValues(field, data, inits -> {
        });
        return field;
    }

    private UserField buildUserField(Data data, Importer importer) {
        String[] roles = data.getValues().stream()
                .map(value -> importer.getRoles().get(value.getValue()).getStringId())
                .toArray(String[]::new);
        UserField field = new UserField(roles);
        setDefaultValues(field, data, inits -> {
            field.setDefaultValue(null);
        });
        return field;
    }

    private FileField buildFileField(Data data) {
        FileField fileField = new FileField();
        fileField.setRemote(data.getRemote() != null);
        setDefaultValue(fileField, data, defaultValue -> {
            if (defaultValue != null) {
                fileField.setDefaultValue(defaultValue);
            }
        });
        return fileField;
    }

    private FileListField buildFileListField(Data data) {
        FileListField fileListField = new FileListField();
        fileListField.setRemote(data.getRemote() != null);
        setDefaultValues(fileListField, data, defaultValues -> {
            if (defaultValues != null && !defaultValues.isEmpty()) {
                fileListField.setDefaultValue(defaultValues);
            }
        });
        return fileListField;
    }

    private FilterField buildFilterField(Data data) {
        AllowedNets nets = data.getAllowedNets();
        if (nets == null) {
            return new FilterField();
        } else {
            return new FilterField(new ArrayList<>(nets.getAllowedNet()));
        }
    }

    private I18nField buildI18nField(Data data, Importer importer) {
        I18nField i18nField = new I18nField();
        String initExpression = getInitExpression(data);
        if (initExpression != null) {
            i18nField.setInitExpression(new Expression(initExpression));
        } else {
            if (data.getInits() != null && data.getInits().getInit() != null && !data.getInits().getInit().isEmpty()) {
                i18nField.setDefaultValue(new I18nString(data.getInits().getInit().get(0).getValue()));
            } else if (data.getInit() != null && (data.getInit().getName() == null || data.getInit().getName().equals(""))) {
                i18nField.setDefaultValue(new I18nString(data.getInit().getValue()));
            } else if (data.getInit() != null && data.getInit().getName() != null && !data.getInit().getName().equals("")) {
                i18nField.setDefaultValue(importer.toI18NString(data.getInit()));
            } else {
                i18nField.setDefaultValue(new I18nString(""));
            }
        }
        return i18nField;
    }

    private void setActions(Field field, Data data) {
        if (data.getAction() != null && data.getAction().size() != 0) {
//            data.getAction().forEach(action -> field.addAction(action.getValue(), action.getTrigger()));
        }
    }

    private void setEncryption(Field field, Data data) {
        if (data.getEncryption() != null && data.getEncryption().isValue()) {
            String encryption = data.getEncryption().getAlgorithm();
            if (encryption == null)
                encryption = "PBEWITHSHA256AND256BITAES-CBC-BC";
            field.setEncryption(encryption);
        }
    }

    public Field buildFieldWithoutValidation(Case useCase, String fieldId, String transitionId) {
        return buildField(useCase, fieldId, false, transitionId);
    }

    public Field buildFieldWithValidation(Case useCase, String fieldId, String transitionId) {
        return buildField(useCase, fieldId, true, transitionId);
    }

    private Field buildField(Case useCase, String fieldId, boolean withValidation, String transitionId) {
        Field field = useCase.getPetriNet().getDataSet().get(fieldId);

        resolveDataValues(field, useCase, fieldId);
        resolveComponent(field, useCase, transitionId);
        if (field instanceof ChoiceField)
            resolveChoices((ChoiceField) field, useCase);
        if (field instanceof MapOptionsField)
            resolveMapOptions((MapOptionsField) field, useCase);
        if (field instanceof FieldWithAllowedNets)
            resolveAllowedNets((FieldWithAllowedNets) field, useCase);
        if (field instanceof FilterField)
            resolveFilterMetadata((FilterField) field, useCase);
        if (withValidation)
            resolveValidations(field, useCase);
        return field;
    }

    @SuppressWarnings({"all", "rawtypes", "unchecked"})
    private void resolveValidations(Field field, Case useCase) {
        List<com.netgrif.application.engine.petrinet.domain.dataset.logic.validation.Validation> validations = useCase.getDataField(field.getImportId()).getValidations();
        if (validations != null) {
            field.setValidations(validations.stream().map(it -> it.clone()).collect(Collectors.toList()));
        }
        if (field.getValidations() == null) return;

        ((List<com.netgrif.application.engine.petrinet.domain.dataset.logic.validation.Validation>) field.getValidations()).stream()
                .filter(it -> it instanceof DynamicValidation).map(it -> (DynamicValidation) it).forEach(valid -> {
                    valid.setCompiledRule(dataValidationExpressionEvaluator.compile(useCase, valid.getExpression()));
                });
    }

    private void resolveChoices(ChoiceField field, Case useCase) {
        Set<Serializable> choices = useCase.getDataField(field.getImportId()).getChoices();
        if (choices == null)
            return;
        field.setChoices(choices);
    }

    private void resolveComponent(Field field, Case useCase, String transitionId) {
        if (transitionId == null) {
            return;
        }
        com.netgrif.application.engine.petrinet.domain.Transition transition = useCase.getPetriNet().getTransition(transitionId);
        Component transitionComponent = transition.getDataSet().get(field.getImportId()).getComponent();
        if (transitionComponent != null) {
            field.setComponent(transitionComponent);
        }
    }

    private void resolveMapOptions(MapOptionsField field, Case useCase) {
        Map options = useCase.getDataField(field.getImportId()).getOptions();
        if (options == null)
            return;
        field.setOptions(options);
    }

    private void resolveAllowedNets(FieldWithAllowedNets field, Case useCase) {
        List<String> allowedNets = useCase.getDataField(field.getImportId()).getAllowedNets();
        if (allowedNets == null)
            return;
        field.setAllowedNets(allowedNets);
    }

    private void resolveFilterMetadata(FilterField field, Case useCase) {
        Map<String, Object> metadata = useCase.getDataField(field.getImportId()).getFilterMetadata();
        if (metadata == null)
            return;
        field.setFilterMetadata(metadata);
    }

    public Field buildImmediateField(Case useCase, String fieldId) {
        Field field = useCase.getPetriNet().getDataSet().get(fieldId);
        resolveDataValues(field, useCase, fieldId);
        resolveAttributeValues(field, useCase, fieldId);
        return field;
    }

    @SuppressWarnings("RedundantCast")
    private void resolveDataValues(Field field, Case useCase, String fieldId) {
        switch (field.getType()) {
            case DATE:
                parseDateValue((DateField) field, fieldId, useCase);
                parseDateDefaultValue((DateField) field);
                break;
            case NUMBER:
                field.setValue(parseNumberValue(useCase, fieldId));
                break;
            case ENUMERATION:
                field.setValue(parseEnumValue(useCase, fieldId, (EnumerationField) field));
                ((EnumerationField) field).setChoices(getFieldChoices((ChoiceField<?>) field, useCase));
                break;
            case ENUMERATION_MAP:
                field.setValue(parseEnumerationMapValue(useCase, fieldId));
                ((EnumerationMapField) field).setOptions(getFieldOptions((MapOptionsField<?, ?>) field, useCase));
                break;
            case MULTICHOICE_MAP:
                field.setValue(parseMultichoiceMapValue(useCase, fieldId));
                ((MultichoiceMapField) field).setOptions(getFieldOptions((MapOptionsField<?, ?>) field, useCase));
                break;
            case MULTICHOICE:
                field.setValue(parseMultichoiceValue(useCase, fieldId, (MultichoiceField) field));
                ((MultichoiceField) field).setChoices(getFieldChoices((ChoiceField<?>) field, useCase));
                break;
            case DATETIME:
                parseDateTimeValue((DateTimeField) field, fieldId, useCase);
                break;
            case FILE:
                parseFileValue((FileField) field, useCase, fieldId);
                break;
            case USER:
                parseUserValues((UserField) field, useCase, fieldId);
                break;
            case LIST:
                parseListValues((CollectionField<?>) field, useCase, fieldId);
                break;
            default:
                field.setValue(useCase.getFieldValue(fieldId));
        }
    }

    private void parseUserValues(UserField field, Case useCase, String fieldId) {
        DataField userField = useCase.getDataField(fieldId);
        if (userField.getUserChoices() != null) {
            Set<String> roles = userField.getUserChoices().stream().map(I18nString::getDefaultValue).collect(Collectors.toSet());
            field.setRoles(roles);
        }
        field.setValue((UserFieldValue) useCase.getFieldValue(fieldId));
    }

    private void parseListValues(CollectionField<?> field, Case useCase, String fieldId) {
        Object values = useCase.getFieldValue(fieldId);
        if (values == null) {
            return;
        }
        if (values instanceof Collection) {
            if (field instanceof FileListField) {
                Set<FileFieldValue> newValues = parseFileListValue((Collection<?>) values);
                ((FileListField) field).setValue(newValues);
            } else {
                List<Serializable> parsedValues = ((Collection<?>) values).stream()
                        .map(val -> {
                            if (val == null) {
                                return null;
                            } else if (val instanceof Serializable) {
                                return (Serializable) val;
                            }
                            return resolveCollectionValue(val.toString(), field.getCollectionDataType());
                        })
                        .collect(Collectors.toList());
                ((ListField) field).setValue(parsedValues);
            }
        } else {
            throw new IllegalArgumentException("List value: " + values + " is not a collection.");
        }
    }

    public Set<Serializable> parseMultichoiceValue(Case useCase, String fieldId, MultichoiceField field) {
        Object values = useCase.getFieldValue(fieldId);
        if (values == null) {
            return null;
        }
        if (values instanceof Collection) {
            return ((Collection<?>) values).stream()
                    .map(val -> val instanceof Serializable
                            ? (Serializable) val
                            : resolveCollectionValue(val.toString(), field.getCollectionDataType()))
                    .collect(Collectors.toSet());
        }
        throw new IllegalArgumentException("Multichoice value: " + values + " is not a collection.");
    }

    public static Set<String> parseMultichoiceMapValue(Case useCase, String fieldId) {
        Object values = useCase.getFieldValue(fieldId);
        if (values instanceof ArrayList) {
            return (Set<String>) ((ArrayList) values).stream().map(val -> val.toString()).collect(Collectors.toSet());
        } else {
            return (Set<String>) values;
        }
    }

    private Double parseNumberValue(Case useCase, String fieldId) {
        Object value = useCase.getFieldValue(fieldId);
        return parseDouble(value);
    }

    public static Double parseDouble(Object value) {
        if (value instanceof String) {
            return Double.parseDouble((String) value);
        } else if (value instanceof Integer) {
            return ((Integer) value) * 1D;
        } else if (value instanceof Double) {
            return (Double) value;
        }
        return null;
    }

    private void parseDateValue(DateField field, String fieldId, Case useCase) {
        Object value = useCase.getFieldValue(fieldId);
        field.setValue(parseDate(value));
    }

    private void parseDateDefaultValue(DateField field) {
        Object value = field.getDefaultValue();
        field.setDefaultValue(parseDate(value));
    }

    public static LocalDate parseDate(Object value) {
        if (value instanceof Date) {
            return ((Date) value).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        } else if (value instanceof String) {
            return parseDateFromString((String) value);
        } else if (value instanceof LocalDate) {
            return (LocalDate) value;
        }
        return null;
    }

    /**
     * Available formats - YYYYMMDD; YYYY-MM-DD; DD.MM.YYYY
     *
     * @param value - Date as string
     * @return Parsed date as LocalDate object or null if date cannot be parsed
     */
    public static LocalDate parseDateFromString(String value) {
        if (value == null)
            return null;

        List<String> patterns = Arrays.asList("dd.MM.yyyy");
        try {
            return LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE);
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(value, DateTimeFormatter.ISO_DATE);
            } catch (DateTimeParseException ex) {
                for (String pattern : patterns) {
                    try {
                        return LocalDate.parse(value, DateTimeFormatter.ofPattern(pattern));
                    } catch (DateTimeParseException | IllegalArgumentException exc) {
                        continue;
                    }
                }
            }
        }
        return null;
    }

    private void parseDateTimeValue(DateTimeField field, String fieldId, Case useCase) {
        Object value = useCase.getFieldValue(fieldId);
        field.setValue(parseDateTime(value));
    }

    public static LocalDateTime parseDateTime(Object value) {
        if (value == null)
            return null;

        if (value instanceof LocalDate)
            return LocalDateTime.of((LocalDate) value, LocalTime.NOON);
        else if (value instanceof String)
            return parseDateTimeFromString((String) value);
        else if (value instanceof Date)
            return LocalDateTime.ofInstant(((Date) value).toInstant(), ZoneId.systemDefault());
        return (LocalDateTime) value;
    }

    public static LocalDateTime parseDateTimeFromString(String value) {
        if (value == null)
            return null;

        List<String> patterns = Arrays.asList("dd.MM.yyyy HH:mm", "dd.MM.yyyy HH:mm:ss");
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException ex) {
                try {
                    return LocalDateTime.parse(value, DateTimeFormatter.ISO_INSTANT);
                } catch (DateTimeParseException exc) {
                    for (String pattern : patterns) {
                        try {
                            return LocalDateTime.parse(value, DateTimeFormatter.ofPattern(pattern));
                        } catch (DateTimeParseException | IllegalArgumentException excp) {
                            continue;
                        }
                    }
                }
            }
        }
        return null;
    }

    public Serializable parseEnumValue(Case useCase, String fieldId, EnumerationField field) {
        Object value = useCase.getFieldValue(fieldId);
        if (value == null) {
            return null;
        }
        if (value instanceof Serializable) {
            return (Serializable) value;
        } else {
            return resolveCollectionValue(value.toString(), field.getCollectionDataType());
        }
    }

//    private boolean compareCollectionValue(Serializable first, Serializable second, String collectionDataType) {
//        if (first == null || second == null) {
//            return false;
//        }
//        if (FieldType.fromString(collectionDataType).equals(FieldType.DATETIME)) {
//            if (first instanceof Date && second instanceof Date) {
//                return ((Date) first).getTime() / 1000 == ((Date) second).getTime() / 1000;
//            }
//        }
//        return first.equals(second);
//    }

    public static String parseEnumerationMapValue(Case useCase, String fieldId) {
        Object value = useCase.getFieldValue(fieldId);
        return value != null ? value.toString() : null;
    }

    private void parseFileValue(FileField field, Case useCase, String fieldId) {
        Object value = useCase.getFieldValue(fieldId);
        if (value == null)
            return;

        if (value instanceof String) {
            field.setValue((String) value);
        } else if (value instanceof FileFieldValue) {
            field.setValue((FileFieldValue) value);
        } else
            throw new IllegalArgumentException("Object " + value.toString() + " cannot be set as value to the File field [" + fieldId + "] !");
    }

    private Set<FileFieldValue> parseFileListValue(Collection<?> values) {
        return values.stream()
                .map(val -> {
                    if (val instanceof String) {
                        return FileFieldValue.fromString((String) val);
                    } else if (val instanceof FileFieldValue) {
                        return (FileFieldValue) val;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private void resolveAttributeValues(Field field, Case useCase, String fieldId) {
        DataField dataField = useCase.getDataSet().get(fieldId);
        if (field.getType().equals(FieldType.CASE_REF) || field.getType().equals(FieldType.FILTER)) {
            List<String> allowedNets = new ArrayList<>(dataField.getAllowedNets());
            ((FieldWithAllowedNets) field).setAllowedNets(allowedNets);
        }
        if (field.getType().equals(FieldType.FILTER)) {
            Map<String, Object> filterMetadata = new HashMap<>(dataField.getFilterMetadata());
            ((FilterField) field).setFilterMetadata(filterMetadata);
        }
    }

    private <T> void setDefaultValue(Field<T> field, Data data, Consumer<String> setDefault) {
        String initExpression = getInitExpression(data);
        if (initExpression != null) {
            field.setInitExpression(new Expression(initExpression));
        } else {
            setDefault.accept(resolveInit(data));
        }
    }

    private <T> void setDefaultValues(Field<T> field, Data data, Consumer<List<String>> setDefault) {
        String initExpression = getInitExpression(data);
        if (initExpression != null) {
            field.setInitExpression(new Expression(initExpression));
        } else {
            setDefault.accept(resolveInits(data));
        }
    }

    private String getInitExpression(Data data) {
        if (data.getInit() != null) {
            if (data.getInit().isDynamic()) {
                return data.getInit().getValue();
            }
        }
        return null;
    }

    private String resolveInit(Data data) {
        if (data.getInits() != null && data.getInits().getInit() != null) {
            return data.getInits().getInit().get(0).getValue();
        }
        if (data.getInit() != null) return data.getInit().getValue();
        return null;
    }

    private List<String> resolveInits(Data data) {
        if (data.getInits() != null && data.getInits().getInit() != null) {
            return data.getInits().getInit().stream().map(Init::getValue).collect(Collectors.toList());
        }
        if (data.getInit() != null) return Arrays.asList(data.getInit().getValue().split(","));
        return Collections.emptyList();
    }

    private Set<Serializable> getFieldChoices(ChoiceField<?> field, Case useCase) {
        if (useCase.getDataField(field.getImportId()).getChoices() == null) {
            return field.getChoices();
        } else {
            return useCase.getDataField(field.getImportId()).getChoices();
        }
    }

    private Map<String, I18nString> getFieldOptions(MapOptionsField<?, ?> field, Case useCase) {
        if (useCase.getDataField(field.getImportId()).getOptions() == null) {
            return (Map<String, I18nString>) field.getOptions();
        } else {
            return useCase.getDataField(field.getImportId()).getOptions();
        }
    }

}