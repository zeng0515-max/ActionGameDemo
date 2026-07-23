using System.Collections.Generic;
using UnityEngine;
using ActionGameDemo.Core;

namespace ActionGameDemo.UI
{
    public class UIManager : MonoBehaviour
    {
        [Header("面板引用")]
        [SerializeField] private UIBase _mainMenuPanel;
        [SerializeField] private UIBase _pausePanel;
        [SerializeField] private UIBase _settingsPanel;

        private readonly Stack<UIBase> _panelStack = new Stack<UIBase>();

        public static UIManager Instance { get; private set; }

        private void Awake()
        {
            if (Instance != null && Instance != this) { Destroy(gameObject); return; }
            Instance = this;
            DontDestroyOnLoad(gameObject);
        }

        private void Start()
        {
            if (_mainMenuPanel != null) _mainMenuPanel.Manager = this;
            if (_pausePanel != null) _pausePanel.Manager = this;
            if (_settingsPanel != null) _settingsPanel.Manager = this;

            // 初始隐藏暂停和设置面板
            _pausePanel?.Hide();
            _settingsPanel?.Hide();
        }

        private void OnEnable()
        {
            EventManager.OnGameStateChanged += OnGameStateChanged;
        }

        private void OnDisable()
        {
            EventManager.OnGameStateChanged -= OnGameStateChanged;
        }

        private void OnGameStateChanged(GameManager.GameState newState)
        {
            switch (newState)
            {
                case GameManager.GameState.Menu:
                    ShowPanel(_mainMenuPanel);
                    break;
                case GameManager.GameState.Paused:
                    ShowPanel(_pausePanel);
                    break;
                case GameManager.GameState.Playing:
                    HideAllPanels();
                    break;
            }
        }

        public void ShowPanel(UIBase panel)
        {
            if (panel == null) return;
            panel.Show();
            _panelStack.Push(panel);
        }

        public void HideTopPanel()
        {
            if (_panelStack.Count > 0)
            {
                var top = _panelStack.Pop();
                top.Hide();
            }
        }

        public void HideAllPanels()
        {
            while (_panelStack.Count > 0)
            {
                _panelStack.Pop().Hide();
            }
            _pausePanel?.Hide();
            _settingsPanel?.Hide();
        }

        // ---------- 菜单按钮回调 ----------

        public void OnStartGame()
        {
            GameManager.Instance.ChangeGameState(GameManager.GameState.Playing);
        }

        public void OnPause()
        {
            GameManager.Instance.TogglePause();
        }

        public void OnResume()
        {
            GameManager.Instance.TogglePause();
        }

        public void OnOpenSettings()
        {
            ShowPanel(_settingsPanel);
        }

        public void OnCloseSettings()
        {
            HideTopPanel();
        }

        public void OnReturnToMenu()
        {
            GameManager.Instance.ChangeGameState(GameManager.GameState.Menu);
        }

        public void OnQuitGame()
        {
            GameManager.Instance.QuitGame();
        }
    }
}
