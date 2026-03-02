package hua.dy.image.feature.paths

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hua.dy.image.data.repository.ImageRepository
import hua.dy.image.data.settings.AppSettings
import hua.dy.image.data.settings.AppSettingsStore
import hua.dy.image.db.ScanPathEntity
import hua.dy.image.db.ScanSchemeEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PathConfigUiState(
    val schemes: List<ScanSchemeEntity> = emptyList(),
    val activeSchemeId: Long = 1L,
    val paths: List<ScanPathEntity> = emptyList()
)

class PathConfigViewModel : ViewModel() {

    private val _messageFlow = MutableSharedFlow<String>()
    val messageFlow = _messageFlow.asSharedFlow()

    private val settingsFlow = AppSettingsStore.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings()
    )

    private val schemesFlow = ImageRepository.observeSchemes().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val pathsFlow = settingsFlow.flatMapLatest { settings ->
        ImageRepository.observePathConfigs(settings.activeSchemeId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val uiState = combine(schemesFlow, settingsFlow, pathsFlow) { schemes, settings, paths ->
        PathConfigUiState(
            schemes = schemes,
            activeSchemeId = settings.activeSchemeId,
            paths = paths
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PathConfigUiState()
    )

    init {
        viewModelScope.launch {
            runCatching {
                ImageRepository.ensureDefaultSchemeAndPaths()
                val activeId = settingsFlow.value.activeSchemeId
                if (ImageRepository.getSchemeById(activeId) == null) {
                    schemesFlow.value.firstOrNull()?.id?.let { AppSettingsStore.setActiveSchemeId(it) }
                }
            }.onFailure {
                _messageFlow.emit("初始化失败：${it.message ?: "未知错误"}")
            }
        }
    }

    fun selectScheme(id: Long) {
        viewModelScope.launch {
            AppSettingsStore.setActiveSchemeId(id)
            val name = uiState.value.schemes.firstOrNull { it.id == id }?.name ?: "当前方案"
            _messageFlow.emit("已切换到 $name")
        }
    }

    fun saveScheme(id: Long?, name: String, packageName: String) {
        val schemeName = name.trim()
        val packageValue = packageName.trim()
        if (schemeName.isBlank()) {
            viewModelScope.launch { _messageFlow.emit("方案名称不能为空") }
            return
        }
        if (!isValidPackageName(packageValue)) {
            viewModelScope.launch { _messageFlow.emit("包名格式不正确") }
            return
        }
        viewModelScope.launch {
            val rootPath = "/sdcard/Android/data/$packageValue"
            val folder = packageValue.replace('.', '_')
            ImageRepository.upsertScheme(
                ScanSchemeEntity(
                    id = id ?: 0L,
                    name = schemeName,
                    packageName = packageValue,
                    rootPath = rootPath,
                    saveFolder = folder
                )
            )
            _messageFlow.emit("方案已保存")
        }
    }

    fun deleteScheme(entity: ScanSchemeEntity) {
        viewModelScope.launch {
            if (uiState.value.schemes.size <= 1) {
                _messageFlow.emit("至少保留一个扫描方案")
                return@launch
            }
            ImageRepository.deleteScheme(entity)
            if (uiState.value.activeSchemeId == entity.id) {
                val fallback = uiState.value.schemes.firstOrNull { it.id != entity.id }?.id
                if (fallback != null) AppSettingsStore.setActiveSchemeId(fallback)
            }
            _messageFlow.emit("方案已删除")
        }
    }

    fun savePath(id: Long?, relativePath: String, note: String, isEnabled: Boolean) {
        val path = normalizePath(relativePath)
        if (path.isBlank() || !path.startsWith("/")) {
            viewModelScope.launch {
                _messageFlow.emit("路径必须以 / 开头，例如 /cache/picture/fresco_cache/*")
            }
            return
        }
        viewModelScope.launch {
            val activeSchemeId = uiState.value.activeSchemeId
            val orderIndex = if (id != null && id != 0L) {
                uiState.value.paths.firstOrNull { it.id == id }?.orderIndex ?: uiState.value.paths.size
            } else {
                uiState.value.paths.size
            }
            ImageRepository.upsertPathConfig(
                ScanPathEntity(
                    id = id ?: 0L,
                    schemeId = activeSchemeId,
                    relativePath = path,
                    note = note.trim(),
                    isEnabled = isEnabled,
                    orderIndex = orderIndex
                )
            )
            _messageFlow.emit("路径已保存")
        }
    }

    fun updatePathEnabled(entity: ScanPathEntity, enabled: Boolean) {
        viewModelScope.launch {
            ImageRepository.upsertPathConfig(entity.copy(isEnabled = enabled))
            _messageFlow.emit(if (enabled) "已启用路径" else "已禁用路径")
        }
    }

    fun deletePath(entity: ScanPathEntity) {
        viewModelScope.launch {
            ImageRepository.deletePathConfig(entity)
            _messageFlow.emit("路径已删除")
        }
    }

    fun restoreDefaults() {
        viewModelScope.launch {
            ImageRepository.restoreDefaultPathConfigs(uiState.value.activeSchemeId)
            _messageFlow.emit("已恢复默认路径")
        }
    }

    private fun normalizePath(path: String): String {
        return path.trim().replace("\\\\", "/")
    }

    private fun isValidPackageName(value: String): Boolean {
        if (value.isBlank() || value.length < 3) return false
        if (!value.contains('.')) return false
        return value.split('.').all { part ->
            part.isNotBlank() && (part.first().isLetter() || part.first() == '_') &&
                part.all { it.isLetterOrDigit() || it == '_' }
        }
    }
}
