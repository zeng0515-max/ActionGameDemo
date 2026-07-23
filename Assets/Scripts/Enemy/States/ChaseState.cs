using UnityEngine;
using ActionGameDemo.StateMachine;

namespace ActionGameDemo.Enemy
{
    /// <summary>
    /// 追击状态：使用 NavMeshAgent 追踪玩家。
    /// 玩家脱离追击范围后回到待机。敌人不攻击，仅追踪。
    /// </summary>
    public class ChaseState : EnemyState
    {
        private float _pathUpdateTimer;
        private const float PATH_UPDATE_INTERVAL = 0.3f;

        public ChaseState(EnemyController owner, StateMachine<EnemyController> stateMachine)
            : base(owner, stateMachine) { }

        public override void EnterState()
        {
            _pathUpdateTimer = 0f;
        }

        public override void UpdateState()
        {
            // 定期更新路径，避免每帧 SetDestination 开销
            _pathUpdateTimer += Time.deltaTime;
            if (_pathUpdateTimer >= PATH_UPDATE_INTERVAL)
            {
                _pathUpdateTimer = 0f;
                if (Target != null)
                {
                    owner.MoveTo(Target.position);
                }
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

            // 目标丢失或死亡 → 待机
            if (!owner.HasTarget || owner.TargetIsDead)
            {
                owner.SetTarget(null);
                SwitchTo(EnemyStateType.Idle);
                return;
            }

            // 低血量 → 逃跑
            if (owner.ShouldFlee())
            {
                SwitchTo(EnemyStateType.Flee);
                return;
            }

            // 目标脱离追击范围 → 待机
            if (!owner.TargetInChaseRange())
            {
                SwitchTo(EnemyStateType.Idle);
                return;
            }
        }

        public override void ExitState()
        {
        }
    }
}
