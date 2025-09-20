package com.example.padel_app.event;

import com.example.padel_app.model.Match;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class MatchFinishedEvent extends ApplicationEvent {
    
    private final Match match;
    
    public MatchFinishedEvent(Object source, Match match) {
        super(source);
        this.match = match;
    }
}