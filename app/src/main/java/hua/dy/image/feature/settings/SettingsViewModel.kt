package hua.dy.image.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hua.dy.image.data.repository.ImageRepository
import hua.dy.image.data.settings.AppSettings
import hua.dy.image.data.settings.AppSettingsStore
import hua.dy.image.data.settings.ThemeMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {

    private val _messageFlow = MutableSharedFlow<String>()
    val messageFlow = _messageFlow.asSharedFlow()

    private val _isClearing = MutableStateFlow(false)
    val isClearing = _isClearing.asStateFlow()

    val uiState = AppSettingsStore.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings()
    )

    fun updateMinScanFileSizeKb(value: Int) {
        viewModelScope.launch {
            AppSettingsStore.setMinScanFileSizeKb(value)
            _messageFlow.emit("最小扫描文件大小已更新")
        }
    }

    fun updatePreferShizuku(value: Boolean) {
        viewModelScope.launch {
            AppSettingsStore.setPreferShizuku(value)
            _messageFlow.emit(if (value) "已优先使用 Shizuku" else "已切换为优先使用 SAF")
        }
    }

    fun updateSortType(value: Int) {
        viewModelScope.launch {
            AppSettingsStore.setSortType(value)
            _messageFlow.emit("默认排序已更新")
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            AppSettingsStore.setThemeMode(mode)
            _messageFlow.emit("主题模式已更新")
        }
    }

    fun updateFollowSystemDynamicColor(value: Boolean) {
        viewModelScope.launch {
            AppSettingsStore.setFollowSystemDynamicColor(value)
            _messageFlow.emit(if (value) "已启用系统动态色" else "已关闭系统动态色")
        }
    }

    fun clearDatabaseAndRescan() {
        if (_isClearing.value) return

        viewModelScope.launch {
            _isClearing.value = true
            try {
                _messageFlow.emit("正在清空扫描数据与缓存并重新扫描…")
                val settings = uiState.value
                val result = ImageRepository.clearAllDatabaseAndRescan(
                    preferredSchemeId = settings.activeSchemeId,
                    minSizeKb = settings.minScanFileSizeKb,
                    preferShizuku = settings.preferShizuku
                )
                if (result.schemeId != settings.activeSchemeId) {
                    AppSettingsStore.setActiveSchemeId(result.schemeId)
                }
                val summary = result.scanSummary
                _messageFlow.emit(
                    "完成：扫描 ${summary.scanned} 个文件，新增 ${summary.added}，重复 ${summary.duplicates}，失败 ${summary.failed}"
                )
            } catch (e: Exception) {
                _messageFlow.emit("清理失败：${e.message ?: "未知错误"}")
            } finally {
                _isClearing.value = false
            }
        }
    }
}
