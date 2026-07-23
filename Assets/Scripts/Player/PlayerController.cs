using System.Collections.Generic;
using UnityEngine;
using ActionGameDemo.Character;
using ActionGameDemo.Config;
using ActionGameDemo.Combat;
using ActionGameDemo.Combat.Buff; // 修复 CS0234: 补充 using，使 BuffData 可直接引用
using ActionGameDemo.Progression;
// 修复 CS0234 (L79/L84-86/L276-279/L282/L363/L368/L375/L383/L707/L782/L832-836):
// 本文件位于 ActionGameDemo.Player，含子命名空间 ActionGameDemo.Player.Combat。
// 代码中的 Combat.X 前缀被优先解析为 ActionGameDemo.Player.Combat.X（不存在）。
// 修复方案：移除 Combat. 前缀，直接使用类型名（通过已声明的 using 指令解析）。

namespace ActionGameDemo.Player
{
    [RequireComponent(typeof(CharacterController))]
    [RequireComponent(typeof(PlayerInputHandler))]
    [RequireComponent(typeof(Animator))]
    public class PlayerController : CharacterBase
    {
        private CharacterController _characterController;
        private PlayerInputHandler _inputHandler;
        private Animator _animator;
        private Transform _cameraTransform;

        private PlayerStateMachine _stateMachine;
        private InputBuffer _inputBuffer;

        [Header("移动参数")]
        [SerializeField, Tooltip("地面移动速度")] private float _moveSpeed = 5f;
        [SerializeField, Tooltip("旋转速度")] private float _rotationSpeed = 10f;
        [SerializeField, Tooltip("空中水平移动衰减系数")] private float _airControlFactor = 0.3f;

        [Header("跳跃参数")]
        [SerializeField] private float _jumpForce = 8f;
        [SerializeField] private float _gravity = -15f;
        [SerializeField] private float _maxFallSpeed = -30f;

        [Header("战斗参数")]
        [SerializeField, Tooltip("普攻判定半径")] private float _attackRange = 2f;
        [SerializeField, Tooltip("技能判定半径")] private float _skillRange = 4f;
        [SerializeField, Tooltip("技能伤害倍率")] private float _skillDamageMultiplier = 2f;
        [SerializeField, Tooltip("技能冷却时间")] private float _skillCooldown = 5f;
        [SerializeField, Tooltip("大招最大能量值")] private float _ultimateMaxEnergy = 100f;
        [SerializeField, Tooltip("大招判定半径")] private float _ultimateRange = 5f;
        [SerializeField, Tooltip("大招伤害倍率")] private float _ultimateDamageMultiplier = 3f;

        [Header("命中检测")]
        [SerializeField, Tooltip("敌人层级掩码（仅检测此层的碰撞体）")] private LayerMask _enemyLayerMask = 1 << 9;
        [SerializeField, Tooltip("普攻检测中心偏移（相对角色前方）")] private Vector3 _attackHitOffset = new Vector3(0f, 1f, 0.5f);
        [SerializeField, Tooltip("技能检测中心偏移")] private Vector3 _skillHitOffset = new Vector3(0f, 1f, 1f);
        [SerializeField, Tooltip("大招检测中心偏移")] private Vector3 _ultimateHitOffset = new Vector3(0f, 1f, 1.5f);

        [Header("输入缓冲")]
        [SerializeField] private float _inputBufferTime = 0.15f;

        [Header("边界限制")]
        [SerializeField] private float _minYPosition = -10f;
        // 优化-26 修复：重置坐标改为可配置
        [SerializeField] private Vector3 _fallbackResetPosition = new Vector3(0f, 1f, 0f);
        // 优化-25 修复：位移截断阈值改为可配置
        [SerializeField] private float _maxMovementPerFrame = 10f;

        [Header("技能配置")]
        [SerializeField] private SkillData _attackSkillData;
        [SerializeField] private SkillData _activeSkillData;

        [Header("连击系统")]
        [SerializeField, Tooltip("最大连段数")] private int _maxComboStep = 4;
        [SerializeField, Tooltip("连击超时时间（秒）")] private float _comboTimeout = 1.0f;
        [SerializeField, Tooltip("连击基础震屏强度")] private float _baseShakeIntensity = 0.1f;
        [SerializeField, Tooltip("连击最大震屏强度")] private float _maxShakeIntensity = 0.5f;

        [Header("攻击动画参数")]
        [SerializeField, Tooltip("单次攻击动画总时长（秒），需与实际动画 Clip 匹配")] private float _attackDuration = 0.6f;
        [SerializeField, Tooltip("取消点时间（秒），在此之后可输入下一段连招")] private float _attackCancelTime = 0.35f;

        [Header("元素系统")]
        [SerializeField, Tooltip("当前攻击元素（按 1/2/3/4 切换火/冰/雷/水）")]
        private ElementType _currentElement = ElementType.Fire;
        [SerializeField, Tooltip("伤害飘字预制体")] private GameObject _damageNumberPrefab;

        [Header("Buff 配置")]
        [SerializeField, Tooltip("攻击命中时附加 Buff 的概率")] private float _buffProcChance = 1.0f;
        [SerializeField] private BuffData _burnBuffData;
        [SerializeField] private BuffData _freezeBuffData;
        [SerializeField] private BuffData _shockBuffData;

        [Header("养成系统")]
        [SerializeField, Tooltip("等级系统（自动获取）")] private LevelSystem _levelSystem;
        [SerializeField, Tooltip("装备管理器（自动获取）")] private EquipmentManager _equipmentManager;
        [SerializeField, Tooltip("技能树资产")] private SkillTree _skillTree;
        [SerializeField, Tooltip("每只敌人击杀给予的基础经验")] private float _expPerKill = 50f;

        private ComboSystem _comboSystem;

        private float _verticalVelocity;
        private Vector3 _movementInputDirection;
        private Vector3 _additionalMovement;

        private float _skillCooldownEndTime;
        private float _ultimateEnergy;
        private Transform _modelRoot;

        // ---------- Hitbox 命中检测状态 ----------
        /// <summary>当前挥击的 Hitbox 是否激活（由动画事件 EnableHitbox/DisableHitbox 控制）</summary>
        private bool _hitboxActive;
        /// <summary>本次挥击已命中的目标实例ID集合，防止同一挥击重复伤害同一敌人</summary>
        private readonly HashSet<int> _hitTargetsThisSwing = new HashSet<int>();

        // ---------- 攻击状态锁 ----------
        /// <summary>是否正在攻击中（攻击期间禁止重复发起攻击）</summary>
        private bool _isAttackLocked;

        // 属性
        public CharacterController CharacterController => _characterController;
        public PlayerInputHandler InputHandler => _inputHandler;
        public Animator Animator => _animator;
        public InputBuffer InputBuffer => _inputBuffer;
        public PlayerStateMachine StateMachine => _stateMachine;

        /// <summary>等级系统</summary>
        public LevelSystem LevelSystem => _levelSystem;
        /// <summary>装备管理器</summary>
        public EquipmentManager EquipmentManager => _equipmentManager;
        /// <summary>技能树</summary>
        public SkillTree SkillTree => _skillTree;

        /// <summary>连击系统</summary>
        public ComboSystem ComboSystem => _comboSystem;

        /// <summary>攻击状态锁：攻击期间为 true，禁止重复发起攻击</summary>
        public bool IsAttackLocked => _isAttackLocked;

        /// <summary>
        /// 释放攻击状态锁。
        /// 由 AttackState.ExitState() 调用，确保即使动画事件丢失也不会永久锁定。
        /// </summary>
        public void ReleaseAttackLock()
        {
            _isAttackLocked = false;
            _hitboxActive = false;
            _hitTargetsThisSwing.Clear();
        }

        public bool IsGrounded => _characterController != null && _characterController.isGrounded;
        public float VerticalVelocity => _verticalVelocity;

        // SkillData 驱动属性
        public float AttackRange => _attackSkillData != null ? _attackSkillData.range : _attackRange;
        public float AttackDamageMultiplier => _attackSkillData != null ? _attackSkillData.damageMultiplier : 1f;
        public int ComboIndex => _attackSkillData != null ? _attackSkillData.comboIndex : 0;
        public bool CanCancel => _attackSkillData != null ? _attackSkillData.canCancel : true;
        public SkillData AttackSkillData => _attackSkillData;

        public float SkillRange => _activeSkillData != null ? _activeSkillData.range : _skillRange;
        public float SkillDamageMultiplier => _activeSkillData != null ? _activeSkillData.damageMultiplier : _skillDamageMultiplier;
        private float EffectiveSkillCooldown => _activeSkillData != null ? _activeSkillData.cooldown : _skillCooldown;

        public float UltimateRange => _ultimateRange;
        public float UltimateDamageMultiplier => _ultimateDamageMultiplier;

        /// <summary>单次攻击动画总时长（秒），AttackState 参考此值判断攻击结束</summary>
        public float AttackDuration => _attackDuration;
        /// <summary>取消点时间（秒），在此之后可输入下一段连招</summary>
        public float AttackCancelTime => _attackCancelTime;

        public bool IsSkillReady => Time.unscaledTime >= _skillCooldownEndTime;
        public float SkillCooldownRemaining => Mathf.Max(0f, _skillCooldownEndTime - Time.unscaledTime);
        public float SkillCooldownProgress => EffectiveSkillCooldown <= 0f ? 1f : 1f - (SkillCooldownRemaining / EffectiveSkillCooldown);

        public bool IsUltimateReady => _ultimateEnergy >= _ultimateMaxEnergy;
        public float UltimateEnergy => _ultimateEnergy;
        public float UltimateEnergyPercent => _ultimateMaxEnergy > 0f ? _ultimateEnergy / _ultimateMaxEnergy : 0f;
        public float UltimateMaxEnergy => _ultimateMaxEnergy;

        // 缺陷-20 修复：_cameraTransform 改为属性懒查找
        private Transform CameraTransform
        {
            get
            {
                if (_cameraTransform == null && Camera.main != null)
                    _cameraTransform = Camera.main.transform;
                return _cameraTransform;
            }
        }

        protected override void Awake()
        {
            base.Awake();
            _characterController = GetComponent<CharacterController>();
            _inputHandler = GetComponent<PlayerInputHandler>();
            _animator = GetComponent<Animator>();

            if (_characterController == null) Debug.LogError("[PlayerController] 缺少 CharacterController！");
            if (_inputHandler == null) Debug.LogError("[PlayerController] 缺少 PlayerInputHandler！");
            if (_animator == null) Debug.LogError("[PlayerController] 缺少 Animator！");

            _modelRoot = _animator.transform.GetChild(0);

            // 自动获取养成系统组件
            _levelSystem = GetComponent<LevelSystem>();
            if (_levelSystem == null) _levelSystem = gameObject.AddComponent<LevelSystem>();
            _equipmentManager = GetComponent<EquipmentManager>();
            if (_equipmentManager == null) _equipmentManager = gameObject.AddComponent<EquipmentManager>();
        }

        private void Start()
        {
            _inputBuffer = new InputBuffer(_inputBufferTime);
            _comboSystem = new ComboSystem(_maxComboStep, _comboTimeout);
            _comboSystem.OnComboShakeRequested += OnComboShakeRequested;
            _stateMachine = new PlayerStateMachine(this);
            _stateMachine.Initialize();

            CombatCoordinator.OnEnemyKilled += OnEnemyKilled;
            CombatCoordinator.OnPlayerHurt += OnPlayerHurt;

            // 养成系统事件接线
            if (_levelSystem != null && _skillTree != null)
            {
                _levelSystem.OnLevelUp += OnPlayerLevelUp;
            }
        }

        private void OnDestroy()
        {
            CombatCoordinator.OnEnemyKilled -= OnEnemyKilled;
            CombatCoordinator.OnPlayerHurt -= OnPlayerHurt;
            if (_levelSystem != null && _skillTree != null)
            {
                _levelSystem.OnLevelUp -= OnPlayerLevelUp;
            }
        }

        private void OnPlayerLevelUp(int newLevel)
        {
            // 每升一级给予 1 技能点
            _skillTree?.AddSkillPoints(1);
        }

        private void OnEnemyKilled(GameObject enemy)
        {
            AddUltimateEnergy(20f);

            // 击杀敌人获得经验
            if (_levelSystem != null)
            {
                float exp = _expPerKill;
                // Boss 给予更多经验
                var boss = enemy.GetComponent<Enemy.BossController>();
                if (boss != null) exp = boss.ExpReward;
                _levelSystem.AddExperience(exp);
            }
        }

        private void OnPlayerHurt(GameObject player, float damage)
        {
            if (player == gameObject)
                AddUltimateEnergy(Mathf.Clamp(damage * 0.5f, 1f, 15f));
        }

        private void Update()
        {
            if (_characterController == null || _inputHandler == null || _stateMachine == null)
                return;

            HandleElementSwitch();
            CheckBounds();
            _stateMachine.Update();
            _inputBuffer.Update();
            _comboSystem?.Update();
        }

        private void HandleElementSwitch()
        {
            if (Input.GetKeyDown(KeyCode.Alpha1)) _currentElement = ElementType.Fire;
            else if (Input.GetKeyDown(KeyCode.Alpha2)) _currentElement = ElementType.Ice;
            else if (Input.GetKeyDown(KeyCode.Alpha3)) _currentElement = ElementType.Lightning;
            else if (Input.GetKeyDown(KeyCode.Alpha4)) _currentElement = ElementType.Water;
        }

        public override ElementType GetElementType() => _currentElement;

        private void LateUpdate()
        {
            if (_modelRoot != null)
                _modelRoot.localPosition = Vector3.zero;
        }

        public void SetAdditionalMovement(Vector3 movement) => _additionalMovement = movement;
        public void ClearAdditionalMovement() => _additionalMovement = Vector3.zero;

        public void HandleMovementInput()
        {
            Vector2 input = _inputHandler.MoveInput;
            if (input.sqrMagnitude < 0.001f) { _movementInputDirection = Vector3.zero; return; }

            var cam = CameraTransform;
            if (cam != null)
            {
                Vector3 f = cam.forward; f.y = 0; f.Normalize();
                Vector3 r = cam.right; r.y = 0; r.Normalize();
                _movementInputDirection = (f * input.y + r * input.x).normalized;
            }
            else
            {
                _movementInputDirection = new Vector3(input.x, 0f, input.y).normalized;
            }
        }

        public Vector3 GetCurrentMovementDirection() => _movementInputDirection;

        /// <summary>清空移动方向，防止攻击/闪避中残留方向导致位移叠加</summary>
        public void ClearMovementDirection() => _movementInputDirection = Vector3.zero;

        /// <summary>更新模型根节点引用（模型切换时调用）</summary>
        public void UpdateModelRoot(Transform newRoot) => _modelRoot = newRoot;

        public void ExecuteMovement()
        {
            Vector3 h = CalculateHorizontalMovement();
            Vector3 v = new Vector3(0f, _verticalVelocity * Time.deltaTime, 0f);
            Vector3 total = h + v + _additionalMovement;
            _additionalMovement = Vector3.zero;

            // 优化-25 修复：可配置截断阈值
            if (total.sqrMagnitude > _maxMovementPerFrame * _maxMovementPerFrame)
            {
                Debug.LogWarning($"[PlayerController] 异常位移截断: {total}");
                total = Vector3.ClampMagnitude(total, _maxMovementPerFrame);
            }
            _characterController.Move(total);
        }

        private Vector3 CalculateHorizontalMovement()
        {
            if (_movementInputDirection.sqrMagnitude < 0.001f) return Vector3.zero;
            
            float speed = Stats != null ? Stats.EffectiveMoveSpeed : _moveSpeed;
            if (!IsGrounded) speed *= _airControlFactor;
            return _movementInputDirection * speed * Time.deltaTime;
        }

        public void ApplyJump() => _verticalVelocity = _jumpForce;

        public void ApplyGravity()
        {
            if (IsGrounded && _verticalVelocity < 0f) _verticalVelocity = -0.5f;
            else { _verticalVelocity += _gravity * Time.deltaTime; _verticalVelocity = Mathf.Max(_verticalVelocity, _maxFallSpeed); }
        }

        public void HandleRotation()
        {
            if (_movementInputDirection.sqrMagnitude < 0.001f) return;
            var target = Quaternion.LookRotation(_movementInputDirection);
            transform.rotation = Quaternion.Slerp(transform.rotation, target, _rotationSpeed * Time.deltaTime);
        }

        public void ConsumeSkillCooldown() => _skillCooldownEndTime = Time.unscaledTime + EffectiveSkillCooldown;
        public void AddUltimateEnergy(float a) => _ultimateEnergy = Mathf.Clamp(_ultimateEnergy + a, 0f, _ultimateMaxEnergy);
        public void ConsumeUltimateEnergy() => _ultimateEnergy = 0f;

        private HitFeedback _cachedHitFeedback;

        public void TriggerHitFeedback(GameObject target)
        {
            if (_cachedHitFeedback == null)
                _cachedHitFeedback = FindFirstObjectByType<HitFeedback>();
            _cachedHitFeedback?.PlayFullFeedback(target);
        }

        public void TriggerHitFeedback(GameObject target, bool isCritical, int attackType)
        {
            if (_cachedHitFeedback == null)
                _cachedHitFeedback = FindFirstObjectByType<HitFeedback>();
            _cachedHitFeedback?.PlayFullFeedback(target, 0f, isCritical, GetElementType(), attackType);
        }

        /// <summary>连击震动梯度反馈：连击数越高震屏越强</summary>
        private void OnComboShakeRequested(float intensity)
        {
            if (_cachedHitFeedback == null)
                _cachedHitFeedback = FindFirstObjectByType<HitFeedback>();
            if (_cachedHitFeedback != null)
            {
                float shakeAmount = Mathf.Lerp(_baseShakeIntensity, _maxShakeIntensity, intensity);
                _cachedHitFeedback.TriggerScreenShakeCustom(shakeAmount, 0.15f + intensity * 0.15f);
            }
        }

        public override void TakeDamage(float damage)
        {
            if (IsDead || IsInvincible) return;
            base.TakeDamage(damage);
            // 攻击/大招/闪避/技能中不切换到受击状态（避免被打断）；受击中不重复触发
            if (!IsDead && _stateMachine != null
                && !_stateMachine.IsInState(PlayerStateType.Ultimate)
                && !_stateMachine.IsInState(PlayerStateType.Attack)
                && !_stateMachine.IsInState(PlayerStateType.Dodge)
                && !_stateMachine.IsInState(PlayerStateType.Skill)
                && !_stateMachine.IsInState(PlayerStateType.Hurt))
                _stateMachine.ChangeState(PlayerStateType.Hurt);
        }

        protected override void Die()
        {
            base.Die();
            _stateMachine?.ChangeState(PlayerStateType.Dead);
        }

        private void CheckBounds()
        {
            if (transform.position.y < _minYPosition)
            {
                Debug.LogWarning("[PlayerController] 掉落虚空，重置。");
                _characterController.enabled = false;
                transform.position = _fallbackResetPosition;
                _verticalVelocity = 0f;
                _characterController.enabled = true;
            }
        }

        public void LockInput() => _inputHandler?.DisableInput();
        public void UnlockInput() => _inputHandler?.EnableInput();

        // 优化-31 修复：移除重复的 UnlockInput（DeadState.ExitState 已处理）
        public void Revive()
        {
            Stats.ResetToFullHealth();
            SetInvincible(false);
            CancelPendingDestroy(); // ISSUE 10 修复：取消延迟销毁
            _ultimateEnergy = 0f;
            _skillCooldownEndTime = 0f;
            _verticalVelocity = 0f;
            _additionalMovement = Vector3.zero;
            _inputBuffer?.Clear();
            // UnlockInput 由 DeadState.ExitState 调用
            _stateMachine?.ChangeState(PlayerStateType.Idle);
        }

        // ---------- 动画事件回调 ----------
        /// <summary>
        /// 动画事件：打开 Hitbox（在攻击动画的命中帧前调用）。
        /// 设置攻击状态锁，清空去重列表，激活命中检测。
        /// </summary>
        public void EnableHitbox()
        {
            _isAttackLocked = true;
            _hitboxActive = true;
            _hitTargetsThisSwing.Clear();
            
#if UNITY_EDITOR
            Debug.Log($"[PlayerController] 动画事件: EnableHitbox - 攻击锁: {_isAttackLocked}, Hitbox: {_hitboxActive}");
#endif
        }

        /// <summary>
        /// 动画事件：关闭 Hitbox（在攻击动画的命中帧后调用）。
        /// 停止命中检测，清空去重列表，但保留攻击锁（由 OnAnimationEnd 释放）。
        /// </summary>
        public void DisableHitbox()
        {
            _hitboxActive = false;
            _hitTargetsThisSwing.Clear();
            
#if UNITY_EDITOR
            Debug.Log($"[PlayerController] 动画事件: DisableHitbox - Hitbox: {_hitboxActive}");
#endif
        }

        /// <summary>
        /// 动画事件：命中判定帧（Attack / Skill / Ultimate 共用）。
        /// 若动画未配置 EnableHitbox 事件，则自动激活 Hitbox 作为兜底（但不设攻击锁，锁由 EnableHitbox 或 AttackState 管理）。
        /// </summary>
        public void OnHitFrame()
        {
#if UNITY_EDITOR
            Debug.Log($"[PlayerController] 动画事件: OnHitFrame - 当前状态: {(_stateMachine != null ? _stateMachine.CurrentStateType.ToString() : "无状态机")}");
#endif

            if (_stateMachine == null)
            {
#if UNITY_EDITOR
                Debug.LogError("[PlayerController] OnHitFrame: 状态机为空");
#endif
                return;
            }

            // 兜底：若动画未配置 EnableHitbox 事件，则在此自动激活 Hitbox
            if (!_hitboxActive)
            {
                _hitboxActive = true;
                _hitTargetsThisSwing.Clear();
                
#if UNITY_EDITOR
                Debug.Log("[PlayerController] OnHitFrame: Hitbox 未激活，自动激活作为兜底");
#endif
            }

            if (_stateMachine.IsInState(PlayerStateType.Attack))
            {
#if UNITY_EDITOR
                Debug.Log($"[PlayerController] OnHitFrame: 执行普攻检测 - 范围: {AttackRange}, 倍率: {AttackDamageMultiplier}");
#endif
                PerformHitDamage(AttackRange, AttackDamageMultiplier, _attackHitOffset, true);
            }
            else if (_stateMachine.IsInState(PlayerStateType.Skill))
            {
#if UNITY_EDITOR
                Debug.Log($"[PlayerController] OnHitFrame: 执行技能检测 - 范围: {SkillRange}, 倍率: {SkillDamageMultiplier}");
#endif
                PerformHitDamage(SkillRange, SkillDamageMultiplier, _skillHitOffset, false);
            }
            else if (_stateMachine.IsInState(PlayerStateType.Ultimate))
            {
#if UNITY_EDITOR
                Debug.Log($"[PlayerController] OnHitFrame: 执行大招检测 - 范围: {UltimateRange}, 倍率: {UltimateDamageMultiplier}");
#endif
                PerformHitDamage(UltimateRange, UltimateDamageMultiplier, _ultimateHitOffset, false);
            }
            else
            {
#if UNITY_EDITOR
                Debug.LogWarning($"[PlayerController] OnHitFrame: 当前状态不是攻击状态 ({_stateMachine.CurrentStateType})，跳过检测");
#endif
            }
        }

        /// <summary>
        /// 动画事件：取消点（允许输入下一段连招）。
        /// 在取消点之前先关闭 Hitbox，防止残留检测。
        /// </summary>
        public void OnCancelPoint()
        {
#if UNITY_EDITOR
            Debug.Log("[PlayerController] 动画事件: OnCancelPoint");
#endif

            // 关闭当前 Hitbox，为下一段连招做准备
            if (_hitboxActive)
            {
                _hitboxActive = false;
                _hitTargetsThisSwing.Clear();
                
#if UNITY_EDITOR
                Debug.Log("[PlayerController] OnCancelPoint: 关闭 Hitbox，准备连招衔接");
#endif
            }

            // 检查缓冲输入，若有攻击则立即衔接到下一段
            if (_inputBuffer != null && _inputBuffer.HasAction(BufferedAction.Attack))
            {
#if UNITY_EDITOR
                Debug.Log("[PlayerController] OnCancelPoint: 检测到缓冲攻击输入，衔接到下一段");
#endif
                _inputBuffer.ConsumeBufferedAction();
                _stateMachine?.ChangeState(PlayerStateType.Attack);
                return;
            }

            // 也可以在取消点接受闪避等动作
            if (_inputBuffer != null && _inputBuffer.HasAction(BufferedAction.Dodge))
            {
#if UNITY_EDITOR
                Debug.Log("[PlayerController] OnCancelPoint: 检测到缓冲闪避输入");
#endif
                _inputBuffer.ConsumeBufferedAction();
                _stateMachine?.ChangeState(PlayerStateType.Dodge);
                return;
            }
        }

        /// <summary>
        /// 动画事件：动画结束。
        /// 释放攻击锁，检查缓冲输入决定下一状态（支持连招衔接）。
        /// 仅在攻击相关状态（Attack/Skill/Ultimate/Hurt）下才切换，防止已切走时重复触发。
        /// </summary>
        public void OnAnimationEnd()
        {
#if UNITY_EDITOR
            Debug.Log("[PlayerController] 动画事件: OnAnimationEnd");
#endif

            if (_stateMachine == null)
            {
#if UNITY_EDITOR
                Debug.LogError("[PlayerController] OnAnimationEnd: 状态机为空");
#endif
                return;
            }
            
            if (_stateMachine.IsInState(PlayerStateType.Dead))
            {
#if UNITY_EDITOR
                Debug.Log("[PlayerController] OnAnimationEnd: 角色已死亡，跳过");
#endif
                return;
            }

            // 只在攻击相关状态下才切换（防止已被 CheckSwitchState 切走时重复触发）
            bool isAttackState = _stateMachine.IsInState(PlayerStateType.Attack)
                              || _stateMachine.IsInState(PlayerStateType.Skill)
                              || _stateMachine.IsInState(PlayerStateType.Ultimate)
                              || _stateMachine.IsInState(PlayerStateType.Hurt);

            // 释放攻击状态锁和 Hitbox（无论当前状态如何，安全释放）
            _isAttackLocked = false;
            _hitboxActive = false;
            _hitTargetsThisSwing.Clear();
            
#if UNITY_EDITOR
            Debug.Log($"[PlayerController] OnAnimationEnd: 释放攻击锁 - 攻击锁: {_isAttackLocked}, Hitbox: {_hitboxActive}");
#endif

            if (!isAttackState)
            {
#if UNITY_EDITOR
                Debug.Log($"[PlayerController] OnAnimationEnd: 当前状态不是攻击状态 ({_stateMachine.CurrentStateType})，跳过切换");
#endif
                return;
            }

            // 检查缓冲输入，若有攻击则衔接到下一段连招
            if (_inputBuffer != null && _inputBuffer.HasAction(BufferedAction.Attack))
            {
                _inputBuffer.ConsumeBufferedAction();
                _stateMachine.ChangeState(PlayerStateType.Attack);
                return;
            }

            if (_inputBuffer != null && _inputBuffer.HasAction(BufferedAction.Dodge))
            {
                _inputBuffer.ConsumeBufferedAction();
                _stateMachine.ChangeState(PlayerStateType.Dodge);
                return;
            }

            // 根据移动输入决定回到 Idle 还是 Move
            if (_inputHandler != null && _inputHandler.MoveInput.sqrMagnitude > 0.01f)
                _stateMachine.ChangeState(PlayerStateType.Move);
            else
                _stateMachine.ChangeState(PlayerStateType.Idle);
        }

        /// <summary>动画事件：死亡动画结束</summary>
        public void OnDeadEnd()
        {
            // 死亡动画播放完毕，保持死亡状态
        }

        /// <summary>
        /// 执行命中检测和伤害结算。
        /// 使用 LayerMask 只检测敌人层，使用 HashSet 防止同一挥击重复伤害同一目标。
        /// 只有实际命中敌人才注册连击。
        /// </summary>
        /// <param name="range">检测半径</param>
        /// <param name="multiplier">伤害倍率</param>
        /// <param name="offset">检测中心偏移（相对角色世界坐标）</param>
        /// <param name="isAttack">是否为普攻（普攻才参与连击计数）</param>
        private void PerformHitDamage(float range, float multiplier, Vector3 offset, bool isAttack)
        {
            // 计算检测中心点：角色位置 + 前方偏移
            Vector3 center = transform.position + transform.forward * offset.z + Vector3.up * offset.y;
            
#if UNITY_EDITOR
            Debug.Log($"[PlayerController] --------------- 命中检测开始 ---------------");
            Debug.Log($"[PlayerController] 攻击类型: {(isAttack ? "普攻" : "技能/大招")}");
            Debug.Log($"[PlayerController] 检测中心: {center}");
            Debug.Log($"[PlayerController] 检测半径: {range}");
            Debug.Log($"[PlayerController] 敌人层级掩码: {_enemyLayerMask} (二进制: {System.Convert.ToString(_enemyLayerMask, 2)})");
            Debug.Log($"[PlayerController] 当前元素: {GetElementType()}");
            Debug.Log($"[PlayerController] 攻击力: {Stats.EffectiveAttackPower}, 伤害倍率: {multiplier}");
#endif

            // 使用 LayerMask 过滤，只检测敌人层（不检测地形、墙体、Trigger）
            Collider[] hits = Physics.OverlapSphere(center, range, _enemyLayerMask);
            
#if UNITY_EDITOR
            Debug.Log($"[PlayerController] OverlapSphere 检测到 {hits.Length} 个碰撞体");
            for (int i = 0; i < hits.Length; i++)
            {
                Debug.Log($"[PlayerController]   碰撞体 {i}: {hits[i].gameObject.name}, 层: {LayerMask.LayerToName(hits[i].gameObject.layer)}");
            }
#endif

            bool hasHit = false;

            foreach (Collider hit in hits)
            {
                if (hit.gameObject == gameObject)
                {
#if UNITY_EDITOR
                    Debug.Log($"[PlayerController] 跳过自身碰撞体");
#endif
                    continue;
                }

                int targetId = hit.GetInstanceID();
                if (_hitTargetsThisSwing.Contains(targetId))
                {
#if UNITY_EDITOR
                    Debug.Log($"[PlayerController] 目标 {hit.gameObject.name} 已在本次挥击中被命中，跳过");
#endif
                    continue;
                }

                var damageable = hit.GetComponent<IDamageable>();
                if (damageable == null)
                {
#if UNITY_EDITOR
                    Debug.LogWarning($"[PlayerController] 碰撞体 {hit.gameObject.name} 没有 IDamageable 接口，无法造成伤害");
#endif
                    continue;
                }

                _hitTargetsThisSwing.Add(targetId);
                
#if UNITY_EDITOR
                Debug.Log($"[PlayerController] 命中目标: {hit.gameObject.name} (ID: {targetId})");
#endif

                DamageInfo info = new DamageInfo
                {
                    baseDamage = Stats.EffectiveAttackPower,
                    skillMultiplier = multiplier,
                    attackElement = GetElementType(),
                    attacker = gameObject,
                    hitPosition = hit.ClosestPoint(center)
                };

                var targetStats = hit.GetComponent<Character.CharacterStats>();
                bool wasCritical = false;
                float actualDamage;

#if UNITY_EDITOR
                Debug.Log($"[PlayerController] 目标 Stats 组件: {(targetStats != null ? "存在" : "不存在")}");
                if (targetStats != null)
                {
                    Debug.Log($"[PlayerController] 目标生命值: {targetStats.CurrentHealth}/{targetStats.MaxHealth}");
                    Debug.Log($"[PlayerController] 目标防御力: {targetStats.EffectiveDefense}");
                    Debug.Log($"[PlayerController] 目标受伤倍率: {targetStats.DamageTakenMultiplier}");
                }
#endif

                if (targetStats != null)
                {
                    DamageInfo result = DamagePipeline.Calculate(
                        info, Stats.EffectiveAttackPower, targetStats.EffectiveDefense,
                        info.attackElement, damageable.GetElementType(),
                        Stats.EffectiveCriticalRate, Stats.CriticalDamageMultiplier,
                        targetStats.DamageTakenMultiplier);
                    
#if UNITY_EDITOR
                    Debug.Log($"[PlayerController] 伤害计算结果 - 暴击: {result.isCritical}, 最终伤害: {result.finalDamage}");
#endif
                    
                    damageable.TakeDamage(result);
                    wasCritical = result.isCritical;
                    actualDamage = result.finalDamage;
                }
                else
                {
                    info.finalDamage = Stats.EffectiveAttackPower * multiplier;
                    
#if UNITY_EDITOR
                    Debug.Log($"[PlayerController] 目标无 Stats 组件，使用简化伤害: {info.finalDamage}");
#endif
                    
                    damageable.TakeDamage(info);
                    actualDamage = info.finalDamage;
                }

#if UNITY_EDITOR
                Debug.Log($"[PlayerController] 成功对 {hit.gameObject.name} 造成 {actualDamage} 点伤害");
#endif

                AddUltimateEnergy(5f);
                int attackType = isAttack ? 0 : (_stateMachine.IsInState(PlayerStateType.Skill) ? 1 : 2);
                TriggerHitFeedback(hit.gameObject, wasCritical, attackType);

                ElementType elem = GetElementType();
                Color elemColor = ElementSystem.GetElementColor(elem);
                if (wasCritical)
                    VFXEffectManager.Instance?.PlayEffect(VFXEffectType.CriticalHit, hit.ClosestPoint(center), Quaternion.identity);
                else if (elem != ElementType.None)
                    VFXEffectManager.Instance?.PlayEffectTinted(VFXEffectType.HitImpact, hit.ClosestPoint(center), Quaternion.identity, elemColor);
                else
                    VFXEffectManager.Instance?.PlayEffect(VFXEffectType.HitImpact, hit.ClosestPoint(center), Quaternion.identity);

                if (isAttack && elem != ElementType.None)
                    VFXEffectManager.Instance?.PlayEffectTinted(VFXEffectType.Slash, transform.position + transform.forward * range * 0.5f + Vector3.up, transform.rotation, elemColor);
                else if (isAttack)
                    VFXEffectManager.Instance?.PlayEffect(VFXEffectType.Slash, transform.position + transform.forward * range * 0.5f + Vector3.up, transform.rotation);

                TryProcElementBuff(hit.gameObject);
                ShowDamageNumber(actualDamage, wasCritical, hit.ClosestPoint(center));
                hasHit = true;
            }

            if (isAttack && hasHit)
                _comboSystem?.RegisterHit();

#if UNITY_EDITOR
            Debug.Log($"[PlayerController] --------------- 命中检测结束 ---------------");
            Debug.Log($"[PlayerController] 本次挥击命中目标数: {(hasHit ? _hitTargetsThisSwing.Count : 0)}");
            if (isAttack)
                Debug.Log($"[PlayerController] 连击计数: {(hasHit ? "增加" : "不增加")}, 当前连击: {(_comboSystem != null ? _comboSystem.CurrentComboStep.ToString() : "无连击系统")}");
#endif
        }

        private void ShowDamageNumber(float damage, bool isCritical, Vector3 worldPos)
        {
            if (_damageNumberPrefab == null) return;
            var pool = FindFirstObjectByType<Core.ObjectPool>();
            if (pool == null) return;
            var obj = pool.GetObject(_damageNumberPrefab);
            if (obj == null) return;
            var canvas = FindFirstObjectByType<Canvas>();
            if (canvas != null) obj.transform.SetParent(canvas.transform, false);
            var dn = obj.GetComponent<DamageNumber>();
            if (dn != null)
                dn.Show(damage, isCritical, GetElementType(), worldPos);
        }

        private void TryProcElementBuff(GameObject target)
        {
            if (Random.value > _buffProcChance) return;

            var charBase = target.GetComponent<CharacterBase>();
            if (charBase == null) return;

            BuffData buffData = _currentElement switch
            {
                ElementType.Fire => _burnBuffData,
                ElementType.Ice => _freezeBuffData,
                ElementType.Lightning => _shockBuffData,
                _ => null
            };

            if (buffData != null)
            {
                charBase.AddBuff(buffData, gameObject);
            }
        }
    }
}