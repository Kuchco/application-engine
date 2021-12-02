package com.netgrif.workflow.history.service;

import com.netgrif.workflow.history.domain.baseevent.EventLog;
import com.netgrif.workflow.history.domain.caseevents.CreateCaseEventLog;
import com.netgrif.workflow.history.domain.caseevents.DeleteCaseEventLog;
import com.netgrif.workflow.history.domain.dataevents.GetDataEventLog;
import com.netgrif.workflow.history.domain.dataevents.SetDataEventLog;
import com.netgrif.workflow.history.domain.petrinetevents.DeletePetriNetEventLog;
import com.netgrif.workflow.history.domain.petrinetevents.ImportPetriNetEventLog;
import com.netgrif.workflow.history.domain.taskevents.AssignTaskEventLog;
import com.netgrif.workflow.history.domain.taskevents.CancelTaskEventLog;
import com.netgrif.workflow.history.domain.taskevents.FinishTaskEventLog;
import org.bson.types.ObjectId;

import java.util.List;

public interface IHistoryService {

    void save(EventLog eventLog);

    List<EventLog> findAllByIds(List<ObjectId> eventsIds);

    List<CreateCaseEventLog> findAllCreateCaseEventLogsByCaseId(ObjectId caseId);

    List<DeleteCaseEventLog> findAllDeleteCaseEventLogsByCaseId(ObjectId caseId);

    List<GetDataEventLog> findAllGetDataEventLogsByCaseId(ObjectId caseId);

    List<GetDataEventLog> findAllGetDataEventLogsByTaskId(ObjectId taskId);

    List<SetDataEventLog> findAllSetDataEventLogsByCaseId(ObjectId caseId);

    List<SetDataEventLog> findAllSetDataEventLogsByTaskId(ObjectId taskId);

    List<DeletePetriNetEventLog> findAllDeletePetriNetEventLogsByNetId(ObjectId netId);

    List<ImportPetriNetEventLog> findAllImportPetriNetEventLogsByNetId(ObjectId netId);

    List<AssignTaskEventLog> findAllAssignTaskEventLogsByTaskId(ObjectId taskId);

    List<AssignTaskEventLog> findAllAssignTaskEventLogsByUserId(String userId);

    List<CancelTaskEventLog> findAllCancelTaskEventLogsByTaskId(ObjectId taskId);

    List<CancelTaskEventLog> findAllCancelTaskEventLogsByUserId(String userId);

    List<FinishTaskEventLog> findAllFinishTaskEventLogsByTaskId(ObjectId taskId);

    List<FinishTaskEventLog> findAllFinishTaskEventLogsByUserId(String userId);
}
