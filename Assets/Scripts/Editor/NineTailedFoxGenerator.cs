using UnityEditor;
using UnityEngine;
using System.Collections.Generic;
using System.IO;
using Codely.Newtonsoft.Json.Linq;

namespace ActionGameDemo.Editor
{
    /// <summary>
    /// 使用TJGenerators生成九尾狐模型。
    /// 提示词从ModelPromptConfig读取，可在Unity Inspector中编辑。
    /// 菜单: Tools/ActionGameDemo/生成九尾狐模型（保留骨骼）
    /// </summary>
    public static class NineTailedFoxGenerator
    {
        private const string ConfigPath = "Assets/Config/ModelPromptConfig.asset";

        [MenuItem("Tools/ActionGameDemo/生成九尾狐模型（保留骨骼）")]
        public static void GenerateNineTailedFox()
        {
            ModelPromptConfig config = GetOrCreateConfig();
            if (config == null)
            {
                EditorUtility.DisplayDialog("错误", "无法创建或加载提示词配置文件", "确定");
                return;
            }

            var parameters = new Dictionary<string, object>
            {
                { "prompt", config.prompt },
                { "prefab_output_path", config.prefabOutputPath },
                { "force_overwrite", config.forceOverwrite },
                { "face_count", config.faceCount },
                { "enable_pbr", config.enablePbr },
                { "result_format", config.resultFormat }
            };

            Debug.Log($"[NineTailedFoxGenerator] 提示词:\n{config.prompt}");
            Debug.Log($"[NineTailedFoxGenerator] 输出路径: {config.prefabOutputPath}");

            var result = UnityTcp.Editor.Tools.Generate3DModelTool.Generate3DModel(JObject.FromObject(parameters));

            var resultDict = result as Dictionary<string, object>;
            if (resultDict != null && (bool)resultDict["success"])
            {
                Debug.Log($"[NineTailedFoxGenerator] 模型生成任务已提交");
                Debug.Log($"  task_id: {resultDict["task_id"]}");
                Debug.Log($"  预计等待时间: {resultDict["estimated_wait_seconds"]}秒");

                EditorUtility.DisplayDialog("生成任务已提交",
                    "九尾狐模型生成任务已提交到TJGenerators后台。\n\n" +
                    $"输出路径: {config.prefabOutputPath}\n\n" +
                    "模型将保留Humanoid骨骼，不带动画片段。\n\n" +
                    "生成完成后:\n" +
                    "1. 运行「构建九尾狐模型（保留骨骼）」清理动画数据\n" +
                    "2. 运行「配置Mixamo动画」挂载战斗动画",
                    "知道了");

                Selection.activeObject = config;
            }
            else
            {
                string message = resultDict != null ? resultDict["message"]?.ToString() : "未知错误";
                Debug.LogError($"[NineTailedFoxGenerator] 生成失败: {message}");
                EditorUtility.DisplayDialog("生成失败",
                    $"生成任务提交失败:\n{message}\n\n" +
                    "可先使用「构建九尾狐模型（保留骨骼）」\n" +
                    "处理已有的FBX模型资源",
                    "确定");
            }
        }

        [MenuItem("Tools/ActionGameDemo/查询模型生成状态")]
        public static void QueryStatus()
        {
            var tasksResult = UnityTcp.Editor.Tools.Generate3DModelTool.ListTasks(new JObject());
            var tasksDict = tasksResult as Dictionary<string, object>;

            if (tasksDict != null && (bool)tasksDict["success"])
            {
                var tasks = tasksDict["tasks"] as List<object>;
                if (tasks != null && tasks.Count > 0)
                {
                    string statusText = "";
                    foreach (var taskObj in tasks)
                    {
                        var task = taskObj as Dictionary<string, object>;
                        if (task != null)
                        {
                            statusText += $"任务ID: {task["task_id"]}\n";
                            statusText += $"状态: {task["status"]}\n";
                            statusText += $"进度: {task["progress"]}%\n";
                            string promptStr = task["prompt"]?.ToString() ?? "";
                            statusText += $"提示词: {promptStr.Substring(0, Mathf.Min(50, promptStr.Length))}...\n";
                            if (task.ContainsKey("model_path"))
                                statusText += $"模型路径: {task["model_path"]}\n";
                            if (task.ContainsKey("prefab_path"))
                                statusText += $"预制件路径: {task["prefab_path"]}\n";
                            statusText += "------------------------\n";
                        }
                    }
                    EditorUtility.DisplayDialog("生成任务状态", statusText, "确定");
                }
                else
                {
                    EditorUtility.DisplayDialog("生成任务状态", "没有找到任何生成任务", "确定");
                }
            }
        }

        [MenuItem("Tools/ActionGameDemo/打开提示词配置")]
        public static void OpenConfig()
        {
            ModelPromptConfig config = GetOrCreateConfig();
            if (config != null)
            {
                Selection.activeObject = config;
                EditorUtility.FocusProjectWindow();
            }
        }

        /// <summary>
        /// 查找或创建ModelPromptConfig配置资产
        /// </summary>
        private static ModelPromptConfig GetOrCreateConfig()
        {
            ModelPromptConfig config = AssetDatabase.LoadAssetAtPath<ModelPromptConfig>(ConfigPath);
            if (config != null) return config;

            string dir = Path.GetDirectoryName(ConfigPath);
            if (!Directory.Exists(Path.GetFullPath(dir)))
            {
                Directory.CreateDirectory(Path.GetFullPath(dir));
            }

            config = ScriptableObject.CreateInstance<ModelPromptConfig>();
            AssetDatabase.CreateAsset(config, ConfigPath);
            AssetDatabase.SaveAssets();
            AssetDatabase.Refresh();

            Debug.Log($"[NineTailedFoxGenerator] 已创建提示词配置: {ConfigPath}");
            return config;
        }
    }
}
