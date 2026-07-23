using UnityEngine;
using ActionGameDemo.StateMachine;

namespace ActionGameDemo.Player
{
    public class HurtState : PlayerState
    {
        private float _stateTimer;
        private float _hurtDuration = 0.3f;

        public HurtState(PlayerController owner, StateMachine<PlayerController> stateMachine)
            : base(owner, stateMachine) { }

        public override void EnterState()
        {
            IsInterrupted = false;
            _stateTimer = 0f;
            SetAnimTrigger("Hurt");
            Buffer.Clear();
            owner.ClearMovementDirection();
        }

        public override void UpdateState()
        {
            _stateTimer += Time.deltaTime;
            owner.ApplyGravity();
            owner.ExecuteMovement();
        }

        public override void CheckSwitchState()
        {
            // BUG-04 修复：受击期间死亡时切换到 DeadState
            if (owner.IsDead)
            {
                SwitchTo(PlayerStateType.Dead);
                return;
            }

            if (IsInterrupted) return;

            if (_stateTimer >= _hurtDuration)
            {
                if (InputHandler.MoveInput.sqrMagnitude > 0.01f)
                    SwitchTo(PlayerStateType.Move);
                else
                    SwitchTo(PlayerStateType.Idle);
            }
        }

        public override void ExitState() { }
    }
}
