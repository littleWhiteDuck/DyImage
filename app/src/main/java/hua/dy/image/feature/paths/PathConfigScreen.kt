package hua.dy.image.feature.paths

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.outlined.RestoreFromTrash
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
    var showRestoreConfirm by remember { mutableStateOf(false) }

    val activeScheme = uiState.schemes.firstOrNull { it.id == uiState.activeSchemeId }

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
                actions = {
                    Row(modifier = Modifier.padding(end = 16.dp)) {
                        FilledIconButton(
                            onClick = { showRestoreConfirm = true },
                            enabled = activeScheme != null,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.RestoreFromTrash,
                                contentDescription = "恢复默认路径",
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
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingPath = null
                    showPathEditor = true
                },
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
                    onShowSchemeMenu = { showSchemeMenu = it },
                    onSelectScheme = viewModel::selectScheme,
                    onAddScheme = {
                        editingScheme = null
                        showSchemeEditor = true
                    },
                    onEditScheme = {
                        editingScheme = activeScheme
                        showSchemeEditor = true
                    },
                    onDeleteScheme = {
                        deletingScheme = activeScheme
                    }
                )
            }

            if (uiState.paths.isEmpty()) {
                item {
                    EmptyPathCard(onAdd = {
                        editingPath = null
                        showPathEditor = true
                    })
                }
            } else {
                items(uiState.paths, key = { it.id }) { item ->
                    PathCard(
                        entity = item,
                        onEdit = {
                            editingPath = item
                            showPathEditor = true
                        },
                        onDelete = {
                            deletingPath = item
                        },
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
            onDismiss = { showPathEditor = false },
            onConfirm = { id, path, note, enabled ->
                viewModel.savePath(id, path, note, enabled)
                showPathEditor = false
            }
        )
    }

    if (showSchemeEditor) {
        SchemeEditorDialog(
            initialValue = editingScheme,
            onDismiss = { showSchemeEditor = false },
            onConfirm = { id, name, pkg ->
                viewModel.saveScheme(id, name, pkg)
                showSchemeEditor = false
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
                deletingPath = null
            },
            onDismiss = { deletingPath = null }
        )
    }

    deletingScheme?.let { scheme ->
        ConfirmDialog(
            title = "删除方案",
            message = "删除方案“${scheme.name}”后，其路径配置也会一起删除。",
            confirmLabel = "删除",
            onConfirm = {
                viewModel.deleteScheme(scheme)
                deletingScheme = null
            },
            onDismiss = { deletingScheme = null }
        )
    }

    if (showRestoreConfirm) {
        ConfirmDialog(
            title = "恢复默认路径",
            message = "将清空当前方案的路径并恢复系统默认值。",
            confirmLabel = "恢复",
            onConfirm = {
                viewModel.restoreDefaults()
                showRestoreConfirm = false
            },
            onDismiss = { showRestoreConfirm = false }
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
                OutlinedTextField(
                    value = activeScheme?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("方案") },
                    placeholder = { Text("请选择方案") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShowSchemeMenu(true) },
                    shape = RoundedCornerShape(12.dp)
                )
                DropdownMenu(
                    expanded = showSchemeMenu,
                    onDismissRequest = { onShowSchemeMenu(false) }
                ) {
                    schemes.forEach { scheme ->
                        DropdownMenuItem(
                            text = { Text("${scheme.name} (${scheme.packageName})") },
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
                text = "你可以新增一条路径，或点击右上角恢复默认路径",
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
                    Icon(Icons.Outlined.Edit, contentDescription = "编辑", modifier = Modifier.size(18.dp))
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
                    Icon(Icons.Outlined.Delete, contentDescription = "删除", modifier = Modifier.size(18.dp))
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
    onConfirm: (id: Long?, name: String, pkg: String) -> Unit
) {
    var name by remember(initialValue) { mutableStateOf(initialValue?.name ?: "") }
    var pkg by remember(initialValue) { mutableStateOf(initialValue?.packageName ?: "") }

    val nameValid = name.trim().isNotEmpty()
    val pkgValid = pkg.trim().contains('.')

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
                    value = pkg,
                    onValueChange = { pkg = it },
                    label = { Text("目标应用包名") },
                    placeholder = { Text("com.ss.android.ugc.aweme") },
                    isError = pkg.isNotBlank() && !pkgValid,
                    supportingText = {
                        if (pkg.isNotBlank() && !pkgValid) {
                            Text("请输入完整包名")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                enabled = nameValid && pkgValid,
                onClick = { onConfirm(initialValue?.id, name.trim(), pkg.trim()) },
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
