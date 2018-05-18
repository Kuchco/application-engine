package com.netgrif.workflow.auth.service;

import com.netgrif.workflow.auth.domain.User;
import com.netgrif.workflow.auth.domain.UserState;
import com.netgrif.workflow.auth.domain.repositories.UserRepository;
import com.netgrif.workflow.auth.service.interfaces.IRegistrationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.jws.soap.SOAPBinding;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;

@RunWith(SpringRunner.class)
@ActiveProfiles({"test"})
@SpringBootTest
public class TokenServiceTest {

    @Autowired
    IRegistrationService service;

    @Autowired
    UserRepository repository;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    @Before
    public void setUp() {
        repository.deleteAll();
    }

    @After
    public void cleanUp() {
        repository.deleteAll();
    }

    @Test
    public void removeExpired() throws Exception {
        User expired = new User("test@test.com", null, User.UNKNOWN, User.UNKNOWN);
        expired.setToken("token");
        expired.setExpirationDate(LocalDateTime.now().minusDays(10));
        expired.setState(UserState.INVITED);
        repository.save(expired);

        User expired2 = new User("test2@test.com", null, User.UNKNOWN, User.UNKNOWN);
        expired2.setToken("token2");
        expired2.setState(UserState.INVITED);
        repository.save(expired2);

        service.removeExpiredUsers();

        assert repository.findAll().size() == 1;
    }

    @Test
    public void authorizeToken() throws Exception {
        User expired = new User("test3@test.com",null,User.UNKNOWN,User.UNKNOWN);
        expired.setToken("token3");
        expired.setExpirationDate(LocalDateTime.now().plusMinutes(10));
        expired.setState(UserState.INVITED);
        repository.save(expired);

        boolean authorized = service.verifyToken(service.encodeToken("test3@test.com", "token3"));
        User token = repository.findByEmail("test3@test.com");

        assertTokenRemoved(authorized, token);
    }

    private void assertTokenRemoved(boolean authorized, User token) {
        assert authorized;
        assert token != null;
    }
}