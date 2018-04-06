package com.netgrif.workflow.workflow.domain.repositories;

import com.netgrif.workflow.workflow.domain.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Collection;
import java.util.List;

public interface TaskRepository extends MongoRepository<Task, String> {

    List<Task> findAllByCaseId(String id);

    Page<Task> findByCaseIdIn(Pageable pageable, Collection<String> ids);

    Page<Task> findByTransitionIdIn(Pageable pageable, Collection<String> ids);

    /**
     * For Case search purpose only
     * @param ids collection of transition objectIds
     * @return list of tasks with only set caseId
     */
    @Query(fields = "{caseId:1}")
    List<Task> findAllByTransitionIdIn(Collection<String> ids);

    Page<Task> findByUserId(Pageable pageable, Long userId);

    List<Task> findByUserIdAndFinishDateNotNull(Long userId);

    Task findByTransitionIdAndCaseId(String transitionId, String caseId);

    List<Task> findAllByTransitionIdInAndCaseId(Collection<String> transitionIds, String caseId);

    void deleteAllByCaseIdAndUserIdIsNull(String caseId);

    void deleteAllByCaseIdAndFinishDateIsNotNull(String caseId);

    void deleteAllByCaseId(String caseId);
}
