using UnityEngine;
using ActionGameDemo.StateMachine;
using ActionGameDemo.Combat;

namespace ActionGameDemo.Player
{
    /// <summary>
    /// 攻击状态：处理普攻连招的动画播放、命中检测和状态切换。
    /// 攻击时长和取消点由 PlayerController 配置，可在 Inspector 面板调整。
    /// 攻击期间通过 _isAttackLocked 防止重复发起攻击。
    /// 动画结束通过 OnAnimationEnd 回调自动释放锁并切换状态。
    /// </summary>
    public class AttackState : PlayerState
    {
        private float _stateTimer;
        private int _currentComboStep;

        // 从 PlayerController 获取攻击参数（Inspector 可配）
        private float AttackDuration => owner.AttackDuration;
        private float CancelTime => owner.AttackCancelTime;

        public AttackState(PlayerController owner, StateMachine<PlayerController> stateMachine)
            : base(owner, stateMachine) { }

        public override void EnterState()
        {
            IsInterrupted = false;
            _stateTimer = 0f;
            owner.ClearMovementDirection();

            // 自动面向最近的敌人
            FaceNearestEnemy();

            if (owner.ComboSystem != null)
            {
                _currentComboStep = owner.ComboSystem.TryAdvanceCombo();

                // 构建连段动画触发器名（如 Attack_01, Attack_02）
                string baseName = owner.AttackSkillData != null && !string.IsNullOrEmpty(owner.AttackSkillData.animationTriggerName)
                    ? owner.AttackSkillData.animationTriggerName
                    : "Attack";
                string stepTrigger = $"{baseName}_{_currentComboStep:D2}";

                // 检查 Animator 是否有该触发器，没有则回退到基础名
                bool hasStepTrigger = false;
                if (owner.Animator != null)
                {
                    foreach (var param in owner.Animator.parameters)
                    {
                        if (param.name == stepTrigger && param.type == UnityEngine.AnimatorControllerParameterType.Trigger)
                        {
                            hasStepTrigger = true;
                            break;
                        }
                    }
                }
                SetAnimTrigger(hasStepTrigger ? stepTrigger : baseName);
            }
            else
            {
                _currentComboStep = 0;
                SetAnimTrigger("Attack");
            }
        }

        public override void UpdateState()
        {
            _stateTimer += Time.deltaTime;
            owner.ApplyGravity();
            owner.ExecuteMovement();
        }

        public override void CheckSwitchState()
        {
            if (IsInterrupted) return;

            // 取消点之后允许移动输入中断攻击
            if (_stateTimer >= CancelTime)
            {
                if (InputHandler.MoveInput.sqrMagnitude > 0.01f && !Buffer.HasAction(BufferedAction.Attack))
                {
                    SwitchTo(PlayerStateType.Move);
                    return;
                }
            }

            // 攻击时长达到后，优先检查动画事件回调（OnAnimationEnd 会处理主要切换逻辑）
            // 此处作为安全兜底，防止动画事件丢失导致卡死
            if (_stateTimer >= AttackDuration)
            {
                // 安全检查：若动画事件已触发状态切换（IsInterrupted），不再重复
                if (owner.StateMachine.CurrentStateType != PlayerStateType.Attack) return;

                // 检查缓冲输入
                if (Buffer.HasAction(BufferedAction.Attack))
                {
                    Buffer.ConsumeBufferedAction();
                    SwitchTo(PlayerStateType.Attack);
                    return;
                }
                if (Buffer.HasAction(BufferedAction.Dodge))
                {
                    Buffer.ConsumeBufferedAction();
                    SwitchTo(PlayerStateType.Dodge);
                    return;
                }

                SwitchTo(InputHandler.MoveInput.sqrMagnitude > 0.01f ? PlayerStateType.Move : PlayerStateType.Idle);
                return;
            }

            // 取消点内允许衔接下一段攻击
            if (_stateTimer >= CancelTime && owner.CanCancel)
            {
                if (InputHandler.AttackPressed)
                {
                    InputHandler.ConsumeAttack();
                    Buffer.BufferInput(BufferedAction.Attack);
                }
                else if (InputHandler.DodgePressed && IsGrounded)
                {
                    InputHandler.ConsumeDodge();
                    Buffer.BufferInput(BufferedAction.Dodge);
                }
            }
        }

        public override void ExitState()
        {
            // 释放攻击锁：此处的安全兜底，确保即使动画事件 OnAnimationEnd 丢失
            // 攻击锁也会被释放，防止永久锁定导致无法再次攻击
            owner.ReleaseAttackLock();
        }

        private void FaceNearestEnemy()
        {
            float detectionRange = owner.AttackRange * 2f;
            Vector3 center = owner.transform.position;
            Collider[] hits = Physics.OverlapSphere(center, detectionRange);
            Transform nearest = null;
            float nearestDist = float.MaxValue;

            foreach (var hit in hits)
            {
                if (hit.gameObject == owner.gameObject) continue;
                if (!hit.CompareTag("Enemy")) continue;
                float dist = Vector3.Distance(center, hit.transform.position);
                if (dist < nearestDist)
                {
                    nearestDist = dist;
                    nearest = hit.transform;
                }
            }

            if (nearest != null)
            {
                Vector3 dir = nearest.position - owner.transform.position;
                dir.y = 0f;
                if (dir.sqrMagnitude > 0.01f)
                    owner.transform.rotation = Quaternion.LookRotation(dir);
            }
        }
    }
}
