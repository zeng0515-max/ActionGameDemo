using UnityEngine;
using UnityEngine.UI;
using ActionGameDemo.Progression;
using ActionGameDemo.Character;

namespace ActionGameDemo.UI
{
    public class SkillTreePanel : MonoBehaviour
    {
        [SerializeField] private Transform _nodeContainer;
        [SerializeField] private GameObject _nodePrefab;
        [SerializeField] private Text _skillPointsText;
        [SerializeField] private GameObject _panelRoot;

        [SerializeField] private SkillTree _skillTree;
        private CharacterBase _character;
        private LevelSystem _levelSystem;

        private void Start()
        {
            if (_panelRoot != null) _panelRoot.SetActive(false);
            var player = FindFirstObjectByType<Player.PlayerController>();
            if (player != null)
            {
                _character = player.GetComponent<CharacterBase>();
                _levelSystem = player.GetComponent<LevelSystem>();
            }
            RebuildNodeList();
        }

        private void RebuildNodeList()
        {
            if (_nodeContainer == null || _nodePrefab == null || _skillTree == null) return;

            foreach (Transform child in _nodeContainer)
                Destroy(child.gameObject);

            for (int i = 0; i < _skillTree.Nodes.Count; i++)
            {
                var node = _skillTree.Nodes[i];
                GameObject item = Instantiate(_nodePrefab, _nodeContainer);

                var nameText = item.GetComponentInChildren<Text>();
                if (nameText != null)
                    nameText.text = $"{node.displayName}\n(Lv.{node.requiredLevel}, {node.cost}SP)\n{node.effectDescription}";

                var button = item.GetComponentInChildren<Button>();
                if (button != null)
                {
                    int capturedIndex = i;
                    button.onClick.AddListener(() => TryUnlockNode(capturedIndex));
                }

                var image = item.GetComponent<Image>();
                if (image != null)
                    image.color = _skillTree.IsUnlocked(node.nodeId) ? new Color(0.3f, 0.8f, 0.3f) : new Color(0.5f, 0.5f, 0.5f);
            }

            UpdateSkillPointsDisplay();
        }

        private void TryUnlockNode(int index)
        {
            if (_skillTree == null || _character == null || _levelSystem == null) return;
            if (index < 0 || index >= _skillTree.Nodes.Count) return;

            var node = _skillTree.Nodes[index];
            if (_skillTree.TryUnlock(node.nodeId, _levelSystem.Level, _character.Stats))
            {
                RebuildNodeList();
            }
        }

        private void UpdateSkillPointsDisplay()
        {
            if (_skillPointsText != null && _skillTree != null)
                _skillPointsText.text = $"技能点: {_skillTree.AvailableSkillPoints}";
        }

        public void TogglePanel()
        {
            if (_panelRoot != null)
                _panelRoot.SetActive(!_panelRoot.activeInHierarchy);
        }

        public void ShowPanel()
        {
            if (_panelRoot != null) _panelRoot.SetActive(true);
        }

        public void HidePanel()
        {
            if (_panelRoot != null) _panelRoot.SetActive(false);
        }
    }
}
