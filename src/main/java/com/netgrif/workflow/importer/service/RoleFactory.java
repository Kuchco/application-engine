package com.netgrif.workflow.importer.service;


import com.netgrif.workflow.importer.model.CaseLogic;
import com.netgrif.workflow.importer.model.Logic;
import com.netgrif.workflow.petrinet.domain.PetriNet;
import com.netgrif.workflow.petrinet.domain.Transition;
import com.netgrif.workflow.petrinet.domain.roles.ProcessRole;
import com.netgrif.workflow.petrinet.domain.roles.ProcessRolePermission;
import com.netgrif.workflow.petrinet.domain.roles.ProcessRoleRepository;
import com.netgrif.workflow.petrinet.domain.roles.RolePermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class RoleFactory {

    @Autowired
    private ProcessRoleRepository repository;

    Map<String, Boolean> getPermissions(Logic roleLogic) {
        Map<String, Boolean> permissions = new HashMap<>();

        addPerform(permissions, roleLogic);
        addDelegate(permissions, roleLogic);
        addCancel(permissions, roleLogic);
        addAssign(permissions, roleLogic);
        addView(permissions, roleLogic);

        return permissions;
    }

    Map<String, Boolean> getProcessPermissions(CaseLogic roleLogic) {
        Map<String, Boolean> permissions = new HashMap<>();

        addCreate(permissions, roleLogic);
        addDelete(permissions, roleLogic);
        addCaseView(permissions, roleLogic);

        return permissions;
    }

    private void addPerform(Map<String, Boolean> permissions, Logic roleLogic) {
        if (roleLogic.isPerform() != null)
            permissions.put(RolePermission.PERFORM.toString(), roleLogic.isPerform());
    }

    private void addDelegate(Map<String, Boolean> permissions, Logic roleLogic) {
        if (roleLogic.isDelegate() != null)
            permissions.put(RolePermission.DELEGATE.toString(), roleLogic.isDelegate());
    }

    private void addCancel(Map<String, Boolean> permissions, Logic roleLogic) {
        if (roleLogic.isCancel() != null)
            permissions.put(RolePermission.CANCEL.toString(), roleLogic.isCancel());
    }

    private void addAssign(Map<String, Boolean> permissions, Logic roleLogic) {
        /* TODO: roleLogic.isAssigned() is deprecated and can be removed in future releases. */
        if (roleLogic.isAssigned() != null || roleLogic.isAssign() != null)
            permissions.put(RolePermission.ASSIGN.toString(), roleLogic.isAssigned());
    }

    private void addCreate(Map<String, Boolean> permissions, CaseLogic roleLogic) {
        if (roleLogic.isCreate() != null)
            permissions.put(ProcessRolePermission.CREATE.toString(), roleLogic.isCreate());
    }

    private void addDelete(Map<String, Boolean> permissions, CaseLogic roleLogic) {
        if (roleLogic.isDelete() != null)
            permissions.put(ProcessRolePermission.DELETE.toString(), roleLogic.isDelete());
    }

    private void addView(Map<String, Boolean> permissions, Logic roleLogic) {
        if (roleLogic.isView() != null)
            permissions.put(RolePermission.VIEW.toString(), roleLogic.isView());
    }

    private void addCaseView(Map<String, Boolean> permissions, CaseLogic roleLogic) {
        if (roleLogic.isView() != null)
            permissions.put(ProcessRolePermission.VIEW.toString(), roleLogic.isView());
    }

    ProcessRole transitionRole(PetriNet net, Transition transition) {
        ProcessRole role = new ProcessRole();
        role.setName(transition.getImportId());
        role.setImportId(net.getStringId() + "_" + transition.getImportId());
//        role.setDescription("Default role of transition "+transition.getTitle().getDefaultValue() + " in process "+net.getTitle().getDefaultValue());
        role = repository.save(role);
        return role;
    }
}