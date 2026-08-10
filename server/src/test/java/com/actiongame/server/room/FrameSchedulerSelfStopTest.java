package com.actiongame.server.room;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("帧调度器自我停止")
class FrameSchedulerSelfStopTest {

    @Test
    @DisplayName("should_stopWithoutDeadlock_when_stoppedFromSchedulerThread")
    void should_stopWithoutDeadlock_when_stoppedFromSchedulerThread() throws Exception {
        AtomicReference<FrameScheduler> ref = new AtomicReference<>();
        CountDownLatch executed = new CountDownLatch(1);
        FrameScheduler scheduler = new FrameScheduler((frameIndex, deltaTime) -> {
            ref.get().stop();
            executed.countDown();
        }, 20);
        ref.set(scheduler);

        scheduler.start();

        assertThat(executed.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(scheduler.isRunning()).isFalse();
    }
}
