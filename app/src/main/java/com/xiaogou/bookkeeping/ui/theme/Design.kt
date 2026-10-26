package com.xiaogou.bookkeeping.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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

/** 设置行：先分割线，分割线左缩进 50dp，左侧 40dp 图标位。 */
@Composable
fun SettingsRow(icon: @Composable () -> Unit, title: String, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Divider(modifier = Modifier.padding(start = WalkDimens.settingsDividerInset))
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(WalkDimens.iconBox), contentAlignment = Alignment.Center) { icon() }
            TextButton(onClick = onClick, modifier = Modifier.weight(1f)) {
                Text(title, modifier = Modifier.fillMaxWidth())
            }
        }
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

/** 简单饼图（Canvas）：按分类金额占比画扇形，尺寸 120dp。 */
@Composable
fun PieChart(values: List<Pair<String, Long>>, colors: List<Color>, modifier: Modifier = Modifier) {
    val total = values.sumOf { it.second }.takeIf { it > 0 } ?: 1L
    Canvas(modifier = modifier.size(WalkDimens.pieSize)) {
        var start = -90f
        values.forEachIndexed { i, (_, v) ->
            val sweep = 360f * v / total
            drawArc(
                color = colors[i % colors.size],
                startAngle = start,
                sweepAngle = sweep,
                useCenter = true,
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
