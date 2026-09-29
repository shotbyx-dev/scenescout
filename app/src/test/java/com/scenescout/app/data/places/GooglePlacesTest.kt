package com.scenescout.app.data.places

import com.scenescout.app.data.ShootType
import com.scenescout.app.data.Spot
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class GooglePlacesTest {

    // --- request builders ---

    @Test
    fun `nearby body is valid JSON with shoot types and circle`() {
        val body = GooglePlaces.nearbyBody(26.1, -80.2, 10000)
        val o = JSONObject(body)
        val types = o.getJSONArray("includedTypes")
        val set = (0 until types.length()).map { types.getString(it) }.toSet()
        assertTrue(set.contains("museum"))
        assertTrue(set.contains("tourist_attraction"))
        assertTrue(set.contains("beach"))
        val circle = o.getJSONObject("locationRestriction").getJSONObject("circle")
        assertEquals(26.1, circle.getJSONObject("center").getDouble("latitude"), 1e-9)
        assertEquals(-80.2, circle.getJSONObject("center").getDouble("longitude"), 1e-9)
        assertEquals(10000.0, circle.getDouble("radius"), 1e-9)
        assertEquals(20, o.getInt("maxResultCount"))
    }

    @Test
    fun `text body carries the query with location bias`() {
        val body = GooglePlaces.textBody("neon mural", 26.1, -80.2)
        val o = JSONObject(body)
        assertEquals("neon mural", o.getString("textQuery"))
        assertTrue(o.has("locationBias"))
    }

    @Test
    fun `photo url format`() {
        val url = GooglePlaces.photoUrl("places/abc/photos/xyz", 800)
        assertEquals(
            "https://places.googleapis.com/v1/places/abc/photos/xyz/media?maxWidthPx=800",
            url,
        )
    }

    @Test
    fun `photo url never embeds the api key`() {
        // The key travels in request headers (PhotoAuth), never in the URL,
        // so it can't leak into logs or caches.
        val url = GooglePlaces.photoUrl("places/abc/photos/xyz", 1600)
        assertFalse(url.contains("key=", ignoreCase = true))
        assertFalse(url.contains("SECRET"))
    }

    // --- parsing ---

    private val sample = """
        {"places": [
          {"id": "ChIJ1", "displayName": {"text": "Neon Museum"},
           "formattedAddress": "123 Main St",
           "location": {"latitude": 26.1, "longitude": -80.2},
           "rating": 4.6, "userRatingCount": 210,
           "types": ["museum", "tourist_attraction"],
           "editorialSummary": {"text": "Glowing signs."},
           "photos": [
             {"name": "places/ChIJ1/photos/p1",
              "authorAttributions": [{"displayName": "Jane"}]},
             {"name": "places/ChIJ1/photos/p2",
              "authorAttributions": [{"displayName": ""}]}
           ]},
          {"id": "ChIJ2", "displayName": {"text": "No Coords"},
           "location": {},
           "types": ["park"]},
          {"displayName": {"text": "No Id"},
           "location": {"latitude": 1.0, "longitude": 2.0}}
        ]}
    """.trimIndent()

    @Test
    fun `parse places maps fields and skips invalid entries`() {
        val spots = GooglePlaces.parsePlaces(sample)
        assertEquals(1, spots.size)
        val s = spots[0]
        assertEquals("google:ChIJ1", s.id)
        assertEquals("Neon Museum", s.name)
        assertEquals(26.1, s.latitude, 1e-9)
        assertEquals("Glowing signs.", s.description)
        assertEquals(4.6, s.communityRating, 1e-9)
        assertEquals(210, s.reviewCount)
        assertEquals(
            listOf("places/ChIJ1/photos/p1", "places/ChIJ1/photos/p2"),
            s.photoRefs,
        )
        assertEquals(listOf("Jane", "Google"), s.photoCredits)
        assertTrue(s.tags.contains("museum"))
        assertTrue(s.tags.contains("attraction"))
        assertTrue(s.bestFor.contains(ShootType.NARRATIVE))
        assertEquals("Google Places", s.submittedBy)
    }

    @Test
    fun `parse malformed json returns empty`() {
        assertTrue(GooglePlaces.parsePlaces("not json").isEmpty())
        assertTrue(GooglePlaces.parsePlaces("{}").isEmpty())
    }

    // --- mapping ---

    @Test
    fun `tags humanize underscores with overrides`() {
        assertEquals(
            listOf("attraction", "art gallery", "church"),
            GooglePlaces.tagsFor(
                listOf("tourist_attraction", "art_gallery", "church")),
        )
    }

    @Test
    fun `bestFor maps types to shoot types`() {
        val bf = GooglePlaces.bestFor(listOf("night_club", "beach"))
        assertTrue(bf.contains(ShootType.MUSIC_VIDEO))
        assertTrue(bf.contains(ShootType.SCENIC))
    }

    @Test
    fun `bestFor gives multi-category types every category`() {
        val stadium = GooglePlaces.bestFor(listOf("stadium"))
        assertTrue(stadium.contains(ShootType.MUSIC_VIDEO))
        assertTrue(stadium.contains(ShootType.DRONE))
        val beach = GooglePlaces.bestFor(listOf("beach"))
        assertTrue(beach.contains(ShootType.SCENIC))
        assertTrue(beach.contains(ShootType.DRONE))
    }

    @Test
    fun `bestFor falls back to run and gun`() {
        assertEquals(
            listOf(ShootType.RUN_AND_GUN),
            GooglePlaces.bestFor(listOf("car_wash")),
        )
    }

    // --- merging ---

    private fun spot(id: String, lat: Double, lng: Double) = Spot(
        id = id, name = id, latitude = lat, longitude = lng, description = "",
    )

    @Test
    fun `merge drops live spots too close to static ones`() {
        val static = listOf(spot("s1", 26.0, -80.0))
        val live = listOf(
            spot("l1", 26.0005, -80.0005), // ~70m away -> dropped
            spot("l2", 26.01, -80.01), // ~1.5km away -> kept
        )
        val merged = GooglePlaces.mergeSpots(static, live)
        assertEquals(listOf("s1", "l2"), merged.map { it.id })
    }

    @Test
    fun `haversine sanity`() {
        // ~111km per degree of latitude.
        val m = GooglePlaces.haversineM(26.0, -80.0, 27.0, -80.0)
        assertTrue(m in 110000.0..112000.0)
    }

    // --- key ---

    @Test
    fun `hasKey rejects blank and placeholder`() {
        assertFalse(GooglePlaces.hasKey(""))
        assertFalse(GooglePlaces.hasKey("MAPS_API_KEY_NOT_SET"))
        assertTrue(GooglePlaces.hasKey("AIza..."))
    }

    // --- friendly errors ---

    @Test
    fun `friendlyError explains common failures`() {
        assertTrue(
            GooglePlaces.friendlyError(Exception("Places HTTP 400: {bad}"))
                .contains("invalid", ignoreCase = true),
        )
        assertTrue(
            GooglePlaces.friendlyError(Exception("Places HTTP 403: {denied}"))
                .contains("billing", ignoreCase = true),
        )
        assertTrue(
            GooglePlaces.friendlyError(Exception("Places HTTP 429: {slow}"))
                .contains("quota", ignoreCase = true),
        )
        assertTrue(
            GooglePlaces.friendlyError(java.net.UnknownHostException("x"))
                .contains("internet", ignoreCase = true),
        )
        assertTrue(
            GooglePlaces.friendlyError(null).isNotBlank(),
        )
    }

    // --- visual-appeal heuristic (regression: Google spots showed AI 0) ---

    @Test
    fun `estimateVisualAppeal rewards scenic types ratings and photos`() {
        val scenic = GooglePlaces.estimateVisualAppeal(
            listOf("tourist_attraction", "park"), 4.7, 3)
        assertTrue(scenic > 70)
    }

    @Test
    fun `estimateVisualAppeal penalizes dull types`() {
        val dull = GooglePlaces.estimateVisualAppeal(listOf("parking"), 0.0, 0)
        assertTrue(dull < 50)
    }

    @Test
    fun `estimateVisualAppeal baseline for a plain place`() {
        assertEquals(55, GooglePlaces.estimateVisualAppeal(listOf("store"), 0.0, 0))
    }

    @Test
    fun `estimateVisualAppeal stays within 0 to 100`() {
        val high = GooglePlaces.estimateVisualAppeal(
            listOf("beach", "park", "tourist_attraction"), 5.0, 10)
        assertTrue(high in 0..100)
        val low = GooglePlaces.estimateVisualAppeal(
            listOf("parking", "gas_station"), 1.0, 0)
        assertTrue(low in 0..100)
    }

    @Test
    fun `parsePlace assigns a nonzero scenic score to google spots`() {
        val json = """{"places":[{
            "id":"p1","displayName":{"text":"Sunset Park"},
            "location":{"latitude":26.1,"longitude":-80.2},
            "types":["park","tourist_attraction"],
            "rating":4.6,"userRatingCount":120,
            "photos":[{"name":"places/p1/photos/a",
                "authorAttributions":[{"displayName":"X"}]}]
        }]}"""
        val spot = GooglePlaces.parsePlaces(json).single()
        assertTrue(spot.aiScore > 0)
    }
}
