package com.netgrif.workflow.workflow.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.netgrif.workflow.auth.domain.Author;
import com.netgrif.workflow.petrinet.domain.PetriNet;
import com.netgrif.workflow.petrinet.domain.Place;
import com.netgrif.workflow.petrinet.domain.dataset.Field;
import com.netgrif.workflow.petrinet.domain.dataset.FieldWithDefault;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Document
public class Case {

    @Id
    private ObjectId _id;

    @DBRef
    @NotNull
    @JsonIgnore
    @Setter
    private PetriNet petriNet;

    @NotNull
    @Getter @Setter
    private String processIdentifier;

    @org.springframework.data.mongodb.core.mapping.Field("activePlaces")
    @Getter
    @Setter
    @JsonIgnore
    private Map<String, Integer> activePlaces;

    @NotNull
    @Getter
    @Setter
    private String title;

    @Getter
    private String color;

    @Setter
    private String icon;

    @Getter
    @Setter
    private LocalDateTime creationDate;

    @Getter
    @Setter
    @JsonIgnore
    private LinkedHashMap<String, DataField> dataSet;

    @Getter
    @Setter
    @JsonIgnore
    private LinkedHashSet<String> immediateDataFields;

    @Getter
    @Setter
    @Transient
    private List<Field> immediateData;

    @Getter
    @Setter
    private Author author;

    @Getter
    @Setter
    private Map<String, Integer> resetArcTokens;

    @Getter
    @Setter
    @JsonIgnore
    private Set<TaskPair> tasks;

    public Case() {
        _id = new ObjectId();
        activePlaces = new HashMap<>();
        dataSet = new LinkedHashMap<>();
        immediateDataFields = new LinkedHashSet<>();
        resetArcTokens = new HashMap<>();
        tasks = new HashSet<>();
    }

    public Case(String title) {
        this();
        this.title = title;
    }

    public Case(String title, PetriNet petriNet, Map<String, Integer> activePlaces) {
        this(title);
        this.petriNet = petriNet;
        this.activePlaces = activePlaces;
        populateDataSet();
        this.immediateDataFields = new LinkedHashSet<>(this.petriNet.getImmediateFields().stream().map(Field::getStringId).collect(Collectors.toList()));
    }

    public ObjectId get_id() {
        return _id;
    }

    public String getStringId() {
        return _id.toString();
    }

    public String getIcon() {
        return icon;
    }

    public PetriNet getPetriNet() {
        if (petriNet.isNotInitialized())
            petriNet.initializeArcs();
        return petriNet;
    }

    public void addActivePlace(String placeId, Integer tokens) {
        this.activePlaces.put(placeId, tokens);
    }

    public void setColor(String color) {
        this.color = color == null || color.isEmpty() ? "color-fg-fm-500" : color;
    }

    private void addTokensToPlace(Place place, Integer tokens) {
        Integer newTokens = tokens;
        String id = place.getStringId();
        if (activePlaces.containsKey(id))
            newTokens += activePlaces.get(id);
        activePlaces.put(id, newTokens);
    }

    public boolean hasFieldBehavior(String field, String transition) {
        return this.dataSet.get(field).hasDefinedBehavior(transition);
    }

    public String getVisualId() {
        int n = _id.getTimestamp() + title.length();
        if (this.petriNet != null) return petriNet.getInitials() + "-" + n;
        return n + "";
    }

    public String getPetriNetId() {
        if (this.petriNet != null) return petriNet.getStringId();
        return null;
    }

    public void addImmediateDataField(String fieldId) {
        this.immediateDataFields.add(fieldId);
    }

    private void removeTokensFromActivePlace(Place place, Integer tokens) {
        String id = place.getStringId();
        activePlaces.put(id, activePlaces.get(id) - tokens);
    }

    private boolean isNotActivePlace(Place place) {
        return !isActivePlace(place);
    }

    private boolean isActivePlace(Place place) {
        return activePlaces.containsKey(place.getStringId());
    }

    private void populateDataSet() {
        petriNet.getDataSet().forEach((key, field) -> {
            if (field instanceof FieldWithDefault)
                this.dataSet.put(key, new DataField(((FieldWithDefault) field).getDefaultValue()));
            else
                this.dataSet.put(key, new DataField());
        });
    }

    public Object getFieldValue(String fieldId) {
        return dataSet.get(fieldId).getValue();
    }

    public boolean addTask(Task task) {
        return this.tasks.add(new TaskPair(task.getStringId(), task.getTransitionId()));
    }

    public boolean removeTask(Task task) {
//        return this.tasks.remove(new TaskPair(task.getStringId(), task.getTransitionId()));
        return this.removeTasks(Collections.singletonList(task));
    }

    public boolean removeTasks(List<Task> tasks) {
//        List<TaskPair> taskPairsToRemove = tasks.stream().map(task -> new TaskPair(task.getStringId(), task.getTransitionId()))
//                .collect(Collectors.toList());
//        return this.tasks.removeAll(taskPairsToRemove);
        int sizeBeforeChange = this.tasks.size();
        Set<String> tasksTransitions = tasks.stream().map(Task::getTransitionId).collect(Collectors.toSet());
        this.tasks = this.tasks.stream().filter(pair -> !tasksTransitions.contains(pair.getTransition())).collect(Collectors.toSet());
        return this.tasks.size() != sizeBeforeChange;
    }

    public Field getField(String id) {
        return petriNet.getDataSet().get(id);
    }
}