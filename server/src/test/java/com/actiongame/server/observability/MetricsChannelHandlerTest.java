package com.actiongame.server.observability;

import com.actiongame.server.audit.ServerMetrics;
import com.actiongame.server.net.util.MessageHelper;
import com.actiongame.server.proto.MessageWrapperProto.MessageId;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Netty 指标采集")
class MetricsChannelHandlerTest {

    @Test
    @DisplayName("should_countInboundAndOutboundMessages")
    void should_countInboundAndOutboundMessages() {
        ServerMetrics metrics = new ServerMetrics();
        MetricsChannelHandler handler = new MetricsChannelHandler(metrics);
        EmbeddedChannel channel = new EmbeddedChannel(handler);

        channel.writeInbound(MessageHelper.wrap(MessageId.HEARTBEAT_REQ, 1, new byte[0]));
        channel.writeOutbound(MessageHelper.wrap(MessageId.HEARTBEAT_RESP, 1, new byte[0]));

        assertThat(metrics.getTotalMessagesReceived()).isEqualTo(1);
        assertThat(metrics.getTotalMessagesSent()).isEqualTo(1);
    }
}
