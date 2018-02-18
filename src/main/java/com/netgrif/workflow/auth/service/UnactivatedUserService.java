package com.netgrif.workflow.auth.service;

import com.netgrif.workflow.auth.domain.Organization;
import com.netgrif.workflow.auth.domain.UnactivatedUser;
import com.netgrif.workflow.auth.domain.User;
import com.netgrif.workflow.auth.domain.UserProcessRole;
import com.netgrif.workflow.auth.domain.repositories.OrganizationRepository;
import com.netgrif.workflow.auth.domain.repositories.UnactivatedUserRepository;
import com.netgrif.workflow.auth.domain.repositories.UserProcessRoleRepository;
import com.netgrif.workflow.auth.service.interfaces.IUnactivatedUserService;
import com.netgrif.workflow.auth.web.requestbodies.NewUserRequest;
import com.netgrif.workflow.auth.web.requestbodies.RegistrationRequest;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UnactivatedUserService implements IUnactivatedUserService {

    private static final Logger log = Logger.getLogger(UnactivatedUserService.class.getName());

    @Autowired
    private UnactivatedUserRepository repository;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private UserProcessRoleRepository processRoleRepository;

    @Scheduled(cron = "0 0 1 * * *")
    public void removeExpired() {
        log.info("Removing expired unactivated users");
        List<UnactivatedUser> expired = repository.removeByExpirationDateBefore(LocalDateTime.now());
        log.info("Removed " + expired.size() + " unactivated users");
    }

    @Override
    public boolean authorizeToken(String email, String token) {
        UnactivatedUser user = repository.findByEmail(email);
        return isTokenValid(user) && user.getToken().equals(token);
    }

    @Override
    public UnactivatedUser createUnactivatedUser(NewUserRequest request) {
        UnactivatedUser user = new UnactivatedUser(request.email, new BigInteger(260, new SecureRandom()).toString(32));
        user.setOrganizations(request.organizations);
        user.setProcessRoles(request.processRoles);

        repository.deleteAllByEmail(user.getEmail());
        user = repository.save(user);

        return user;
    }

    @Override
    public User createUser(RegistrationRequest request){
        UnactivatedUser unactivatedUser = repository.findByEmail(request.email);
        User user = new User(unactivatedUser.getEmail(),request.password,request.name,request.surname);

        user.setOrganizations(getUsersOrganizations(unactivatedUser.getOrganizations()));
        user.setUserProcessRoles(getUsersUserProcessRoles(unactivatedUser.getProcessRoles()));

        return user;
    }

    @Override
    public String getEmail(String token) {
        UnactivatedUser user = repository.findByToken(token);
        return user != null ? user.getEmail() : null;
    }

    private boolean isTokenValid(UnactivatedUser user) {
        return user != null && user.getToken() != null && user.getExpirationDate().isAfter(LocalDateTime.now());
    }

    private Set<Organization> getUsersOrganizations(Set<Long> orgs){
        return new HashSet<>(organizationRepository.findAll(orgs));
    }

    private Set<UserProcessRole> getUsersUserProcessRoles(Set<String> roles){
        return new HashSet<>(processRoleRepository.findByRoleIdIn(roles));
    }


}