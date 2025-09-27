package com.ai.lawyer.domain.chatbot.exception;


public class HistoryNotFoundException extends RuntimeException {

    public HistoryNotFoundException(Long historyId) {
        super("존재하지 않는 채팅방(History)입니다. id=" + historyId);
    }

}