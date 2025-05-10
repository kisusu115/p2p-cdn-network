package com.p2pnetwork.network;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.p2pnetwork.message.Message;
import com.p2pnetwork.message.MessageType;
import com.p2pnetwork.message.dto.IntroduceContent;

import java.io.IOException;

// 제네릭 타입의 message.content에 대해 Class 지정 후 Deserialize를 하기위한 클래스
public class MessageDeserializer extends StdDeserializer<Message<?>> {

    public MessageDeserializer() {
        super(Message.class);
    }

    @Override
    public Message<?> deserialize(JsonParser p, com.fasterxml.jackson.databind.DeserializationContext ctxt)
            throws IOException {

        JsonNode node = p.getCodec().readTree(p);

        MessageType type = MessageType.valueOf(node.get("type").asText());
        String senderId = node.get("senderId").asText();
        String targetId = node.get("targetId").asText();
        long timestamp = node.get("timestamp").asLong();

        // type 기반 Deserialize 진행
        JsonNode contentNode = node.get("content");
        Object content = null;

        switch (type) {
            case INTRODUCE:
                content = p.getCodec().treeToValue(contentNode, IntroduceContent.class);
                break;
            case DIRECT_MESSAGE:
                content = p.getCodec().treeToValue(contentNode, String.class);
                break;
            // 추가적인 MessageType에 대해 처리 요함
            // message type 별로 어떤 클래스의 content를 받는지
            default:
                break;
        }

        return new Message<>(type, senderId, targetId, content, timestamp);
    }
}