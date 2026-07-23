using UnityEngine;
using UnityEngine.UI;
using UnityEngine.Audio;

namespace ActionGameDemo.UI
{
    public class SettingsPanel : UIBase
    {
        [SerializeField] private Slider _masterVolumeSlider;
        [SerializeField] private Slider _musicVolumeSlider;
        [SerializeField] private Slider _sfxVolumeSlider;
        [SerializeField] private Dropdown _qualityDropdown;
        [SerializeField] private Button _closeButton;

        [SerializeField] private AudioMixer _audioMixer;

        private const string MASTER_VOLUME_KEY = "MasterVolume";
        private const string MUSIC_VOLUME_KEY = "MusicVolume";
        private const string SFX_VOLUME_KEY = "SFXVolume";
        private const string QUALITY_KEY = "QualityLevel";

        protected override void OnShow()
        {
            InitSliders();
            InitQualityDropdown();
            if (_closeButton != null) _closeButton.onClick.AddListener(OnClose);
        }

        protected override void OnHide()
        {
            SaveSettings();
            if (_closeButton != null) _closeButton.onClick.RemoveListener(OnClose);
        }

        private void InitSliders()
        {
            if (_masterVolumeSlider != null)
            {
                _masterVolumeSlider.value = PlayerPrefs.GetFloat(MASTER_VOLUME_KEY, 1f);
                _masterVolumeSlider.onValueChanged.AddListener(SetMasterVolume);
            }
            if (_musicVolumeSlider != null)
            {
                _musicVolumeSlider.value = PlayerPrefs.GetFloat(MUSIC_VOLUME_KEY, 1f);
                _musicVolumeSlider.onValueChanged.AddListener(SetMusicVolume);
            }
            if (_sfxVolumeSlider != null)
            {
                _sfxVolumeSlider.value = PlayerPrefs.GetFloat(SFX_VOLUME_KEY, 1f);
                _sfxVolumeSlider.onValueChanged.AddListener(SetSfxVolume);
            }
        }

        private void InitQualityDropdown()
        {
            if (_qualityDropdown == null) return;
            _qualityDropdown.ClearOptions();
            _qualityDropdown.AddOptions(new System.Collections.Generic.List<string>(QualitySettings.names));
            _qualityDropdown.value = PlayerPrefs.GetInt(QUALITY_KEY, QualitySettings.GetQualityLevel());
            _qualityDropdown.onValueChanged.AddListener(SetQuality);
        }

        public void SetMasterVolume(float value)
        {
            if (_audioMixer != null) _audioMixer.SetFloat("MasterVolume", Mathf.Log10(Mathf.Max(0.001f, value)) * 20f);
        }

        public void SetMusicVolume(float value)
        {
            if (_audioMixer != null) _audioMixer.SetFloat("MusicVolume", Mathf.Log10(Mathf.Max(0.001f, value)) * 20f);
        }

        public void SetSfxVolume(float value)
        {
            if (_audioMixer != null) _audioMixer.SetFloat("SfxVolume", Mathf.Log10(Mathf.Max(0.001f, value)) * 20f);
        }

        public void SetQuality(int index)
        {
            QualitySettings.SetQualityLevel(index, true);
        }

        private void SaveSettings()
        {
            if (_masterVolumeSlider != null) PlayerPrefs.SetFloat(MASTER_VOLUME_KEY, _masterVolumeSlider.value);
            if (_musicVolumeSlider != null) PlayerPrefs.SetFloat(MUSIC_VOLUME_KEY, _musicVolumeSlider.value);
            if (_sfxVolumeSlider != null) PlayerPrefs.SetFloat(SFX_VOLUME_KEY, _sfxVolumeSlider.value);
            if (_qualityDropdown != null) PlayerPrefs.SetInt(QUALITY_KEY, _qualityDropdown.value);
            PlayerPrefs.Save();
        }

        private void OnClose()
        {
            Manager?.OnCloseSettings();
        }
    }
}
