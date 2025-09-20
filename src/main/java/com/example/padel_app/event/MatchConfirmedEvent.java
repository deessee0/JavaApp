package com.example.padel_app.event;

import com.example.padel_app.model.Match;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class MatchConfirmedEvent extends ApplicationEvent {
    
    private final Match match;
    
    public MatchConfirmedEvent(Object source, Match match) {
        super(source);
        this.match = match;
    }
}