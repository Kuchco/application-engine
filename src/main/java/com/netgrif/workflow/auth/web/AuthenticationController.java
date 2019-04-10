package com.netgrif.workflow.auth.web;

import com.netgrif.workflow.auth.domain.LoggedUser;
import com.netgrif.workflow.auth.domain.User;
import com.netgrif.workflow.auth.service.InvalidUserTokenException;
import com.netgrif.workflow.auth.service.UserDetailsServiceImpl;
import com.netgrif.workflow.auth.service.interfaces.IRegistrationService;
import com.netgrif.workflow.auth.service.interfaces.IUserService;
import com.netgrif.workflow.auth.web.requestbodies.ChangePasswordRequest;
import com.netgrif.workflow.auth.web.requestbodies.NewUserRequest;
import com.netgrif.workflow.auth.web.requestbodies.RegistrationRequest;
import com.netgrif.workflow.auth.web.responsebodies.UserResource;
import com.netgrif.workflow.mail.IMailService;
import com.netgrif.workflow.workflow.web.responsebodies.MessageResource;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.mail.MessagingException;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationController.class);

    @Autowired
    private IRegistrationService registrationService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private IMailService mailService;

    @Autowired
    private IUserService userService;

    @Value("${server.auth.open-registration}")
    private boolean openRegistration;

    @PostMapping(value = "/signup")
    public MessageResource signup(@RequestBody RegistrationRequest regRequest) {
        if (!registrationService.verifyToken(regRequest.token))
            return MessageResource.errorMessage("Registration of " + regRequest.email + " has failed! Invalid token!");

        regRequest.password = new String(Base64.getDecoder().decode(regRequest.password));
        User user = registrationService.registerUser(regRequest);
        if (user == null)
            return MessageResource.errorMessage("Registration of " + regRequest.email + " has failed! No user with this email was found.");

        return MessageResource.successMessage("Registration complete");
    }

    @PostMapping(value = "/invite")
    public MessageResource invite(@RequestBody NewUserRequest newUserRequest, Authentication auth) {
        try {
            if (!openRegistration && (auth == null || !((LoggedUser) auth.getPrincipal()).isAdmin())) {
                return MessageResource.errorMessage("Only admin can invite new users!");
            }

            newUserRequest.email = URLDecoder.decode(newUserRequest.email, StandardCharsets.UTF_8.name());
            User user = registrationService.createNewUser(newUserRequest);
            if (user == null)
                return MessageResource.errorMessage("User with email " + newUserRequest.email + " has been already registered.");
            mailService.sendRegistrationEmail(user);

            return MessageResource.successMessage("Mail was sent to " + user.getEmail());
        } catch (IOException | TemplateException | MessagingException e) {
            log.error(e.toString());
            return MessageResource.errorMessage("Sending mail to " + newUserRequest.email + " failed!");
        }
    }

    @PostMapping(value = "/token/verify")
    public MessageResource verifyToken(@RequestBody String token) {
        try {
            if (registrationService.verifyToken(token))
                return MessageResource.successMessage(registrationService.decodeToken(token)[0]);
            else
                return MessageResource.errorMessage("Invalid token!");
        } catch (InvalidUserTokenException e) {
            log.error(e.getMessage());
            return MessageResource.errorMessage("Invalid token!");
        }
    }

    @GetMapping(value = "/login")
    public UserResource login(Authentication auth, Locale locale) {
        return new UserResource(userService.findByAuth(auth), "profile", locale);
    }

    @PostMapping(value = "/reset")
    public MessageResource resetPassword(@RequestBody String recoveryEmail) {
        try {
            User user = registrationService.resetPassword(recoveryEmail);
            if (user != null) {
                mailService.sendPasswordResetEmail(user);
                return MessageResource.successMessage("Email with reset link was sent to address " + recoveryEmail);
            } else {
                return MessageResource.errorMessage("User with email " + recoveryEmail + " has not yet registered");
            }
        } catch (MessagingException | IOException | TemplateException e) {
            log.error(e.toString());
            return MessageResource.errorMessage("Reset email has failed to be sent to address " + recoveryEmail);
        }
    }

    @PostMapping(value = "/recover")
    public MessageResource recoverAccount(@RequestBody RegistrationRequest request) {
        try {
            if (!registrationService.verifyToken(request.token))
                return MessageResource.errorMessage("Invalid token!");
            User user = registrationService.recover(registrationService.decodeToken(request.token)[0], new String(Base64.getDecoder().decode(request.password)));
            if (user == null)
                return MessageResource.errorMessage("Recovery of account has failed!");
            return MessageResource.successMessage("Account is successfully recovered. You can login now.");
        } catch (InvalidUserTokenException e) {
            log.error(e.getMessage());
            return MessageResource.errorMessage("Invalid token!");
        }
    }

    @PostMapping(value = "/changePassword")
    public MessageResource changePassword(Authentication auth, @RequestBody ChangePasswordRequest request) {
        try {
            User user = userService.findByEmail(request.login, false);
            if (user == null || request.password == null || request.newPassword == null) {
                return MessageResource.errorMessage("Incorrect login!");
            }

            String newPassword = new String(Base64.getDecoder().decode(request.newPassword));
            if (!registrationService.isPasswordSufficient(newPassword)) {
                return MessageResource.errorMessage("Insufficient password!");
            }

            String password = new String(Base64.getDecoder().decode(request.password));
            if (userService.stringMatchesUserPassword(user, password)) {
                registrationService.changePassword(user, newPassword);
                userDetailsService.reloadSecurityContext((LoggedUser) auth.getPrincipal());

            } else {
                return MessageResource.errorMessage("Incorrect password!");
            }

            return MessageResource.successMessage("Password is successfully changed.");
        } catch (Exception e) {
            log.error(e.getMessage());
            return MessageResource.errorMessage("There has been a problem!");
        }
    }

}