using UnityEngine;
using ActionGameDemo.StateMachine;
using ActionGameDemo.Combat;

namespace ActionGameDemo.Player
{
    public class UltimateState : PlayerState
    {
        private float _stateTimer;
        private float _ultimateDuration = 0.42f;

        public UltimateState(PlayerController owner, StateMachine<PlayerController> stateMachine)
            : base(owner, stateMachine) { }

        public override void EnterState()
        {
            IsInterrupted = false;
            _stateTimer = 0f;
            owner.ConsumeUltimateEnergy();
            SetAnimTrigger("Ultimate");
            owner.SetInvincible(true);

            // 元素染色的大招释放特效
            var elem = owner.GetElementType();
            var elemColor = ElementSystem.GetElementColor(elem);
            if (elem != ElementType.None)
                VFXEffectManager.Instance?.PlayEffectTinted(VFXEffectType.UltimateCast, owner.transform.position + Vector3.up, owner.transform.rotation, elemColor);
            else
                VFXEffectManager.Instance?.PlayEffect(VFXEffectType.UltimateCast, owner.transform.position + Vector3.up, owner.transform.rotation);
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

            if (_stateTimer >= _ultimateDuration)
            {
                if (InputHandler.MoveInput.sqrMagnitude > 0.01f)
                    SwitchTo(PlayerStateType.Move);
                else
                    SwitchTo(PlayerStateType.Idle);
            }
        }

        public override void ExitState()
        {
            owner.SetInvincible(false);
            // 安全释放攻击锁，防止动画事件丢失导致永久锁定
            owner.ReleaseAttackLock();
        }
    }
}
