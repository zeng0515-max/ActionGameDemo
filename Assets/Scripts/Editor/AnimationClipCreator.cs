using UnityEditor;
using UnityEngine;

namespace ActionGameDemo.Editor
{
    /// <summary>
        /// 编辑器工具：一键创建玩家占位动画剪辑（9个基础动作）。
    /// 注意：这些是简单的占位动画，用于测试状态机，后续替换为正式动画即可。
    /// 使用方法：Unity 菜单 → Tools（工具）→ ActionGameDemo → 创建玩家占位动画
    /// </summary>
    public static class AnimationClipCreator
    {
        private const string OutputFolder = "Assets/Animations/Player";

        [MenuItem("Tools/ActionGameDemo/创建玩家占位动画")]
        public static void CreatePlayerAnimations()
        {
            // 确保目录存在
            if (!System.IO.Directory.Exists(OutputFolder))
            {
                System.IO.Directory.CreateDirectory(OutputFolder);
                AssetDatabase.Refresh();
            }

            // 创建 9 个占位动画
            CreateIdleClip();
            CreateWalkClip();
            CreateJumpClip();
            CreateAttackClip();
            CreateSkillClip();
            CreateUltimateClip();
            CreateDodgeClip();
            CreateHurtClip();
            CreateDeadClip();

            AssetDatabase.SaveAssets();
            AssetDatabase.Refresh();

            Debug.Log($"[AnimationClipCreator] 玩家占位动画已创建到：{OutputFolder}");
            EditorUtility.DisplayDialog("创建成功",
                "9 个玩家占位动画已创建到：\n" + OutputFolder + "\n\n" +
                "包含：Idle、Walk、Jump、Attack、Skill、Ultimate、Dodge、Hurt、Dead\n\n" +
                "使用方法：\n" +
                "1. 打开 PlayerAnimator.controller\n" +
                "2. 把对应动画拖到状态里（Idle拖到Idle状态，以此类推）\n" +
                "3. 这些是简单的占位动画（轻微缩放/旋转），后续可替换为正式动画",
                "好的");
        }

        // ---------- Idle（待机）：轻微上下呼吸效果 ----------
        private static void CreateIdleClip()
        {
            AnimationClip clip = new AnimationClip();
            clip.name = "Player_Idle";
            clip.wrapMode = WrapMode.Loop;

            // 用 LocalPosition.y 做一个简单的呼吸效果
            AnimationCurve curve = new AnimationCurve();
            curve.AddKey(0f, 0f);
            curve.AddKey(1f, 0.02f);
            curve.AddKey(2f, 0f);
            clip.SetCurve("", typeof(Transform), "m_LocalPosition.y", curve);

            AssetDatabase.CreateAsset(clip, System.IO.Path.Combine(OutputFolder, "Player_Idle.anim"));
        }

        // ---------- Walk（行走）：轻微左右摇摆 ----------
        private static void CreateWalkClip()
        {
            AnimationClip clip = new AnimationClip();
            clip.name = "Player_Walk";
            clip.wrapMode = WrapMode.Loop;

            // LocalRotation.z 左右摇摆
            AnimationCurve curve = new AnimationCurve();
            curve.AddKey(0f, -2f);
            curve.AddKey(0.25f, 0f);
            curve.AddKey(0.5f, 2f);
            curve.AddKey(0.75f, 0f);
            curve.AddKey(1f, -2f);
            clip.SetCurve("", typeof(Transform), "m_LocalRotation.z", curve);

            AssetDatabase.CreateAsset(clip, System.IO.Path.Combine(OutputFolder, "Player_Walk.anim"));
        }

        // ---------- Jump（跳跃）：向上缩放 ----------
        private static void CreateJumpClip()
        {
            AnimationClip clip = new AnimationClip();
            clip.name = "Player_Jump";
            clip.wrapMode = WrapMode.Once;

            // LocalScale.y 先拉伸再恢复
            AnimationCurve curve = new AnimationCurve();
            curve.AddKey(0f, 1f);
            curve.AddKey(0.15f, 1.2f);
            curve.AddKey(0.4f, 1f);
            clip.SetCurve("", typeof(Transform), "m_LocalScale.y", curve);

            AssetDatabase.CreateAsset(clip, System.IO.Path.Combine(OutputFolder, "Player_Jump.anim"));
        }

        // ---------- Attack（普攻）：向前冲一下 ----------
        private static void CreateAttackClip()
        {
            AnimationClip clip = new AnimationClip();
            clip.name = "Player_Attack";
            clip.wrapMode = WrapMode.Once;

            // LocalPosition.z 前冲
            AnimationCurve curveZ = new AnimationCurve();
            curveZ.AddKey(0f, 0f);
            curveZ.AddKey(0.15f, 0.5f);
            curveZ.AddKey(0.4f, 0f);
            clip.SetCurve("", typeof(Transform), "m_LocalPosition.z", curveZ);

            // LocalScale.x 轻微变宽
            AnimationCurve curveX = new AnimationCurve();
            curveX.AddKey(0f, 1f);
            curveX.AddKey(0.15f, 1.1f);
            curveX.AddKey(0.4f, 1f);
            clip.SetCurve("", typeof(Transform), "m_LocalScale.x", curveX);

            AssetDatabase.CreateAsset(clip, System.IO.Path.Combine(OutputFolder, "Player_Attack.anim"));
        }

        // ---------- Skill（技能）：旋转一圈 ----------
        private static void CreateSkillClip()
        {
            AnimationClip clip = new AnimationClip();
            clip.name = "Player_Skill";
            clip.wrapMode = WrapMode.Once;

            // LocalRotation.y 旋转 360 度
            AnimationCurve curve = new AnimationCurve();
            curve.AddKey(0f, 0f);
            curve.AddKey(0.5f, 360f);
            curve.AddKey(0.8f, 0f);
            clip.SetCurve("", typeof(Transform), "m_LocalRotation.y", curve);

            // 缩放变大
            AnimationCurve scaleCurve = new AnimationCurve();
            scaleCurve.AddKey(0f, 1f);
            scaleCurve.AddKey(0.25f, 1.3f);
            scaleCurve.AddKey(0.8f, 1f);
            clip.SetCurve("", typeof(Transform), "m_LocalScale.y", scaleCurve);

            AssetDatabase.CreateAsset(clip, System.IO.Path.Combine(OutputFolder, "Player_Skill.anim"));
        }

        // ---------- Ultimate（大招）：发光变大 ----------
        private static void CreateUltimateClip()
        {
            AnimationClip clip = new AnimationClip();
            clip.name = "Player_Ultimate";
            clip.wrapMode = WrapMode.Once;

            // 放大一圈再恢复
            AnimationCurve curve = new AnimationCurve();
            curve.AddKey(0f, 1f);
            curve.AddKey(0.3f, 1.5f);
            curve.AddKey(0.6f, 1.5f);
            curve.AddKey(1.2f, 1f);
            clip.SetCurve("", typeof(Transform), "m_LocalScale.x", curve);
            clip.SetCurve("", typeof(Transform), "m_LocalScale.y", curve);
            clip.SetCurve("", typeof(Transform), "m_LocalScale.z", curve);

            AssetDatabase.CreateAsset(clip, System.IO.Path.Combine(OutputFolder, "Player_Ultimate.anim"));
        }

        // ---------- Dodge（闪避）：快速平移 ----------
        private static void CreateDodgeClip()
        {
            AnimationClip clip = new AnimationClip();
            clip.name = "Player_Dodge";
            clip.wrapMode = WrapMode.Once;

            // 向侧面快速移动（相对坐标，实际由代码控制位移，这里只做视觉效果）
            AnimationCurve curve = new AnimationCurve();
            curve.AddKey(0f, 1f);
            curve.AddKey(0.1f, 0.8f);
            curve.AddKey(0.3f, 1f);
            clip.SetCurve("", typeof(Transform), "m_LocalScale.y", curve);

            AssetDatabase.CreateAsset(clip, System.IO.Path.Combine(OutputFolder, "Player_Dodge.anim"));
        }

        // ---------- Hurt（受击）：向后缩一下 ----------
        private static void CreateHurtClip()
        {
            AnimationClip clip = new AnimationClip();
            clip.name = "Player_Hurt";
            clip.wrapMode = WrapMode.Once;

            // 向后退
            AnimationCurve curveZ = new AnimationCurve();
            curveZ.AddKey(0f, 0f);
            curveZ.AddKey(0.1f, -0.3f);
            curveZ.AddKey(0.3f, 0f);
            clip.SetCurve("", typeof(Transform), "m_LocalPosition.z", curveZ);

            // 变红（缩放示意，实际用材质更好）
            AnimationCurve curveY = new AnimationCurve();
            curveY.AddKey(0f, 1f);
            curveY.AddKey(0.1f, 0.9f);
            curveY.AddKey(0.3f, 1f);
            clip.SetCurve("", typeof(Transform), "m_LocalScale.y", curveY);

            AssetDatabase.CreateAsset(clip, System.IO.Path.Combine(OutputFolder, "Player_Hurt.anim"));
        }

        // ---------- Dead（死亡）：倒下 ----------
        private static void CreateDeadClip()
        {
            AnimationClip clip = new AnimationClip();
            clip.name = "Player_Dead";
            clip.wrapMode = WrapMode.Once;

            // 倒下（绕X轴旋转90度）
            AnimationCurve curve = new AnimationCurve();
            curve.AddKey(0f, 0f);
            curve.AddKey(0.5f, 90f);
            curve.AddKey(1f, 90f);
            clip.SetCurve("", typeof(Transform), "m_LocalRotation.x", curve);

            // 下沉一点
            AnimationCurve posY = new AnimationCurve();
            posY.AddKey(0f, 0f);
            posY.AddKey(0.5f, -0.5f);
            posY.AddKey(1f, -0.5f);
            clip.SetCurve("", typeof(Transform), "m_LocalPosition.y", posY);

            AssetDatabase.CreateAsset(clip, System.IO.Path.Combine(OutputFolder, "Player_Dead.anim"));
        }
    }
}
