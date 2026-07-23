using System.Collections.Generic;
using UnityEngine;
using ActionGameDemo.Core;

namespace ActionGameDemo.Combat
{
    /// <summary>
    /// 动画特效管理器：统一管理所有战斗视觉特效的播放、对象池和自动回收。
    /// 通过 VFXEffectType 枚举索引特效预制体，支持在世界坐标或跟随目标播放。
    /// 使用 Singleton 模式，全局可通过 VFXEffectManager.Instance 访问。
    /// </summary>
    public class VFXEffectManager : Singleton<VFXEffectManager>
    {
        [System.Serializable]
        public class VFXEntry
        {
            [Tooltip("特效类型")] public VFXEffectType effectType;
            [Tooltip("特效预制体")] public GameObject prefab;
            [Tooltip("自动回收时长（秒），0 表示自动检测粒子时长")]
            public float lifetime = 2f;
            [Tooltip("是否跟随目标移动")] public bool followTarget = false;
            [Tooltip("初始池大小")] public int initialPoolSize = 3;
            [Tooltip("池是否自动扩展")] public bool autoExpand = true;
        }

        [Header("特效配置")]
        [SerializeField] private List<VFXEntry> _vfxEntries = new List<VFXEntry>();

        [Header("默认设置")]
        [SerializeField, Tooltip("自动检测粒子时长时的最大限制（秒）")]
        private float _maxAutoLifetime = 10f;

        private Dictionary<VFXEffectType, VFXEntry> _entryMap;
        private Dictionary<VFXEffectType, Queue<GameObject>> _poolMap;
        private List<ActiveEffect> _activeEffects;
        private Dictionary<GameObject, VFXEffectType> _instanceTypeMap;

        /// <summary>当前活跃的特效数量</summary>
        public int ActiveEffectCount => _activeEffects != null ? _activeEffects.Count : 0;

        private struct ActiveEffect
        {
            public GameObject instance;
            public VFXEffectType type;
            public float remainingTime;
            public Transform followTarget;
            public Vector3 followOffset;
        }

        protected override void Awake()
        {
            base.Awake();
            InitMaps();
        }

        private void InitMaps()
        {
            _entryMap = new Dictionary<VFXEffectType, VFXEntry>();
            _poolMap = new Dictionary<VFXEffectType, Queue<GameObject>>();
            _activeEffects = new List<ActiveEffect>();
            _instanceTypeMap = new Dictionary<GameObject, VFXEffectType>();

            foreach (var entry in _vfxEntries)
            {
                if (entry.effectType == VFXEffectType.None || entry.prefab == null) continue;
                if (!_entryMap.ContainsKey(entry.effectType))
                {
                    _entryMap.Add(entry.effectType, entry);
                    var pool = new Queue<GameObject>();
                    for (int i = 0; i < entry.initialPoolSize; i++)
                    {
                        var obj = Instantiate(entry.prefab, transform);
                        obj.SetActive(false);
                        pool.Enqueue(obj);
                    }
                    _poolMap.Add(entry.effectType, pool);
                }
            }
        }

        private void Update()
        {
            if (_activeEffects == null) return;

            for (int i = _activeEffects.Count - 1; i >= 0; i--)
            {
                var effect = _activeEffects[i];

                if (effect.followTarget != null && effect.instance != null)
                {
                    effect.instance.transform.position = effect.followTarget.position + effect.followOffset;
                }

                effect.remainingTime -= Time.deltaTime;
                _activeEffects[i] = effect;

                if (effect.remainingTime <= 0f)
                {
                    if (effect.instance != null)
                        Despawn(effect.instance, effect.type);
                    _activeEffects.RemoveAt(i);
                }
            }
        }

        /// <summary>
        /// 在指定世界坐标播放特效。
        /// </summary>
        public GameObject PlayEffect(VFXEffectType type, Vector3 position,
            Quaternion rotation = default, Transform followTarget = null)
        {
            return PlayEffectInternal(type, position, rotation, followTarget, null);
        }

        /// <summary>
        /// 在指定位置播放带颜色染色的特效（用于元素特效区分）。
        /// </summary>
        public GameObject PlayEffectTinted(VFXEffectType type, Vector3 position,
            Quaternion rotation, Color tintColor)
        {
            return PlayEffectInternal(type, position, rotation, null, tintColor);
        }

        private GameObject PlayEffectInternal(VFXEffectType type, Vector3 position,
            Quaternion rotation, Transform followTarget, Color? tintColor)
        {
            if (type == VFXEffectType.None) return null;
            if (_entryMap == null || !_entryMap.TryGetValue(type, out VFXEntry entry))
            {
#if UNITY_EDITOR
                Debug.LogWarning($"[VFXEffectManager] 未配置特效类型: {type}");
#endif
                return null;
            }

            GameObject obj = GetFromPool(type);
            if (obj == null) return null;

            // 重置为原始颜色（防止对象池复用时残留上次的染色）
            ResetTint(obj);

            obj.transform.position = position;
            obj.transform.rotation = (rotation.x != 0f || rotation.y != 0f || rotation.z != 0f || rotation.w != 0f)
                ? rotation
                : Quaternion.identity;

            // 应用元素颜色染色（在激活前设置）
            if (tintColor.HasValue)
            {
                ApplyTint(obj, tintColor.Value);
            }

            obj.SetActive(true);

            // 播放所有粒子系统
            var particleSystems = obj.GetComponentsInChildren<ParticleSystem>();
            foreach (var ps in particleSystems)
            {
                ps.Clear(true);
                ps.Play(true);
            }

            // 播放所有动画
            var animators = obj.GetComponentsInChildren<Animator>();
            foreach (var anim in animators)
            {
                anim.Rebind();
                anim.Play(0, 0, 0f);
            }

            // 计算生命周期
            float lifetime = entry.lifetime;
            if (lifetime <= 0f)
            {
                lifetime = DetectMaxDuration(obj);
                lifetime = Mathf.Min(lifetime, _maxAutoLifetime);
            }

            var activeEffect = new ActiveEffect
            {
                instance = obj,
                type = type,
                remainingTime = lifetime,
                followTarget = entry.followTarget ? followTarget : null,
                followOffset = followTarget != null ? position - followTarget.position : Vector3.zero
            };
            _activeEffects.Add(activeEffect);
            _instanceTypeMap[obj] = type;

            return obj;
        }

        /// <summary>
        /// 在指定 Transform 位置播放特效并跟随。
        /// </summary>
        public GameObject PlayEffectFollow(VFXEffectType type, Transform target,
            Vector3 localOffset = default, Quaternion rotation = default)
        {
            if (target == null) return null;
            Vector3 worldPos = target.position + localOffset;
            return PlayEffect(type, worldPos, rotation, target);
        }

        /// <summary>
        /// 停止并回收指定特效实例。
        /// </summary>
        public void StopEffect(GameObject instance)
        {
            if (instance == null) return;
            if (_instanceTypeMap == null || !_instanceTypeMap.TryGetValue(instance, out VFXEffectType type))
                return;

            for (int i = _activeEffects.Count - 1; i >= 0; i--)
            {
                if (_activeEffects[i].instance == instance)
                {
                    Despawn(instance, type);
                    _activeEffects.RemoveAt(i);
                    return;
                }
            }
        }

        /// <summary>
        /// 停止指定目标上所有跟随特效。
        /// </summary>
        public void StopEffectsOnTarget(Transform target)
        {
            if (target == null || _activeEffects == null) return;

            for (int i = _activeEffects.Count - 1; i >= 0; i--)
            {
                if (_activeEffects[i].followTarget == target)
                {
                    if (_activeEffects[i].instance != null)
                        Despawn(_activeEffects[i].instance, _activeEffects[i].type);
                    _activeEffects.RemoveAt(i);
                }
            }
        }

        /// <summary>
        /// 停止指定类型的所有特效。
        /// </summary>
        public void StopAllOfType(VFXEffectType type)
        {
            if (_activeEffects == null) return;

            for (int i = _activeEffects.Count - 1; i >= 0; i--)
            {
                if (_activeEffects[i].type == type)
                {
                    if (_activeEffects[i].instance != null)
                        Despawn(_activeEffects[i].instance, type);
                    _activeEffects.RemoveAt(i);
                }
            }
        }

        /// <summary>清除所有活跃特效</summary>
        public void ClearAll()
        {
            if (_activeEffects == null) return;

            for (int i = _activeEffects.Count - 1; i >= 0; i--)
            {
                if (_activeEffects[i].instance != null)
                    Despawn(_activeEffects[i].instance, _activeEffects[i].type);
            }
            _activeEffects.Clear();
        }

        /// <summary>是否配置了指定特效类型</summary>
        public bool HasEffect(VFXEffectType type)
        {
            return _entryMap != null && _entryMap.ContainsKey(type);
        }

        private GameObject GetFromPool(VFXEffectType type)
        {
            if (!_poolMap.TryGetValue(type, out var pool))
                return null;

            if (pool.Count > 0)
            {
                var obj = pool.Dequeue();
                _instanceTypeMap.Remove(obj);
                return obj;
            }

            if (_entryMap[type].autoExpand)
            {
                var prefab = _entryMap[type].prefab;
                var obj = Instantiate(prefab, transform);
                return obj;
            }

#if UNITY_EDITOR
            Debug.LogWarning($"[VFXEffectManager] 特效池已耗尽且不允许扩展: {type}");
#endif
            return null;
        }

        private void Despawn(GameObject obj, VFXEffectType type)
        {
            if (obj == null) return;

            // 重置染色
            ResetTint(obj);

            var particleSystems = obj.GetComponentsInChildren<ParticleSystem>();
            foreach (var ps in particleSystems)
            {
                ps.Stop(true, ParticleSystemStopBehavior.StopEmitting);
            }

            obj.SetActive(false);
            obj.transform.parent = transform;

            if (_poolMap.TryGetValue(type, out var pool))
            {
                pool.Enqueue(obj);
            }
            else
            {
                Destroy(obj);
            }

            _instanceTypeMap.Remove(obj);
        }

        /// <summary>应用元素颜色到粒子和材质</summary>
        private void ApplyTint(GameObject obj, Color tintColor)
        {
            // 修改粒子系统的 startColor
            var pss = obj.GetComponentsInChildren<ParticleSystem>();
            foreach (var ps in pss)
            {
                var main = ps.main;
                Color original = main.startColor.color;
                main.startColor = Color.Lerp(original, tintColor, 0.8f);
            }

            // 修改材质颜色（创建临时副本，不修改原始材质）
            var renderers = obj.GetComponentsInChildren<Renderer>();
            foreach (var r in renderers)
            {
                if (r.sharedMaterial == null) continue;
                // 使用 material（自动创建实例），而非 sharedMaterial
                r.material.color = Color.Lerp(r.sharedMaterial.color, tintColor, 0.8f);
            }
        }

        /// <summary>重置染色，恢复到原始状态</summary>
        private void ResetTint(GameObject obj)
        {
            // 恢复材质：直接清除实例材质，让渲染器回退到 sharedMaterial
            var renderers = obj.GetComponentsInChildren<Renderer>();
            foreach (var r in renderers)
            {
                if (r.sharedMaterial == null) continue;
                // 通过赋予 sharedMaterial 来清除实例材质
                var sm = r.sharedMaterial;
                r.material = sm;
            }

            // 恢复粒子系统颜色：从预制体读取原始值
            var entry = GetEntryForInstance(obj);
            if (entry == null || entry.prefab == null) return;
            var prefabPss = entry.prefab.GetComponentsInChildren<ParticleSystem>();
            var instancePss = obj.GetComponentsInChildren<ParticleSystem>();

            for (int i = 0; i < prefabPss.Length && i < instancePss.Length; i++)
            {
                var mainSrc = prefabPss[i].main;
                var mainDst = instancePss[i].main;
                mainDst.startColor = mainSrc.startColor;
            }
        }

        private VFXEntry GetEntryForInstance(GameObject obj)
        {
            if (_instanceTypeMap != null && _instanceTypeMap.TryGetValue(obj, out VFXEffectType type))
            {
                if (_entryMap != null && _entryMap.TryGetValue(type, out VFXEntry entry))
                    return entry;
            }

            // Fallback: scan all entries
            if (_entryMap != null)
            {
                foreach (var entry in _entryMap.Values)
                {
                    if (entry.prefab != null && obj.name.StartsWith(entry.prefab.name))
                        return entry;
                }
            }
            return null;
        }

        /// <summary>自动检测对象中最长粒子系统时长</summary>
        private float DetectMaxDuration(GameObject obj)
        {
            float maxDuration = 0f;
            var particleSystems = obj.GetComponentsInChildren<ParticleSystem>();
            foreach (var ps in particleSystems)
            {
                if (ps.main.loop) continue;
                float duration = ps.main.duration;
                float lifetime = ps.main.startLifetime.constant;
                float total = duration + lifetime;
                if (total > maxDuration)
                    maxDuration = total;
            }

            var animators = obj.GetComponentsInChildren<Animator>();
            foreach (var anim in animators)
            {
                if (anim.runtimeAnimatorController != null)
                {
                    var clips = anim.runtimeAnimatorController.animationClips;
                    foreach (var clip in clips)
                    {
                        if (clip.length > maxDuration)
                            maxDuration = clip.length;
                    }
                }
            }

            return maxDuration > 0f ? maxDuration : 2f;
        }

        private void OnDestroy()
        {
            ClearAll();
        }
    }
}
