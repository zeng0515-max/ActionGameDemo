using UnityEngine;

namespace ActionGameDemo.Core
{
    /// <summary>
    /// JSON 存档管理器：负责存档的读写、容错与版本迁移。
    /// 缺陷-19 修复：添加 DontDestroyOnLoad，场景切换时不销毁。
    /// </summary>
    public class SaveManager : Singleton<SaveManager>
    {
        [Header("存档设置")]
        [SerializeField, Tooltip("存档文件名（不含扩展名）")] private string _saveFileName = "savedata";
        [SerializeField, Tooltip("备份文件名（不含扩展名）")] private string _backupFileName = "savedata_backup";
        [SerializeField, Tooltip("当前存档版本号")] private int _saveVersion = 1;

        private string SavePath => $"{Application.persistentDataPath}/{_saveFileName}.json";
        private string BackupPath => $"{Application.persistentDataPath}/{_backupFileName}.json";

        // 缺陷-19 修复：重写 Awake 添加 DontDestroyOnLoad
        protected override void Awake()
        {
            base.Awake();
            DontDestroyOnLoad(gameObject);
        }

        public void Save<T>(T data) where T : class
        {
            if (data == null)
            {
                Debug.LogError("[SaveManager] 存档数据为 null，已取消写入。");
                return;
            }

            try
            {
                SaveWrapper<T> wrapper = new SaveWrapper<T>
                {
                    saveVersion = _saveVersion,
                    payload = data
                };

                string json = JsonUtility.ToJson(wrapper, true);

                if (System.IO.File.Exists(SavePath))
                    System.IO.File.Copy(SavePath, BackupPath, true);

                System.IO.File.WriteAllText(SavePath, json);
#if UNITY_EDITOR
                Debug.Log("[SaveManager] 存档写入成功。");
#endif
            }
            catch (System.Exception e)
            {
                Debug.LogError($"[SaveManager] 存档写入失败: {e.Message}");
            }
        }

        public T Load<T>() where T : class
        {
            T data = TryReadFile<T>(SavePath);
            if (data != null)
                return data;

            Debug.LogWarning("[SaveManager] 主存档读取失败，尝试回退到备份。");
            data = TryReadFile<T>(BackupPath);
            if (data != null)
            {
#if UNITY_EDITOR
                Debug.Log("[SaveManager] 从备份恢复存档成功。");
#endif
                Save(data);
            }
            else
            {
                Debug.LogWarning("[SaveManager] 无可用存档，返回默认值。");
            }
            return data;
        }

        private T TryReadFile<T>(string path) where T : class
        {
            if (!System.IO.File.Exists(path))
                return null;

            try
            {
                string json = System.IO.File.ReadAllText(path);
                SaveWrapper<T> wrapper = JsonUtility.FromJson<SaveWrapper<T>>(json);

                if (wrapper == null || wrapper.payload == null)
                {
                    Debug.LogWarning($"[SaveManager] 存档反序列化失败: {path}");
                    return null;
                }

                if (wrapper.saveVersion != _saveVersion)
                {
                    wrapper.payload = Migrate<T>(wrapper.payload, wrapper.saveVersion, _saveVersion);
                }

                return wrapper.payload;
            }
            catch (System.Exception e)
            {
                Debug.LogWarning($"[SaveManager] 读取存档异常 ({path}): {e.Message}");
                return null;
            }
        }

        private T Migrate<T>(T data, int fromVersion, int toVersion) where T : class
        {
#if UNITY_EDITOR
            Debug.Log($"[SaveManager] 存档版本迁移: {fromVersion} → {toVersion}");
#endif
            return data;
        }

        public bool HasSave()
        {
            return System.IO.File.Exists(SavePath) || System.IO.File.Exists(BackupPath);
        }

        public void DeleteSave()
        {
            if (System.IO.File.Exists(SavePath))
                System.IO.File.Delete(SavePath);
            if (System.IO.File.Exists(BackupPath))
                System.IO.File.Delete(BackupPath);
#if UNITY_EDITOR
            Debug.Log("[SaveManager] 存档已删除。");
#endif
        }
    }

    [System.Serializable]
    public class SaveWrapper<T> where T : class
    {
        public int saveVersion;
        public T payload;
    }
}
