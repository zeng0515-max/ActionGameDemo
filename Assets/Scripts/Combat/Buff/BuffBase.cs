using UnityEngine;
using ActionGameDemo.Character;

namespace ActionGameDemo.Combat.Buff
{
    /// <summary>
        /// Buff 基类：定义生命周期和效果接口。
    /// 生命周期：OnApply() → OnUpdate(deltaTime) → OnRemove()
    /// 堆叠时调用 OnStack(newStacks)。
    /// </summary>
    public abstract class BuffBase
    {
        protected BuffData _data;
        protected GameObject _target;
        protected GameObject _source;
        protected float _remainingTime;
        protected int _stacks = 1;
        protected bool _isActive;

        /// <summary>Buff 配置数据</summary>
        public BuffData Data => _data;

        /// <summary>目标角色</summary>
        public GameObject Target => _target;

        /// <summary>来源角色</summary>
        public GameObject Source => _source;

        /// <summary>剩余时间</summary>
        public float RemainingTime => _remainingTime;

        /// <summary>当前层数</summary>
        public int Stacks => _stacks;

        /// <summary>是否激活中</summary>
        public bool IsActive => _isActive;

        /// <summary>Buff 名称</summary>
        public string Name => _data != null ? _data.buffName : "Unknown";

        /// <summary>是否已过期</summary>
        public bool IsExpired => _remainingTime <= 0f;

        /// <summary>初始化 Buff（由 BuffSystem 调用）</summary>
        public virtual void Initialize(BuffData data, GameObject target, GameObject source)
        {
            _data = data;
            _target = target;
            _source = source;
            _remainingTime = data.duration;
            _stacks = 1;
            _isActive = false;
        }

        /// <summary>应用 Buff（进入激活状态）</summary>
        public virtual void OnApply()
        {
            _isActive = true;
#if UNITY_EDITOR
            Debug.Log($"[Buff] {_data.buffName} 应用到 {_target.name}，持续 {_data.duration}s");
#endif
        }

        /// <summary>每帧更新 Buff</summary>
        public virtual void OnUpdate(float deltaTime)
        {
            if (!_isActive) return;

            _remainingTime -= deltaTime;
            if (_remainingTime <= 0f)
            {
                _remainingTime = 0f;
            }
        }

        /// <summary>移除 Buff（恢复效果）</summary>
        public virtual void OnRemove()
        {
            _isActive = false;
#if UNITY_EDITOR
            Debug.Log($"[Buff] {_data.buffName} 从 {_target.name} 移除");
#endif
        }

        /// <summary>堆叠时调用（层数变化）</summary>
        public virtual void OnStack(int newStacks)
        {
            _stacks = newStacks;
        }

        /// <summary>刷新持续时间</summary>
        public virtual void RefreshDuration()
        {
            _remainingTime = _data.duration;
        }

        /// <summary>获取目标角色的 CharacterStats</summary>
        protected CharacterStats GetTargetStats()
        {
            if (_target == null) return null;
            return _target.GetComponent<CharacterStats>();
        }

        /// <summary>获取目标角色的 CharacterBase</summary>
        protected CharacterBase GetTargetCharacter()
        {
            if (_target == null) return null;
            return _target.GetComponent<CharacterBase>();
        }
    }
}
