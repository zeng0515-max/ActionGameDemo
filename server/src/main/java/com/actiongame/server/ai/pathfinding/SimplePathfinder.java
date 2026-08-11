package com.actiongame.server.ai.pathfinding;

import com.actiongame.server.util.MathUtils;
import com.actiongame.server.util.RandomUtil;
import com.actiongame.server.util.Vector3;

/**
 * 简化寻路系统 (Demo轻量化方案)
 *
 * 【技术标注】此处Java服务端寻路为Demo轻量化方案，采用直线导航替代Unity NavMesh。
 * 生产环境应替换为专业寻路库（如Recast Navigation）或独立寻路微服务。
 * Demo阶段直线导航满足验证需求，面试时需主动说明这是轻量化替代方案并阐述生产环境的演进路线。
 */
public class SimplePathfinder {

    /**
     * 生成巡逻随机点 (在原点周围的圆内)
     * @param origin 巡逻原点
     * @param radius 巡逻半径
     * @return 随机巡逻点
     */
    public Vector3 getRandomPatrolPoint(Vector3 origin, float radius) {
        float angle = RandomUtil.nextFloat() * (float) (Math.PI * 2);
        float dist = RandomUtil.nextFloat() * radius;
        return new Vector3(
            origin.x + (float) Math.cos(angle) * dist,
            origin.y,
            origin.z + (float) Math.sin(angle) * dist
        );
    }

    /**
     * 直线导航: 计算从当前位置朝目标移动一步后的位置
     * @param current 当前位置
     * @param target 目标位置
     * @param speed 移动速度 (米/秒)
     * @param deltaTime 时间增量 (秒)
     * @return 移动后的位置
     */
    public Vector3 moveTowards(Vector3 current, Vector3 target, float speed, float deltaTime) {
        Vector3 direction = target.subtract(current);
        float distance = direction.magnitude();
        if (distance < MathUtils.EPSILON) return target;

        float step = Math.min(speed * deltaTime, distance);
        Vector3 moveDir = direction.normalized();
        return current.add(moveDir.multiply(step));
    }

    /**
     * 计算远离方向 (逃跑用)
     * @param self 自身位置
     * @param threat 威胁位置
     * @param distance 逃跑距离
     * @return 逃跑目标点
     */
    public Vector3 getFleeDestination(Vector3 self, Vector3 threat, float distance) {
        Vector3 fleeDir = self.subtract(threat).normalized();
        if (fleeDir.magnitude() < MathUtils.EPSILON) {
            // 重叠时随机方向
            fleeDir = new Vector3(RandomUtil.nextFloat() - 0.5f, 0, RandomUtil.nextFloat() - 0.5f).normalized();
        }
        // 添加随机偏移避免直线逃跑
        float offsetX = (RandomUtil.nextFloat() - 0.5f) * 4f;
        float offsetZ = (RandomUtil.nextFloat() - 0.5f) * 4f;
        return self.add(fleeDir.multiply(distance)).add(new Vector3(offsetX, 0, offsetZ));
    }

    /**
     * 是否到达指定位置
     * @param current 当前位置
     * @param destination 目标位置
     * @param threshold 到达阈值
     * @return 是否已到达
     */
    public boolean hasReached(Vector3 current, Vector3 destination, float threshold) {
        return current.distance(destination) <= threshold;
    }
}
