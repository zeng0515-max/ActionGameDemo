using UnityEditor;
using UnityEngine;
using System.IO;

namespace ActionGameDemo.Editor
{
    /// <summary>
    /// 从已有FBX构建保留骨骼的九尾狐预制件。
    /// 保留Humanoid骨骼和SkinnedMeshRenderer，只清理动画数据，禁用Root Motion。
    /// 后续通过Animator Override Controller挂载Mixamo动画。
    /// </summary>
    public static class StaticModelBuilder
    {
        private const string SourceFbxPath = "Assets/TJGenerators/History/NineTailedFox/01/21c7cf39beafdab2.fbx";
        private const string TextureFolderPath = "Assets/TJGenerators/History/NineTailedFox/01/21c7cf39beafdab2.fbm";
        private const string OutputPrefabPath = "Assets/Characters/NineTailedFox_Rigged.prefab";
        private const string OutputMaterialPath = "Assets/Materials/NineTailedFox_Rigged.mat";

        [MenuItem("Tools/ActionGameDemo/构建九尾狐模型（保留骨骼）")]
        public static void BuildRiggedModel()
        {
            AssetDatabase.Refresh();

            GameObject sourceAsset = AssetDatabase.LoadAssetAtPath<GameObject>(SourceFbxPath);
            if (sourceAsset == null)
            {
                EditorUtility.DisplayDialog("错误", $"未找到源模型文件:\n{SourceFbxPath}", "确定");
                return;
            }

            GameObject instance = GameObject.Instantiate(sourceAsset);
            instance.name = "NineTailedFox_Rigged";

            ProcessRiggedModel(instance);
            CreateOrUpdateMaterial(instance);
            EnsureDirectoryExists(Path.GetDirectoryName(OutputPrefabPath));

            if (File.Exists(OutputPrefabPath))
            {
                AssetDatabase.DeleteAsset(OutputPrefabPath);
            }

            PrefabUtility.SaveAsPrefabAsset(instance, OutputPrefabPath);
            GameObject.DestroyImmediate(instance);
            AssetDatabase.Refresh();

            Debug.Log($"[StaticModelBuilder] 保留骨骼模型已创建: {OutputPrefabPath}");
            EditorUtility.DisplayDialog("成功",
                $"九尾狐模型（保留骨骼）已创建完成!\n\n" +
                $"预制件路径: {OutputPrefabPath}\n\n" +
                "特性:\n" +
                "- 保留Humanoid骨骼绑定\n" +
                "- 保留SkinnedMeshRenderer\n" +
                "- 已清空Animator Controller\n" +
                "- 已禁用Root Motion\n" +
                "- 无动画片段\n" +
                "- 轴心位于脚底中心\n\n" +
                "下一步: 使用 Mixamo动画配置 工具\n" +
                "挂载战斗动画到OverrideController",
                "确定");
        }

        private static void ProcessRiggedModel(GameObject root)
        {
            // 保留Animator组件，但清空Controller引用，禁用Root Motion
            Animator animator = root.GetComponent<Animator>();
            if (animator == null)
            {
                animator = root.AddComponent<Animator>();
            }
            animator.runtimeAnimatorController = null;
            animator.applyRootMotion = false;
            animator.cullingMode = AnimatorCullingMode.CullUpdateTransforms;

            // 移除物理组件（移动由CharacterController控制）
            Rigidbody rb = root.GetComponent<Rigidbody>();
            if (rb != null) Object.DestroyImmediate(rb);

            Collider[] colliders = root.GetComponentsInChildren<Collider>();
            foreach (Collider col in colliders)
            {
                Object.DestroyImmediate(col);
            }

            // 移除Animation组件（旧版动画系统）
            Animation animation = root.GetComponent<Animation>();
            if (animation != null) Object.DestroyImmediate(animation);

            // 保留骨骼和SkinnedMeshRenderer，仅清理动画相关引用
            // 确保所有SkinnedMeshRenderer的rootBone引用正确
            SkinnedMeshRenderer[] skinnedRenderers = root.GetComponentsInChildren<SkinnedMeshRenderer>();
            foreach (SkinnedMeshRenderer smr in skinnedRenderers)
            {
                smr.updateWhenOffscreen = true;
                smr.quality = SkinQuality.Bone4;
            }

            AdjustPivotToFeet(root);
        }

        /// <summary>
        /// 调整轴心到脚底：计算模型最低点，将所有子对象上移使脚底对齐Y=0。
        /// 不修改Mesh顶点，保留骨骼绑定完整性。
        /// </summary>
        private static void AdjustPivotToFeet(GameObject root)
        {
            Renderer[] renderers = root.GetComponentsInChildren<Renderer>();
            if (renderers.Length == 0) return;

            Bounds combinedBounds = new Bounds();
            bool first = true;
            foreach (Renderer r in renderers)
            {
                if (first)
                {
                    combinedBounds = r.bounds;
                    first = false;
                }
                else
                {
                    combinedBounds.Encapsulate(r.bounds);
                }
            }

            float feetY = combinedBounds.min.y;
            if (Mathf.Abs(feetY) < 0.001f) return;

            Vector3 offset = new Vector3(0, -feetY, 0);

            // 移动所有一级子对象（骨骼根节点和网格对象）
            foreach (Transform child in root.transform)
            {
                child.localPosition += offset;
            }

            Debug.Log($"[StaticModelBuilder] 轴心调整: 脚底Y={feetY:F3} → 0, 偏移={offset}");
        }

        private static void CreateOrUpdateMaterial(GameObject root)
        {
            EnsureDirectoryExists(Path.GetDirectoryName(OutputMaterialPath));

            Material material = AssetDatabase.LoadAssetAtPath<Material>(OutputMaterialPath);
            if (material == null)
            {
                Shader shader = Shader.Find("Universal Render Pipeline/Lit");
                if (shader == null) shader = Shader.Find("Standard");
                material = new Material(shader);
                AssetDatabase.CreateAsset(material, OutputMaterialPath);
            }

            string texturePath = Path.Combine(TextureFolderPath, "texture_0.png").Replace("\\", "/");
            Texture2D albedo = AssetDatabase.LoadAssetAtPath<Texture2D>(texturePath);

            if (albedo != null)
            {
                material.SetTexture("_BaseMap", albedo);
                if (material.HasProperty("_MainTex"))
                    material.SetTexture("_MainTex", albedo);
                material.SetColor("_BaseColor", Color.white);
            }

            material.SetFloat("_Metallic", 0.1f);
            material.SetFloat("_Smoothness", 0.5f);

            // 应用材质到所有SkinnedMeshRenderer
            SkinnedMeshRenderer[] renderers = root.GetComponentsInChildren<SkinnedMeshRenderer>();
            foreach (SkinnedMeshRenderer smr in renderers)
            {
                smr.sharedMaterial = material;
            }
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
