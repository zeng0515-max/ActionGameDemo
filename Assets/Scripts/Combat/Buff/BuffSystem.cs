using System.Collections.Generic;
using UnityEngine;

namespace ActionGameDemo.Combat.Buff
{
    /// <summary>
        /// Buff 管理器：管理角色身上所有 Buff。
    /// 负责 Buff 的添加（按堆叠规则）、移除、更新和清除。
    /// 附着在角色上（由 CharacterBase 持有引用）。
    /// </summary>
    public class BuffSystem : MonoBehaviour
    {
        private readonly List<BuffBase> _activeBuffs = new List<BuffBase>();

        /// <summary>当前活跃的 Buff 数量</summary>
        public int BuffCount => _activeBuffs.Count;

        /// <summary>当前活跃的 Buff 列表（只读）</summary>
        public IReadOnlyList<BuffBase> ActiveBuffs => _activeBuffs;

        /// <summary>Buff 添加事件（buff）</summary>
        public System.Action<BuffBase> OnBuffAdded;

        /// <summary>Buff 移除事件（buff）</summary>
        public System.Action<BuffBase> OnBuffRemoved;

        
        private readonly Dictionary<BuffBase, GameObject> _buffVfxMap = new Dictionary<BuffBase, GameObject>();

        private void Update()
        {
            UpdateBuffs(Time.deltaTime);
        }

        /// <summary>添加 Buff</summary>
        public BuffBase AddBuff(BuffData data, GameObject source)
        {
            if (data == null)
            {
                Debug.LogWarning("[BuffSystem] 尝试添加空 BuffData！");
                return null;
            }

            // 查找是否已有相同 Buff
            BuffBase existing = FindBuffByName(data.buffName);

            if (existing != null)
            {
                // 按堆叠规则处理
                switch (data.stackingRule)
                {
                    case BuffStackingRule.RefreshDuration:
                        existing.RefreshDuration();
                        
                        StopBuffVfx(existing);
                        PlayBuffVfx(existing, data);
#if UNITY_EDITOR
                        Debug.Log($"[BuffSystem] 刷新 {data.buffName} 持续时间");
#endif
                        return existing;

                    case BuffStackingRule.StackStacks:
                        int newStacks = Mathf.Min(existing.Stacks + 1, data.maxStacks);
                        existing.OnStack(newStacks);
                        existing.RefreshDuration();
                        
                        StopBuffVfx(existing);
                        PlayBuffVfx(existing, data);
                        return existing;

                    case BuffStackingRule.TakeHighest:
                        existing.RefreshDuration();
                        
                        StopBuffVfx(existing);
                        PlayBuffVfx(existing, data);
                        return existing;
                }
            }

            // 创建新 Buff
            BuffBase newBuff = BuffFactory.CreateBuff(data, gameObject, source);
            newBuff.OnApply();
            _activeBuffs.Add(newBuff);
            OnBuffAdded?.Invoke(newBuff);

            
            PlayBuffVfx(newBuff, data);

#if UNITY_EDITOR
            Debug.Log($"[BuffSystem] 添加 Buff: {data.buffName} 到 {gameObject.name}");
#endif

            return newBuff;
        }

        /// <summary>移除指定 Buff</summary>
        public void RemoveBuff(BuffBase buff)
        {
            if (buff == null) return;
            buff.OnRemove();
            _activeBuffs.Remove(buff);
            OnBuffRemoved?.Invoke(buff);

            
            StopBuffVfx(buff);
        }

        /// <summary>按名称移除 Buff</summary>
        public void RemoveBuffByName(string buffName)
        {
            for (int i = _activeBuffs.Count - 1; i >= 0; i--)
            {
                if (_activeBuffs[i].Name == buffName)
                {
                    RemoveBuff(_activeBuffs[i]);
                }
            }
        }

        /// <summary>按类型移除所有 Buff（驱散）</summary>
        public void RemoveBuffByType(BuffType type)
        {
            for (int i = _activeBuffs.Count - 1; i >= 0; i--)
            {
                if (_activeBuffs[i].Data.buffType == type)
                {
                    RemoveBuff(_activeBuffs[i]);
                }
            }
        }

        /// <summary>清除所有 Buff（死亡时调用）</summary>
        public void ClearAllBuffs()
        {
            for (int i = _activeBuffs.Count - 1; i >= 0; i--)
            {
                _activeBuffs[i].OnRemove();
                OnBuffRemoved?.Invoke(_activeBuffs[i]);
                StopBuffVfx(_activeBuffs[i]);
            }
            _activeBuffs.Clear();
        }

        /// <summary>查找指定名称的 Buff</summary>
        public BuffBase FindBuffByName(string buffName)
        {
            foreach (var buff in _activeBuffs)
            {
                if (buff.Name == buffName)
                    return buff;
            }
            return null;
        }

        /// <summary>是否拥有指定名称的 Buff</summary>
        public bool HasBuff(string buffName)
        {
            return FindBuffByName(buffName) != null;
        }

        /// <summary>更新所有 Buff</summary>
        private void UpdateBuffs(float deltaTime)
        {
            for (int i = _activeBuffs.Count - 1; i >= 0; i--)
            {
                _activeBuffs[i].OnUpdate(deltaTime);

                if (_activeBuffs[i].IsExpired)
                {
                    RemoveBuff(_activeBuffs[i]);
                }
            }
        }

        // ----------

        /// <summary>播放 Buff 对应的特效（跟随目标）</summary>
        private void PlayBuffVfx(BuffBase buff, BuffData data)
        {
            if (buff == null || buff.Target == null) return;

            VFXEffectType effectType = VFXEffectMapper.GetBuffEffectType(data);
            if (effectType == VFXEffectType.None) return;

            var vfxInstance = Combat.VFXEffectManager.Instance?.PlayEffectFollow(
                effectType, buff.Target.transform, Vector3.up * 0.5f);

            if (vfxInstance != null)
                _buffVfxMap[buff] = vfxInstance;
        }

        /// <summary>停止 Buff 对应的特效</summary>
        private void StopBuffVfx(BuffBase buff)
        {
            if (buff == null) return;
            if (_buffVfxMap.TryGetValue(buff, out var vfxInstance))
            {
                Combat.VFXEffectManager.Instance?.StopEffect(vfxInstance);
                _buffVfxMap.Remove(buff);
            }
        }
    }
}
