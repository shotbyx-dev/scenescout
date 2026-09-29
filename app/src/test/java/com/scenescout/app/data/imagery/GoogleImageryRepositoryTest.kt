package com.scenescout.app.data.imagery

import com.scenescout.app.data.Spot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/**
 * Regression test for the v0.10.2 startup crash:
 * heroForAsync cached a null hero in a ConcurrentHashMap, and
 * ConcurrentHashMap.put(key, null) throws NullPointerException.
 */
class GoogleImageryRepositoryTest {

    private fun spot(photoRefs: List<String> = emptyList()) = Spot(
        id = "test-spot",
        name = "Test Spot",
        latitude = 26.1,
        longitude = -80.2,
        description = "test",
        photoRefs = photoRefs,
    )

    @Test
    fun `heroForAsync with no API key returns null without crashing`() = runBlocking {
        val repo = GoogleImageryRepository(apiKey = { "" })
        // Twice: the second call exercises the cache lookup path.
        assertNull(repo.heroForAsync(spot()))
        assertNull(repo.heroForAsync(spot()))
    }

    @Test
    fun `heroForAsync with key but no photos returns null without crashing`() =
        runBlocking {
            val repo = GoogleImageryRepository(apiKey = { "fake-key" })
            assertNull(repo.heroForAsync(spot()))
            assertNull(repo.heroForAsync(spot()))
        }

    @Test
    fun `heroForAsync with key and photos returns a hero and caches it`() =
        runBlocking {
            val repo = GoogleImageryRepository(apiKey = { "fake-key" })
            val s = spot(listOf("places/abc/photos/xyz"))
            val first = repo.heroForAsync(s)
            assertNotNull(first)
            // The key travels in request headers (PhotoAuth), never in the URL.
            assertFalse(first!!.url.contains("fake-key"))
            // Second call hits the cache — same instance.
            assertSame(first, repo.heroForAsync(s))
        }
}
