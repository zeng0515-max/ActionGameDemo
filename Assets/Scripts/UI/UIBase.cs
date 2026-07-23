using UnityEngine;

namespace ActionGameDemo.UI
{
    public abstract class UIBase : MonoBehaviour
    {
        protected UIManager _uiManager;
        public UIManager Manager { get => _uiManager; set => _uiManager = value; }

        public virtual void Show()
        {
            gameObject.SetActive(true);
            OnShow();
        }

        public virtual void Hide()
        {
            OnHide();
            gameObject.SetActive(false);
        }

        public bool IsVisible => gameObject.activeInHierarchy;

        protected virtual void OnShow() { }
        protected virtual void OnHide() { }
    }
}
