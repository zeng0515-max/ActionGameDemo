using UnityEngine;
using ActionGameDemo.StateMachine;

namespace ActionGameDemo.Enemy
{
    /// <summary>
    /// 巡逻状态：在出生点周围随机移动，到达后等待。
    /// 检测到玩家后进入追击。
    /// </summary>
    public class PatrolState : EnemyState
    {
        private Vector3 _patrolDestination;
        private bool _isMoving;
        private float _waitTimer;

        public PatrolState(EnemyController owner, StateMachine<EnemyController> stateMachine)
            : base(owner, stateMachine) { }

        public override void EnterState()
        {
            _waitTimer = 0f;
            _isMoving = false;
            PickNewDestination();
        }

        public override void UpdateState()
        {
            if (_isMoving)
            {
                // 检查是否到达目的地
                if (owner.HasReachedPosition(_patrolDestination))
                {
                    _isMoving = false;
                    owner.StopMoving();
                    _waitTimer = 0f;
                }
            }
            else
            {
                _waitTimer += Time.deltaTime;
            }
        }

        public override void CheckSwitchState()
        {
            // 死亡检查
            if (Stats != null && Stats.CurrentHealth <= 0f)
            {
                SwitchTo(EnemyStateType.Dead);
                return;
            }

            // 检测到玩家 → 追击
            if (owner.DetectTarget())
            {
                SwitchTo(EnemyStateType.Chase);
                return;
            }

            // 低血量 → 逃跑
            if (owner.ShouldFlee() && owner.HasTarget)
            {
                SwitchTo(EnemyStateType.Flee);
                return;
            }

            // 等待超时 → 选择新巡逻点
            if (!_isMoving && _waitTimer >= owner.PatrolWaitTime)
            {
                PickNewDestination();
            }
        }

        public override void ExitState()
        {
        }

        private void PickNewDestination()
        {
            _patrolDestination = owner.GetRandomPatrolPoint();
            owner.MoveTo(_patrolDestination);
            _isMoving = true;
        }
    }
}
