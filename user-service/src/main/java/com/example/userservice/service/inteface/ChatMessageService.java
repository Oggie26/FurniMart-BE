package com.example.userservice.service.inteface;

import com.example.userservice.request.ChatMessageRequest;
import com.example.userservice.response.ChatMessageResponse;
import com.example.userservice.response.PageResponse;

import java.util.List;

public interface ChatMessageService {

    ChatMessageResponse sendMessage(ChatMessageRequest messageRequest);

    List<ChatMessageResponse> getChatMessages(String chatId);

    PageResponse<ChatMessageResponse> getChatMessagesWithPagination(String chatId, int page, int size);

    ChatMessageResponse getMessageById(String messageId);

    ChatMessageResponse editMessage(String messageId, String newContent);

    void deleteMessage(String messageId);

    List<ChatMessageResponse> searchMessagesInChat(String chatId, String searchTerm);

    PageResponse<ChatMessageResponse> searchMessagesInChatWithPagination(String chatId, String searchTerm, int page, int size);

    List<ChatMessageResponse> getMessageReplies(String messageId);

    void markMessageAsRead(String messageId);

    List<ChatMessageResponse> getUnreadMessages(String chatId);
}
