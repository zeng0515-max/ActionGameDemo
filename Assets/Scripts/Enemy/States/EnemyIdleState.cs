using UnityEngine;
using ActionGameDemo.StateMachine;

namespace ActionGameDemo.Enemy
{
    /// <summary>
    /// 待机状态：停留原地，检测到玩家后进入追击。
    /// 超时后自动进入巡逻。
    /// </summary>
    public class EnemyIdleState : EnemyState
    {
        private float _idleTimer;

        public EnemyIdleState(EnemyController owner, StateMachine<EnemyController> stateMachine)
            : base(owner, stateMachine) { }

        public override void EnterState()
        {
            _idleTimer = 0f;
            owner.StopMoving();
        }

        public override void UpdateState()
        {
            _idleTimer += Time.deltaTime;
        }

        public override void CheckSwitchState()
        {
            // 死亡检查
            if (Stats != null && Stats.CurrentHealth <= 0f)
            {
                SwitchTo(EnemyStateType.Dead);
                return;
            }

            // 检测到玩家 → 追击
            if (owner.DetectTarget())
            {
                SwitchTo(EnemyStateType.Chase);
                return;
            }

            // 低血量 → 逃跑
            if (owner.ShouldFlee() && owner.HasTarget)
            {
                SwitchTo(EnemyStateType.Flee);
                return;
            }

            // 待机超时 → 巡逻
            float waitTime = owner.PatrolWaitTime;
            if (_idleTimer >= waitTime)
            {
                SwitchTo(EnemyStateType.Patrol);
                return;
            }
        }

        public override void ExitState()
        {
        }
    }
}
