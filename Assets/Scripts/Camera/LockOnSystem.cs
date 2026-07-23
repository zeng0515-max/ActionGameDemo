using System.Collections.Generic;
using UnityEngine;

namespace ActionGameDemo.CameraSystem
{
    public class LockOnSystem : MonoBehaviour
    {
        [Header("锁定设置")]
        [SerializeField] private float _lockRange = 15f;
        [SerializeField] private float _lockFOV = 60f;
        [SerializeField] private LayerMask _enemyLayer = 1 << 9;
        [SerializeField] private float _switchCooldown = 0.3f;

        private Transform _lockedTarget;
        private bool _isLocked;
        private float _lastSwitchTime;
        private CameraRig _cameraRig;

        public Transform LockedTarget => _lockedTarget;
        public bool IsLocked => _isLocked;

        private void Start()
        {
            _cameraRig = GetComponent<CameraRig>();
            if (_cameraRig == null) _cameraRig = GetComponentInParent<CameraRig>();
        }

        private void Update()
        {
            if (Input.GetKeyDown(KeyCode.Q))
            {
                if (_isLocked)
                    Unlock();
                else
                    TryLock();
            }

            if (Input.GetKeyDown(KeyCode.E) && _isLocked && Time.time - _lastSwitchTime > _switchCooldown)
            {
                SwitchTarget();
                _lastSwitchTime = Time.time;
            }
        }

        public void TryLock()
        {
            Transform target = FindBestTarget();
            if (target != null)
            {
                _lockedTarget = target;
                _isLocked = true;
            }
        }

        public void Unlock()
        {
            _lockedTarget = null;
            _isLocked = false;
        }

        private void SwitchTarget()
        {
            List<Transform> candidates = GetNearbyEnemies();
            if (candidates.Count <= 1) return;

            int currentIdx = candidates.IndexOf(_lockedTarget);
            int nextIdx = (currentIdx + 1) % candidates.Count;
            _lockedTarget = candidates[nextIdx];
        }

        private Transform FindBestTarget()
        {
            List<Transform> candidates = GetNearbyEnemies();
            if (candidates.Count == 0) return null;

            Transform best = null;
            float bestScore = float.MaxValue;
            Vector3 forward = UnityEngine.Camera.main != null ? UnityEngine.Camera.main.transform.forward : transform.forward;
            foreach (var t in candidates)
            {
                Vector3 dir = (t.position - transform.position).normalized;
                float angle = Vector3.Angle(forward, dir);
                if (angle > _lockFOV * 0.5f) continue;

                float dist = Vector3.Distance(transform.position, t.position);
                float score = dist + angle * 0.1f;
                if (score < bestScore)
                {
                    bestScore = score;
                    best = t;
                }
            }
            return best;
        }

        private List<Transform> GetNearbyEnemies()
        {
            Collider[] hits = Physics.OverlapSphere(transform.position, _lockRange, _enemyLayer);
            List<Transform> result = new List<Transform>();
            foreach (var hit in hits)
            {
                var character = hit.GetComponent<ActionGameDemo.Character.CharacterBase>();
                if (character != null && !character.IsDead)
                    result.Add(hit.transform);
            }
            return result;
        }
    }
}
