using UnityEngine;
using UnityEngine.AI;
using ActionGameDemo.Character;
using ActionGameDemo.Config;
using ActionGameDemo.StateMachine;

namespace ActionGameDemo.Enemy
{
    /// <summary>
    /// 敌人控制器：继承 CharacterBase，集成 NavMeshAgent 寻路与有限状态机。
    /// 设计原则：与 PlayerController 结构对称，复用现有战斗/伤害/Buff 基础设施。
    /// </summary>
    [RequireComponent(typeof(NavMeshAgent))]
    [RequireComponent(typeof(Animator))]
    public class EnemyController : CharacterBase
    {
        [Header("敌人配置")]
        [SerializeField, Tooltip("敌人数据表")] private EnemyData _enemyData;

        [Header("受击参数")]
        [SerializeField, Tooltip("受击恢复时间")] private float _hurtRecoveryTime = 0.5f;

        [Header("巡逻参数")]
        [SerializeField, Tooltip("巡逻到达阈值距离")] private float _patrolArrivalThreshold = 0.5f;
        [SerializeField, Tooltip("巡逻等待时间（秒）")] private float _patrolWaitTime = 2f;

        private NavMeshAgent _navMeshAgent;
        private Animator _animator;
        private EnemyStateMachine _stateMachine;
        private Transform _target;

        // 属性
        public NavMeshAgent Agent => _navMeshAgent;
        public Animator Anim => _animator;
        public EnemyStateMachine StateMachine => _stateMachine;
        public EnemyData EnemyData => _enemyData;
        public Transform Target => _target;

        public float HurtRecoveryTime => _hurtRecoveryTime;
        public float PatrolArrivalThreshold => _patrolArrivalThreshold;
        public float PatrolWaitTime => _patrolWaitTime;

        public bool HasTarget => _target != null;
        public bool TargetIsDead => _target != null && _target.GetComponent<CharacterBase>()?.IsDead == true;

        // 动画参数名缓存
        private static readonly int AnimSpeedParam = Animator.StringToHash("Speed");
        private static readonly int AnimHurtParam = Animator.StringToHash("Hurt");
        private static readonly int AnimDeadParam = Animator.StringToHash("Dead");

        /// <summary>检查 Animator 是否存在指定参数</summary>
        private bool HasAnimatorParameter(int paramHash)
        {
            if (_animator == null || _animator.runtimeAnimatorController == null) return false;
            foreach (var param in _animator.parameters)
            {
                if (param.nameHash == paramHash) return true;
            }
            return false;
        }

        protected override void Awake()
        {
            base.Awake();
            _navMeshAgent = GetComponent<NavMeshAgent>();
            _animator = GetComponent<Animator>();

            if (_navMeshAgent == null) Debug.LogError($"[EnemyController] {gameObject.name} 缺少 NavMeshAgent！");
            if (_animator == null) Debug.LogError($"[EnemyController] {gameObject.name} 缺少 Animator！");

            // 用 EnemyData 初始化 NavMeshAgent 参数
            if (_enemyData != null && _navMeshAgent != null)
            {
                _navMeshAgent.speed = _enemyData.moveSpeed;
                _navMeshAgent.stoppingDistance = _enemyData.attackRange * 0.8f;
            }

            // 标记为 Enemy tag
            if (gameObject.tag == "Untagged")
                gameObject.tag = "Enemy";

            // 安全确保：将敌人及其所有子物体设为 Enemy 层（Layer 9）
            // 这样 OverlapSphere(LayerMask 1<<9) 才能检测到敌人碰撞体
            int enemyLayer = LayerMask.NameToLayer("Enemy");
            if (enemyLayer >= 0 && gameObject.layer != enemyLayer)
            {
                SetLayerRecursive(gameObject, enemyLayer);
#if UNITY_EDITOR
                Debug.Log($"[EnemyController] {gameObject.name} 自动设为 Enemy 层 (Layer {enemyLayer})");
#endif
            }
        }

        /// <summary>递归设置 GameObject 及所有子物体的 Layer</summary>
        private static void SetLayerRecursive(GameObject go, int layer)
        {
            go.layer = layer;
            foreach (Transform child in go.transform)
                SetLayerRecursive(child.gameObject, layer);
        }

        protected virtual void Start()
        {
            _stateMachine = new EnemyStateMachine(this);
            _stateMachine.Initialize();
        }

        private void Update()
        {
            if (_stateMachine == null || IsDead) return;
            _stateMachine.Update();
            AutoDetectPlayer();

            // 更新动画速度参数（检查 Animator 是否有该参数）
            if (_animator != null && _navMeshAgent != null)
            {
                float speed = _navMeshAgent.velocity.magnitude;
                if (HasAnimatorParameter(AnimSpeedParam))
                    _animator.SetFloat(AnimSpeedParam, speed);
            }
        }

        // ---------- 目标管理 ----------
        /// <summary>设置目标</summary>
        public void SetTarget(Transform target)
        {
            _target = target;
        }

        /// <summary>自动搜索玩家目标（每帧由 Update 调用，无目标时扫描）</summary>
        private void AutoDetectPlayer()
        {
            if (_target != null) return;
            if (_enemyData == null) return;

            Collider[] hits = Physics.OverlapSphere(transform.position, _enemyData.detectionRange);
            foreach (var hit in hits)
            {
                if (hit.CompareTag("Player"))
                {
                    var charBase = hit.GetComponent<CharacterBase>();
                    if (charBase != null && !charBase.IsDead)
                    {
                        _target = hit.transform;
                        return;
                    }
                }
            }
        }

        /// <summary>检测玩家是否在视野范围内</summary>
        public bool DetectTarget()
        {
            if (_target == null) return false;

            float dist = Vector3.Distance(transform.position, _target.position);
            if (dist > _enemyData?.detectionRange) return false;

            // 视角检测
            if (_enemyData != null && _enemyData.viewAngle < 360f)
            {
                Vector3 dirToTarget = (_target.position - transform.position).normalized;
                float angle = Vector3.Angle(transform.forward, dirToTarget);
                if (angle > _enemyData.viewAngle * 0.5f) return false;
            }

            return true;
        }

        /// <summary>目标是否在攻击范围内</summary>
        public bool TargetInAttackRange()
        {
            if (_target == null || _enemyData == null) return false;
            float dist = Vector3.Distance(transform.position, _target.position);
            return dist <= _enemyData.attackRange;
        }

        /// <summary>目标是否在追击范围内（超出则放弃追击）</summary>
        public bool TargetInChaseRange()
        {
            if (_target == null || _enemyData == null) return false;
            float dist = Vector3.Distance(transform.position, _target.position);
            return dist <= _enemyData.detectionRange * 1.5f;
        }

        /// <summary>当前血量比例是否低于逃跑阈值</summary>
        public bool ShouldFlee()
        {
            if (_enemyData == null || Stats == null) return false;
            return Stats.HealthPercent <= _enemyData.fleeThreshold;
        }

        // ---------- 移动 ----------
        /// <summary>移动到指定位置</summary>
        public void MoveTo(Vector3 destination)
        {
            if (_navMeshAgent == null || !_navMeshAgent.isOnNavMesh) return;
            _navMeshAgent.isStopped = false;
            _navMeshAgent.SetDestination(destination);
        }

        /// <summary>停止移动</summary>
        public void StopMoving()
        {
            if (_navMeshAgent == null || !_navMeshAgent.isOnNavMesh) return;
            _navMeshAgent.isStopped = true;
            _navMeshAgent.velocity = Vector3.zero;
        }

        /// <summary>面朝目标</summary>
        public void FaceTarget()
        {
            if (_target == null) return;
            Vector3 dir = _target.position - transform.position;
            dir.y = 0f;
            if (dir.sqrMagnitude > 0.01f)
                transform.rotation = Quaternion.LookRotation(dir);
        }

        // ---------- 巡逻 ----------
        private Vector3 _patrolOrigin;
        private bool _patrolOriginSet;

        /// <summary>获取巡逻原点（首次调用时记录）</summary>
        public Vector3 GetPatrolOrigin()
        {
            if (!_patrolOriginSet)
            {
                _patrolOrigin = transform.position;
                _patrolOriginSet = true;
            }
            return _patrolOrigin;
        }

        /// <summary>生成巡逻随机点</summary>
        public Vector3 GetRandomPatrolPoint()
        {
            Vector3 origin = GetPatrolOrigin();
            float radius = _enemyData != null ? _enemyData.patrolRadius : 5f;
            Vector2 random2D = Random.insideUnitCircle * radius;
            Vector3 point = origin + new Vector3(random2D.x, 0f, random2D.y);
            return point;
        }

        /// <summary>是否到达指定位置</summary>
        public bool HasReachedPosition(Vector3 destination, float threshold = -1f)
        {
            if (threshold < 0f) threshold = _patrolArrivalThreshold;
            float dist = Vector3.Distance(transform.position, destination);
            return dist <= threshold;
        }

        // ---------- 受击与死亡 ----------
        public override void TakeDamage(float damage)
        {
            if (IsDead || IsInvincible) return;
            base.TakeDamage(damage);

            if (!IsDead && _stateMachine != null
                && !_stateMachine.IsInState(EnemyStateType.Dead))
            {
                _stateMachine.ChangeState(EnemyStateType.Hurt);
            }
        }

        protected override void Die()
        {
            base.Die();
            if (_navMeshAgent != null) _navMeshAgent.isStopped = true;
            _stateMachine?.ChangeState(EnemyStateType.Dead);
        }

        // ---------- 动画事件回调 ----------
        /// <summary>动画事件：受击动画结束</summary>
        public void OnAnimationEnd()
        {
            // 状态机在 CheckSwitchState 中处理状态切换
        }

        /// <summary>动画事件：死亡动画结束</summary>
        public void OnDeadEnd()
        {
            // CharacterBase.Die 已设置延迟销毁
        }

        // ---------- Gizmos ----------
#if UNITY_EDITOR
        private void OnDrawGizmosSelected()
        {
            // 检测范围
            if (_enemyData != null)
            {
                Gizmos.color = new Color(1f, 1f, 0f, 0.3f);
                Gizmos.DrawWireSphere(transform.position, _enemyData.detectionRange);

                Gizmos.color = new Color(1f, 0f, 0f, 0.3f);
                Gizmos.DrawWireSphere(transform.position, _enemyData.attackRange);

                Gizmos.color = new Color(0f, 0f, 1f, 0.2f);
                Gizmos.DrawWireSphere(GetPatrolOrigin(), _enemyData.patrolRadius);
            }

        }
#endif
    }
}
