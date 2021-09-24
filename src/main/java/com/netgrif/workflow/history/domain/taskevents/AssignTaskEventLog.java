package com.netgrif.workflow.history.domain.taskevents;

import com.netgrif.workflow.petrinet.domain.events.EventPhase;
import com.netgrif.workflow.workflow.domain.Case;
import com.netgrif.workflow.workflow.domain.Task;

public class AssignTaskEventLog extends TaskEventLog{

    private Long userId;

    public AssignTaskEventLog(Task task, Case useCase, EventPhase eventPhase, Long userId) {
        super(task, useCase, eventPhase);
        this.userId = userId;
    }
}
