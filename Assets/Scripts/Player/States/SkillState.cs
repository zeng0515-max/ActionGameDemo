using UnityEngine;
using ActionGameDemo.StateMachine;
using ActionGameDemo.Combat;

namespace ActionGameDemo.Player
{
    public class SkillState : PlayerState
    {
        private float _stateTimer;
        private float _skillDuration = 0.53f;

        public SkillState(PlayerController owner, StateMachine<PlayerController> stateMachine)
            : base(owner, stateMachine) { }

        public override void EnterState()
        {
            IsInterrupted = false;
            _stateTimer = 0f;
            owner.ConsumeSkillCooldown();
            SetAnimTrigger("Skill");
            owner.AddUltimateEnergy(10f);

            // 元素染色的技能释放特效
            var elem = owner.GetElementType();
            var elemColor = ElementSystem.GetElementColor(elem);
            if (elem != ElementType.None)
                VFXEffectManager.Instance?.PlayEffectTinted(VFXEffectType.SkillCast, owner.transform.position + Vector3.up * 0.5f, owner.transform.rotation, elemColor);
            else
                VFXEffectManager.Instance?.PlayEffectFollow(VFXEffectType.SkillCast, owner.transform, Vector3.up * 0.5f, owner.transform.rotation);
        }

        public override void UpdateState()
        {
            _stateTimer += Time.deltaTime;
            owner.ApplyGravity();
            owner.ExecuteMovement();
        }

        public override void CheckSwitchState()
        {
            if (owner.IsDead) { SwitchTo(PlayerStateType.Dead); return; }
            if (IsInterrupted) return;

            if (_stateTimer >= _skillDuration)
            {
                if (InputHandler.MoveInput.sqrMagnitude > 0.01f)
                    SwitchTo(PlayerStateType.Move);
                else
                    SwitchTo(PlayerStateType.Idle);
            }
        }

        public override void ExitState()
        {
            // 安全释放攻击锁，防止动画事件丢失导致永久锁定
            owner.ReleaseAttackLock();
        }
    }
}
