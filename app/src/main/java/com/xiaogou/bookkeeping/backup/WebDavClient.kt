package com.xiaogou.bookkeeping.backup

import android.util.Base64
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import org.xmlpull.v1.XmlPullParserFactory

/**
 * WebDAV 客户端（WebDAV 备份第 2 步）。
 *
 * 只用系统 HttpURLConnection，不引入新依赖；支持的动作刚好覆盖备份所需：
 * 建目录（MKCOL）、上传（PUT）、下载（GET）、列目录（PROPFIND Depth:1）。
 *
 * 服务端目录固定为地址下的 ElephantBookkeeping/，与旧版约定一致。
 * 地址末尾有没有斜杠都行，这里统一处理成 “…/ElephantBookkeeping/”。
 * 文件名只会是数据库那几个固定名，不做通用路径转义。
 */
class WebDavClient(
    address: String,
    private val username: String,
    private val password: String,
) {
    companion object {
        const val DIR_NAME = "ElephantBookkeeping/"
        val DB_FILES = listOf("MoneyKeeper.db", "MoneyKeeper.db-shm", "MoneyKeeper.db-wal")
    }

    /** 备份根目录 URL，末尾带斜杠。 */
    val baseUrl: String = address.trim().trimEnd('/') + "/" + DIR_NAME

    private val authHeader: String =
        "Basic " + Base64.encodeToString(
            "$username:$password".toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP,
        )

    private fun open(url: String, method: String): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.setRequestProperty("Authorization", authHeader)
        connection.connectTimeout = 15_000
        connection.readTimeout = 60_000
        return connection
    }

    private fun requireSuccess(connection: HttpURLConnection, action: String) {
        val code = connection.responseCode
        if (code !in 200..299 && code != 207) {
            val detail = try {
                connection.errorStream?.bufferedReader()?.readText()?.take(200)
            } catch (_: Exception) {
                null
            }
            throw IOException("WebDAV $action failed: HTTP $code${if (detail.isNullOrBlank()) "" else " $detail"}")
        }
    }

    /** 建备份目录；目录已存在时服务端一般返回 405，视为成功。 */
    fun ensureDirectory() {
        val connection = open(baseUrl, "MKCOL")
        try {
            val code = connection.responseCode
            if (code !in 200..299 && code != 405) {
                throw IOException("WebDAV create directory failed: HTTP $code")
            }
        } finally {
            connection.disconnect()
        }
    }

    /** 上传一个文件到备份目录；服务端名与本地名相同。 */
    fun upload(name: String, file: File) {
        val connection = open(baseUrl + name, "PUT")
        try {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/octet-stream")
            connection.setFixedLengthStreamingMode(file.length())
            file.inputStream().use { input ->
                connection.outputStream.use { output -> input.copyTo(output) }
            }
            requireSuccess(connection, "upload $name")
        } finally {
            connection.disconnect()
        }
    }

    /** 从备份目录下载一个文件到 dst；dst 不存在时创建，存在时覆盖。 */
    fun download(name: String, dst: File) {
        val connection = open(baseUrl + name, "GET")
        try {
            requireSuccess(connection, "download $name")
            connection.inputStream.use { input ->
                FileOutputStream(dst).use { output -> input.copyTo(output) }
            }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * 列出备份目录下的文件名（只要文件，不要目录本身）。
     * PROPFIND 返回的 href 可能是完整 URL 或绝对路径，这里只取最后一段并 URL 解码。
     */
    fun list(): List<String> {
        val connection = open(baseUrl, "PROPFIND")
        try {
            connection.doOutput = true
            connection.setRequestProperty("Depth", "1")
            connection.setRequestProperty("Content-Type", "application/xml; charset=utf-8")
            val body = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" +
                "<d:propfind xmlns:d=\"DAV:\"><d:propname/></d:propfind>"
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            requireSuccess(connection, "list")
            return parseHrefs(connection.inputStream)
                .map { URLDecoder.decode(it.substringAfterLast('/'), "UTF-8") }
                .filter { it.isNotEmpty() }
                .distinct()
        } finally {
            connection.disconnect()
        }
    }

    /** 连通性检查：能列目录即算配通；鉴权失败、地址不对都会抛 IOException。 */
    fun checkConnection() {
        list()
    }

    private fun parseHrefs(stream: InputStream): List<String> {
        val names = mutableListOf<String>()
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setFeature(XmlPullParserFactory.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(stream, "UTF-8")
        var event = parser.eventType
        while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            // 不处理命名空间时标签名可能是 d:href，按后缀匹配。
            if (event == org.xmlpull.v1.XmlPullParser.START_TAG && parser.name.endsWith("href")) {
                parser.nextText()?.takeIf { it.isNotBlank() }?.let { names.add(it.trim()) }
            }
            event = parser.next()
        }
        // 目录本身的 href（末尾带斜杠）取最后一段为空，已在 list() 里过滤掉。
        return names
    }
}
