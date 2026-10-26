package com.xiaogou.bookkeeping.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 全应用共享的设计尺寸（dp/sp），各屏统一用这里，避免每屏各写一套。 */
object WalkDimens {
    val toolbarHeight = 52.dp
    val bottomTabHeight = 76.dp
    val bottomTabSideMargin = 22.dp
    val bottomTabBottomMargin = 16.dp
    val cardCorner = 24.dp
    val listCorner = 18.dp
    val rowCorner = 16.dp
    val smallCorner = 8.dp
    val tinyCorner = 4.dp
    val dialogCorner = 30.dp
    val dialogPadding = 24.dp
    val dialogButtonHeight = 40.dp
    val iconBox = 40.dp
    val iconBoxLarge = 50.dp
    val assetRowHeight = 62.dp
    val typeRowInset = 66.dp
    val searchBoxHeight = 40.dp
    val settingsDividerInset = 50.dp
    val keyboardRowHeight = 58.dp
    val keyboardSideColumnWidth = 64.dp
    val pieSize = 120.dp
    val lockTopSpace = 120.dp
    val lockLogoSize = 64.dp
    val lockBottomButtonHeight = 40.dp
}

/** 底部悬浮 tab 条：高 76dp，左右 22dp，底部 16dp，圆角 24dp，无指示条。 */
@Composable
fun FloatingBottomBar(tabs: List<Pair<String, () -> Unit>>, current: Int) {
    Card(
        modifier = Modifier.fillMaxWidth()
            .padding(
                start = WalkDimens.bottomTabSideMargin,
                end = WalkDimens.bottomTabSideMargin,
                bottom = WalkDimens.bottomTabBottomMargin,
            )
            .height(WalkDimens.bottomTabHeight),
        shape = RoundedCornerShape(WalkDimens.cardCorner),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, (label, onClick) ->
                TextButton(onClick = onClick) {
                    Text(
                        label,
                        color = if (index == current) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** 设置行：先分割线，分割线左缩进 50dp，左侧 40dp 图标位（12dp 圆角深色底）。 */
@Composable
fun SettingsRow(icon: @Composable () -> Unit, title: String, onClick: () -> Unit, subtitle: String = "") {
    Column(modifier = Modifier.fillMaxWidth()) {
        Divider(modifier = Modifier.padding(start = WalkDimens.settingsDividerInset))
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(WalkDimens.iconBox)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) { icon() }
            Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                Text(title, fontSize = 16.sp)
                if (subtitle.isNotEmpty()) {
                    Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("›", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** 设置开关行：与 SettingsRow 同骨架，右侧是 Switch，展开区在开关打开时显示。 */
@Composable
fun SettingsSwitchRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String = "",
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Divider(modifier = Modifier.padding(start = WalkDimens.settingsDividerInset))
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(WalkDimens.iconBox)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) { icon() }
            Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                Text(title, fontSize = 16.sp)
                if (subtitle.isNotEmpty()) {
                    Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

/**
 * 主题色选择行（setting_item_theme 口径）：一行 5 个 40dp 圆点，
 * 圆角 16dp，选中打勾，点即切换，全应用立即生效。
 */
@Composable
fun ThemeColorPicker(selectedKey: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        WalkThemeId.values().forEach { theme ->
            Box(
                modifier = Modifier.size(40.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(theme.primary)
                    .clickable { onSelect(theme.key) },
                contentAlignment = Alignment.Center,
            ) {
                if (theme.key == selectedKey) {
                    Text(
                        "✓",
                        color = if (theme == WalkThemeId.YELLOW || theme == WalkThemeId.CYAN)
                            Color(0xFF3C424A) else Color.White,
                        fontSize = 18.sp,
                    )
                }
            }
        }
    }
}

/** 统计区块标题：20sp 标题 + 30° 旋转的主题色标志 + 右侧更多。 */
@Composable
fun StatisticsBlockHeader(title: String, onMore: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "◆",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 16.sp,
            modifier = Modifier.graphicsLayer(rotationZ = 30f).padding(end = 4.dp),
        )
        Text(title, fontSize = 20.sp, modifier = Modifier.weight(1f))
        if (onMore != null) {
            TextButton(onClick = onMore) { Text("›", fontSize = 20.sp) }
        }
    }
}

/** 排行行：名 + 进度条 + 右侧 80dp 数字（swipe_hist_item 口径）。 */
@Composable
fun RankRow(name: String, value: Long, total: Long, label: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, modifier = Modifier.padding(end = 10.dp))
        LinearProgressIndicator(
            progress = { if (total > 0) (value.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f },
            modifier = Modifier.weight(1f),
        )
        Text(label, modifier = Modifier.width(80.dp), textAlign = TextAlign.End)
    }
}

/** 柱状图：118dp 高，底部 18dp 轴线区，柱子按最大值等比。 */
@Composable
fun HistBarChart(values: List<Long>, color: Color, modifier: Modifier = Modifier) {
    val max = (values.maxOrNull() ?: 0L).takeIf { it > 0 } ?: 1L
    Canvas(modifier = modifier.fillMaxWidth().height(118.dp)) {
        val axisH = 18.dp.toPx()
        val plotH = size.height - axisH
        drawLine(
            color = Color.Gray,
            start = Offset(0f, size.height - axisH),
            end = Offset(size.width, size.height - axisH),
            strokeWidth = 1.dp.toPx(),
        )
        if (values.isNotEmpty()) {
            val slot = size.width / values.size
            val barW = slot * 0.5f
            values.forEachIndexed { i, v ->
                val h = plotH * v / max
                drawRect(
                    color = color,
                    topLeft = Offset(slot * i + (slot - barW) / 2, size.height - axisH - h),
                    size = Size(barW, h),
                )
            }
        }
    }
}

/** 折线图：118dp 高，点连线，首尾不贴边。 */
@Composable
fun CurveChart(values: List<Long>, color: Color, modifier: Modifier = Modifier) {
    val max = (values.maxOrNull() ?: 0L).takeIf { it > 0 } ?: 1L
    val min = values.minOrNull() ?: 0L
    val range = (max - min).takeIf { it > 0 } ?: 1L
    Canvas(modifier = modifier.fillMaxWidth().height(118.dp)) {
        if (values.size < 2) return@Canvas
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = size.width * i / (values.size - 1)
            val y = size.height - (size.height * 0.9f * (v - min) / range) - size.height * 0.05f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = color, style = Stroke(width = 2.dp.toPx()))
    }
}

/** 通用对话框壳：圆角 30dp，内边距 24dp，按钮高 40dp。 */
@Composable
fun WalkDialog(title: String, onDismiss: () -> Unit, onConfirm: () -> Unit, content: @Composable () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(WalkDimens.dialogCorner),
        title = { Text(title) },
        text = { Box(modifier = Modifier.padding(WalkDimens.dialogPadding)) { content() } },
        confirmButton = {
            Button(onClick = onConfirm, modifier = Modifier.height(WalkDimens.dialogButtonHeight)) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.height(WalkDimens.dialogButtonHeight)) { Text("取消") }
        },
    )
}

/**
 * 饼图（Canvas，环形）：120dp，外半径 60dp、内半径 20dp，
 * 即环厚 40dp，与源码 MathPieChartView 口径一致。
 */
@Composable
fun PieChart(values: List<Pair<String, Long>>, colors: List<Color>, modifier: Modifier = Modifier) {
    val total = values.sumOf { it.second }.takeIf { it > 0 } ?: 1L
    Canvas(modifier = modifier.size(WalkDimens.pieSize)) {
        var start = -90f
        val stroke = size.width / 3f
        values.forEachIndexed { i, (_, v) ->
            val sweep = 360f * v / total
            drawArc(
                color = colors[i % colors.size],
                startAngle = start,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = stroke),
                size = Size(size.width, size.height),
            )
            start += sweep
        }
    }
    Column(modifier = Modifier.padding(top = 8.dp)) {
        values.forEachIndexed { i, (name, v) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors[i % colors.size]),
                )
                Text(" $name ${v * 100 / total}%", fontSize = 12.sp)
            }
        }
    }
}
