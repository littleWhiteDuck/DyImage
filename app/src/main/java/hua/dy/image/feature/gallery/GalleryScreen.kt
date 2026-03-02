package hua.dy.image.feature.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Badge
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    contentPadding: PaddingValues,
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

    LaunchedEffect(activeScheme.packageName) {
        permissionState = viewModel.hasPermission
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
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
                },
                actions = {
                    Row(
                        modifier = Modifier.padding(end = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            FilledIconButton(
                                onClick = { showFilterMenu = true },
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
                                onDismissRequest = { showFilterMenu = false }
                            ) {
                                filterOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            val prefix = if (selectedFilter.path == option.path) "✓ " else ""
                                            Text(prefix + option.label)
                                        },
                                        onClick = {
                                            viewModel.updatePathFilter(option)
                                            showFilterMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        FilledIconButton(
                            onClick = { showSortDialog = true },
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackBarHostState,
                modifier = Modifier.padding(bottom = contentPadding.calculateBottomPadding() + 8.dp)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!selectedFilter.isAll) {
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
                        bottomPadding = contentPadding.calculateBottomPadding() + 16.dp,
                        onImageClick = { item -> dialogImage = item.imagePath to item.fileType },
                        onRetry = {
                            viewModel.refresh()
                            imageData.retry()
                        }
                    )
                }
            }
        }
    }

    if (showSortDialog) {
        SortBottomDialog(
            sortValue = sortType,
            onclick = {
                viewModel.updateSortType(it)
                showSortDialog = false
            },
            onDismiss = { showSortDialog = false }
        )
    }

    dialogImage?.let { (path, type) ->
        ShareImageDialog(
            imagePath = path,
            fileType = type,
            onDismiss = { dialogImage = null },
            onShare = { sharePath -> context.shareOtherApp(sharePath) }
        )
    }

    if (!permissionState) {
        key(permissionRequestKey, activeScheme.packageName) {
            GetDyPermission(
                needShizuku = viewModel.needShizuku,
                packageName = activeScheme.packageName
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
private fun GalleryGridPage(
    imageData: LazyPagingItems<ImageBean>,
    type: FileType,
    imageLoader: ImageLoader,
    bottomPadding: Dp,
    onImageClick: (ImageBean) -> Unit,
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
                Card(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clickable { onImageClick(item) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 2.dp,
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
