package com.actiongame.server.net.handler;

import com.actiongame.server.benchmark.LoadTestPayloads;
import com.actiongame.server.net.session.ConnectionManager;
import com.actiongame.server.net.session.GameSession;
import com.actiongame.server.net.util.MessageHelper;
import com.actiongame.server.persistence.InMemoryMatchResultRepository;
import com.actiongame.server.persistence.InMemoryRoomStateCache;
import com.actiongame.server.persistence.InMemoryStringStore;
import com.actiongame.server.persistence.StringStore;
import com.actiongame.server.proto.MessageWrapperProto.MessageId;
import com.actiongame.server.proto.MessageWrapperProto.MessageWrapper;
import com.actiongame.server.room.RoomManager;
import com.actiongame.server.routing.NodeRegistry;
import com.actiongame.server.routing.RoomRouter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JoinRoom 房间路由")
class JoinRoomHandlerTest {

    @Test
    @DisplayName("should_respondWithRedirect_whenRoomBelongsToAnotherNode")
    void should_respondWithRedirect_whenRoomBelongsToAnotherNode() throws Exception {
        StringStore store = new InMemoryStringStore();
        RoomManager nodeA = new RoomManager(
            new InMemoryMatchResultRepository(),
            new InMemoryRoomStateCache(),
            new RoomRouter(store, "node-a"),
            new NodeRegistry(store, "node-a", "game-a:9090", 3600));
        RoomManager nodeB = new RoomManager(
            new InMemoryMatchResultRepository(),
            new InMemoryRoomStateCache(),
            new RoomRouter(store, "node-b"),
            new NodeRegistry(store, "node-b", "game-b:9090", 3600));
        nodeA.createRoom("room-redirect-test");

        ConnectionManager connectionManager = new ConnectionManager();
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast("dummy", new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) {
            }
        });
        GameSession session = connectionManager.createSession(channel);
        session.setAuthenticated(true);

        JoinRoomHandler handler = new JoinRoomHandler(connectionManager, null, nodeB);
        byte[] payload = LoadTestPayloads.buildJoinRoomPayload(session.getPlayerId(), "room-redirect-test");
        MessageWrapper request = MessageHelper.wrap(MessageId.JOIN_ROOM_REQ, 1, payload);
        handler.handle(channel.pipeline().firstContext(), request, payload);

        MessageWrapper response = channel.readOutbound();
        assertThat(response).isNotNull();
        assertThat(response.getMessageId()).isEqualTo(MessageId.JOIN_ROOM_RESP);

        LoadTestPayloads.JoinResult result = LoadTestPayloads.parseJoinResponse(response.getPayload().toByteArray());
        assertThat(result.code()).isEqualTo(-3);
        assertThat(result.roomId()).isEqualTo("room-redirect-test");
        assertThat(result.redirectNodeId()).isEqualTo("node-a");
        assertThat(result.redirectAddress()).isEqualTo("game-a:9090");

        nodeA.destroyRoom("room-redirect-test");
        channel.finishAndReleaseAll();
    }
}
