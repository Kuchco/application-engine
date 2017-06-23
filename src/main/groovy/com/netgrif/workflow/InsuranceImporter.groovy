package com.netgrif.workflow

import com.netgrif.workflow.auth.domain.Authority
import com.netgrif.workflow.auth.domain.Organization
import com.netgrif.workflow.auth.domain.User
import com.netgrif.workflow.auth.domain.UserProcessRole
import com.netgrif.workflow.auth.domain.repositories.AuthorityRepository
import com.netgrif.workflow.auth.domain.repositories.OrganizationRepository
import com.netgrif.workflow.auth.domain.repositories.UserProcessRoleRepository
import com.netgrif.workflow.auth.service.interfaces.IUserService
import com.netgrif.workflow.importer.Importer
import com.netgrif.workflow.petrinet.domain.PetriNet
import com.netgrif.workflow.petrinet.domain.repositories.PetriNetRepository
import com.netgrif.workflow.workflow.domain.Case
import com.netgrif.workflow.workflow.domain.DataField
import com.netgrif.workflow.workflow.domain.repositories.CaseRepository
import com.netgrif.workflow.workflow.service.TaskService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class InsuranceImporter {

    @Autowired
    private PetriNetRepository petriNetRepository
    @Autowired
    private IUserService userService
    @Autowired
    private OrganizationRepository organizationRepository
    @Autowired
    private CaseRepository caseRepository
    @Autowired
    private UserProcessRoleRepository userProcessRoleRepository
    @Autowired
    private AuthorityRepository authorityRepository
    @Autowired
    private TaskService taskService
    @Autowired
    private Importer importer


    void run(String... strings) throws Exception {
        //def net = importer.importPetriNet(new File("src/test/resources/poistenie_rozsirene.xml"), "Poistenie", "INS")
        def net = importer.importPetriNet(new File("petriNets/poistenie_rozsirene.xml"), "Poistenie", "INS")

        def orgs = createOrganizations()
        def auths = createAuthorities()
        createUsers(orgs,auths,net)
        createCases(net)
    }

    private Map<String, Organization> createOrganizations(){
        Map<String, Organization> orgs = new HashMap<>()
        orgs.put("premium",organizationRepository.save(new Organization("PREMIUM Insurance Company Limited")))
        return orgs
    }

    private Map<String, Authority> createAuthorities(){
        Map<String, Authority> auths = new HashMap<>()
        auths.put(Authority.user,authorityRepository.save(new Authority(Authority.user)))
        return auths
    }

    private void createUsers(Map<String, Organization> orgs, Map<String, Authority> auths, PetriNet net){
        def agentRole = userProcessRoleRepository.save(new UserProcessRole(
                roleId: net.roles.values().find { it -> it.name == "Agent" }.objectId
        ))
        def systemRole = userProcessRoleRepository.save(new UserProcessRole(
                roleId: net.roles.values().find { it -> it.name == "System" }.objectId
        ))
        User agent = new User(
                name: "Fero",
                surname: "Poisťovák",
                email: "agent@premium.com",
                password: "password",
                authorities: [auths.get(Authority.user)] as Set<Authority>,
                organizations: [orgs.get("premium")] as Set<Organization>)
        agent.addProcessRole(agentRole)
        User user = new User(
                name: "Ján",
                surname: "Kováč",
                email: "kovac@gmail.com",
                password: "password",
                authorities: [auths.get(Authority.user)] as Set<Authority>)
        user.addProcessRole(agentRole)
        User system = new User(
                name: "System",
                surname: "System",
                email: "system@premium.com",
                password: "password",
                authorities: [auths.get(Authority.user)] as Set<Authority>)
        system.addProcessRole(systemRole)
        userService.saveNew(agent)
        userService.saveNew(user)
        userService.saveNew(system)
    }

    private void createCases(PetriNet net){
        Case useCase = new Case(
                title: "Poistenie nehnuteľnosti",
                petriNet: net,
                color: StartRunner.randomColor())
        useCase.dataSet = new HashMap<>(net.dataSet.collectEntries {[(it.key): new DataField()]})
        useCase.activePlaces.put(net.places.find { it -> it.value.title == "E" }.key, 1)
        useCase.activePlaces.put(net.places.find { it -> it.value.title == "F" }.key, 1)
        useCase.activePlaces.put(net.places.find { it -> it.value.title == "G" }.key, 1)
        useCase.activePlaces.put(net.places.find { it -> it.value.title == "K" }.key, 1)
        useCase.activePlaces.put(net.places.find { it -> it.value.title == "I" }.key, 1)
        useCase.activePlaces.put(net.places.find { it -> it.value.title == "J" }.key, 1)
        useCase.setAuthor(1L)
        caseRepository.save(useCase)
        net.initializeTokens(useCase.activePlaces)
        taskService.createTasks(useCase)

        useCase = new Case(
                title: "Poistenie domu",
                petriNet: net,
                color: StartRunner.randomColor())
        useCase.dataSet = new HashMap<>(net.dataSet.collectEntries {[(it.key): new DataField()]})
        useCase.activePlaces.put(net.places.find { it -> it.value.title == "B" }.key, 1)
        useCase.setAuthor(1L)
        caseRepository.save(useCase)
        net.initializeTokens(useCase.activePlaces)
        taskService.createTasks(useCase)

        useCase = new Case(
                title: "Poistenie domácnosti",
                petriNet: net,
                color: StartRunner.randomColor())
        useCase.dataSet = new HashMap<>(net.dataSet.collectEntries {[(it.key): new DataField()]})
        useCase.activePlaces.put(net.places.find { it -> it.value.title == "C" }.key, 1)
        useCase.setAuthor(1L)
        useCase = caseRepository.save(useCase)
        net.initializeTokens(useCase.activePlaces)
        taskService.createTasks(useCase)
    }
}