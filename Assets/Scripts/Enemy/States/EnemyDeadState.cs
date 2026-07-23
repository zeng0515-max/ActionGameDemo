using UnityEngine;
using ActionGameDemo.StateMachine;

namespace ActionGameDemo.Enemy
{
    /// <summary>
    /// 死亡状态：停止移动，播放死亡动画，等待销毁。
    /// 实际销毁由 CharacterBase.Die 的延迟协程处理。
    /// </summary>
    public class EnemyDeadState : EnemyState
    {
        public EnemyDeadState(EnemyController owner, StateMachine<EnemyController> stateMachine)
            : base(owner, stateMachine) { }

        public override void EnterState()
        {
            owner.StopMoving();
            SetAnimTrigger("Dead");
        }

        public override void UpdateState()
        {
            // 死亡状态不执行逻辑更新
        }

        public override void CheckSwitchState()
        {
            // 死亡是终态，不切换
        }

        public override void ExitState()
        {
        }
    }
}
