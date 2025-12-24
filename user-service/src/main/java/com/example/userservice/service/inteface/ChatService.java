package com.example.userservice.service.inteface;

import com.example.userservice.request.ChatRequest;
import com.example.userservice.response.ChatResponse;
import com.example.userservice.response.PageResponse;

import java.util.List;

public interface ChatService {

    ChatResponse createChat(ChatRequest chatRequest);

    ChatResponse getChatById(String chatId);

    List<ChatResponse> getUserChats();

    List<ChatResponse> getLatestChats();

    PageResponse<ChatResponse> getUserChatsWithPagination(int page, int size);

    List<ChatResponse> getLatestChatsWithUnreadPriority(int limit);

    ChatResponse updateChat(String chatId, ChatRequest chatRequest);

    void deleteChat(String chatId);

    ChatResponse addParticipant(String chatId, String userId);

    ChatResponse removeParticipant(String chatId, String userId);

    ChatResponse updateParticipantRole(String chatId, String userId, String role);

    List<ChatResponse> searchChats(String searchTerm);

    ChatResponse getOrCreatePrivateChat(String otherUserId);

    void markChatAsRead(String chatId);

    ChatResponse muteChat(String chatId, boolean muted);

    ChatResponse pinChat(String chatId, boolean pinned);

    // Methods for AI chat to staff flow
    ChatResponse requestStaffConnection(String chatId);
    ChatResponse acceptStaffConnection(String chatId);
    ChatResponse endStaffChat(String chatId);
    List<com.example.userservice.entity.Employee> getOnlineStaff();
    boolean isStaffOnline(String staffId);
}
