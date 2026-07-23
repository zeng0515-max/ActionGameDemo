using UnityEngine;
using UnityEngine.UI;
using ActionGameDemo.Combat.Buff;

namespace ActionGameDemo.UI.HUD
{
    /// <summary>
        /// Buff 栏 UI 显示，管理角色身上所有 Buff 图标的创建和更新。
    /// </summary>
    public class BuffBar : MonoBehaviour
    {
        [Header("Buff 栏设置")]
        [SerializeField, Tooltip("Buff 图标容器")] private Transform _buffIconContainer;
        [SerializeField, Tooltip("Buff 图标预制体")] private GameObject _buffIconPrefab;

        private BuffSystem _buffSystem;

        private void Awake()
        {
            if (_buffIconContainer == null)
                _buffIconContainer = transform;
        }

        /// <summary>绑定目标角色的 BuffSystem</summary>
        public void Bind(BuffSystem buffSystem)
        {
            if (_buffSystem != null)
            {
                _buffSystem.OnBuffAdded -= OnBuffAdded;
                _buffSystem.OnBuffRemoved -= OnBuffRemoved;
            }

            _buffSystem = buffSystem;

            if (_buffSystem != null)
            {
                _buffSystem.OnBuffAdded += OnBuffAdded;
                _buffSystem.OnBuffRemoved += OnBuffRemoved;
            }
        }

        private void OnBuffAdded(BuffBase buff)
        {
            RefreshBuffIcons();
        }

        private void OnBuffRemoved(BuffBase buff)
        {
            RefreshBuffIcons();
        }

        private void RefreshBuffIcons()
        {
            if (_buffSystem == null || _buffIconContainer == null) return;

            // 清除旧图标
            for (int i = _buffIconContainer.childCount - 1; i >= 0; i--)
            {
                Destroy(_buffIconContainer.GetChild(i).gameObject);
            }

            // 创建新图标
            foreach (var buff in _buffSystem.ActiveBuffs)
            {
                if (_buffIconPrefab != null)
                {
                    var iconObj = Instantiate(_buffIconPrefab, _buffIconContainer);
                    var icon = iconObj.GetComponent<BuffIcon>();
                    if (icon != null)
                        icon.SetBuff(buff);
                }
            }
        }

        private void OnDestroy()
        {
            if (_buffSystem != null)
            {
                _buffSystem.OnBuffAdded -= OnBuffAdded;
                _buffSystem.OnBuffRemoved -= OnBuffRemoved;
            }
        }
    }
}
