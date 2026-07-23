using UnityEngine;
using UnityEngine.UI;
using ActionGameDemo.Combat.Buff;

namespace ActionGameDemo.UI.HUD
{
    /// <summary>
        /// 单个 Buff 图标组件，显示 Buff 图标、剩余时间和层数。
    /// </summary>
    public class BuffIcon : MonoBehaviour
    {
        [Header("组件引用")]
        [SerializeField, Tooltip("Buff 图标 Image")] private Image _iconImage;
        [SerializeField, Tooltip("剩余时间 Text")] private Text _durationText;
        [SerializeField, Tooltip("层数 Text")] private Text _stackText;

        private BuffBase _buff;

        private void Update()
        {
            if (_buff == null || !_buff.IsActive) return;

            if (_durationText != null)
            {
                _durationText.text = Mathf.CeilToInt(_buff.RemainingTime).ToString() + "s";
            }
        }

        /// <summary>设置显示的 Buff</summary>
        public void SetBuff(BuffBase buff)
        {
            _buff = buff;

            if (_buff == null || _buff.Data == null) return;

            if (_iconImage != null)
            {
                _iconImage.sprite = _buff.Data.icon;
                _iconImage.color = Color.white;
            }

            if (_stackText != null)
            {
                _stackText.gameObject.SetActive(_buff.Stacks > 1);
                _stackText.text = "x" + _buff.Stacks;
            }
        }
    }
}
