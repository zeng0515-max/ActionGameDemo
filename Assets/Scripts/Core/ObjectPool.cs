using UnityEngine;
using System.Collections.Generic;

namespace ActionGameDemo.Core
{
    public interface IPoolable
    {
        void OnGetFromPool();
        void OnReturnToPool();
    }

    public class ObjectPool : MonoBehaviour
    {
        [System.Serializable]
        public class PoolEntry
        {
            public GameObject prefab;
            public int initialSize;
            public bool autoExpand;
        }

        [SerializeField] private List<PoolEntry> pools = new List<PoolEntry>();
        private Dictionary<GameObject, Queue<GameObject>> poolDictionary = new Dictionary<GameObject, Queue<GameObject>>();
        // BUG-06 修复：实例→预制体精确映射，替代名称前缀匹配
        private Dictionary<GameObject, GameObject> instanceToPrefab = new Dictionary<GameObject, GameObject>();

        private void Awake()
        {
            foreach (var entry in pools)
            {
                CreatePool(entry);
            }
        }

        private void CreatePool(PoolEntry entry)
        {
            Queue<GameObject> objectPool = new Queue<GameObject>();

            for (int i = 0; i < entry.initialSize; i++)
            {
                GameObject obj = Instantiate(entry.prefab);
                obj.SetActive(false);
                obj.transform.SetParent(transform, false);
                objectPool.Enqueue(obj);
                instanceToPrefab[obj] = entry.prefab;
            }

            poolDictionary.Add(entry.prefab, objectPool);
        }

        public GameObject GetObject(GameObject prefab)
        {
            if (!poolDictionary.ContainsKey(prefab))
            {
                foreach (var entry in pools)
                {
                    if (entry.prefab == prefab)
                    {
                        CreatePool(entry);
                        break;
                    }
                }
            }

            if (poolDictionary.ContainsKey(prefab) && poolDictionary[prefab].Count > 0)
            {
                GameObject obj = poolDictionary[prefab].Dequeue();
                obj.SetActive(true);
                var poolable = obj.GetComponent<IPoolable>();
                poolable?.OnGetFromPool();
                return obj;
            }

            foreach (var entry in pools)
            {
                if (entry.prefab == prefab && entry.autoExpand)
                {
                    // 缺陷-10 修复：设置 parent 为 pool 根节点
                    GameObject obj = Instantiate(prefab, transform);
                    obj.SetActive(true);
                    instanceToPrefab[obj] = prefab;
                    var poolable = obj.GetComponent<IPoolable>();
                    poolable?.OnGetFromPool();
                    return obj;
                }
            }

            return null;
        }

        public void ReturnObject(GameObject obj)
        {
            // BUG-06 修复：使用精确映射而非名称前缀匹配
            if (instanceToPrefab.TryGetValue(obj, out GameObject prefab))
            {
                obj.SetActive(false);
                obj.transform.SetParent(transform, false);
                var poolable = obj.GetComponent<IPoolable>();
                poolable?.OnReturnToPool();
                poolDictionary[prefab].Enqueue(obj);
                return;
            }

            // 不属于任何池，直接销毁
            Destroy(obj);
        }
    }
}
