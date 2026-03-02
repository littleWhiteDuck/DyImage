package hua.dy.image.feature.gallery

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import hua.dy.image.bean.ImageBean
import hua.dy.image.data.repository.ImageRepository
import hua.dy.image.data.settings.AppSettings
import hua.dy.image.data.settings.AppSettingsStore
import hua.dy.image.db.ScanSchemeEntity
import hua.dy.image.utils.FileExplorerServiceManager
import hua.dy.image.utils.FileType
import hua.dy.image.utils.ShizukuUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PathFilterOption(
    val label: String,
    val path: String?
) {
    val isAll: Boolean get() = path == null
}

class GalleryViewModel : ViewModel() {

    private val _refreshToken = MutableStateFlow(0)
    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _messageFlow = MutableSharedFlow<String>()
    val messageFlow = _messageFlow.asSharedFlow()

    private val _selectedPathFilter = MutableStateFlow(PathFilterOption(label = ALL_PATHS_LABEL, path = null))
    val selectedPathFilter = _selectedPathFilter.asStateFlow()

    var needShizuku by mutableStateOf(true)

    private val settingsFlow = AppSettingsStore.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings()
    )

    val sortType = settingsFlow.map { it.sortType }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 0
    )

    private val schemesFlow = ImageRepository.observeSchemes().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val activeScheme = combine(schemesFlow, settingsFlow) { schemes, settings ->
        schemes.firstOrNull { it.id == settings.activeSchemeId } ?: schemes.firstOrNull()
    }.filterNotNull().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ScanSchemeEntity(
            id = 1L,
            name = "抖音",
            packageName = "com.ss.android.ugc.aweme",
            rootPath = "/sdcard/Android/data/com.ss.android.ugc.aweme",
            saveFolder = "douyin"
        )
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val currentSchemePaths = activeScheme.flatMapLatest { scheme ->
        ImageRepository.observePathConfigs(scheme.id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val pathFilterOptions = currentSchemePaths.map { paths ->
        buildList {
            add(PathFilterOption(label = ALL_PATHS_LABEL, path = null))
            paths.filter { it.isEnabled }.forEach { path ->
                add(
                    PathFilterOption(
                        label = ImageRepository.getPathFilterLabel(path),
                        path = path.relativePath
                    )
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = listOf(PathFilterOption(label = ALL_PATHS_LABEL, path = null))
    )

    private val pageFlowCache = mutableMapOf<FileType, Flow<PagingData<ImageBean>>>()
    private val autoLoadCheckedSchemes = mutableSetOf<Long>()

    init {
        viewModelScope.launch {
            runCatching {
                ImageRepository.ensureDefaultSchemeAndPaths()
                val settings = settingsFlow.value
                if (ImageRepository.getSchemeById(settings.activeSchemeId) == null) {
                    val first = schemesFlow.value.firstOrNull()?.id ?: 1L
                    AppSettingsStore.setActiveSchemeId(first)
                }
            }.onFailure {
                _messageFlow.emit("初始化失败：${it.message ?: "未知错误"}")
            }
        }

        viewModelScope.launch {
            activeScheme.map { it.id }.distinctUntilChanged().collect {
                _selectedPathFilter.value = PathFilterOption(label = ALL_PATHS_LABEL, path = null)
            }
        }

        viewModelScope.launch {
            pathFilterOptions.collect { options ->
                val selected = _selectedPathFilter.value
                if (options.none { it.path == selected.path }) {
                    _selectedPathFilter.value = PathFilterOption(label = ALL_PATHS_LABEL, path = null)
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun pagedImagesForType(type: FileType): Flow<PagingData<ImageBean>> {
        return pageFlowCache.getOrPut(type) {
            combine(
                settingsFlow,
                _selectedPathFilter,
                activeScheme,
                _refreshToken
            ) { settings, filter, scheme, _ ->
                GalleryQuery(
                    type = type,
                    sortType = settings.sortType,
                    filter = filter,
                    scheme = scheme
                )
            }.flatMapLatest { query ->
                Pager(
                    config = PagingConfig(
                        pageSize = 60,
                        enablePlaceholders = true,
                        maxSize = 240
                    ),
                    pagingSourceFactory = {
                        ImageRepository.getImagePagingSource(
                            type = query.type,
                            sortType = query.sortType,
                            schemeTag = ImageRepository.schemeTag(query.scheme.id)
                        )
                    }
                ).flow.map { pagingData ->
                    if (query.filter.isAll) {
                        pagingData
                    } else {
                        pagingData.filter { it.cachePath == query.filter.path }
                    }
                }
            }.cachedIn(viewModelScope)
        }
    }

    fun updateSortType(value: Int) {
        viewModelScope.launch {
            AppSettingsStore.setSortType(value)
        }
    }

    fun updatePathFilter(option: PathFilterOption) {
        _selectedPathFilter.value = option
    }

    fun refresh() {
        if (_isScanning.value) return
        viewModelScope.launch {
            val enabledPathCount = currentSchemePaths.value.count { it.isEnabled }
            if (enabledPathCount == 0) {
                _messageFlow.emit("当前方案没有启用的扫描路径")
                return@launch
            }

            val scheme = activeScheme.value
            val settings = settingsFlow.value
            _isScanning.value = true
            val result = runCatching {
                ImageRepository.scanConfiguredPaths(
                    scheme = scheme,
                    minFileSizeKb = settings.minScanFileSizeKb,
                    preferShizuku = settings.preferShizuku
                )
            }
            _isScanning.value = false
            result.onSuccess { summary ->
                val source = if (summary.useShizuku) "Shizuku" else "SAF"
                _messageFlow.emit(
                    "扫描完成（$source）：新增 ${summary.added}，重复 ${summary.duplicates}，小文件过滤 ${summary.skippedSmallFiles}，失败 ${summary.failed}"
                )
                _refreshToken.value += 1
            }.onFailure {
                _messageFlow.emit("扫描失败：${it.message ?: "未知错误"}")
            }
        }
    }

    fun refreshIfEmptyAndPermitted(hasPermission: Boolean) {
        if (!hasPermission) return
        val schemeId = activeScheme.value.id
        if (autoLoadCheckedSchemes.contains(schemeId)) return
        autoLoadCheckedSchemes.add(schemeId)
        viewModelScope.launch {
            val settings = settingsFlow.value
            val count = runCatching {
                ImageRepository.getSchemeImageCount(schemeId)
            }.getOrDefault(0)
            if (count == 0) {
                refresh()
                return@launch
            }

            val integrity = runCatching {
                ImageRepository.checkImagePathIntegrity(
                    schemeId = schemeId,
                    sampleSize = STARTUP_PATH_SAMPLE_SIZE,
                    missingThreshold = STARTUP_MISSING_THRESHOLD
                )
            }.getOrNull() ?: return@launch

            if (integrity.shouldRebuild) {
                _messageFlow.emit(
                    "启动检测到 ${integrity.sampled} 条图片路径中有 ${integrity.missing} 条失效，扫描数据可能被清理软件删除，正在重建扫描索引…"
                )
                clearAllAndRescan(settings)
            }
        }
    }

    private suspend fun clearAllAndRescan(settings: AppSettings) {
        _isScanning.value = true
        val result = runCatching {
            ImageRepository.clearAllDatabaseAndRescan(
                preferredSchemeId = settings.activeSchemeId,
                minSizeKb = settings.minScanFileSizeKb,
                preferShizuku = settings.preferShizuku
            )
        }
        _isScanning.value = false

        result.onSuccess { clearSummary ->
            if (clearSummary.schemeId != settings.activeSchemeId) {
                AppSettingsStore.setActiveSchemeId(clearSummary.schemeId)
            }
            val summary = clearSummary.scanSummary
            val source = if (summary.useShizuku) "Shizuku" else "SAF"
            _messageFlow.emit(
                "重建完成（$source）：新增 ${summary.added}，重复 ${summary.duplicates}，小文件过滤 ${summary.skippedSmallFiles}，失败 ${summary.failed}"
            )
            _refreshToken.value += 1
        }.onFailure {
            _messageFlow.emit("重建失败：${it.message ?: "未知错误"}")
        }
    }

    fun bindService() {
        FileExplorerServiceManager.bindService()
    }

    val hasPermission: Boolean
        get() {
            val settings = settingsFlow.value
            val scheme = activeScheme.value
            val useShizuku = settings.preferShizuku && ShizukuUtils.isShizukuAvailable
            return if (useShizuku) {
                ShizukuUtils.isShizukuPermission
            } else {
                ImageRepository.isSafPermissionGranted(scheme.packageName)
            }
        }

    companion object {
        private const val ALL_PATHS_LABEL = "全部路径"
        private const val STARTUP_PATH_SAMPLE_SIZE = 10
        private const val STARTUP_MISSING_THRESHOLD = 2
    }
}

private data class GalleryQuery(
    val type: FileType,
    val sortType: Int,
    val filter: PathFilterOption,
    val scheme: ScanSchemeEntity
)
