package com.p2pnetwork.network;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.p2pnetwork.message.Message;
import com.p2pnetwork.message.MessageType;
import com.p2pnetwork.routing.RoutingEntry;
import com.p2pnetwork.message.dto.AssignSuperNodeContent;
import com.p2pnetwork.message.dto.JoinResponseContent;

import java.io.IOException;

public class MessageDeserializer extends StdDeserializer<Message<?>> {
    public MessageDeserializer() { super(Message.class); }
    @Override
    public Message<?> deserialize(JsonParser p, com.fasterxml.jackson.databind.DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        MessageType type = MessageType.valueOf(node.get("type").asText());
        String senderId = node.get("senderId").asText();
        String targetId = node.get("targetId").asText();
        long timestamp = node.get("timestamp").asLong();
        JsonNode contentNode = node.get("content");

        Object content = null;

        switch (type) {
            case INTRODUCE:
            case JOIN_REQUEST:
            case NEW_PEER_BROADCAST:
            case NEW_SUPERNODE_BROADCAST:
            case NEW_REDUNDANCY_BROADCAST:
                content = p.getCodec().treeToValue(contentNode, RoutingEntry.class);
                break;
            case ASSIGN_SUPERNODE:
                content = p.getCodec().treeToValue(contentNode, AssignSuperNodeContent.class);
                break;
            case JOIN_RESPONSE:
                content = p.getCodec().treeToValue(contentNode, JoinResponseContent.class);
                break;
            default:
                break;
        }

        return new Message<>(type, senderId, targetId, content, timestamp);
    }
}
