package com.wuzuy.telegram;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.CreateChatInviteLink;
import org.telegram.telegrambots.meta.api.objects.ChatInviteLink;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class TelegramBot extends TelegramLongPollingBot {

    private final String botToken;
    private final String botUsername;
    private final String groupChatId; // ID do grupo do Telegram

    public TelegramBot(String botToken, String botUsername, String groupChatId) {
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.groupChatId = groupChatId;
    }

    @Override
    public void onUpdateReceived(Update update) {
        // Não é necessário para esta funcionalidade
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    public String generateUniqueInviteLink() {
        try {
            CreateChatInviteLink createInviteLink = CreateChatInviteLink.builder()
                    .chatId(groupChatId)
                    .createsJoinRequest(false) // Define se o link requer aprovação
                    .build();

            ChatInviteLink inviteLink = execute(createInviteLink);
            return inviteLink.getInviteLink();
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return null;
        }
    }
}
