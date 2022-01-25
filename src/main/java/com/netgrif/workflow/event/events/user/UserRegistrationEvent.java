package com.netgrif.workflow.event.events.user;

import com.netgrif.workflow.auth.domain.IUser;
import com.netgrif.workflow.auth.domain.LoggedUser;
import com.netgrif.workflow.auth.domain.RegisteredUser;
import com.netgrif.workflow.utils.DateUtils;

public class UserRegistrationEvent extends UserEvent {

    public UserRegistrationEvent(RegisteredUser user) {
        super(new LoggedUser(
                user.getStringId(),
                user.getEmail(),
                user.getPassword(),
                user.getAuthorities()
        ));
    }

    public UserRegistrationEvent(LoggedUser user) {
        super(user);
    }

    public UserRegistrationEvent(IUser user) {
        super(new LoggedUser(
                user.getStringId(),
                user.getEmail(),
                "",
                user.getAuthorities()
        ));
    }

    @Override
    public String getMessage() {
        return "New user " + user.getUsername() + " registered on " + DateUtils.toString(time);
    }
}