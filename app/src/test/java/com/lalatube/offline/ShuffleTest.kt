package com.lalatube.offline

import com.lalatube.offline.data.Video
import com.lalatube.offline.util.shuffledForSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShuffleTest {
    private fun videos(count: Int): List<Video> = (1..count).map { index ->
        Video(
            uri = "content://video/$index",
            title = "Video $index",
            channel = "Kanal",
            folder = "Anak",
        )
    }

    @Test
    fun urutanTetapSamaUntukSeedYangSama() {
        val list = videos(40)
        val first = list.shuffledForSession(12345L).map { it.uri }
        val second = list.shuffledForSession(12345L).map { it.uri }
        assertEquals(first, second)
    }

    @Test
    fun seedBerbedaMengubahUrutan() {
        val list = videos(60)
        val first = list.shuffledForSession(1L).map { it.uri }
        val second = list.shuffledForSession(2L).map { it.uri }
        assertTrue("urutan harus berubah saat seed berganti", first != second)
    }

    @Test
    fun pengacakanTidakMenghilangkanVideo() {
        val list = videos(30)
        val shuffled = list.shuffledForSession(99L)
        assertEquals(list.size, shuffled.size)
        assertEquals(list.map { it.uri }.toSet(), shuffled.map { it.uri }.toSet())
    }
}
