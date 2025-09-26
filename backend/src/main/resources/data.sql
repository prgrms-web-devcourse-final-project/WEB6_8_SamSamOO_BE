-- MySQL 용
CREATE TABLE IF NOT EXISTS SPRING_AI_CHAT_MEMORY (
                                                     `conversation_id` VARCHAR(36) NOT NULL,
    `content` TEXT NOT NULL,
    `type` ENUM('USER', 'ASSISTANT', 'SYSTEM', 'TOOL') NOT NULL,
    `timestamp` TIMESTAMP NOT NULL,

    INDEX `SPRING_AI_CHAT_MEMORY_CONVERSATION_ID_TIMESTAMP_IDX` (`conversation_id`, `timestamp`)
    );

/*-- H2 용
CREATE TABLE IF NOT EXISTS SPRING_AI_CHAT_MEMORY
(
    conversation_id
    VARCHAR
(
    36
) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR
(
    10
) NOT NULL,
    "timestamp" TIMESTAMP NOT NULL,
    PRIMARY KEY
(
    conversation_id,
    "timestamp"
)
    );

CREATE INDEX IF NOT EXISTS SPRING_AI_CHAT_MEMORY_CONVERSATION_ID_TIMESTAMP_IDX
    ON SPRING_AI_CHAT_MEMORY(conversation_id, "timestamp");*/