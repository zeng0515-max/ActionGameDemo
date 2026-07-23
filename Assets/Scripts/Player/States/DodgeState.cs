using UnityEngine;
using ActionGameDemo.StateMachine;
using ActionGameDemo.Combat;

namespace ActionGameDemo.Player
{
    public class DodgeState : PlayerState
    {
        private float _stateTimer;
        private float _dodgeDuration = 0.4f;
        private float _dodgeDistance = 3f;
        private Vector3 _dodgeDirection;

        public DodgeState(PlayerController owner, StateMachine<PlayerController> stateMachine)
            : base(owner, stateMachine) { }

        public override void EnterState()
        {
            IsInterrupted = false;
            _stateTimer = 0f;
            _dodgeDirection = owner.GetCurrentMovementDirection();
            if (_dodgeDirection.sqrMagnitude < 0.01f)
                _dodgeDirection = owner.transform.forward;
            owner.ClearMovementDirection();
            owner.SetInvincible(true);
            SetAnimTrigger("Dodge");

            
            VFXEffectManager.Instance?.PlayEffect(VFXEffectType.Dodge, owner.transform.position + Vector3.up, owner.transform.rotation);
        }

        public override void UpdateState()
        {
            _stateTimer += Time.deltaTime;

            float progress = _stateTimer / _dodgeDuration;
            if (progress < 1f)
            {
                float speedMultiplier = progress < 0.5f
                    ? Mathf.Lerp(0f, 1f, progress * 2f)
                    : Mathf.Lerp(1f, 0f, (progress - 0.5f) * 2f);

                Vector3 dodgeMovement = _dodgeDirection * (_dodgeDistance / _dodgeDuration) * speedMultiplier * Time.deltaTime;
                dodgeMovement.y = 0f;
                owner.SetAdditionalMovement(dodgeMovement);
            }

            owner.ApplyGravity();
            owner.ExecuteMovement();

            if (_dodgeDirection.sqrMagnitude > 0.01f)
            {
                Quaternion targetRot = Quaternion.LookRotation(_dodgeDirection);
                owner.transform.rotation = Quaternion.Slerp(owner.transform.rotation, targetRot, 15f * Time.deltaTime);
            }
        }

        public override void CheckSwitchState()
        {
            // 缺陷-18 修复：闪避后半段可缓冲攻击/技能
            if (_stateTimer >= _dodgeDuration * 0.6f)
            {
                if (InputHandler.AttackPressed)
                {
                    InputHandler.ConsumeAttack();
                    Buffer.BufferInput(BufferedAction.Attack);
                }
                else if (InputHandler.SkillPressed && owner.IsSkillReady)
                {
                    InputHandler.ConsumeSkill();
                    Buffer.BufferInput(BufferedAction.Skill);
                }
            }

            if (_stateTimer >= _dodgeDuration)
            {
                if (Buffer.HasAction(BufferedAction.Attack))
                {
                    Buffer.ConsumeBufferedAction();
                    SwitchTo(PlayerStateType.Attack);
                    return;
                }
                if (Buffer.HasAction(BufferedAction.Skill))
                {
                    Buffer.ConsumeBufferedAction();
                    SwitchTo(PlayerStateType.Skill);
                    return;
                }

                if (InputHandler.MoveInput.sqrMagnitude > 0.01f)
                    SwitchTo(PlayerStateType.Move);
                else
                    SwitchTo(PlayerStateType.Idle);
            }
        }

        public override void ExitState()
        {
            owner.SetInvincible(false);
            owner.ClearAdditionalMovement();
        }
    }
}
