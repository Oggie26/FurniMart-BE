package com.example.userservice.repository;

import com.example.userservice.entity.Chat;
import com.example.userservice.entity.ChatParticipant;
import com.example.userservice.entity.User;
import com.example.userservice.enums.EnumStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, String> {

    @Query("SELECT p FROM ChatParticipant p WHERE p.chat.id = :chatId AND p.status = 'ACTIVE'")
    List<ChatParticipant> findActiveParticipantsByChatId(@Param("chatId") String chatId);

    @Query("SELECT p FROM ChatParticipant p WHERE p.user.id = :userId AND p.status = 'ACTIVE'")
    List<ChatParticipant> findActiveParticipationsByUserId(@Param("userId") String userId);

    @Query("SELECT p FROM ChatParticipant p WHERE p.chat.id = :chatId AND p.user.id = :userId AND p.status = 'ACTIVE'")
    Optional<ChatParticipant> findActiveParticipantByChatIdAndUserId(@Param("chatId") String chatId, @Param("userId") String userId);

    @Query("SELECT p FROM ChatParticipant p WHERE p.chat.id = :chatId AND p.role = :role AND p.status = 'ACTIVE'")
    List<ChatParticipant> findActiveParticipantsByChatIdAndRole(@Param("chatId") String chatId, @Param("role") ChatParticipant.ParticipantRole role);

    @Query("SELECT COUNT(p) FROM ChatParticipant p WHERE p.chat.id = :chatId AND p.status = 'ACTIVE'")
    Long countActiveParticipantsByChatId(@Param("chatId") String chatId);

    @Query("SELECT p FROM ChatParticipant p WHERE p.chat.id = :chatId AND p.user.id = :userId")
    Optional<ChatParticipant> findByChatIdAndUserId(@Param("chatId") String chatId, @Param("userId") String userId);

    @Query("SELECT p FROM ChatParticipant p WHERE p.user.id = :userId AND p.chat.type = :chatType AND p.status = 'ACTIVE'")
    List<ChatParticipant> findActiveParticipationsByUserIdAndChatType(@Param("userId") String userId, @Param("chatType") Chat.ChatType chatType);
}
