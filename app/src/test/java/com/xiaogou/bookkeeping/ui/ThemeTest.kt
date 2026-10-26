package com.xiaogou.bookkeeping.ui

import com.xiaogou.bookkeeping.ui.theme.NightMode
import com.xiaogou.bookkeeping.ui.theme.WalkThemeId
import org.junit.Assert.assertEquals
import org.junit.Test

/** 主题键映射的 JVM 单元测试：已知键、未知键、空键的兜底。 */
class ThemeTest {
    @Test
    fun fromKey_knownKeys() {
        assertEquals(WalkThemeId.YELLOW, WalkThemeId.fromKey("theme_yellow"))
        assertEquals(WalkThemeId.GREEN, WalkThemeId.fromKey("theme_green"))
        assertEquals(WalkThemeId.RED, WalkThemeId.fromKey("theme_red"))
        assertEquals(WalkThemeId.BLUE, WalkThemeId.fromKey("theme_blue"))
        assertEquals(WalkThemeId.CYAN, WalkThemeId.fromKey("theme_cyan"))
    }

    @Test
    fun fromKey_unknownOrNullFallsBackToYellow() {
        assertEquals(WalkThemeId.YELLOW, WalkThemeId.fromKey("theme_purple"))
        assertEquals(WalkThemeId.YELLOW, WalkThemeId.fromKey(null))
        assertEquals(WalkThemeId.YELLOW, WalkThemeId.fromKey(""))
    }

    @Test
    fun themeCountAndNightLabels() {
        assertEquals(5, WalkThemeId.entries.size)
        assertEquals(listOf("跟随系统", "白天", "夜间"), NightMode.labels)
        assertEquals(0, NightMode.SYSTEM)
        assertEquals(1, NightMode.DAY)
        assertEquals(2, NightMode.NIGHT)
    }
}
