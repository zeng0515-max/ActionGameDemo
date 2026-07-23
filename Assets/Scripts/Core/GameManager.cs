using UnityEngine;
using UnityEngine.SceneManagement;

namespace ActionGameDemo.Core
{
    /// <summary>
        /// 全局游戏管理器：负责游戏生命周期、状态切换、时间管理。
    /// 设计原则：不直接引用业务层对象（Player/UI），所有状态变更通过 EventManager 广播，降低耦合。
    /// </summary>
    public class GameManager : Singleton<GameManager>
    {
        /// <summary>游戏全局状态枚举</summary>
        public enum GameState { Menu, Playing, Paused, GameOver, Victory }

        // ---------- 属性 ----------
        public GameState CurrentState { get; private set; }

        // ---------- 配置字段 ----------
        [Header("时间设置")]
        [SerializeField, Tooltip("暂停时 timeScale 的值")] private float _pausedTimeScale = 0f;
        [SerializeField, Tooltip("正常游玩时 timeScale 的值")] private float _playingTimeScale = 1f;

        // ---------- 生命周期 ----------
        protected override void Awake()
        {
            base.Awake();
            DontDestroyOnLoad(gameObject);
        }

        private void Start()
        {
            // 初始状态直接设为 Playing，无菜单流程
            ChangeGameState(GameState.Playing);
        }

        // ---------- 状态管理 ----------
        /// <summary>
        /// 切换游戏全局状态。内部处理 timeScale 与输入锁定，并通过 EventManager 广播事件。
        /// </summary>
        public void ChangeGameState(GameState newState)
        {
            if (CurrentState == newState)
            {
                Debug.LogWarning($"[GameManager] 重复切换到相同状态: {newState}");
                return;
            }

            GameState previousState = CurrentState;
            CurrentState = newState;

            // 处理时间缩放与输入锁定
            switch (newState)
            {
                case GameState.Playing:
                    Time.timeScale = _playingTimeScale;
                    break;
                case GameState.Paused:
                    Time.timeScale = _pausedTimeScale;
                    break;
                case GameState.GameOver:
                case GameState.Victory:
                    Time.timeScale = _pausedTimeScale;
                    break;
            }

            // 通过事件总线广播状态变化，解耦 UI、输入、玩家等模块
            EventManager.TriggerOnGameStateChanged(newState);

#if UNITY_EDITOR
            Debug.Log($"[GameManager] 状态切换: {previousState} → {newState}");
#endif
        }

        /// <summary>在 Playing 与 Paused 之间切换</summary>
        public void TogglePause()
        {
            if (CurrentState == GameState.Playing)
                ChangeGameState(GameState.Paused);
            else if (CurrentState == GameState.Paused)
                ChangeGameState(GameState.Playing);
        }

        /// <summary>重新加载当前场景</summary>
        public void RestartGame()
        {
            Time.timeScale = _playingTimeScale;
            SceneManager.LoadScene(SceneManager.GetActiveScene().buildIndex);
        }

        /// <summary>退出游戏（编译器下停止播放）</summary>
        public void QuitGame()
        {
#if UNITY_EDITOR
            UnityEditor.EditorApplication.isPlaying = false;
#else
            Application.Quit();
#endif
        }

        // ---------- 边界判断 ----------
        /// <summary>当前是否处于可游玩状态</summary>
        public bool IsPlaying => CurrentState == GameState.Playing;

        /// <summary>当前是否处于暂停/结算等不可操作状态</summary>
        public bool IsGameFrozen => CurrentState == GameState.Paused
                                 || CurrentState == GameState.GameOver
                                 || CurrentState == GameState.Victory;
    }
}
