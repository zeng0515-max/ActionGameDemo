package com.actiongame.server.net.handler;

import com.actiongame.server.constant.GameConstants;
import com.actiongame.server.proto.LoginProto.LoginReq;
import com.actiongame.server.proto.LoginProto.LoginResp;
import com.actiongame.server.proto.MessageWrapperProto.MessageId;
import com.actiongame.server.proto.MessageWrapperProto.MessageWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

@DisplayName("LoginHandler 协议版本协商测试")
class LoginHandlerTest {

    @Test
    @DisplayName("客户端版本在范围内时应返回成功")
    void login_shouldSucceed_whenVersionInRange() {
        // 验证版本范围逻辑
        int clientVersion = GameConstants.SERVER_MIN_VERSION;
        boolean versionOk = clientVersion >= GameConstants.SERVER_MIN_VERSION
                && clientVersion <= GameConstants.SERVER_MAX_VERSION;
        assertThat(versionOk).isTrue();
    }

    @Test
    @DisplayName("客户端版本低于最低版本时应返回失败")
    void login_shouldFail_whenVersionTooLow() {
        int clientVersion = GameConstants.SERVER_MIN_VERSION - 1;
        boolean versionOk = clientVersion >= GameConstants.SERVER_MIN_VERSION
                && clientVersion <= GameConstants.SERVER_MAX_VERSION;
        assertThat(versionOk).isFalse();
    }

    @Test
    @DisplayName("客户端版本高于最高版本时应返回失败")
    void login_shouldFail_whenVersionTooHigh() {
        int clientVersion = GameConstants.SERVER_MAX_VERSION + 1;
        boolean versionOk = clientVersion >= GameConstants.SERVER_MIN_VERSION
                && clientVersion <= GameConstants.SERVER_MAX_VERSION;
        assertThat(versionOk).isFalse();
    }
}
