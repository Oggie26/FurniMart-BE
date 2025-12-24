package com.example.userservice.repository;

import com.example.userservice.entity.Chat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRepository extends JpaRepository<Chat, String> {

    @Query("SELECT c FROM Chat c JOIN c.participants p WHERE p.user.id = :userId AND c.isDeleted = false AND p.status = 'ACTIVE'")
    List<Chat> findChatsByUserId(@Param("userId") String userId);

    @Query("SELECT c FROM Chat c JOIN c.participants p WHERE p.user.id = :userId AND c.isDeleted = false AND p.status = 'ACTIVE'")
    Page<Chat> findChatsByUserId(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT c FROM Chat c WHERE c.type = 'PRIVATE' AND c.isDeleted = false AND " +
           "EXISTS (SELECT p1 FROM ChatParticipant p1 WHERE p1.chat = c AND p1.user.id = :userId1 AND p1.status = 'ACTIVE') AND " +
           "EXISTS (SELECT p2 FROM ChatParticipant p2 WHERE p2.chat = c AND p2.user.id = :userId2 AND p2.status = 'ACTIVE')")
    Optional<Chat> findPrivateChatBetweenUsers(@Param("userId1") String userId1, @Param("userId2") String userId2);

    @Query("SELECT c FROM Chat c WHERE c.name LIKE %:searchTerm% AND c.isDeleted = false")
    List<Chat> searchChatsByName(@Param("searchTerm") String searchTerm);

    @Query("SELECT c FROM Chat c WHERE c.type = :type AND c.isDeleted = false")
    List<Chat> findChatsByType(@Param("type") Chat.ChatType type);

    @Query("SELECT c FROM Chat c WHERE c.createdBy.id = :userId AND c.isDeleted = false")
    List<Chat> findChatsCreatedByUser(@Param("userId") String userId);

    @Query("SELECT c FROM Chat c WHERE c.isDeleted = false AND " +
           "(c.name LIKE %:searchTerm% OR c.description LIKE %:searchTerm%)")
    Page<Chat> searchChats(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT c FROM Chat c WHERE c.chatMode = 'WAITING_STAFF' AND c.isDeleted = false ORDER BY c.staffRequestedAt ASC")
    List<Chat> findChatsWaitingForStaff();
}
