package com.example.padel_app.service;

import com.example.padel_app.model.User;
import com.example.padel_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserContext {
    
    private final UserRepository userRepository;
    
    /**
     * Simula l'utente attualmente loggato nell'applicazione.
     * Per questo progetto, l'utente è sempre Margherita Biffi.
     */
    public User getCurrentUser() {
        return userRepository.findByUsername("margherita")
                .orElseThrow(() -> new IllegalStateException("Utente Margherita Biffi non trovato nel sistema"));
    }
}
