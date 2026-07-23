using UnityEngine;
using ActionGameDemo.StateMachine;

namespace ActionGameDemo.Enemy
{
    /// <summary>
    /// 逃跑状态：远离玩家，直到脱离检测范围或恢复血量。
    /// </summary>
    public class FleeState : EnemyState
    {
        private float _fleeTimer;
        private float _fleeUpdateTimer;
        private const float FLEE_UPDATE_INTERVAL = 0.2f;
        private const float FLEE_MAX_DURATION = 5f;

        public FleeState(EnemyController owner, StateMachine<EnemyController> stateMachine)
            : base(owner, stateMachine) { }

        public override void EnterState()
        {
            _fleeTimer = 0f;
            _fleeUpdateTimer = 0f;
        }

        public override void UpdateState()
        {
            _fleeTimer += Time.deltaTime;
            _fleeUpdateTimer += Time.deltaTime;

            // 定期更新逃跑方向
            if (_fleeUpdateTimer >= FLEE_UPDATE_INTERVAL)
            {
                _fleeUpdateTimer = 0f;
                UpdateFleeDestination();
            }
        }

        public override void CheckSwitchState()
        {
            // 死亡检查
            if (Stats != null && Stats.CurrentHealth <= 0f)
            {
                SwitchTo(EnemyStateType.Dead);
                return;
            }

            // 逃跑超时或脱离检测范围 → 待机
            if (_fleeTimer >= FLEE_MAX_DURATION || !owner.DetectTarget())
            {
                SwitchTo(EnemyStateType.Idle);
                return;
            }

            // 血量恢复到安全水平（不再低于逃跑阈值）→ 待机
            if (!owner.ShouldFlee())
            {
                SwitchTo(EnemyStateType.Idle);
                return;
            }
        }

        public override void ExitState()
        {
            owner.StopMoving();
        }

        private void UpdateFleeDestination()
        {
            if (Target == null) return;

            // 计算远离玩家的方向
            Vector3 fleeDir = (owner.transform.position - Target.position).normalized;
            // 添加随机偏移避免直线逃跑
            Vector3 randomOffset = Random.insideUnitSphere * 2f;
            randomOffset.y = 0f;
            Vector3 fleeDestination = owner.transform.position + (fleeDir * 5f) + randomOffset;

            owner.MoveTo(fleeDestination);
        }
    }
}
