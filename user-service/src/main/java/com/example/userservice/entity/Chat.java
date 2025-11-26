package com.example.userservice.entity;

import com.example.userservice.enums.EnumStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "chats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Chat extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnumStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ChatMessage> messages;

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ChatParticipant> participants;

    // Fields for AI chat to staff flow
    @Enumerated(EnumType.STRING)
    @Column(name = "chat_mode")
    @Builder.Default
    private ChatMode chatMode = ChatMode.AI;

    @Column(name = "assigned_staff_id")
    private String assignedStaffId;

    @Column(name = "staff_requested_at")
    private java.time.LocalDateTime staffRequestedAt;

    @Column(name = "staff_chat_ended_at")
    private java.time.LocalDateTime staffChatEndedAt;

    public enum ChatType {
        PRIVATE,
        GROUP,
        CHANNEL
    }

    public enum ChatMode {
        AI,
        WAITING_STAFF,
        STAFF_CONNECTED
    }
}
