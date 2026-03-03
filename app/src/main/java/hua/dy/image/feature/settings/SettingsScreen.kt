package hua.dy.image.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import hua.dy.image.data.SortOptions
import hua.dy.image.data.settings.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isClearing by viewModel.isClearing.collectAsState()
    val minSizeOptions = remember {
        listOf(2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096)
            .map { SettingSelectOption(value = it, label = "$it KB") }
    }
    val sortOptions = remember {
        SortOptions.labels.mapIndexed { index, label ->
            SettingSelectOption(value = index, label = label)
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }

    var showThemeMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showMinSizeMenu by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val openClearConfirm = { showClearConfirm = true }
    val closeClearConfirm = { showClearConfirm = false }

    LaunchedEffect(Unit) {
        viewModel.messageFlow.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = contentPadding.calculateBottomPadding())
            )
        },
        containerColor = Color.Transparent,
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
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = "设置",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsCard(
                    title = "外观设置",
                    icon = Icons.Outlined.Palette,
                    iconColor = MaterialTheme.colorScheme.primary
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SettingsDropdownItem(
                            title = "主题",
                            subtitle = "选择应用的主题模式",
                            selectedValue = uiState.themeMode,
                            options = themeModeOptions(),
                            expanded = showThemeMenu,
                            onExpandedChange = { showThemeMenu = it },
                            onValueSelected = viewModel::updateThemeMode
                        )

                        SettingsItem(
                            title = "跟随系统动态色",
                            subtitle = "仅在 Android 12+ 生效",
                            trailing = {
                                Switch(
                                    checked = uiState.followSystemDynamicColor,
                                    onCheckedChange = viewModel::updateFollowSystemDynamicColor,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                        )
                    }
                }
            }

            item {
                SettingsCard(
                    title = "扫描设置",
                    icon = Icons.Outlined.Speed,
                    iconColor = MaterialTheme.colorScheme.secondary
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SettingsDropdownItem(
                            title = "默认排序",
                            subtitle = "扫描结果默认展示顺序",
                            selectedValue = uiState.sortType,
                            options = sortOptions,
                            expanded = showSortMenu,
                            onExpandedChange = { showSortMenu = it },
                            onValueSelected = viewModel::updateSortType
                        )

                        SettingsDropdownItem(
                            title = "最小扫描大小",
                            subtitle = "小于该值的文件将被忽略",
                            selectedValue = uiState.minScanFileSizeKb,
                            options = minSizeOptions,
                            expanded = showMinSizeMenu,
                            onExpandedChange = { showMinSizeMenu = it },
                            onValueSelected = viewModel::updateMinScanFileSizeKb
                        )

                        Button(
                            enabled = !isClearing,
                            onClick = openClearConfirm,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            if (isClearing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("清理中…")
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteSweep,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("清空扫描数据并重扫")
                            }
                        }
                    }
                }
            }

            item {
                SettingsCard(
                    title = "高级设置",
                    icon = Icons.Outlined.Tune,
                    iconColor = MaterialTheme.colorScheme.primary
                ) {
                    SettingsItem(
                        title = "优先使用 Shizuku 扫描",
                        subtitle = "关闭后改为使用 SAF 授权扫描",
                        trailing = {
                            Switch(
                                checked = uiState.preferShizuku,
                                onCheckedChange = viewModel::updatePreferShizuku,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    )
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = closeClearConfirm,
            title = { Text("清空扫描数据并重扫") },
            text = { Text("此操作只会清空扫描产生的图片索引与缓存图片，不会删除路径配置和设置项，然后立即重新扫描。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        closeClearConfirm()
                        viewModel.clearDatabaseAndRescan()
                    }
                ) {
                    Text("继续")
                }
            },
            dismissButton = {
                TextButton(onClick = closeClearConfirm) {
                    Text("取消")
                }
            }
        )
    }
}

private data class SettingSelectOption<T>(
    val value: T,
    val label: String,
    val supportingText: String? = null
)

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    content: @Composable () -> Unit
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
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = iconColor.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}

@Composable
private fun <T> SettingsDropdownItem(
    title: String,
    subtitle: String? = null,
    selectedValue: T,
    options: List<SettingSelectOption<T>>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onValueSelected: (T) -> Unit
) {
    val selectedOption = options.firstOrNull { it.value == selectedValue }
    val selectedLabel = selectedOption?.label ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onExpandedChange(true) }
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (expanded) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                }
            ) {
                Row(
                    modifier = Modifier
                        .clickable { onExpandedChange(!expanded) }
                        .widthIn(min = 108.dp, max = 168.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedLabel,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (expanded) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = if (expanded) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Icon(
                        imageVector = Icons.Outlined.ArrowDropDown,
                        contentDescription = null,
                        tint = if (expanded) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                offset = DpOffset(x = 0.dp, y = 4.dp),
                modifier = Modifier
                    .widthIn(min = 196.dp)
                    .heightIn(max = 280.dp)
            ) {
                options.forEach { option ->
                    val isSelected = option.value == selectedValue
                    DropdownMenuItem(
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                )
                                option.supportingText?.let { supporting ->
                                    Text(
                                        text = supporting,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.Transparent
                                }
                            )
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            leadingIconColor = MaterialTheme.colorScheme.primary
                        ),
                        onClick = {
                            onValueSelected(option.value)
                            onExpandedChange(false)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String? = null,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        trailing()
    }
}

private fun themeModeOptions(): List<SettingSelectOption<ThemeMode>> {
    return listOf(
        SettingSelectOption(
            value = ThemeMode.System,
            label = "跟随系统",
            supportingText = "使用系统亮色/暗色设置"
        ),
        SettingSelectOption(
            value = ThemeMode.Light,
            label = "浅色",
            supportingText = "始终使用浅色主题"
        ),
        SettingSelectOption(
            value = ThemeMode.Dark,
            label = "深色",
            supportingText = "始终使用深色主题"
        )
    )
}
