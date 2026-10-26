package com.xiaogou.bookkeeping.backup

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * WebDavClient 地址归一化的 JVM 单元测试。
 * 注意：构造 WebDavClient 本身会用到 android.util.Base64，在 JVM 单元测试里
 * 跑不起来，所以这里只测抽出来的纯函数 normalizeBaseUrl；真正联网的
 * 上传 / 下载 / 列目录不在 CI 单元测试里，留给第 5 步拿真实服务器做往返验证。
 */
class WebDavClientTest {
    @Test
    fun normalize_addsDirectoryWithoutTrailingSlash() {
        assertEquals(
            "https://dav.example.com/ElephantBookkeeping/",
            WebDavClient.normalizeBaseUrl("https://dav.example.com"),
        )
    }

    @Test
    fun normalize_keepsSingleTrailingSlash() {
        assertEquals(
            "https://dav.example.com/ElephantBookkeeping/",
            WebDavClient.normalizeBaseUrl("https://dav.example.com/"),
        )
    }

    @Test
    fun normalize_trimsSpacesAndExtraSlashes() {
        assertEquals(
            "https://dav.example.com/foo/ElephantBookkeeping/",
            WebDavClient.normalizeBaseUrl("  https://dav.example.com/foo///  "),
        )
    }
}
