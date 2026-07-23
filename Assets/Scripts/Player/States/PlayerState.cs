using UnityEngine;
using ActionGameDemo.StateMachine;
using ActionGameDemo.Character;

namespace ActionGameDemo.Player
{
    public enum PlayerStateType
    {
        Idle, Move, Jump, Attack, Skill, Ultimate, Dodge, Hurt, Dead
    }

    /// <summary>
    /// 玩家状态基类：继承泛型 BaseState，提供快捷访问和中断标志。
    /// 优化-23 修复：Input 属性改名为 InputHandler 避免遮挡 UnityEngine.Input。
    /// </summary>
    public abstract class PlayerState : BaseState<PlayerController>
    {
        protected bool IsInterrupted;

        protected PlayerState(PlayerController owner, StateMachine<PlayerController> stateMachine)
            : base(owner, stateMachine) { }

        // 优化-23 修复：Input → InputHandler，避免与 UnityEngine.Input 冲突
        protected CharacterController CC => owner.CharacterController;
        protected PlayerInputHandler InputHandler => owner.InputHandler;
        protected Animator Anim => owner.Animator;
        protected CharacterStats Stats => owner.Stats;
        protected InputBuffer Buffer => owner.InputBuffer;
        protected bool IsGrounded => owner.IsGrounded;

        protected void SwitchTo(PlayerStateType type)
        {
            owner.StateMachine.ChangeState(type);
        }

        private bool HasAnimator => Anim != null && Anim.runtimeAnimatorController != null;

        /// <summary>清除所有 Trigger 参数，防止残留 Trigger 干扰状态切换</summary>
        protected void ResetTriggers()
        {
            if (!HasAnimator) return;
            foreach (var param in Anim.parameters)
            {
                if (param.type == AnimatorControllerParameterType.Trigger)
                    Anim.ResetTrigger(param.name);
            }
        }

        protected void SetAnimTrigger(string name)
        {
            if (!HasAnimator) return;
            ResetTriggers();
            Anim.SetTrigger(name);
        }

        protected void SetAnimBool(string name, bool value)
        {
            if (HasAnimator) Anim.SetBool(name, value);
        }

        protected void SetAnimFloat(string name, float value)
        {
            if (HasAnimator) Anim.SetFloat(name, value);
        }
    }
}
