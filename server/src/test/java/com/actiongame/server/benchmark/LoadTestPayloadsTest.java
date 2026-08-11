package com.actiongame.server.benchmark;

import com.actiongame.server.net.util.BinaryCodec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("压测协议负载")
class LoadTestPayloadsTest {

    @Test
    @DisplayName("should_buildLoginPayload_when_called")
    void should_buildLoginPayload_when_called() {
        byte[] payload = LoadTestPayloads.buildLoginPayload("token-1", "bench-0");

        int offset = 0;
        int version = BinaryCodec.readInt(payload, offset); offset += 4;
        String token = BinaryCodec.readString(payload, offset); offset += BinaryCodec.stringSize(token);
        String playerName = BinaryCodec.readString(payload, offset); offset += BinaryCodec.stringSize(playerName);
        int configId = BinaryCodec.readInt(payload, offset);

        assertThat(version).isEqualTo(10);
        assertThat(token).isEqualTo("token-1");
        assertThat(playerName).isEqualTo("bench-0");
        assertThat(configId).isEqualTo(1);
    }

    @Test
    @DisplayName("should_buildJoinPayload_when_called")
    void should_buildJoinPayload_when_called() {
        byte[] payload = LoadTestPayloads.buildJoinRoomPayload("bench-0", "room-0");

        int offset = 0;
        int version = BinaryCodec.readInt(payload, offset); offset += 4;
        String playerId = BinaryCodec.readString(payload, offset); offset += BinaryCodec.stringSize(playerId);
        String roomId = BinaryCodec.readString(payload, offset);

        assertThat(version).isEqualTo(10);
        assertThat(playerId).isEqualTo("bench-0");
        assertThat(roomId).isEqualTo("room-0");
    }

    @Test
    @DisplayName("should_buildActionPayload_when_called")
    void should_buildActionPayload_when_called() {
        byte[] payload = LoadTestPayloads.buildActionPayload(7, 99L, 1);

        int offset = 0;
        int version = BinaryCodec.readInt(payload, offset); offset += 4;
        int entityId = BinaryCodec.readInt(payload, offset); offset += 4;
        long frameIndex = BinaryCodec.readLong(payload, offset); offset += 8;
        int actionCount = BinaryCodec.readInt(payload, offset); offset += 4;
        int actionType = payload[offset] & 0xFF;

        assertThat(version).isEqualTo(10);
        assertThat(entityId).isEqualTo(7);
        assertThat(frameIndex).isEqualTo(99L);
        assertThat(actionCount).isEqualTo(1);
        assertThat(actionType).isEqualTo(1);
    }

    @Test
    @DisplayName("should_parseJoinResponse_when_called")
    void should_parseJoinResponse_when_called() {
        byte[] payload = new byte[4 + 4 + BinaryCodec.stringSize("room-0") + 4 + 4];
        int offset = 0;
        BinaryCodec.writeInt(payload, offset, 10); offset += 4;
        BinaryCodec.writeInt(payload, offset, 0); offset += 4;
        BinaryCodec.writeString(payload, offset, "room-0"); offset += BinaryCodec.stringSize("room-0");
        BinaryCodec.writeInt(payload, offset, 7); offset += 4;
        BinaryCodec.writeInt(payload, offset, 0);

        LoadTestPayloads.JoinResult result = LoadTestPayloads.parseJoinResponse(payload);

        assertThat(result.code()).isZero();
        assertThat(result.roomId()).isEqualTo("room-0");
        assertThat(result.entityId()).isEqualTo(7);
        assertThat(result.redirectNodeId()).isEmpty();
        assertThat(result.redirectAddress()).isEmpty();
    }

    @Test
    @DisplayName("should_parseRedirectFields_when_responseContainsThem")
    void should_parseRedirectFields_when_responseContainsThem() {
        byte[] payload = new byte[4 + 4 + BinaryCodec.stringSize("room-0") + 4 + 4
            + BinaryCodec.stringSize("node-2") + BinaryCodec.stringSize("game-node-2:9090")];
        int offset = 0;
        BinaryCodec.writeInt(payload, offset, 10); offset += 4;
        BinaryCodec.writeInt(payload, offset, -3); offset += 4;
        BinaryCodec.writeString(payload, offset, "room-0"); offset += BinaryCodec.stringSize("room-0");
        BinaryCodec.writeInt(payload, offset, -1); offset += 4;
        BinaryCodec.writeInt(payload, offset, 0); offset += 4;
        BinaryCodec.writeString(payload, offset, "node-2"); offset += BinaryCodec.stringSize("node-2");
        BinaryCodec.writeString(payload, offset, "game-node-2:9090");

        LoadTestPayloads.JoinResult result = LoadTestPayloads.parseJoinResponse(payload);

        assertThat(result.code()).isEqualTo(-3);
        assertThat(result.redirectNodeId()).isEqualTo("node-2");
        assertThat(result.redirectAddress()).isEqualTo("game-node-2:9090");
    }
}
