using UnityEngine;
using ActionGameDemo.StateMachine;

namespace ActionGameDemo.Player
{
    /// <summary>
        /// 死亡状态：锁定输入，播放死亡动画，为最终状态。
    /// </summary>
    public class DeadState : PlayerState
    {
        public DeadState(PlayerController owner, StateMachine<PlayerController> stateMachine)
            : base(owner, stateMachine) { }

        public override void EnterState()
        {
            IsInterrupted = true;
            Buffer.Clear();
            owner.LockInput();
            SetAnimTrigger("Dead");
            SetAnimBool("IsDead", true);
        }

        public override void UpdateState()
        {
            // 死亡后不再处理任何输入和移动
        }

        public override void CheckSwitchState()
        {
            // 死亡为最终状态，不允许切换
        }

        public override void ExitState()
        {
            // 复活时由外部调用
            SetAnimBool("IsDead", false);
            owner.UnlockInput();
        }
    }
}
