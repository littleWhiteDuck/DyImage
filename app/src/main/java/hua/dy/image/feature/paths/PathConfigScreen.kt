package hua.dy.image.feature.paths

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import hua.dy.image.db.ScanPathEntity
import hua.dy.image.db.ScanSchemeEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PathConfigScreen(
    contentPadding: PaddingValues,
    viewModel: PathConfigViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    var editingPath by remember { mutableStateOf<ScanPathEntity?>(null) }
    var showPathEditor by remember { mutableStateOf(false) }
    var editingScheme by remember { mutableStateOf<ScanSchemeEntity?>(null) }
    var showSchemeEditor by remember { mutableStateOf(false) }
    var showSchemeMenu by remember { mutableStateOf(false) }
    var deletingPath by remember { mutableStateOf<ScanPathEntity?>(null) }
    var deletingScheme by remember { mutableStateOf<ScanSchemeEntity?>(null) }

    val activeScheme = uiState.schemes.firstOrNull { it.id == uiState.activeSchemeId }
    val setSchemeMenuExpanded: (Boolean) -> Unit = { expanded -> showSchemeMenu = expanded }

    val openPathEditorForCreate = {
        editingPath = null
        showPathEditor = true
    }
    val openPathEditorForEdit: (ScanPathEntity) -> Unit = { path ->
        editingPath = path
        showPathEditor = true
    }
    val closePathEditor = { showPathEditor = false }

    val openSchemeEditorForCreate = {
        editingScheme = null
        showSchemeEditor = true
    }
    val openSchemeEditorForEdit = {
        editingScheme = activeScheme
        showSchemeEditor = true
    }
    val closeSchemeEditor = { showSchemeEditor = false }

    val requestDeletePath: (ScanPathEntity) -> Unit = { path -> deletingPath = path }
    val dismissDeletePath = { deletingPath = null }
    val requestDeleteScheme = { deletingScheme = activeScheme }
    val dismissDeleteScheme = { deletingScheme = null }

    LaunchedEffect(Unit) {
        viewModel.messageFlow.collect { snackbar.showSnackbar(it) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = "扫描路径",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = openPathEditorForCreate,
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "新增路径",
                        modifier = Modifier.size(20.dp)
                    )
                },
                text = { Text("新增路径") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.padding(bottom = contentPadding.calculateBottomPadding())
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                top = 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 88.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SchemeCard(
                    schemes = uiState.schemes,
                    activeScheme = activeScheme,
                    showSchemeMenu = showSchemeMenu,
                    onShowSchemeMenu = setSchemeMenuExpanded,
                    onSelectScheme = viewModel::selectScheme,
                    onAddScheme = openSchemeEditorForCreate,
                    onEditScheme = openSchemeEditorForEdit,
                    onDeleteScheme = requestDeleteScheme
                )
            }

            item {
                PathSectionHeader(
                    onAddPath = openPathEditorForCreate
                )
            }

            if (uiState.paths.isEmpty()) {
                item {
                    EmptyPathCard(onAdd = openPathEditorForCreate)
                }
            } else {
                items(uiState.paths, key = { it.id }) { item ->
                    PathCard(
                        entity = item,
                        onEdit = { openPathEditorForEdit(item) },
                        onDelete = { requestDeletePath(item) },
                        onToggleEnabled = { enabled ->
                            viewModel.updatePathEnabled(item, enabled)
                        }
                    )
                }
            }
        }
    }

    if (showPathEditor) {
        PathEditorDialog(
            initialValue = editingPath,
            onDismiss = closePathEditor,
            onConfirm = { id, path, note, enabled ->
                viewModel.savePath(id, path, note, enabled)
                closePathEditor()
            }
        )
    }

    if (showSchemeEditor) {
        SchemeEditorDialog(
            initialValue = editingScheme,
            onDismiss = closeSchemeEditor,
            onConfirm = { id, name, rootPath ->
                viewModel.saveScheme(id, name, rootPath)
                closeSchemeEditor()
            }
        )
    }

    deletingPath?.let { path ->
        ConfirmDialog(
            title = "删除路径",
            message = "确定删除路径“${path.relativePath}”吗？",
            confirmLabel = "删除",
            onConfirm = {
                viewModel.deletePath(path)
                dismissDeletePath()
            },
            onDismiss = dismissDeletePath
        )
    }

    deletingScheme?.let { scheme ->
        val deletingLastScheme = uiState.schemes.size <= 1
        ConfirmDialog(
            title = "删除方案",
            message = if (deletingLastScheme) {
                "删除方案“${scheme.name}”后会自动创建抖音默认方案。"
            } else {
                "删除方案“${scheme.name}”后，其路径配置也会一起删除。"
            },
            confirmLabel = "删除",
            onConfirm = {
                viewModel.deleteScheme(scheme)
                dismissDeleteScheme()
            },
            onDismiss = dismissDeleteScheme
        )
    }
}

@Composable
private fun SchemeCard(
    schemes: List<ScanSchemeEntity>,
    activeScheme: ScanSchemeEntity?,
    showSchemeMenu: Boolean,
    onShowSchemeMenu: (Boolean) -> Unit,
    onSelectScheme: (Long) -> Unit,
    onAddScheme: () -> Unit,
    onEditScheme: () -> Unit,
    onDeleteScheme: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "当前扫描方案",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Box {
                FilledTonalButton(
                    onClick = { onShowSchemeMenu(true) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = activeScheme?.name ?: "请选择方案",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = activeScheme?.rootPath ?: "点击切换扫描方案",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "切换",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
                DropdownMenu(
                    expanded = showSchemeMenu,
                    onDismissRequest = { onShowSchemeMenu(false) }
                ) {
                    schemes.forEach { scheme ->
                        val isActive = activeScheme?.id == scheme.id
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = if (isActive) "✓ ${scheme.name}" else scheme.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = scheme.rootPath,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                onShowSchemeMenu(false)
                                onSelectScheme(scheme.id)
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(onClick = onAddScheme, modifier = Modifier.weight(1f)) {
                    Text("新增方案")
                }
                OutlinedButton(
                    onClick = onEditScheme,
                    modifier = Modifier.weight(1f),
                    enabled = activeScheme != null
                ) {
                    Text("编辑方案")
                }
            }

            TextButton(
                onClick = onDeleteScheme,
                enabled = activeScheme != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("删除方案", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun PathSectionHeader(onAddPath: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "子路径列表",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "相对于主路径进行扫描",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FloatingActionButton(
            onClick = onAddPath,
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "新增子路径",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun EmptyPathCard(onAdd: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "当前没有扫描路径",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "点击下方按钮添加一条子路径用于扫描",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(onClick = onAdd) {
                Text("新增路径")
            }
        }
    }
}

@Composable
private fun PathCard(
    entity: ScanPathEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit
) {
    val statusColor = if (entity.isEnabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = entity.note.ifBlank { "未命名路径" },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = entity.relativePath,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (entity.isEnabled) "已启用" else "已禁用",
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "参与扫描",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = entity.isEnabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                Spacer(modifier = Modifier.weight(1f))
                FilledIconButton(
                    onClick = onEdit,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "编辑",
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                FilledIconButton(
                    onClick = onDelete,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "删除",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PathEditorDialog(
    initialValue: ScanPathEntity?,
    onDismiss: () -> Unit,
    onConfirm: (id: Long?, path: String, note: String, enabled: Boolean) -> Unit
) {
    var path by remember(initialValue) { mutableStateOf(initialValue?.relativePath ?: "") }
    var note by remember(initialValue) { mutableStateOf(initialValue?.note ?: "") }
    var enabled by remember(initialValue) { mutableStateOf(initialValue?.isEnabled ?: true) }

    val normalizedPath = path.trim()
    val pathValid = normalizedPath.startsWith("/") && normalizedPath.length > 1

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialValue == null) "新增扫描路径" else "编辑扫描路径",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it },
                    label = { Text("相对路径") },
                    placeholder = { Text("/cache/picture/fresco_cache/*") },
                    isError = path.isNotBlank() && !pathValid,
                    supportingText = {
                        if (path.isNotBlank() && !pathValid) {
                            Text("路径必须以 / 开头")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（可选）") },
                    placeholder = { Text("例如：聊天图片缓存") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "启用此路径",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                        )
                        Text(
                            text = "禁用后扫描时会跳过该路径",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = pathValid,
                onClick = { onConfirm(initialValue?.id, normalizedPath, note, enabled) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text("取消")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun SchemeEditorDialog(
    initialValue: ScanSchemeEntity?,
    onDismiss: () -> Unit,
    onConfirm: (id: Long?, name: String, rootPath: String) -> Unit
) {
    var name by remember(initialValue) { mutableStateOf(initialValue?.name ?: "") }
    var rootPath by remember(initialValue) { mutableStateOf(initialValue?.rootPath ?: "") }

    val nameValid = name.trim().isNotEmpty()
    val rootPathValue = rootPath.trim()
    val rootPathValid = rootPathValue.startsWith("/") && rootPathValue.length > 1

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialValue == null) "新增扫描方案" else "编辑扫描方案",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("方案名称") },
                    placeholder = { Text("例如：抖音") },
                    isError = name.isNotBlank() && !nameValid,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = rootPath,
                    onValueChange = { rootPath = it },
                    label = { Text("主路径") },
                    placeholder = { Text("/sdcard/Android/data/com.ss.android.ugc.aweme") },
                    isError = rootPath.isNotBlank() && !rootPathValid,
                    supportingText = {
                        if (rootPath.isNotBlank() && !rootPathValid) {
                            Text("主路径必须以 / 开头")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                enabled = nameValid && rootPathValid,
                onClick = { onConfirm(initialValue?.id, name.trim(), rootPathValue) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text("取消")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
