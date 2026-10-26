package com.xiaogou.bookkeeping.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * PIN 哈希的 JVM 单元测试。
 * hashPin 是纯 SHA-256，不碰 SharedPreferences，所以不用模拟器也能测；
 * 期望值是标准 SHA-256 结果，可用任何工具独立验算。
 */
class SettingsHashTest {
    @Test
    fun hashPin_knownVector() {
        assertEquals(
            "03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4",
            Settings.hashPin("1234"),
        )
    }

    @Test
    fun hashPin_deterministicAndDifferent() {
        assertEquals(Settings.hashPin("1234"), Settings.hashPin("1234"))
        assertNotEquals(Settings.hashPin("1234"), Settings.hashPin("0000"))
        assertEquals(64, Settings.hashPin("1234").length)
    }
}
