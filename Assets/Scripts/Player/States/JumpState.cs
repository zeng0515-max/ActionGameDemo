using UnityEngine;
using ActionGameDemo.StateMachine;

namespace ActionGameDemo.Player
{
    public class JumpState : PlayerState
    {
        public JumpState(PlayerController owner, StateMachine<PlayerController> stateMachine)
            : base(owner, stateMachine) { }

        public override void EnterState()
        {
            IsInterrupted = false;
            owner.ApplyJump();
            SetAnimBool("IsJumping", true);
        }

        public override void UpdateState()
        {
            owner.HandleMovementInput();
            owner.ApplyGravity();
            owner.ExecuteMovement();
            owner.HandleRotation();
        }

        public override void CheckSwitchState()
        {
            if (IsInterrupted) return;

            if (InputHandler.AttackPressed)
            {
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

            if (IsGrounded && owner.VerticalVelocity <= 0f)
            {
                SetAnimBool("IsJumping", false);
                if (InputHandler.MoveInput.sqrMagnitude > 0.01f)
                    SwitchTo(PlayerStateType.Move);
                else
                    SwitchTo(PlayerStateType.Idle);
                return;
            }
        }

        public override void ExitState()
        {
            SetAnimBool("IsJumping", false);
        }
    }
}
