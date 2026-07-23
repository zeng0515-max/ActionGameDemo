using UnityEngine;
using UnityEngine.UI;

namespace ActionGameDemo.UI
{
    public class PausePanel : UIBase
    {
        [SerializeField] private Button _resumeButton;
        [SerializeField] private Button _settingsButton;
        [SerializeField] private Button _returnMenuButton;
        [SerializeField] private Button _quitButton;

        protected override void OnShow()
        {
            if (_resumeButton != null) _resumeButton.onClick.AddListener(OnResume);
            if (_settingsButton != null) _settingsButton.onClick.AddListener(OnSettings);
            if (_returnMenuButton != null) _returnMenuButton.onClick.AddListener(OnReturnToMenu);
            if (_quitButton != null) _quitButton.onClick.AddListener(OnQuit);
        }

        protected override void OnHide()
        {
            if (_resumeButton != null) _resumeButton.onClick.RemoveListener(OnResume);
            if (_settingsButton != null) _settingsButton.onClick.RemoveListener(OnSettings);
            if (_returnMenuButton != null) _returnMenuButton.onClick.RemoveListener(OnReturnToMenu);
            if (_quitButton != null) _quitButton.onClick.RemoveListener(OnQuit);
        }

        private void OnResume() { Manager?.OnResume(); }
        private void OnSettings() { Manager?.OnOpenSettings(); }
        private void OnReturnToMenu() { Manager?.OnReturnToMenu(); }
        private void OnQuit() { Manager?.OnQuitGame(); }
    }
}
