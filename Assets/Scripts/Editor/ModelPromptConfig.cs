using UnityEngine;

namespace ActionGameDemo.Editor
{
    [CreateAssetMenu(fileName = "ModelPromptConfig", menuName = "ActionGameDemo/模型生成提示词配置")]
    public class ModelPromptConfig : ScriptableObject
    {
        [Header("模型生成提示词（可在Inspector中直接编辑）")]
        [TextArea(15, 30)]
        [Tooltip("发送给TJGenerators的模型生成提示词")]
        public string prompt = @"完整单一古风九尾狐女性角色，标准人形结构：
- 头部：白粉色狐耳，银白渐变长发，五官精致，妆容淡雅
- 身体：身姿优雅，标准女性人体比例，左右双臂完整齐全，双手完整，手指完整，双腿完整，不缺失任何肢体
- 尾部：九条蓬松狐尾完整分开且互不穿插，尾部毛发从根部白色渐变到粉色尖端
- 服饰：高腰古风层叠飘逸纱质连衣裙，米白色主色调，裙摆层次感丰富；黑色半透明丝绸质感薄纱长袜；精致古风绣花鞋履
- 法器：右手持悬浮圆形玉石古风法器，法器带有风火水土雷五种元素微光（红色火焰、蓝色水流、绿色大地、黄色雷电、白色光芒）
- 姿态：站立姿势，身体垂直正向，双臂自然下垂，右手抬起托举法器
- 风格：写实PBR次世代游戏美术风格，高品质纹理细节，金属光泽与布料质感表现逼真
- 技术要求：模型拓扑整洁，闭合网格，无破面，无多余方块，无背景几何体，无漂浮物，无穿模
- 骨骼：标准Humanoid人形骨骼绑定，适合Unity Humanoid重定向
- 约束：不生成动画片段，模型轴心位于脚底中心，Y轴向上，模型垂直正向站立，禁用Root Motion";

        [Header("生成参数")]
        [Tooltip("预制件输出路径")]
        public string prefabOutputPath = "Assets/Characters/NineTailedFox_Rigged.prefab";

        [Tooltip("模型面数上限")]
        public int faceCount = 150000;

        [Tooltip("启用PBR材质")]
        public bool enablePbr = true;

        [Tooltip("输出格式")]
        public string resultFormat = "FBX";

        [Tooltip("强制覆盖已有文件")]
        public bool forceOverwrite = true;

        [Header("动画配置（Mixamo重定向）")]
        [Tooltip("基础Animator Controller（状态机模板）")]
        public RuntimeAnimatorController baseController;

        [Tooltip("动画片段映射 - 对应9个状态")]
        public AnimationClipMap[] animationClips = new AnimationClipMap[]
        {
            new AnimationClipMap { stateName = "Idle", clip = null },
            new AnimationClipMap { stateName = "Move", clip = null },
            new AnimationClipMap { stateName = "Jump", clip = null },
            new AnimationClipMap { stateName = "Attack", clip = null },
            new AnimationClipMap { stateName = "Skill", clip = null },
            new AnimationClipMap { stateName = "Ultimate", clip = null },
            new AnimationClipMap { stateName = "Dodge", clip = null },
            new AnimationClipMap { stateName = "Hurt", clip = null },
            new AnimationClipMap { stateName = "Dead", clip = null },
        };

        [System.Serializable]
        public class AnimationClipMap
        {
            public string stateName;
            public AnimationClip clip;
        }
    }
}
