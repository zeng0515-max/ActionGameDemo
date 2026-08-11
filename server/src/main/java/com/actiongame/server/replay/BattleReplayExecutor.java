package com.actiongame.server.replay;

import com.actiongame.server.constant.GameConstants;
import com.actiongame.server.room.BattleFrame;
import com.actiongame.server.room.BattleRoom;
import com.actiongame.server.util.RandomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 回放重建器 (对应文档6.2 BattleReplayExecutor)
 *
 * 加载录制数据, 初始化相同配置和随机种子
 * 按帧重放帧循环, 使用确定性随机数 (RandomUtil.SEEDED模式)
 */
public class BattleReplayExecutor {
    private static final Logger log = LoggerFactory.getLogger(BattleReplayExecutor.class);

    private final BattleRecording recording;
    private BattleRoom replayRoom;
    private final List<BattleFrame> generatedFrames;
    private final Consumer<BattleFrame> frameConsumer;

    public BattleReplayExecutor(BattleRecording recording) {
        this(recording, null);
    }

    public BattleReplayExecutor(BattleRecording recording, Consumer<BattleFrame> frameConsumer) {
        this.recording = recording;
        this.frameConsumer = frameConsumer;
        this.generatedFrames = new ArrayList<>();
    }

    /**
     * 执行回放
     */
    public List<BattleFrame> replay() {
        log.info("Starting replay: roomId={}, frames={}, seed={}",
            recording.getRoomId(), recording.getTotalFrames(), recording.getRandomSeed());

        // 保存当前随机模式, 回放后恢复
        RandomUtil.RandomMode prevMode = RandomUtil.getMode();
        long prevSeed = RandomUtil.getSeed();

        try {
            RandomUtil.setMode(RandomUtil.RandomMode.SEEDED, recording.getRandomSeed());

            replayRoom = new BattleRoom(recording.getRoomId());

            float deltaTime = GameConstants.FRAME_INTERVAL_MS / 1000f;

            for (BattleRecording.FrameInput frameInput : recording.getFrames()) {
                for (BattleRecording.PlayerActionRecord action : frameInput.getActions()) {
                    replayRoom.submitPlayerAction(
                        action.getEntityId(),
                        action.getActionType(),
                        action.getMoveX(),
                        action.getMoveZ(),
                        action.getSkillId(),
                        action.getTargetEntityId()
                    );
                }

                replayRoom.executeFrame(frameInput.getFrameIndex(), deltaTime);

                if (frameConsumer != null) {
                    // frameConsumer.accept(frame); // 待 broadcastFrame 传出
                }
            }

            log.info("Replay completed: roomId={}, frames replayed={}",
                recording.getRoomId(), recording.getFrames().size());
        } finally {
            RandomUtil.setMode(prevMode, prevSeed);
        }

        return generatedFrames;
    }

    public BattleRoom getReplayRoom() { return replayRoom; }
    public BattleRecording getRecording() { return recording; }
}
