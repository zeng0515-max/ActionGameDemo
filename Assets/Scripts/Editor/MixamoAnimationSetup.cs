using UnityEditor;
using UnityEngine;
using System.IO;
using System.Collections.Generic;

namespace ActionGameDemo.Editor
{
    /// <summary>
    /// Mixamo动画配置工具。
    /// 将Mixamo下载的动画片段通过AnimatorOverrideController挂载到九尾狐角色。
    /// 菜单: Tools/ActionGameDemo/配置Mixamo动画
    /// </summary>
    public static class MixamoAnimationSetup
    {
        private const string ConfigPath = "Assets/Config/ModelPromptConfig.asset";
        private const string DefaultBaseController = "Assets/Animations/PlayerController_NineTailedFox.controller";
        private const string OverrideControllerPath = "Assets/Animations/PlayerController_NineTailedFox_Override.overrideController";
        private const string RiggedPrefabPath = "Assets/Characters/NineTailedFox_Rigged.prefab";

        [MenuItem("Tools/ActionGameDemo/配置Mixamo动画")]
        public static void SetupMixamoAnimations()
        {
            ModelPromptConfig config = AssetDatabase.LoadAssetAtPath<ModelPromptConfig>(ConfigPath);
            if (config == null)
            {
                EditorUtility.DisplayDialog("错误",
                    "未找到提示词配置文件。\n" +
                    "请先运行「打开提示词配置」创建配置。", "确定");
                return;
            }

            // 确定基础Controller
            RuntimeAnimatorController baseController = config.baseController;
            if (baseController == null)
            {
                baseController = AssetDatabase.LoadAssetAtPath<RuntimeAnimatorController>(DefaultBaseController);
            }
            if (baseController == null)
            {
                EditorUtility.DisplayDialog("错误",
                    "未找到基础Animator Controller。\n" +
                    $"请检查路径: {DefaultBaseController}\n" +
                    "或在提示词配置中手动指定。", "确定");
                return;
            }

            // 检查是否至少配置了一个动画片段
            int configuredCount = 0;
            foreach (var map in config.animationClips)
            {
                if (map.clip != null) configuredCount++;
            }

            if (configuredCount == 0)
            {
                bool openConfig = EditorUtility.DisplayDialog("提示",
                    "尚未配置任何动画片段。\n\n" +
                    "请在提示词配置中为各状态指定Mixamo动画片段。\n\n" +
                    "Mixamo下载步骤:\n" +
                    "1. 访问 mixamo.com\n" +
                    "2. 搜索所需动作（如 idle, walk, attack）\n" +
                    "3. 下载为 FBX for Unity 格式\n" +
                    "4. 导入Unity后将动画片段拖入配置\n\n" +
                    "是否现在打开配置？",
                    "打开配置", "取消");

                if (openConfig)
                {
                    Selection.activeObject = config;
                    EditorUtility.FocusProjectWindow();
                }
                return;
            }

            // 创建OverrideController
            AnimatorOverrideController overrideController = CreateOverrideController(baseController, config);

            // 保存OverrideController
            EnsureDirectoryExists(Path.GetDirectoryName(OverrideControllerPath));
            if (File.Exists(OverrideControllerPath))
            {
                AssetDatabase.DeleteAsset(OverrideControllerPath);
            }
            AssetDatabase.CreateAsset(overrideController, OverrideControllerPath);
            AssetDatabase.SaveAssets();
            AssetDatabase.Refresh();

            // 应用到角色预制件
            ApplyToPrefab(overrideController);

            Debug.Log($"[MixamoAnimationSetup] Override Controller已创建: {OverrideControllerPath}");
            Debug.Log($"[MixamoAnimationSetup] 已配置 {configuredCount}/{config.animationClips.Length} 个动画片段");

            EditorUtility.DisplayDialog("成功",
                $"Mixamo动画配置完成!\n\n" +
                $"Override Controller: {OverrideControllerPath}\n" +
                $"已配置: {configuredCount}/{config.animationClips.Length} 个动画\n\n" +
                "未配置的状态将使用占位动画。\n" +
                "后续可在提示词配置中继续添加动画，\n" +
                "重新运行此工具即可更新。",
                "确定");
        }

        /// <summary>
        /// 创建AnimatorOverrideController，用Mixamo动画替换基础Controller中的对应片段
        /// </summary>
        private static AnimatorOverrideController CreateOverrideController(
            RuntimeAnimatorController baseController, ModelPromptConfig config)
        {
            AnimatorOverrideController aoc = new AnimatorOverrideController(baseController);

            // 获取基础Controller中所有动画片段
            List<KeyValuePair<AnimationClip, AnimationClip>> overrides =
                new List<KeyValuePair<AnimationClip, AnimationClip>>();
            aoc.GetOverrides(overrides);

            // 构建状态名→新动画片段的查找表
            Dictionary<string, AnimationClip> clipLookup = new Dictionary<string, AnimationClip>();
            foreach (var map in config.animationClips)
            {
                if (map.clip != null)
                {
                    clipLookup[map.stateName.ToLower()] = map.clip;
                }
            }

            // 状态名→基础动画片段名关键词映射
            // 基础Controller中的片段名: Player_Idle, Player_Walk, Player_Attack 等
            var stateToClipName = new Dictionary<string, string[]>
            {
                { "idle", new[] { "idle" } },
                { "move", new[] { "walk", "move", "run" } },
                { "jump", new[] { "jump" } },
                { "attack", new[] { "attack" } },
                { "skill", new[] { "skill", "cast", "magic" } },
                { "ultimate", new[] { "ultimate", "ulta", "power" } },
                { "dodge", new[] { "dodge", "roll", "dash", "backflip" } },
                { "hurt", new[] { "hurt", "hit", "damage" } },
                { "dead", new[] { "dead", "death", "die" } },
            };

            // 遍历所有原始片段，匹配并替换
            for (int i = 0; i < overrides.Count; i++)
            {
                AnimationClip originalClip = overrides[i].Key;
                if (originalClip == null) continue;

                string originalName = originalClip.name.ToLower();

                foreach (var kvp in stateToClipName)
                {
                    if (!clipLookup.ContainsKey(kvp.Key)) continue;

                    foreach (string keyword in kvp.Value)
                    {
                        if (originalName.Contains(keyword))
                        {
                            overrides[i] = new KeyValuePair<AnimationClip, AnimationClip>(
                                originalClip, clipLookup[kvp.Key]);
                            Debug.Log($"[MixamoAnimationSetup] 替换: {originalClip.name} → {clipLookup[kvp.Key].name}");
                            break;
                        }
                    }
                }
            }

            aoc.ApplyOverrides(overrides);
            return aoc;
        }

        /// <summary>
        /// 将OverrideController应用到角色预制件的Animator组件
        /// </summary>
        private static void ApplyToPrefab(AnimatorOverrideController overrideController)
        {
            GameObject prefab = AssetDatabase.LoadAssetAtPath<GameObject>(RiggedPrefabPath);
            if (prefab == null)
            {
                Debug.LogWarning($"[MixamoAnimationSetup] 未找到角色预制件: {RiggedPrefabPath}");
                Debug.LogWarning("请先运行「构建九尾狐模型（保留骨骼）」");
                return;
            }

            // 实例化预制件，修改Animator，保存回去
            GameObject instance = PrefabUtility.LoadPrefabContents(RiggedPrefabPath);

            Animator animator = instance.GetComponent<Animator>();
            if (animator == null)
            {
                animator = instance.AddComponent<Animator>();
            }

            animator.runtimeAnimatorController = overrideController;
            animator.applyRootMotion = false;

            PrefabUtility.SaveAsPrefabAsset(instance, RiggedPrefabPath);
            PrefabUtility.UnloadPrefabContents(instance);

            Debug.Log($"[MixamoAnimationSetup] OverrideController已应用到: {RiggedPrefabPath}");
        }

        [MenuItem("Tools/ActionGameDemo/Mixamo动画导入指南")]
        public static void ShowImportGuide()
        {
            EditorUtility.DisplayDialog("Mixamo动画导入指南",
                "1. 访问 https://www.mixamo.com\n" +
                "2. 注册/登录Adobe账号\n\n" +
                "3. 上传角色FBX（可选，用Mixamo骨架也可）\n" +
                "4. 搜索所需动作:\n" +
                "   - Idle: 搜索 'idle'\n" +
                "   - Walk/Run: 搜索 'walk' 或 'run'\n" +
                "   - Jump: 搜索 'jump'\n" +
                "   - Attack: 搜索 'slash' 'punch' 'kick'\n" +
                "   - Skill: 搜索 'cast' 'magic' 'spell'\n" +
                "   - Ultimate: 搜索 'power' 'ulta'\n" +
                "   - Dodge: 搜索 'dodge' 'roll' 'dash'\n" +
                "   - Hurt: 搜索 'hit' 'hurt' 'react'\n" +
                "   - Dead: 搜索 'death' 'die' 'dead'\n\n" +
                "5. 下载设置:\n" +
                "   - Format: FBX for Unity\n" +
                "   - Skin: Without Skin\n" +
                "   - Frames: 按需\n\n" +
                "6. 导入Unity后:\n" +
                "   - 选中FBX → Rig → Animation Type: Humanoid\n" +
                "   - Avatar Definition: Create From This Model\n" +
                "   - Apply\n\n" +
                "7. 打开提示词配置 (Tools → 打开提示词配置)\n" +
                "   将动画片段拖入对应状态\n\n" +
                "8. 运行「配置Mixamo动画」",
                "知道了");
        }

        private static void EnsureDirectoryExists(string directory)
        {
            if (string.IsNullOrEmpty(directory)) return;
            string fullPath = Path.GetFullPath(directory);
            if (!Directory.Exists(fullPath))
            {
                Directory.CreateDirectory(fullPath);
            }
        }
    }
}
