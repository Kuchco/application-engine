package com.fmworkflow.workflow.domain;

import com.fmworkflow.petrinet.domain.PetriNet;
import com.fmworkflow.petrinet.domain.Place;
import com.fmworkflow.petrinet.domain.Transition;
import com.fmworkflow.petrinet.domain.throwable.TransitionNotStartableException;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.transaction.Transactional;
import java.util.HashMap;
import java.util.Map;

@Document
public class Case {
    @Id
    private ObjectId _id;
    @DBRef
    private PetriNet petriNet;
    @Field("activePlaces")
    private Map<String, Integer> activePlaces;
    private String title;
    private String color;
    private Map<String, Object> dataSetValues;

    public Case() {
        activePlaces = new HashMap<>();
    }

    public Case(String title) {
        this();
        this.title = title;
    }

    public Case(String title, PetriNet petriNet, Map<String, Integer> activePlaces) {
        this(title);
        this.petriNet = petriNet;
        this.activePlaces = activePlaces;
    }

    public ObjectId get_id() {
        return _id;
    }

    public String getStringId() {
        return _id.toString();
    }

    public PetriNet getPetriNet() {
        return petriNet;
    }

    public void setPetriNet(PetriNet petriNet) {
        this.petriNet = petriNet;
    }

    public Map<String, Integer> getActivePlaces() {
        return activePlaces;
    }

    public void setActivePlaces(Map<String, Integer> activePlaces) {
        this.activePlaces = activePlaces;
    }

    public void addActivePlace(String placeId, Integer tokens) {
        this.activePlaces.put(placeId, tokens);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color == null || color.isEmpty() ? "color-fg-fm-500" : color;
    }

    public Map<String, Object> getDataSetValues() {
        return dataSetValues;
    }

    public void setDataSetValues(Map<String, Object> dataSetValues) {
        this.dataSetValues = dataSetValues;
    }

    public void finishTransition(Transition transition) {
        Map<Place, Integer> outputPlaces = petriNet.getOutputPlaces(transition);
        for (Map.Entry<Place, Integer> entry : outputPlaces.entrySet()) {
            addTokensToPlace(entry.getKey(), entry.getValue());
        }
    }

    private void addTokensToPlace(Place place, Integer tokens) {
        Integer newTokens = tokens;
        String id = place.getStringId();
        if (activePlaces.containsKey(id))
            newTokens += activePlaces.get(id);
        activePlaces.put(id, newTokens);
    }

    @Transactional
    public void startTransition(Transition transition) throws TransitionNotStartableException {
        Map<Place, Integer> inputPlaces = petriNet.getInputPlaces(transition);
        for (Map.Entry<Place, Integer> entry : inputPlaces.entrySet()) {
            if (isNotActivePlace(entry.getKey()))
                throw new TransitionNotStartableException();
            removeTokensFromActivePlace(entry.getKey(), entry.getValue());
        }
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
}