package com.example.userservice.repository;

import com.example.userservice.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {

    @Query("SELECT m FROM ChatMessage m WHERE m.chat.id = :chatId AND m.isDeleted = false ORDER BY m.createdAt DESC")
    Page<ChatMessage> findMessagesByChatId(@Param("chatId") String chatId, Pageable pageable);

    @Query("SELECT m FROM ChatMessage m WHERE m.chat.id = :chatId AND m.isDeleted = false ORDER BY m.createdAt DESC")
    List<ChatMessage> findMessagesByChatId(@Param("chatId") String chatId);

    @Query("SELECT m FROM ChatMessage m WHERE m.sender.id = :userId AND m.isDeleted = false ORDER BY m.createdAt DESC")
    Page<ChatMessage> findMessagesBySenderId(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT m FROM ChatMessage m WHERE m.chat.id = :chatId AND m.type = :type AND m.isDeleted = false ORDER BY m.createdAt DESC")
    List<ChatMessage> findMessagesByChatIdAndType(@Param("chatId") String chatId, @Param("type") ChatMessage.MessageType type);

    @Query("SELECT m FROM ChatMessage m WHERE m.chat.id = :chatId AND m.content LIKE %:searchTerm% AND m.isDeleted = false ORDER BY m.createdAt DESC")
    Page<ChatMessage> searchMessagesInChat(@Param("chatId") String chatId, @Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT m FROM ChatMessage m WHERE m.replyTo.id = :messageId AND m.isDeleted = false ORDER BY m.createdAt ASC")
    List<ChatMessage> findRepliesToMessage(@Param("messageId") String messageId);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.chat.id = :chatId AND m.isDeleted = false")
    Long countMessagesInChat(@Param("chatId") String chatId);

    @Query("SELECT m FROM ChatMessage m WHERE m.chat.id = :chatId AND m.isDeleted = false ORDER BY m.createdAt DESC LIMIT 1")
    Optional<ChatMessage> findLastMessageInChat(@Param("chatId") String chatId);

    @Query("SELECT m FROM ChatMessage m WHERE m.chat.id = :chatId AND m.createdAt > :since AND m.isDeleted = false ORDER BY m.createdAt ASC")
    List<ChatMessage> findNewMessagesSince(@Param("chatId") String chatId, @Param("since") java.time.LocalDateTime since);
}
