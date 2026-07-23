using UnityEngine;
using ActionGameDemo.StateMachine;

namespace ActionGameDemo.Player
{
    public class IdleState : PlayerState
    {
        public IdleState(PlayerController owner, StateMachine<PlayerController> stateMachine)
            : base(owner, stateMachine) { }

        public override void EnterState()
        {
            IsInterrupted = false;
            SetAnimBool("IsMoving", false);
        }

        public override void UpdateState()
        {
            owner.ApplyGravity();
            owner.ExecuteMovement();
        }

        public override void CheckSwitchState()
        {
            if (IsInterrupted) return;

            if (InputHandler.AttackPressed)
            {
                // 攻击锁检查：攻击期间禁止重复发起攻击
                if (owner.IsAttackLocked) return;
                InputHandler.ConsumeAttack();
                Buffer.BufferInput(BufferedAction.Attack);
                SwitchTo(PlayerStateType.Attack);
                return;
            }

            if (InputHandler.SkillPressed && owner.IsSkillReady)
            {
                InputHandler.ConsumeSkill();
                SwitchTo(PlayerStateType.Skill);
                return;
            }

            if (InputHandler.UltimatePressed && owner.IsUltimateReady)
            {
                InputHandler.ConsumeUltimate();
                SwitchTo(PlayerStateType.Ultimate);
                return;
            }

            if (InputHandler.DodgePressed && IsGrounded)
            {
                InputHandler.ConsumeDodge();
                Buffer.BufferInput(BufferedAction.Dodge);
                SwitchTo(PlayerStateType.Dodge);
                return;
            }

            if (InputHandler.JumpPressed && IsGrounded)
            {
                InputHandler.ConsumeJump();
                SwitchTo(PlayerStateType.Jump);
                return;
            }

            if (InputHandler.MoveInput.sqrMagnitude > 0.01f)
            {
                SwitchTo(PlayerStateType.Move);
                return;
            }
        }

        public override void ExitState() { }
    }
}
