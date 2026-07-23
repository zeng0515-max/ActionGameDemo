using System.Collections.Generic;
using UnityEngine;
using ActionGameDemo.StateMachine;

namespace ActionGameDemo.Player
{
    /// <summary>
        /// 玩家状态机主控：管理所有状态实例的创建和切换，避免运行时 GC。
    /// 设计原则：
    ///   1. 所有状态在 Initialize 时预创建，运行期间只切换不 new；
    ///   2. ChangeState 为唯一切换入口，通过枚举指定目标状态；
    ///   3. 当前状态类型可通过 CurrentStateType 查询。
    /// </summary>
    public class PlayerStateMachine
    {
        private readonly StateMachine<PlayerController> _stateMachine;
        private readonly Dictionary<PlayerStateType, BaseState<PlayerController>> _states;
        private PlayerStateType _currentStateType;

        public PlayerStateType CurrentStateType => _currentStateType;

        public PlayerStateMachine(PlayerController owner)
        {
            _stateMachine = new StateMachine<PlayerController>(owner);
            _states = new Dictionary<PlayerStateType, BaseState<PlayerController>>
            {
                { PlayerStateType.Idle, new IdleState(owner, _stateMachine) },
                { PlayerStateType.Move, new MoveState(owner, _stateMachine) },
                { PlayerStateType.Jump, new JumpState(owner, _stateMachine) },
                { PlayerStateType.Attack, new AttackState(owner, _stateMachine) },
                { PlayerStateType.Skill, new SkillState(owner, _stateMachine) },
                { PlayerStateType.Ultimate, new UltimateState(owner, _stateMachine) },
                { PlayerStateType.Dodge, new DodgeState(owner, _stateMachine) },
                { PlayerStateType.Hurt, new HurtState(owner, _stateMachine) },
                { PlayerStateType.Dead, new DeadState(owner, _stateMachine) }
            };
        }

        /// <summary>初始化状态机，进入 IdleState</summary>
        public void Initialize()
        {
            _currentStateType = PlayerStateType.Idle;
            _stateMachine.Initialize(_states[PlayerStateType.Idle]);
        }

        /// <summary>切换到指定状态</summary>
        public void ChangeState(PlayerStateType type)
        {
            if (!_states.ContainsKey(type))
            {
                Debug.LogError($"[PlayerStateMachine] 未知状态类型: {type}");
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
        public bool IsInState(PlayerStateType type)
        {
            return _currentStateType == type;
        }
    }
}
