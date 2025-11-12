package com.ai.lawyer.domain.poll.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class PollVotedEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPollVoted(PollVotedEvent event) {
        if (event == null || event.getVoteDto() == null) return;
        String destination = "/topic/poll." + event.getPollId();
        try {
            // 이벤트 도착 로그
            log.debug("PollVotedEvent received for pollId={}, destination={}", event.getPollId(), destination);
            log.trace("Payload: {}", event.getVoteDto());

            messagingTemplate.convertAndSend(destination, event.getVoteDto());

            // 전송 완료 로그
            log.debug("WebSocket message sent to destination={}", destination);
        } catch (Exception e) {
            log.warn("투표({}) 웹소켓 메시지 전송 실패", event.getPollId(), e);
        }
    }
}