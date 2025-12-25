package com.example.userservice.entity;

import com.example.userservice.enums.EnumStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_participants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ChatParticipant extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = true)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipantRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnumStatus status;

    @Column
    private LocalDateTime lastReadAt;

    @Builder.Default
    @Column
    private Boolean isMuted = false;

    @Builder.Default
    @Column
    private Boolean isPinned = false;

    @PrePersist
    @PreUpdate
    private void validateParticipant() {
        if (user == null && employee == null) {
            throw new IllegalStateException("ChatParticipant must have either user or employee");
        }
        if (user != null && employee != null) {
            throw new IllegalStateException("ChatParticipant cannot have both user and employee");
        }
    }

    public enum ParticipantRole {
        ADMIN,
        MODERATOR,
        MEMBER
    }
}
