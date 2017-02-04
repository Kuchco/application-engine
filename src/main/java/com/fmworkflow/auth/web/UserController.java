package com.fmworkflow.auth.web;

import com.fmworkflow.auth.domain.Token;
import com.fmworkflow.auth.domain.User;
import com.fmworkflow.auth.service.ITokenService;
import com.fmworkflow.auth.service.IUserService;
import com.fmworkflow.json.JsonBuilder;
import com.fmworkflow.mail.IMailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.mail.MessagingException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/login")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private IUserService userService;

    @Autowired
    private ITokenService tokenService;

    @Autowired
    private IMailService mailService;

    @RequestMapping(value = "/signup", method = RequestMethod.GET)
    public ModelAndView registrationForward() throws IOException {
        log.info("Forwarding to / from /login/signup");
        return new ModelAndView("forward:/");
    }

    @RequestMapping(value = "/signup", method = RequestMethod.POST)
    public String registration(@RequestBody RegistrationRequest regRequest) {
        if (tokenService.authorizeToken(regRequest.getEmail(), regRequest.getToken())) {
            User user = new User(regRequest.getEmail(), regRequest.getPassword(), regRequest.getName(), regRequest.getSurname());
            userService.save(user);

            return JsonBuilder.init()
                    .addSuccessMessage("fici to")
                    .build();
        } else {
            return JsonBuilder.init()
                    .addErrorMessage("nefici to")
                    .build();
        }
    }

    @RequestMapping(value = "/invite", method = RequestMethod.POST)
    public String invite(@RequestBody String email) {
        try {
            String mail = URLDecoder.decode(email.split("=")[1], StandardCharsets.UTF_8.name());
            Token token = tokenService.createToken(mail);
            mailService.sendRegistrationEmail(mail, token.getHashedToken());

            return JsonBuilder.init()
                    .addSuccessMessage("Mail sent")
                    .build();
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error(e.toString());
            return JsonBuilder.init()
                    .addErrorMessage("Sending mail unsuccessful")
                    .build();
        }
    }

    public class RegistrationRequest {

        private String token;
        private String email;
        private String name;
        private String surname;
        private String password;

        RegistrationRequest(){}

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSurname() {
            return surname;
        }

        public void setSurname(String surname) {
            this.surname = surname;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}