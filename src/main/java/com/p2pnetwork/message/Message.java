package com.p2pnetwork.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.io.Serializable;

@Getter
public class Message<T> implements Serializable {
    private final MessageType type;
    private final String senderId;
    private final String targetId;
    private final T content;  // content를 제네릭 타입으로 변경
    private final long timestamp;

    @JsonCreator
    public Message(
            @JsonProperty("type") MessageType type,
            @JsonProperty("senderId") String senderId,
            @JsonProperty("targetId") String targetId,
            @JsonProperty("content") T content,    // 제네릭 타입으로 변경
            @JsonProperty("timestamp") long timestamp
    ) {
        this.type = type;
        this.senderId = senderId;
        this.targetId = targetId;
        this.content = content;
        this.timestamp = timestamp;
    }

    public static <T> Message<T> of(MessageType type, String senderId, String targetId, T content) {
        return new Message<>(type, senderId, targetId, content, System.currentTimeMillis());
    }
}
