package com.p2pnetwork.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class Message<T> {
    private MessageType type;
    private String senderId;
    private String targetId;
    private T content;
    private long timestamp;

    public static <T> Message<T> copy(Message<T> originMessage) {
        return new Message<>(
                originMessage.getType(),
                originMessage.getSenderId(),
                originMessage.getTargetId(),
                originMessage.getContent(),
                originMessage.getTimestamp()
        );
    }
}
