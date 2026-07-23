using System.Collections.Generic;
using UnityEngine;
using ActionGameDemo.StateMachine;

namespace ActionGameDemo.Enemy
{
    /// <summary>
    /// 敌人状态机主控：预创建所有状态实例，避免运行时 GC。
    /// 结构与 PlayerStateMachine 对称。
    /// </summary>
    public class EnemyStateMachine
    {
        private readonly StateMachine<EnemyController> _stateMachine;
        private readonly Dictionary<EnemyStateType, BaseState<EnemyController>> _states;
        private EnemyStateType _currentStateType;

        public EnemyStateType CurrentStateType => _currentStateType;

        public EnemyStateMachine(EnemyController owner)
        {
            _stateMachine = new StateMachine<EnemyController>(owner);
            _states = new Dictionary<EnemyStateType, BaseState<EnemyController>>
            {
                { EnemyStateType.Idle, new EnemyIdleState(owner, _stateMachine) },
                { EnemyStateType.Patrol, new PatrolState(owner, _stateMachine) },
                { EnemyStateType.Chase, new ChaseState(owner, _stateMachine) },
                { EnemyStateType.Flee, new FleeState(owner, _stateMachine) },
                { EnemyStateType.Hurt, new EnemyHurtState(owner, _stateMachine) },
                { EnemyStateType.Dead, new EnemyDeadState(owner, _stateMachine) }
            };
        }

        /// <summary>初始化状态机，进入 Idle 状态</summary>
        public void Initialize()
        {
            _currentStateType = EnemyStateType.Idle;
            _stateMachine.Initialize(_states[EnemyStateType.Idle]);
        }

        /// <summary>切换到指定状态</summary>
        public void ChangeState(EnemyStateType type)
        {
            if (!_states.ContainsKey(type))
            {
                Debug.LogError($"[EnemyStateMachine] 未知状态类型: {type}");
                return;
            }

            _currentStateType = type;
            _stateMachine.ChangeState(_states[type]);
        }

        /// <summary>每帧更新当前状态</summary>
        public void Update()
        {
            _stateMachine.Update();
        }

        /// <summary>当前是否处于指定状态</summary>
        public bool IsInState(EnemyStateType type)
        {
            return _currentStateType == type;
        }
    }
}
