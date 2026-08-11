package com.actiongame.server.benchmark;

import com.actiongame.server.constant.GameConstants;
import com.actiongame.server.net.util.BinaryCodec;

public final class LoadTestPayloads {
    private LoadTestPayloads() {}

    public static byte[] buildLoginPayload(String token, String playerName) {
        int size = 4 + BinaryCodec.stringSize(token) + BinaryCodec.stringSize(playerName) + 4;
        byte[] payload = new byte[size];
        int offset = 0;
        BinaryCodec.writeInt(payload, offset, GameConstants.PROTOCOL_VERSION); offset += 4;
        BinaryCodec.writeString(payload, offset, token); offset += BinaryCodec.stringSize(token);
        BinaryCodec.writeString(payload, offset, playerName); offset += BinaryCodec.stringSize(playerName);
        BinaryCodec.writeInt(payload, offset, 1);
        return payload;
    }

    public static byte[] buildJoinRoomPayload(String playerId, String roomId) {
        int size = 4 + BinaryCodec.stringSize(playerId) + BinaryCodec.stringSize(roomId);
        byte[] payload = new byte[size];
        int offset = 0;
        BinaryCodec.writeInt(payload, offset, GameConstants.PROTOCOL_VERSION); offset += 4;
        BinaryCodec.writeString(payload, offset, playerId); offset += BinaryCodec.stringSize(playerId);
        BinaryCodec.writeString(payload, offset, roomId);
        return payload;
    }

    public static byte[] buildActionPayload(int entityId, long clientFrameIndex, int actionType) {
        int actionBytes = 1 + 4 + 4 + 4 + 4 + 1 + 4 + 8;
        int size = 4 + 4 + 8 + 4 + actionBytes;
        byte[] payload = new byte[size];
        int offset = 0;
        BinaryCodec.writeInt(payload, offset, GameConstants.PROTOCOL_VERSION); offset += 4;
        BinaryCodec.writeInt(payload, offset, entityId); offset += 4;
        BinaryCodec.writeLong(payload, offset, clientFrameIndex); offset += 8;
        BinaryCodec.writeInt(payload, offset, 1); offset += 4;
        payload[offset++] = (byte) actionType;
        BinaryCodec.writeInt(payload, offset, 0); offset += 4;
        BinaryCodec.writeFloat(payload, offset, 0f); offset += 4;
        BinaryCodec.writeFloat(payload, offset, 0f); offset += 4;
        BinaryCodec.writeInt(payload, offset, 0); offset += 4;
        offset += 1;
        BinaryCodec.writeInt(payload, offset, -1); offset += 4;
        BinaryCodec.writeLong(payload, offset, System.currentTimeMillis());
        return payload;
    }

    public static JoinResult parseJoinResponse(byte[] payload) {
        int offset = 0;
        int protocolVersion = BinaryCodec.readInt(payload, offset); offset += 4;
        int code = BinaryCodec.readInt(payload, offset); offset += 4;
        String roomId = BinaryCodec.readString(payload, offset); offset += BinaryCodec.stringSize(roomId);
        int entityId = BinaryCodec.readInt(payload, offset); offset += 4;
        int startFrameIndex = BinaryCodec.readInt(payload, offset); offset += 4;
        String redirectNodeId = "";
        if (offset < payload.length) {
            redirectNodeId = BinaryCodec.readString(payload, offset);
            offset += BinaryCodec.stringSize(redirectNodeId);
        }
        String redirectAddress = offset < payload.length ? BinaryCodec.readString(payload, offset) : "";
        return new JoinResult(protocolVersion, code, roomId, entityId, startFrameIndex,
            redirectNodeId, redirectAddress);
    }

    public record JoinResult(int protocolVersion, int code, String roomId, int entityId, int startFrameIndex,
                             String redirectNodeId, String redirectAddress) {}
}
