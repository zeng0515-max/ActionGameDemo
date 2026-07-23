using UnityEngine;

namespace ActionGameDemo.StateMachine
{
    public abstract class BaseState<T> where T : class
    {
        protected T owner;
        protected StateMachine<T> stateMachine;

        public BaseState(T owner, StateMachine<T> stateMachine)
        {
            this.owner = owner;
            this.stateMachine = stateMachine;
        }

        public abstract void EnterState();
        public abstract void UpdateState();
        public abstract void ExitState();
        public abstract void CheckSwitchState();
    }

    public class StateMachine<T> where T : class
    {
        public BaseState<T> currentState { get; private set; }
        private T owner;

        public StateMachine(T owner)
        {
            this.owner = owner;
        }

        public void Initialize(BaseState<T> startingState)
        {
            // BUG-05 修复：null 检查
            if (startingState == null)
            {
                Debug.LogError("[StateMachine] Initialize: 起始状态为 null！");
                return;
            }
            currentState = startingState;
            currentState.EnterState();
        }

        public void ChangeState(BaseState<T> newState)
        {
            // BUG-05 修复：null 检查
            if (newState == null)
            {
                Debug.LogError("[StateMachine] ChangeState: 目标状态为 null！");
                return;
            }
            currentState?.ExitState();
            currentState = newState;
            currentState.EnterState();
        }

        public void Update()
        {
            currentState?.UpdateState();
            currentState?.CheckSwitchState();
        }
    }
}
