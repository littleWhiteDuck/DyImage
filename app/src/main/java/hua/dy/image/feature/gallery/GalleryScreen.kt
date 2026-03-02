package hua.dy.image.feature.gallery

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.gif.GifDecoder
import coil3.request.ImageRequest
import coil3.request.crossfade
import hua.dy.image.bean.ImageBean
import hua.dy.image.shareOtherApp
import hua.dy.image.ui.components.SortBottomDialog
import hua.dy.image.utils.FileType
import hua.dy.image.utils.GetDyPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private data class MultiSelectedImage(
    val md5: String,
    val imagePath: String,
    val fileType: FileType
)

private data class BatchConvertSummary(
    val attempted: Int,
    val skipped: Int,
    val successMap: Map<String, String>,
    val failedIds: Set<String>
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    contentPadding: PaddingValues,
    onBottomBarVisibleChange: (Boolean) -> Unit = {},
    viewModel: GalleryViewModel = viewModel()
) {
    val context = LocalContext.current
    val types = remember { FileType.entries.toList() }
    val sortType by viewModel.sortType.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val activeScheme by viewModel.activeScheme.collectAsState()
    val filterOptions by viewModel.pathFilterOptions.collectAsState()
    val selectedFilter by viewModel.selectedPathFilter.collectAsState()
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = types.indexOf(FileType.PNG).coerceAtLeast(0),
        pageCount = { types.size }
    )

    var permissionState by remember { mutableStateOf(viewModel.hasPermission) }
    var permissionRequestKey by remember { mutableIntStateOf(0) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var dialogImage by remember { mutableStateOf<Pair<String, FileType>?>(null) }
    var selectionMode by remember { mutableStateOf(false) }
    var batchJob by remember { mutableStateOf<Job?>(null) }
    val selectedItems = remember { mutableStateMapOf<String, MultiSelectedImage>() }
    val convertedGifByMd5 = remember { mutableStateMapOf<String, String>() }
    val failedConvertByMd5 = remember { mutableStateMapOf<String, Boolean>() }
    val batchWorking = batchJob?.isActive == true

    val selectedCount = selectedItems.size
    val convertibleCount = selectedItems.values.count { item ->
        isGifConvertible(path = item.imagePath, fileType = item.fileType)
    }
    val convertedCount = selectedItems.keys.count { convertedGifByMd5.containsKey(it) }
    val failedCount = selectedItems.keys.count { failedConvertByMd5.containsKey(it) }
    val openFilterMenu = { showFilterMenu = true }
    val closeFilterMenu = { showFilterMenu = false }
    val openSortDialog = { showSortDialog = true }
    val closeSortDialog = { showSortDialog = false }
    val closeImageDialog = { dialogImage = null }

    fun clearSelectionState() {
        selectionMode = false
        batchJob?.cancel()
        selectedItems.clear()
        convertedGifByMd5.clear()
        failedConvertByMd5.clear()
    }

    fun launchBatchWork(block: suspend () -> Unit) {
        if (batchWorking || selectedCount <= 0) return
        batchJob = scope.launch {
            try {
                block()
            } finally {
            }
        }
    }

    fun toggleSelection(item: ImageBean) {
        if (selectedItems.containsKey(item.md5)) {
            selectedItems.remove(item.md5)
            convertedGifByMd5.remove(item.md5)
            failedConvertByMd5.remove(item.md5)
        } else {
            selectedItems[item.md5] = MultiSelectedImage(
                md5 = item.md5,
                imagePath = item.imagePath,
                fileType = item.fileType
            )
        }
        if (selectedItems.isEmpty()) {
            selectionMode = false
        }
    }

    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components { add(GifDecoder.Factory()) }
            .build()
    }

    LaunchedEffect(Unit) {
        viewModel.messageFlow.collect { message ->
            snackBarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(permissionState, activeScheme.id) {
        viewModel.refreshIfEmptyAndPermitted(permissionState)
    }

    LaunchedEffect(activeScheme.id, activeScheme.packageName, activeScheme.rootPath) {
        permissionState = viewModel.hasPermission
    }

    LaunchedEffect(activeScheme.id, selectedFilter.path, pagerState.currentPage) {
        if (selectionMode) {
            clearSelectionState()
        }
    }

    LaunchedEffect(selectionMode) {
        onBottomBarVisibleChange(!selectionMode)
    }

    BackHandler(enabled = selectionMode) {
        clearSelectionState()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    if (selectionMode) {
                        Column {
                            Text(
                                text = "已选择 $selectedCount 项",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "可转 $convertibleCount · 已转 $convertedCount · 失败 $failedCount",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column {
                            Text(
                                text = "EImage",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "方案：${activeScheme.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    if (selectionMode) {
                        TextButton(
                            enabled = !batchWorking,
                            onClick = { clearSelectionState() },
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("退出多选")
                        }
                    } else {
                        Row(
                            modifier = Modifier.padding(end = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box {
                                FilledIconButton(
                                    onClick = openFilterMenu,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = if (selectedFilter.isAll) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        },
                                        contentColor = if (selectedFilter.isAll) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onPrimary
                                        }
                                    ),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.FilterAlt,
                                        contentDescription = "筛选路径",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                if (!selectedFilter.isAll) {
                                    Badge(modifier = Modifier.align(Alignment.TopEnd))
                                }
                                DropdownMenu(
                                    expanded = showFilterMenu,
                                    onDismissRequest = closeFilterMenu
                                ) {
                                    filterOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = {
                                                val prefix = if (selectedFilter.path == option.path) "✓ " else ""
                                                Text(prefix + option.label)
                                            },
                                            onClick = {
                                                viewModel.updatePathFilter(option)
                                                closeFilterMenu()
                                            }
                                        )
                                    }
                                }
                            }

                            FilledIconButton(
                                onClick = openSortDialog,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Sort,
                                    contentDescription = "排序",
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            FilledIconButton(
                                enabled = !isScanning,
                                onClick = {
                                    permissionState = viewModel.hasPermission
                                    if (permissionState) {
                                        viewModel.refresh()
                                    } else {
                                        permissionRequestKey += 1
                                    }
                                },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = "立即扫描",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
        },
        bottomBar = {
            if (selectionMode) {
                BatchActionBar(
                    selectedCount = selectedCount,
                    convertibleCount = convertibleCount,
                    failedCount = failedCount,
                    isWorking = batchWorking,
                    onConvert = {
                        launchBatchWork {
                                val snapshot = selectedItems.values.toList()
                                if (snapshot.none { isGifConvertible(it.imagePath, it.fileType) }) {
                                    snackBarHostState.showSnackbar("当前选择中没有可转换项（仅支持 WEBP/HEIC）")
                                    return@launchBatchWork
                                }
                                val summary = withContext(Dispatchers.IO) {
                                    val successMap = mutableMapOf<String, String>()
                                    val failedIds = mutableSetOf<String>()
                                    var attempted = 0
                                    var skipped = 0
                                    snapshot.forEach { item ->
                                        if (!isGifConvertible(item.imagePath, item.fileType)) {
                                            skipped += 1
                                            return@forEach
                                        }
                                        attempted += 1
                                        val convertedPath = convertToGif(item.imagePath, item.md5)
                                        if (convertedPath != null) {
                                            successMap[item.md5] = convertedPath
                                        } else {
                                            failedIds.add(item.md5)
                                        }
                                    }
                                    BatchConvertSummary(
                                        attempted = attempted,
                                        skipped = skipped,
                                        successMap = successMap,
                                        failedIds = failedIds
                                    )
                                }
                                summary.successMap.forEach { (md5, path) ->
                                    if (selectedItems.containsKey(md5)) {
                                        convertedGifByMd5[md5] = path
                                        failedConvertByMd5.remove(md5)
                                    }
                                }
                                summary.failedIds.forEach { md5 ->
                                    if (selectedItems.containsKey(md5)) {
                                        failedConvertByMd5[md5] = true
                                        convertedGifByMd5.remove(md5)
                                    }
                                }
                                snackBarHostState.showSnackbar(
                                    "转换完成：成功 ${summary.successMap.size}，失败 ${summary.failedIds.size}，跳过 ${summary.skipped}。失败项仍可保存/分享原图"
                                )
                        }
                    },
                    onSave = {
                        launchBatchWork {
                                val snapshot = selectedItems.values.toList()
                                val convertedSnapshot = convertedGifByMd5.toMap()
                                val result = withContext(Dispatchers.IO) {
                                    var success = 0
                                    var failed = 0
                                    snapshot.forEach { item ->
                                        val path = convertedSnapshot[item.md5] ?: item.imagePath
                                        if (saveImageToLocal(path)) success += 1 else failed += 1
                                    }
                                    success to failed
                                }
                                snackBarHostState.showSnackbar("保存完成：成功 ${result.first}，失败 ${result.second}")
                        }
                    },
                    onShare = {
                        launchBatchWork {
                                val snapshot = selectedItems.values.toList()
                                val convertedSnapshot = convertedGifByMd5.toMap()
                                val sharePaths = withContext(Dispatchers.IO) {
                                    snapshot.mapNotNull { item ->
                                        val path = convertedSnapshot[item.md5] ?: item.imagePath
                                        val resolved = resolveSharePath(path)
                                        val file = File(resolved)
                                        if (file.exists()) file.absolutePath else null
                                    }.distinct()
                                }
                                if (sharePaths.isEmpty()) {
                                    snackBarHostState.showSnackbar("没有可分享的文件")
                                } else {
                                    context.shareOtherApp(sharePaths)
                                    snackBarHostState.showSnackbar("已发起分享 ${sharePaths.size} 项")
                                }
                        }
                    },
                    onClear = { clearSelectionState() }
                )
            }
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackBarHostState,
                modifier = Modifier.padding(
                    bottom = if (selectionMode) 0.dp else contentPadding.calculateBottomPadding() + 8.dp
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!selectionMode && !selectedFilter.isAll) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "当前筛选：${selectedFilter.label}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "清除",
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    filterOptions.firstOrNull { it.isAll }?.let(viewModel::updatePathFilter)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                SecondaryScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier
                                .tabIndicatorOffset(selectedTabIndex = pagerState.currentPage)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)),
                            height = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    types.forEachIndexed { index, type ->
                        val isSelected = pagerState.currentPage == index
                        Tab(
                            selected = isSelected,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = {
                                Text(
                                    text = type.displayName.uppercase(),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isScanning,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                ScanningBanner(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }

            if (selectionMode) {
                SelectionHintCard(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    failedCount = failedCount
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val type = types[page]
                val imageData = viewModel.pagedImagesForType(type).collectAsLazyPagingItems()

                if (!permissionState) {
                    PermissionRequiredPane(
                        onRequest = {
                            permissionState = viewModel.hasPermission
                            if (!permissionState) {
                                permissionRequestKey += 1
                            }
                        }
                    )
                } else {
                    GalleryGridPage(
                        imageData = imageData,
                        type = type,
                        imageLoader = imageLoader,
                        bottomPadding = if (selectionMode) 148.dp else contentPadding.calculateBottomPadding() + 16.dp,
                        selectionMode = selectionMode,
                        selectedIds = selectedItems.keys,
                        convertedIds = convertedGifByMd5.keys,
                        failedIds = failedConvertByMd5.keys,
                        onImageTap = { item ->
                            if (selectionMode) {
                                toggleSelection(item)
                            } else {
                                dialogImage = item.imagePath to item.fileType
                            }
                        },
                        onImageLongPress = { item ->
                            if (!selectionMode) {
                                selectionMode = true
                                dialogImage = null
                            }
                            toggleSelection(item)
                        },
                        onRetry = {
                            viewModel.refresh()
                            imageData.retry()
                        }
                    )
                }
            }
        }
    }

    if (!selectionMode && showSortDialog) {
        SortBottomDialog(
            sortValue = sortType,
            onclick = {
                viewModel.updateSortType(it)
                closeSortDialog()
            },
            onDismiss = closeSortDialog
        )
    }

    if (!selectionMode) {
        dialogImage?.let { (path, type) ->
            ShareImageDialog(
                imagePath = path,
                fileType = type,
                onDismiss = closeImageDialog,
                onShare = { sharePath -> context.shareOtherApp(sharePath) }
            )
        }
    }

    if (!permissionState) {
        key(permissionRequestKey, activeScheme.packageName, activeScheme.rootPath) {
            GetDyPermission(
                needShizuku = viewModel.needShizuku,
                rootPath = activeScheme.rootPath,
                permissionKey = activeScheme.packageName
            ) { isGranted, isShizuku ->
                if (isShizuku) {
                    if (isGranted) {
                        viewModel.bindService()
                        permissionState = true
                    } else {
                        viewModel.needShizuku = false
                        permissionState = viewModel.hasPermission
                    }
                } else {
                    permissionState = isGranted || viewModel.hasPermission
                }
            }
        }
    }
}

@Composable
private fun BatchActionBar(
    selectedCount: Int,
    convertibleCount: Int,
    failedCount: Int,
    isWorking: Boolean,
    onConvert: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onClear: () -> Unit
) {
    Surface(
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "多选模式：$selectedCount 项（可转换 $convertibleCount）",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(enabled = !isWorking, onClick = onClear) {
                    Text("清空选择")
                }
            }

            if (failedCount > 0) {
                Text(
                    text = "已有 $failedCount 项转换失败，保存和分享会自动回退到原图",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onConvert,
                    enabled = !isWorking && convertibleCount > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("转换 GIF")
                }
                OutlinedButton(
                    onClick = onSave,
                    enabled = !isWorking && selectedCount > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("保存")
                }
                Button(
                    onClick = onShare,
                    enabled = !isWorking && selectedCount > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("分享")
                }
            }

            if (isWorking) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun SelectionHintCard(
    modifier: Modifier = Modifier,
    failedCount: Int
) {
    val message = if (failedCount > 0) {
        "已标记转换失败项，可继续批量保存/分享（自动使用原图）"
    } else {
        "长按图片进入多选，点按继续勾选。支持批量转换、保存、分享"
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun GalleryGridPage(
    imageData: LazyPagingItems<ImageBean>,
    type: FileType,
    imageLoader: ImageLoader,
    bottomPadding: Dp,
    selectionMode: Boolean,
    selectedIds: Set<String>,
    convertedIds: Set<String>,
    failedIds: Set<String>,
    onImageTap: (ImageBean) -> Unit,
    onImageLongPress: (ImageBean) -> Unit,
    onRetry: () -> Unit
) {
    val context = LocalContext.current
    val refreshState = imageData.loadState.refresh
    val appendState = imageData.loadState.append

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 118.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 10.dp,
            bottom = bottomPadding
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (refreshState is LoadState.Loading && imageData.itemCount == 0) {
            items(count = 8) {
                LoadingTile()
            }
        } else if (refreshState is LoadState.Error && imageData.itemCount == 0) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                GridMessageCard(
                    title = "加载失败",
                    description = refreshState.error.message ?: "请检查权限或稍后重试",
                    actionLabel = "重试",
                    onAction = onRetry
                )
            }
        } else if (imageData.itemCount == 0) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                GridMessageCard(
                    title = "暂无${type.displayName.uppercase()}图片",
                    description = "点击右上角刷新按钮开始扫描，或检查路径筛选",
                    actionLabel = "立即扫描",
                    onAction = onRetry
                )
            }
        } else {
            items(
                count = imageData.itemCount,
                key = { index -> imageData.peek(index)?.md5 ?: "loading_$index" }
            ) { index ->
                val item = imageData[index] ?: return@items
                val isSelected = selectedIds.contains(item.md5)
                val converted = convertedIds.contains(item.md5)
                val convertFailed = failedIds.contains(item.md5)
                Card(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .combinedClickable(
                            onClick = { onImageTap(item) },
                            onLongClick = { onImageLongPress(item) }
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectionMode && isSelected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        }
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (selectionMode && isSelected) 4.dp else 2.dp,
                        pressedElevation = 6.dp
                    )
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (item.fileType == FileType.UNKNOWN || item.fileType == FileType.VVIC) {
                            UnsupportedPreviewTile(
                                message = if (item.fileType == FileType.VVIC) {
                                    "VVIC 暂不支持解码预览"
                                } else {
                                    "暂不支持预览"
                                }
                            )
                        } else {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(item.imagePath)
                                    .size(300, 300)
                                    .crossfade(220)
                                    .build(),
                                imageLoader = imageLoader,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop,
                                contentDescription = null
                            )

                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = item.fileType.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (selectionMode) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .size(22.dp),
                                shape = CircleShape,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f)
                                }
                            ) {
                                if (isSelected) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (isSelected && (converted || convertFailed)) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = if (convertFailed) {
                                    MaterialTheme.colorScheme.errorContainer
                                } else {
                                    MaterialTheme.colorScheme.tertiaryContainer
                                }
                            ) {
                                Text(
                                    text = if (convertFailed) "转失败" else "GIF",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (convertFailed) {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    } else {
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (appendState is LoadState.Loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }

        if (appendState is LoadState.Error) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                GridMessageCard(
                    title = "更多内容加载失败",
                    description = appendState.error.message ?: "下拉后可再次触发加载",
                    actionLabel = "重试",
                    onAction = {
                        imageData.retry()
                    }
                )
            }
        }
    }
}

@Composable
private fun ScanningBanner(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
                Text(
                    text = "正在扫描图片，请稍候…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
private fun GridMessageCard(
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            FilledTonalButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun LoadingTile() {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
    )
}

@Composable
private fun PermissionRequiredPane(onRequest: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        GridMessageCard(
            title = "需要存储访问权限",
            description = "授权后才能扫描目标应用缓存目录中的图片",
            actionLabel = "重新授权",
            onAction = onRequest
        )
    }
}

@Composable
private fun UnsupportedPreviewTile(
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.ImageNotSupported,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
