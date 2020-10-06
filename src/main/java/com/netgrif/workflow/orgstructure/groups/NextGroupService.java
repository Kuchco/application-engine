package com.netgrif.workflow.orgstructure.groups;

import com.netgrif.workflow.auth.domain.User;
import com.netgrif.workflow.auth.service.interfaces.IRegistrationService;
import com.netgrif.workflow.auth.service.interfaces.IUserService;
import com.netgrif.workflow.auth.web.requestbodies.NewUserRequest;
import com.netgrif.workflow.mail.interfaces.IMailAttemptService;
import com.netgrif.workflow.mail.interfaces.IMailService;
import com.netgrif.workflow.orgstructure.groups.interfaces.INextGroupService;
import com.netgrif.workflow.petrinet.domain.I18nString;
import com.netgrif.workflow.petrinet.domain.PetriNet;
import com.netgrif.workflow.petrinet.service.interfaces.IPetriNetService;
import com.netgrif.workflow.startup.ImportHelper;
import com.netgrif.workflow.workflow.domain.Case;
import com.netgrif.workflow.workflow.domain.QCase;
import com.netgrif.workflow.workflow.domain.Task;
import com.netgrif.workflow.workflow.domain.repositories.TaskRepository;
import com.netgrif.workflow.workflow.service.interfaces.IWorkflowService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NextGroupService implements INextGroupService {

    @Autowired
    private IWorkflowService workflowService;

    @Autowired
    private IMailService mailService;

    @Autowired
    private IMailAttemptService mailAttemptService;

    @Autowired
    private IUserService userService;

    @Autowired
    private IRegistrationService registrationService;

    @Autowired
    private IPetriNetService petriNetService;

    @Autowired
    private ImportHelper importHelper;

    @Autowired
    private TaskRepository taskRepository;


    private final static String GROUP_NET_IDENTIFIER = "org_group";
    private final static String GROUP_INIT_TASK_ID = "2";

    private final static String GROUP_CASE_IDENTIFIER = "org_group";
    private final static String GROUP_MEMBERS_FIELD = "members";
    private final static String GROUP_AUTHOR_FIELD = "author";
    private final static String GROUP_TITLE_FIELD = "group_name";

    @Override
    public Case createGroup(User author){
        return createGroup(author.getFullName(), author);
    }

    @Override
    public Case createGroup(String title, User author){
        if(authorHasDefaultGroup(author)){
            return null;
        }
        PetriNet orgGroupNet = importHelper.createNet("engine-processes/org_group.xml", "major", author.transformToLoggedUser()).get();
        Case groupCase = workflowService.createCase(orgGroupNet.getStringId(), title, "", author.transformToLoggedUser());

        groupCase.getDataField(GROUP_MEMBERS_FIELD).setOptions(addUser(author, new HashMap<>()));
        workflowService.save(groupCase);

        List<Task> taskList = taskRepository.findAllByCaseId(groupCase.getStringId());
        Task initTask = taskList.stream().filter(task -> task.getTransitionId().equals(GROUP_INIT_TASK_ID)).findFirst().get();
        Map<String, Map<String,String>> taskData = new HashMap<>();

        Map<String, String> authorData = new HashMap<>();
        authorData.put("type", "user");
        authorData.put("value", author.getId().toString());

        Map<String, String> titleData = new HashMap<>();
        titleData.put("type", "text");
        titleData.put("value", title);

        taskData.put(GROUP_TITLE_FIELD, titleData);
        taskData.put(GROUP_AUTHOR_FIELD, authorData);

        importHelper.setTaskData(initTask.getStringId(), taskData);

        return groupCase;
    }

    @Override
    public Case findGroup(String groupID){
        Case result = workflowService.searchOne(groupCase().and(QCase.case$._id.eq(new ObjectId(groupID))));
        if(!isGroupCase(result)){
            return null;
        }
        return result;
    }

    @Override
    public Case findDefaultGroup(){
        return workflowService.searchOne(groupCase().and(QCase.case$.title.eq("Default system group")));
    }

    @Override
    public List<Case> findByPredicate(Predicate predicate){
        return workflowService.searchAll(predicate).getContent();
    }

    @Override
    public List<Case> findByIds(Collection<String> groupIds) {
        List<BooleanExpression> groupQueries = groupIds.stream().map(ObjectId::new).map(QCase.case$._id::eq).collect(Collectors.toList());
        BooleanBuilder builder = new BooleanBuilder();
        groupQueries.forEach(builder::or);
        return this.workflowService.searchAll(groupCase().and(builder)).getContent();
    }

    @Override
    public List<Case> findAllGroups(){
        return workflowService.searchAll(groupCase()).getContent();
    }

    @Override
    public Map<String, I18nString> inviteUser(String email, Map<String, I18nString> existingUsers, Case groupCase){
        if(!isGroupCase(groupCase)){
            return null;
        }
        User user = userService.findByEmail(email, true);
        if(user != null && user.isRegistered()){
            log.info("User [" + user.getFullName() + "] has already been registered.");
        }else{
            log.info("Inviting new user to group.");
            NewUserRequest newUserRequest = new NewUserRequest();
            newUserRequest.email = email;
            user = registrationService.createNewUser(newUserRequest);

            try {
                mailService.sendRegistrationEmail(user);
                mailAttemptService.mailAttempt(newUserRequest.email);
            } catch (MessagingException | IOException | TemplateException e) {
                log.error(e.getMessage());
            }
        }
        return addUser(user, existingUsers);
    }

    @Override
    public void addUserToDefaultGroup(User user){
        addUser(user, findDefaultGroup());
    }

    @Override
    public void addUser(User user, Case groupCase){
        Map<String, I18nString> existingUsers = groupCase.getDataField(GROUP_MEMBERS_FIELD).getOptions();
        if(existingUsers == null){
            existingUsers = new HashMap<>();
        }
        groupCase.getDataField(GROUP_MEMBERS_FIELD).setOptions(addUser(user, existingUsers));
        workflowService.save(groupCase);
    }

    @Override
    public Map<String, I18nString> addUser(User user, Map<String, I18nString> existingUsers){
        existingUsers.put(user.getId().toString(), new I18nString(user.getEmail()));
        return existingUsers;
    }

    @Override
    public void removeUser(User user, Case groupCase){
        HashSet<String> userIds = new HashSet<>();
        Map<String, I18nString> existingUsers = groupCase.getDataField(GROUP_MEMBERS_FIELD).getOptions();

        userIds.add(user.getId().toString());
        groupCase.getDataField(GROUP_MEMBERS_FIELD).setOptions(removeUser(userIds, existingUsers, groupCase));
        workflowService.save(groupCase);
    }

    @Override
    public Map<String, I18nString> removeUser(HashSet<String> usersToRemove, Map<String, I18nString> existingUsers, Case groupCase){
        String authorId = this.getGroupOwnerId(groupCase).toString();
        usersToRemove.forEach(user -> {
            if(user.equals(authorId)){
                log.error("Author with id [" + authorId + "] cannot be removed from group with ID [" + groupCase.get_id().toString() + "]");
            }else{
                existingUsers.remove(user);
            }
        });
        return existingUsers;
    }

    @Override
    public List<User> getMembers(Case groupCase){
        if(!isGroupCase(groupCase)){
            return null;
        }
        Set<String> userIds = groupCase.getDataSet().get(GROUP_MEMBERS_FIELD).getOptions().keySet();
        List<User> resultList = new ArrayList<>();
        userIds.forEach(id -> resultList.add(userService.findById(Long.parseLong(id), true)));
        return resultList;
    }

    @Override
    public Long getGroupOwnerId(String groupId) {
        return this.getGroupOwnerId(this.findGroup(groupId));
    }

    @Override
    public Collection<Long> getGroupsOwnerIds(Collection<String> groupIds) {
        return this.findByIds(groupIds).stream().map(this::getGroupOwnerId).collect(Collectors.toList());
    }

    @Override
    public String getGroupOwnerEmail(String groupId) {
        return this.getGroupOwnerEmail(this.findGroup(groupId));
    }

    @Override
    public Collection<String> getGroupsOwnerEmails(Collection<String> groupIds) {
        return this.findByIds(groupIds).stream().map(this::getGroupOwnerEmail).collect(Collectors.toList());
    }

    private static BooleanExpression groupCase() {
        return QCase.case$.processIdentifier.eq(GROUP_CASE_IDENTIFIER);
    }

    private boolean isGroupCase(Case aCase){
        if(aCase == null){
            log.error("The input case is a null object.");
            return false;
        }else if(!aCase.getProcessIdentifier().equals(GROUP_CASE_IDENTIFIER)){
            log.error("Case [" + aCase.getTitle() + "] is not an organization group case.");
            return false;
        }
        return true;
    }

    private boolean authorHasDefaultGroup(User author){
        List<Case> allGroups = findAllGroups();
        for (Case group : allGroups){
            if(group.getAuthor().getId().equals(author.getId())){
                return true;
            }
        }
        return false;
    }

    private Long getGroupOwnerId(Case groupCase) {
        return groupCase.getAuthor().getId();
    }

    private String getGroupOwnerEmail(Case groupCase) {
        return groupCase.getAuthor().getEmail();
    }

}
