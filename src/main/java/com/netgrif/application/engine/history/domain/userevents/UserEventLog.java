package com.netgrif.application.engine.history.domain.userevents;

import com.netgrif.application.engine.history.domain.baseevent.EventLog;
import lombok.Getter;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class UserEventLog extends EventLog implements IUserEventLog {

    @Getter
    private String email;

    public UserEventLog(String email) {
        this.email = email;
    }
}