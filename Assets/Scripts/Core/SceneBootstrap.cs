using UnityEngine;

namespace ActionGameDemo.Core
{
    /// <summary>
    /// 场景启动引导器：负责场景加载时自动初始化全局管理器。
    /// R2 修复：所有 Debug.Log 包裹 #if UNITY_EDITOR。
    /// </summary>
    public class SceneBootstrap : MonoBehaviour
    {
        [Header("全局管理器预制体（可选）")]
        [SerializeField, Tooltip("GameManager 预制体")] private GameManager _gameManagerPrefab;
        [SerializeField, Tooltip("EventManager 预制体")] private EventManager _eventManagerPrefab;

        private void Awake()
        {
            InitializeGameManager();
            InitializeEventManager();
            InitializeCamera();
        }

        private void InitializeGameManager()
        {
            if (FindFirstObjectByType<GameManager>() != null)
            {
#if UNITY_EDITOR
                Debug.Log("[SceneBootstrap] GameManager 已存在，跳过创建。");
#endif
                return;
            }

            if (_gameManagerPrefab != null)
                Instantiate(_gameManagerPrefab);
            else
            {
                GameObject go = new GameObject("GameManager");
                go.AddComponent<GameManager>();
            }

#if UNITY_EDITOR
            Debug.Log("[SceneBootstrap] GameManager 初始化完成。");
#endif
        }

        private void InitializeEventManager()
        {
            EventManager existing = FindFirstObjectByType<EventManager>();
            if (existing != null)
            {
#if UNITY_EDITOR
                Debug.Log("[SceneBootstrap] EventManager 已存在，跳过创建。");
#endif
                return;
            }

            if (_eventManagerPrefab != null)
                Instantiate(_eventManagerPrefab);
            else
            {
                GameObject go = new GameObject("EventManager");
                go.AddComponent<EventManager>();
            }

#if UNITY_EDITOR
            Debug.Log("[SceneBootstrap] EventManager 初始化完成。");
#endif
        }

        private void InitializeCamera()
        {
            Camera mainCamera = Camera.main;
            if (mainCamera == null)
            {
                Debug.LogError("[SceneBootstrap] 场景中不存在标记为 MainCamera 的相机！");
                return;
            }

            CameraSystem.CameraRig cameraRig = mainCamera.GetComponent<CameraSystem.CameraRig>();
            if (cameraRig == null)
            {
                cameraRig = mainCamera.gameObject.AddComponent<CameraSystem.CameraRig>();
#if UNITY_EDITOR
                Debug.Log("[SceneBootstrap] 为主相机自动添加 CameraRig 组件。");
#endif
            }

            if (cameraRig.Target == null)
            {
                GameObject player = GameObject.FindGameObjectWithTag("Player");
                if (player != null)
                    cameraRig.Target = player.transform;
            }
        }
    }
}
