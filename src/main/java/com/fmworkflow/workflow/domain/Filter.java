package com.fmworkflow.workflow.domain;

import com.fmworkflow.auth.domain.LoggedUser;
import com.fmworkflow.petrinet.web.responsebodies.PetriNetReference;
import com.fmworkflow.petrinet.web.responsebodies.TransitionReference;
import com.fmworkflow.workflow.web.requestbodies.CreateFilterBody;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document
public class Filter {

    @Id
    private ObjectId _id;
    private String name;
    private String organization;
    private Long user;
    private List<PetriNetReference> petriNets;
    private List<TransitionReference> transitions;

    public Filter() {
        petriNets = new ArrayList<>();
        transitions = new ArrayList<>();
    }

    public Filter(String name) {
        this.name = name;
    }

    public Filter(String name, String organization, Long user) {
        this.name = name;
        this.organization = organization;
        this.user = user;
    }

    public ObjectId get_id() {
        return _id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public Long getUser() {
        return user;
    }

    public void setUser(Long user) {
        this.user = user;
    }

    public List<PetriNetReference> getPetriNets() {
        return petriNets;
    }

    public void setPetriNets(List<PetriNetReference> petriNets) {
        this.petriNets = petriNets;
    }

    public List<TransitionReference> getTransitions() {
        return transitions;
    }

    public void setTransitions(List<TransitionReference> transitions) {
        this.transitions = transitions;
    }

    public void resolveVisibility(int visibility, LoggedUser user){
        switch (visibility){
            case CreateFilterBody.GLOBAL:
                organization = null;
                this.user = null;
                break;
            case CreateFilterBody.PRIVATE:
                organization = null;
                this.user = user.getId();
                break;
            case CreateFilterBody.ORGANIZATION:
                organization = user.getFullName(); //TODO: 24.2.2016 change to organization id
                this.user = null;
                break;
        }
    }
}