package com.example.padel_app.model;

import com.example.padel_app.model.enums.Level;
import com.example.padel_app.model.enums.MatchStatus;
import com.example.padel_app.model.enums.MatchType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "matches")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Match {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "Match type is required")
    @Enumerated(EnumType.STRING)
    private MatchType type;
    
    @NotNull(message = "Match status is required")
    @Enumerated(EnumType.STRING)
    private MatchStatus status = MatchStatus.WAITING;
    
    @NotNull(message = "Required level is required")
    @Enumerated(EnumType.STRING)
    private Level requiredLevel;
    
    @NotNull(message = "Match date and time is required")
    private LocalDateTime dateTime;
    
    @NotBlank(message = "Location is required")
    private String location;
    
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private User creator;
    
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Registration> registrations = new ArrayList<>();
    
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Feedback> feedbacks = new ArrayList<>();
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // Helper method to get active registrations count
    public int getActiveRegistrationsCount() {
        return (int) registrations.stream()
            .filter(r -> r.getStatus() == com.example.padel_app.model.enums.RegistrationStatus.JOINED)
            .count();
    }
    
    // Helper method to check if match is full (4 players)
    public boolean isFull() {
        return getActiveRegistrationsCount() >= 4;
    }
}