using UnityEngine;
using UnityEngine.AI;
using ActionGameDemo.StateMachine;
using ActionGameDemo.Character;

namespace ActionGameDemo.Enemy
{
    /// <summary>敌人状态类型枚举</summary>
    public enum EnemyStateType
    {
        Idle, Patrol, Chase, Flee, Hurt, Dead
    }

    /// <summary>
    /// 敌人状态基类：提供快捷访问和动画触发器管理。
    /// 与 PlayerState 结构对称。
    /// </summary>
    public abstract class EnemyState : BaseState<EnemyController>
    {
        protected EnemyState(EnemyController owner, StateMachine<EnemyController> stateMachine)
            : base(owner, stateMachine) { }

        protected NavMeshAgent Agent => owner.Agent;
        protected Animator Anim => owner.Anim;
        protected CharacterStats Stats => owner.Stats;
        protected Transform Target => owner.Target;

        protected void SwitchTo(EnemyStateType type)
        {
            owner.StateMachine.ChangeState(type);
        }

        private bool HasAnimator => Anim != null && Anim.runtimeAnimatorController != null;

        /// <summary>清除所有 Trigger 参数，防止残留</summary>
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
