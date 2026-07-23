using UnityEngine;

namespace ActionGameDemo.Player
{
    /// <summary>
        /// 玩家输入统一处理器：负责采集所有原始输入，转换为游戏内状态请求。
    /// 设计原则：
    ///   1. 本层为输入层唯一入口，其他脚本禁止直接调用 Input 类；
    ///   2. 支持输入锁定（游戏暂停/结算时自动屏蔽输入）；
    ///   3. 按键触发后需由消费方调用 ConsumeXXX 清零，防止一帧多消费。
    /// </summary>
    public class PlayerInputHandler : MonoBehaviour
    {
        // ---------- 输入状态字段（每帧更新） ----------
        private Vector2 _moveInput;           // WASD / 方向键
        private bool _jumpPressed;            // 空格（触发帧）
        private bool _attackPressed;          // 鼠标左键（触发帧）
        private bool _skillPressed;           // K 键（触发帧）
        private bool _ultimatePressed;        // L 键（触发帧）
        private bool _dodgePressed;           // 左 Shift（触发帧）
        private bool _lockOnPressed;          // Q 键（触发帧）
        private bool _switchCharacterPressed; // Tab 键（触发帧）
        private bool _pausePressed;           // Esc（触发帧）

        // ---------- 输入锁定 ----------
        private bool _isInputEnabled = true;

        // ---------- 属性（只读暴露给上层） ----------
        public Vector2 MoveInput => _moveInput;
        public bool JumpPressed => _jumpPressed;
        public bool AttackPressed => _attackPressed;
        public bool SkillPressed => _skillPressed;
        public bool UltimatePressed => _ultimatePressed;
        public bool DodgePressed => _dodgePressed;
        public bool LockOnPressed => _lockOnPressed;
        public bool SwitchCharacterPressed => _switchCharacterPressed;
        public bool PausePressed => _pausePressed;
        public bool IsInputEnabled => _isInputEnabled;

        // ---------- 生命周期 ----------
        private void Update()
        {
            // 修复：暂停键在冻结状态下也必须能读取（否则无法取消暂停）
            if (Input.GetKeyDown(KeyCode.Escape))
            {
                Core.GameManager.Instance?.TogglePause();
                _pausePressed = false;
                ClearAllInputs();
                return;
            }

            // 游戏冻结状态（暂停/结算）时屏蔽所有输入
            if (!_isInputEnabled || IsGameFrozen())
            {
                ClearAllInputs();
                return;
            }

            ReadMovementInput();
            ReadActionInputs();
            ReadSystemInputs();
        }

        // ---------- 输入读取 ----------
        /// <summary>读取持续型输入：移动轴向</summary>
        private void ReadMovementInput()
        {
            float horizontal = Input.GetAxisRaw("Horizontal");
            float vertical = Input.GetAxisRaw("Vertical");
            _moveInput = new Vector2(horizontal, vertical);

            // 防止对角线移动速度过快：归一化但保留零向量
            if (_moveInput.sqrMagnitude > 1f)
                _moveInput.Normalize();
        }

        /// <summary>读取触发型输入：战斗动作</summary>
        private void ReadActionInputs()
        {
            _jumpPressed = Input.GetButtonDown("Jump");
            _attackPressed = Input.GetMouseButtonDown(0);
            _skillPressed = Input.GetKeyDown(KeyCode.K);
            _ultimatePressed = Input.GetKeyDown(KeyCode.L);
            _dodgePressed = Input.GetKeyDown(KeyCode.LeftShift);
            _lockOnPressed = Input.GetKeyDown(KeyCode.Q);
            _switchCharacterPressed = Input.GetKeyDown(KeyCode.Tab);
        }

        /// <summary>读取系统型输入：暂停/菜单</summary>
        private void ReadSystemInputs()
        {
            _pausePressed = Input.GetKeyDown(KeyCode.Escape);

            // 暂停键直接触发全局暂停
            if (_pausePressed)
            {
                Core.GameManager.Instance?.TogglePause();
                _pausePressed = false; // 已消费
            }
        }

        // ---------- 输入锁定管理 ----------
        /// <summary>启用输入（恢复响应）</summary>
        public void EnableInput()
        {
            _isInputEnabled = true;
        }

        /// <summary>禁用输入（屏蔽所有按键）</summary>
        public void DisableInput()
        {
            _isInputEnabled = false;
            ClearAllInputs();
        }

        // ---------- 消费方法（上层调用以清零触发状态） ----------
        public void ConsumeJump() => _jumpPressed = false;
        public void ConsumeAttack() => _attackPressed = false;
        public void ConsumeSkill() => _skillPressed = false;
        public void ConsumeUltimate() => _ultimatePressed = false;
        public void ConsumeDodge() => _dodgePressed = false;
        public void ConsumeLockOn() => _lockOnPressed = false;
        public void ConsumeSwitchCharacter() => _switchCharacterPressed = false;

        /// <summary>清空所有输入状态（用于状态切换/死亡时重置）</summary>
        public void ClearAllInputs()
        {
            _moveInput = Vector2.zero;
            _jumpPressed = false;
            _attackPressed = false;
            _skillPressed = false;
            _ultimatePressed = false;
            _dodgePressed = false;
            _lockOnPressed = false;
            _switchCharacterPressed = false;
            _pausePressed = false;
        }

        // ---------- 辅助方法 ----------
        /// <summary>判断当前游戏是否处于冻结状态（通过 GameManager 查询）</summary>
        private bool IsGameFrozen()
        {
            if (Core.GameManager.Instance == null)
                return false;
            return Core.GameManager.Instance.IsGameFrozen;
        }
    }
}
