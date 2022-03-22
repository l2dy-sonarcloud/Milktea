package jp.panta.misskeyandroidclient.model.file

import org.junit.Assert.*
import org.junit.Test

class AppFileTest {

    @Test
    fun isAttributeSame() {
        val file1 = AppFile.Local("test", "/test/test3", "image/jpeg", null, false, null, 0)
        val file2 = file1.copy()
        assertTrue(file1.isAttributeSame(file2))

        val file3 = file1.copy(name = "test1")
        assertFalse(file3.isAttributeSame(file1))

        assertFalse(file1.copy(path = "/test/test").isAttributeSame(file1))

        assertFalse(file1.copy(type = "video/mp4").isAttributeSame(file1))
        assertTrue(file1.copy(isSensitive = true).isAttributeSame(file1))
        assertTrue(file1.copy(folderId = "hoge").isAttributeSame(file1))
    }
}