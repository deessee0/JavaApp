package com.example.padel_app.model;

import com.example.padel_app.model.enums.Level;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedbacks",
       uniqueConstraints = @UniqueConstraint(columnNames = {"author_id", "target_user_id", "match_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Feedback {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id", nullable = false)
    private User targetUser;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;
    
    @NotNull(message = "Suggested level is required")
    @Enumerated(EnumType.STRING)
    private Level suggestedLevel;
    
    @Column(length = 1000)
    private String comment;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}