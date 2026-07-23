using UnityEngine;
using UnityEngine.UI;
using UnityEngine.SceneManagement;

namespace ActionGameDemo.UI
{
    public class MainMenuPanel : UIBase
    {
        [SerializeField] private Button _startButton;
        [SerializeField] private Button _settingsButton;
        [SerializeField] private Button _quitButton;

        protected override void OnShow()
        {
            if (_startButton != null) _startButton.onClick.AddListener(OnStart);
            if (_settingsButton != null) _settingsButton.onClick.AddListener(OnSettings);
            if (_quitButton != null) _quitButton.onClick.AddListener(OnQuit);
        }

        protected override void OnHide()
        {
            if (_startButton != null) _startButton.onClick.RemoveListener(OnStart);
            if (_settingsButton != null) _settingsButton.onClick.RemoveListener(OnSettings);
            if (_quitButton != null) _quitButton.onClick.RemoveListener(OnQuit);
        }

        private void OnStart()
        {
            Manager?.OnStartGame();
        }

        private void OnSettings()
        {
            Manager?.OnOpenSettings();
        }

        private void OnQuit()
        {
            Manager?.OnQuitGame();
        }
    }
}
