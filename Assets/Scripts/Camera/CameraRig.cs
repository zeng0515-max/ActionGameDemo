using UnityEngine;

namespace ActionGameDemo.CameraSystem
{
    public class CameraRig : MonoBehaviour
    {
        [Header("跟随目标")]
        [SerializeField] private Transform _target;
        [SerializeField] private Vector3 _positionOffset = new Vector3(0f, 3f, -6f);
        [SerializeField] private Vector3 _lookOffset = new Vector3(0f, 1.5f, 0f);

        [Header("平滑参数")]
        [SerializeField] private float _positionSmooth = 10f;
        [SerializeField] private float _rotationSmooth = 10f;

        [Header("旋转控制")]
        [SerializeField] private float _mouseSensitivity = 3f;
        [SerializeField] private float _minPitch = -30f;
        [SerializeField] private float _maxPitch = 60f;

        [Header("碰撞检测")]
        [SerializeField] private LayerMask _collisionLayers;
        [SerializeField] private float _collisionRadius = 0.3f;
        [SerializeField] private float _minDistance = 2f;

        [Header("锁定系统")]
        [SerializeField] private LockOnSystem _lockOnSystem;

        private float _yaw;
        private float _pitch = 10f;
        private Vector3 _smoothVelocity;
        private Vector3 _basePosition;

        public Transform Target
        {
            get => _target;
            set => _target = value;
        }

        public float Yaw => _yaw;
        public float Pitch => _pitch;

        private void Start()
        {
            if (_target == null)
            {
                var player = GameObject.FindGameObjectWithTag("Player");
                if (player != null) _target = player.transform;
            }
            if (_lockOnSystem == null) _lockOnSystem = GetComponent<LockOnSystem>();
        }

        private void LateUpdate()
        {
            if (_target == null) return;

            HandleInput();
            UpdatePosition();
            UpdateRotation();
            ApplyShakeOffset();
        }

        private void HandleInput()
        {
            if (_lockOnSystem != null && _lockOnSystem.IsLocked)
            {
                HandleLockedRotation();
                return;
            }

            if (Input.GetMouseButton(1))
            {
                _yaw += Input.GetAxis("Mouse X") * _mouseSensitivity;
                _pitch -= Input.GetAxis("Mouse Y") * _mouseSensitivity;
                _pitch = Mathf.Clamp(_pitch, _minPitch, _maxPitch);
            }
        }

        private void HandleLockedRotation()
        {
            if (_lockOnSystem.LockedTarget == null) return;
            Vector3 dir = _lockOnSystem.LockedTarget.position - _target.position;
            _yaw = Mathf.Atan2(dir.x, dir.z) * Mathf.Rad2Deg;
            _pitch = -dir.y * 10f;
            _pitch = Mathf.Clamp(_pitch, _minPitch, _maxPitch);
        }

        private void UpdatePosition()
        {
            Quaternion rotation = Quaternion.Euler(_pitch, _yaw, 0f);
            Vector3 desiredPos = _target.position + rotation * _positionOffset;

            float desiredDist = Vector3.Distance(_target.position, desiredPos);
            Vector3 dir = (desiredPos - _target.position).normalized;

            if (Physics.SphereCast(_target.position, _collisionRadius, dir, out var hit, desiredDist, _collisionLayers))
            {
                desiredPos = _target.position + dir * Mathf.Max(hit.distance - _collisionRadius, _minDistance);
            }

            _basePosition = Vector3.SmoothDamp(_basePosition, desiredPos, ref _smoothVelocity, 1f / _positionSmooth);
            transform.position = _basePosition;
        }

        private void UpdateRotation()
        {
            Vector3 lookTarget = _target.position + _lookOffset;
            if (_lockOnSystem != null && _lockOnSystem.IsLocked && _lockOnSystem.LockedTarget != null)
                lookTarget = (_target.position + _lookOffset + _lockOnSystem.LockedTarget.position) * 0.5f;

            Quaternion lookRot = Quaternion.LookRotation(lookTarget - transform.position);
            transform.rotation = Quaternion.Slerp(transform.rotation, lookRot, _rotationSmooth * Time.deltaTime);
        }

        private void ApplyShakeOffset()
        {
            transform.position = _basePosition + Combat.HitFeedback.ShakeOffset;
        }
    }
}
