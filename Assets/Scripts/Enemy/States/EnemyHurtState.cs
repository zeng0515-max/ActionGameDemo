using UnityEngine;
using ActionGameDemo.StateMachine;

namespace ActionGameDemo.Enemy
{
    /// <summary>
    /// 受击状态：短暂硬直后恢复之前的行为。
    /// </summary>
    public class EnemyHurtState : EnemyState
    {
        private float _hurtTimer;

        public EnemyHurtState(EnemyController owner, StateMachine<EnemyController> stateMachine)
            : base(owner, stateMachine) { }

        public override void EnterState()
        {
            _hurtTimer = 0f;
            owner.StopMoving();
            SetAnimTrigger("Hurt");
        }

        public override void UpdateState()
        {
            _hurtTimer += Time.deltaTime;
        }

        public override void CheckSwitchState()
        {
            // 死亡检查
            if (Stats != null && Stats.CurrentHealth <= 0f)
            {
                SwitchTo(EnemyStateType.Dead);
                return;
            }

            // 受击恢复后
            if (_hurtTimer >= owner.HurtRecoveryTime)
            {
                // 低血量 → 逃跑
                if (owner.ShouldFlee() && owner.HasTarget)
                {
                    SwitchTo(EnemyStateType.Flee);
                    return;
                }

                // 有目标 → 追击
                if (owner.HasTarget && owner.DetectTarget())
                {
                    SwitchTo(EnemyStateType.Chase);
                    return;
                }

                // 无目标 → 待机
                SwitchTo(EnemyStateType.Idle);
            }
        }

        public override void ExitState()
        {
        }
    }
}
