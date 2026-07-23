using UnityEngine;

namespace ActionGameDemo.Player
{
    public enum BufferedAction
    {
        None,
        Attack,
        Skill,
        Ultimate,
        Dodge,
        Jump
    }

    /// <summary>
    /// 输入缓冲系统：在状态切换前缓存玩家输入，切换后自动执行。
    /// 缺陷-14 修复：高优先级动作不覆盖低优先级（Dodge > Ultimate > Skill > Attack > Jump）。
    /// 缺陷-15 修复：ConsumeBufferedAction 检查超时，过期返回 None。
    /// </summary>
    public class InputBuffer
    {
        private readonly float _bufferTime;
        private BufferedAction _bufferedAction = BufferedAction.None;
        private float _bufferTimer;

        // 缺陷-14 修复：动作优先级表
        private static readonly int[] Priority = { 0, 1, 2, 3, 4, 0 }; // None=0,Attack=1,Skill=2,Ultimate=3,Dodge=4,Jump=0

        public BufferedAction CurrentBufferedAction => _bufferedAction;
        public bool HasBufferedAction => _bufferedAction != BufferedAction.None;

        public InputBuffer(float bufferTime = 0.15f)
        {
            _bufferTime = bufferTime;
        }

        public void BufferInput(BufferedAction action)
        {
            if (action == BufferedAction.None)
                return;

            // 缺陷-14 修复：仅当新输入优先级 >= 已有缓冲时才覆盖
            if (!HasBufferedAction || Priority[(int)action] >= Priority[(int)_bufferedAction])
            {
                _bufferedAction = action;
                _bufferTimer = Time.unscaledTime + _bufferTime;
            }
        }

        // 缺陷-15 修复：ConsumeBufferedAction 检查超时
        public BufferedAction ConsumeBufferedAction()
        {
            if (!HasBufferedAction)
                return BufferedAction.None;

            // 检查是否已过期
            if (Time.unscaledTime > _bufferTimer)
            {
                _bufferedAction = BufferedAction.None;
                return BufferedAction.None;
            }

            BufferedAction action = _bufferedAction;
            _bufferedAction = BufferedAction.None;
            return action;
        }

        public bool HasAction(BufferedAction action)
        {
            if (_bufferedAction != action)
                return false;
            // 缺陷-15 修复：检查超时
            return Time.unscaledTime <= _bufferTimer;
        }

        public void Update()
        {
            if (HasBufferedAction && Time.unscaledTime > _bufferTimer)
            {
                _bufferedAction = BufferedAction.None;
            }
        }

        public void Clear()
        {
            _bufferedAction = BufferedAction.None;
        }
    }
}
