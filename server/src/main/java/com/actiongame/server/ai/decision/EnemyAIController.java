package com.actiongame.server.ai.decision;

import com.actiongame.server.ai.fsm.*;
import com.actiongame.server.ai.pathfinding.SimplePathfinder;
import com.actiongame.server.battle.combatsystem.AggroSystem;
import com.actiongame.server.config.MonsterConfig;
import com.actiongame.server.domain.character.Character;
import com.actiongame.server.domain.character.MonsterCharacter;
import com.actiongame.server.util.Vector3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;

/**
 * 敌人AI控制器 (对应Unity EnemyController的AI决策部分)
 * 管理FSM状态机、目标检测、移动决策
 * 服务端权威: 所有AI决策在服务端执行, 客户端仅做表现
 */
public class EnemyAIController {
    private static final Logger log = LoggerFactory.getLogger(EnemyAIController.class);

    protected final MonsterCharacter monster;
    protected final MonsterConfig config;
    protected final SimplePathfinder pathfinder;
    protected final TargetSelector targetSelector;
    protected final AggroSystem aggroSystem;

    protected final Map<EnemyStateType, EnemyState> states;
    protected EnemyStateType currentStateType = EnemyStateType.IDLE;
    protected EnemyState currentState;

    protected Character target;
    protected Vector3 patrolOrigin;
    private boolean patrolOriginSet;

    protected float hurtRecoveryTime = 0.5f;
    protected float patrolArrivalThreshold = 0.5f;
    protected float patrolWaitTime = 2f;
    protected float moveSpeed;

    public EnemyAIController(MonsterCharacter monster, MonsterConfig config) {
        this.monster = monster;
        this.config = config;
        this.pathfinder = new SimplePathfinder();
        this.aggroSystem = new AggroSystem(monster.getEntityId());
        this.targetSelector = new TargetSelector(aggroSystem,
            config != null ? config.getDetectionRange() : 10f,
            config != null ? config.getViewAngle() : 90f);
        this.moveSpeed = config != null ? config.getMoveSpeed() : 3f;

        if (config != null) {
            this.hurtRecoveryTime = 0.5f;
            this.patrolWaitTime = 2f;
        }

        this.states = new EnumMap<>(EnemyStateType.class);
        registerStates();
    }

    private void registerStates() {
        states.put(EnemyStateType.IDLE, new IdleState(this));
        states.put(EnemyStateType.PATROL, new PatrolState(this));
        states.put(EnemyStateType.CHASE, new ChaseState(this));
        states.put(EnemyStateType.FLEE, new FleeState(this));
        states.put(EnemyStateType.HURT, new HurtState(this));
        states.put(EnemyStateType.DEAD, new DeadState(this));
    }

    /**
     * 初始化AI, 进入Idle状态
     */
    public void initialize() {
        currentStateType = EnemyStateType.IDLE;
        currentState = states.get(EnemyStateType.IDLE);
        currentState.onEnter();
    }

    /**
     * 每帧更新AI
     */
    public void update(float deltaTime) {
        if (currentState == null) return;

        // 死亡检查优先于状态更新
        if (monster.isDead() && currentStateType != EnemyStateType.DEAD) {
            changeState(EnemyStateType.DEAD);
            return;
        }

        currentState.onUpdate(deltaTime);
        checkSwitchState();
    }

    /**
     * 检查状态切换条件
     */
    private void checkSwitchState() {
        if (currentState instanceof IdleState) ((IdleState) currentState).checkSwitchState();
        else if (currentState instanceof PatrolState) ((PatrolState) currentState).checkSwitchState();
        else if (currentState instanceof ChaseState) ((ChaseState) currentState).checkSwitchState();
        else if (currentState instanceof FleeState) ((FleeState) currentState).checkSwitchState();
        else if (currentState instanceof HurtState) ((HurtState) currentState).checkSwitchState();
        else if (currentState instanceof DeadState) ((DeadState) currentState).checkSwitchState();
    }

    /**
     * 切换状态
     */
    public void changeState(EnemyStateType type) {
        if (!states.containsKey(type)) {
            log.error("Unknown state type: {}", type);
            return;
        }

        if (currentState != null) currentState.onExit();
        currentStateType = type;
        currentState = states.get(type);
        currentState.onEnter();
    }

    // === 目标检测 ===

    public boolean detectTarget() {
        if (target == null) return false;
        return targetSelector.isTargetInDetectionRange(monster, target);
    }

    public boolean isTargetInChaseRange() {
        return targetSelector.isTargetInChaseRange(monster, target);
    }

    public boolean shouldFlee() {
        float threshold = config != null ? config.getFleeThreshold() : 0.2f;
        return targetSelector.shouldFlee(monster, threshold);
    }

    public boolean hasTarget() { return target != null; }
    public boolean isTargetDead() { return target != null && target.isDead(); }
    public Character getTarget() { return target; }
    public Vector3 getTargetPosition() { return target != null ? target.getPosition() : Vector3.ZERO; }

    public void setTarget(Character target) { this.target = target; }
    public void clearTarget() { this.target = null; }

    // === 移动 ===

    public void moveTo(Vector3 destination) {
        Vector3 newPos = pathfinder.moveTowards(monster.getPosition(), destination, moveSpeed, 0.02f);
        monster.setPosition(newPos);
    }

    public void stopMoving() {
        // 服务端AI无物理引擎, 停止移动即保持当前位置
    }

    public void fleeFromTarget() {
        if (target == null) return;
        Vector3 fleeDest = pathfinder.getFleeDestination(monster.getPosition(), target.getPosition(), 5f);
        moveTo(fleeDest);
    }

    public boolean hasReached(Vector3 destination) {
        return pathfinder.hasReached(monster.getPosition(), destination, patrolArrivalThreshold);
    }

    // === 巡逻 ===

    public Vector3 getPatrolOrigin() {
        if (!patrolOriginSet) {
            patrolOrigin = monster.getPosition();
            patrolOriginSet = true;
        }
        return patrolOrigin;
    }

    public Vector3 getRandomPatrolPoint() {
        float radius = config != null ? config.getPatrolRadius() : 5f;
        return pathfinder.getRandomPatrolPoint(getPatrolOrigin(), radius);
    }

    // === 受击与死亡 ===

    public void onHurt() {
        if (monster.isDead()) return;
        changeState(EnemyStateType.HURT);
    }

    public void onDeath() {
        changeState(EnemyStateType.DEAD);
    }

    // === Getters ===

    public boolean isDead() { return monster.isDead(); }
    public MonsterCharacter getMonster() { return monster; }
    public EnemyStateType getCurrentStateType() { return currentStateType; }
    public boolean isInState(EnemyStateType type) { return currentStateType == type; }
    public float getPatrolWaitTime() { return patrolWaitTime; }
    public float getHurtRecoveryTime() { return hurtRecoveryTime; }
    public MonsterConfig getConfig() { return config; }
}
