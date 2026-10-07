package com.sumit.varnamala.ocr

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import com.sumit.varnamala.ocr.paddle.ModelDownloader
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.security.MessageDigest

/** The downloader behind every model download, run against a small local web server. */
class ModelDownloaderTest {
    @get:Rule val folder = TemporaryFolder()
    private lateinit var server: MockWebServer
    private val payload = ByteArray(300_000) { (it * 31 % 251).toByte() }
    private val sha = MessageDigest.getInstance("SHA-256").digest(payload).joinToString("") { "%02x".format(it) }
    private var requestedRanges = mutableListOf<String?>()
    private var status = 200

    @Before fun start() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val range = request.getHeader("Range")
                requestedRanges += range
                return when {
                    status != 200 -> MockResponse().setResponseCode(status)
                    range != null -> {
                        val from = range.removePrefix("bytes=").removeSuffix("-").toInt()
                        MockResponse().setResponseCode(206).setBody(Buffer().write(payload, from, payload.size - from))
                    }
                    else -> MockResponse().setBody(Buffer().write(payload))
                }
            }
        }
        server.start()
    }

    @After fun stop() = server.shutdown()

    private val url get() = server.url("/model").toString()

    private fun download(target: java.io.File, size: Long = payload.size.toLong(), checksum: String = sha) = runBlocking {
        ModelDownloader.downloadVerified(ModelDownloader.newClient(), url, size, checksum, "test model", target) {}
    }

    @Test fun `downloads, verifies and installs the file`() {
        val target = folder.newFile("model.bin").also { it.delete() }
        download(target)
        assertArrayEquals(payload, target.readBytes())
        assertFalse("the partial file must be gone", java.io.File(target.parentFile, "model.bin.partial").exists())
    }

    @Test fun `resumes a partial download with a range request`() {
        val target = java.io.File(folder.root, "model.bin")
        java.io.File(folder.root, "model.bin.partial").writeBytes(payload.copyOf(100_000))
        download(target)
        assertArrayEquals(payload, target.readBytes())
        assertEquals("bytes=100000-", requestedRanges.single())
    }

    @Test fun `a corrupt download is rejected and removed`() {
        val target = java.io.File(folder.root, "model.bin")
        try {
            download(target, checksum = "0".repeat(64))
            fail("expected a checksum error")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("Checksum mismatch"))
        }
        assertFalse(target.exists())
        assertFalse(java.io.File(folder.root, "model.bin.partial").exists())
    }

    @Test fun `a server error is reported and nothing is installed`() {
        status = 503
        val target = java.io.File(folder.root, "model.bin")
        try {
            download(target)
            fail("expected an error")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("503"))
        }
        assertFalse(target.exists())
    }
}
