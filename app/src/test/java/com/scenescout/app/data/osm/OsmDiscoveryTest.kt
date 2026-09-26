package com.scenescout.app.data.osm

import com.scenescout.app.data.Spot
import com.scenescout.app.data.imagery.ImagerySource
import com.scenescout.app.data.imagery.SpotImage
import com.scenescout.app.data.imagery.WikimediaClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class OsmDiscoveryTest {

    private val sampleJson = """
        {"elements":[
          {"type":"node","id":11,"lat":26.1,"lon":-80.2,
           "tags":{"tourism":"viewpoint","name":"Sunset Bluff"}},
          {"type":"way","id":22,
           "center":{"lat":26.11,"lon":-80.21},
           "tags":{"tourism":"artwork","artwork_type":"mural"}},
          {"type":"node","id":33,"lat":26.12,"lon":-80.22,
           "tags":{"man_made":"pier","name":"Old Pier",
                   "wikimedia_commons":"File:Old Pier.jpg"}},
          {"type":"node","id":44,"lat":26.13,"lon":-80.23,
           "tags":{"shop":"mall","name":"Big Mall"}},
          {"type":"node","id":55,
           "tags":{"tourism":"viewpoint"}},
          {"type":"relation","id":66,"tags":{"tourism":"viewpoint"}}
        ]}
    """.trimIndent()

    @Test fun parseElements_readsNodesAndWayCenters() {
        val places = OsmDiscovery.parseElements(sampleJson)
        // nodes 11, 33 + way 22 (44 is a mall -> kept here, filtered by kind later)
        assertEquals(4, places.size)
        val way = places.first { it.osmId == 22L }
        assertEquals(26.11, way.lat, 1e-9)
        assertEquals(-80.21, way.lng, 1e-9)
        assertNull(way.name)
    }

    @Test fun parseElements_skipsMalformed() {
        assertTrue(OsmDiscovery.parseElements("not json").isEmpty())
        assertTrue(OsmDiscovery.parseElements("{}").isEmpty())
    }

    @Test fun kindFor_matchesCinematicKinds() {
        assertEquals("Scenic viewpoint",
            OsmDiscovery.kindFor(mapOf("tourism" to "viewpoint"))!!.label)
        assertEquals("Mural",
            OsmDiscovery.kindFor(
                mapOf("tourism" to "artwork", "artwork_type" to "mural"))!!.label)
        assertEquals("Pier",
            OsmDiscovery.kindFor(mapOf("man_made" to "pier"))!!.label)
        assertEquals("Beach",
            OsmDiscovery.kindFor(mapOf("natural" to "beach"))!!.label)
        assertEquals("Historic site",
            OsmDiscovery.kindFor(mapOf("historic" to "battlefield"))!!.label)
        assertNull(OsmDiscovery.kindFor(mapOf("shop" to "mall")))
    }

    @Test fun photoRefFor_prefersWikimediaCommonsFile() {
        assertEquals("File:Old Pier.jpg",
            OsmDiscovery.photoRefFor(mapOf("wikimedia_commons" to "File:Old Pier.jpg")))
        assertEquals("https://example.com/p.jpg",
            OsmDiscovery.photoRefFor(mapOf("image" to "https://example.com/p.jpg")))
        assertNull(OsmDiscovery.photoRefFor(mapOf("name" to "x")))
        // Categories aren't directly resolvable — ignored.
        assertNull(OsmDiscovery.photoRefFor(mapOf("wikimedia_commons" to "Category:Piers")))
    }

    @Test fun toSpot_buildsSpotWithFallbackName() {
        val place = OsmDiscovery.OsmPlace(
            "way", 22, null, 26.11, -80.21,
            mapOf("tourism" to "artwork", "artwork_type" to "mural"))
        val spot = OsmDiscovery.toSpot(place, null)!!
        assertEquals("osm-way-22", spot.id)
        assertEquals("Unnamed mural", spot.name)
        assertTrue(spot.tags.contains("mural"))
        assertTrue(spot.aiScore in 1..100)
        assertEquals("OpenStreetMap", spot.submittedBy)
    }

    @Test fun toSpot_attachesPhoto() {
        val place = OsmDiscovery.OsmPlace(
            "node", 33, "Old Pier", 26.12, -80.22, mapOf("man_made" to "pier"))
        val photo = SpotImage("https://x/y.jpg", ImagerySource.WIKIMEDIA_COMMONS)
        val spot = OsmDiscovery.toSpot(place, photo)!!
        assertEquals(1, spot.images.size)
        assertEquals("https://x/y.jpg", spot.images[0].url)
    }

    @Test fun merge_dedupesNearbyLiveSpots() {
        fun spot(id: String, lat: Double, lng: Double) = Spot(
            id = id, name = id, latitude = lat, longitude = lng,
            description = "d", tags = emptyList(), bestFor = emptyList())
        val static = listOf(spot("s1", 26.1, -80.2))
        val live = listOf(
            spot("o1", 26.1005, -80.2005), // ~70m away -> same place, drop
            spot("o2", 26.2, -80.3),      // far -> keep
        )
        val merged = OsmDiscovery.merge(static, live)
        assertEquals(listOf("s1", "o2"), merged.map { it.id })
    }

    @Test fun buildQuery_containsAroundAndTimeout() {
        val q = OsmDiscovery.buildQuery(26.1, -80.2, 10000, parksOnly = false)
        assertTrue(q.contains("around:10000,26.1,-80.2"))
        assertTrue(q.contains("[timeout:25]"))
        assertTrue(q.contains("artwork"))
        assertFalse(q.contains("\"park\""))
    }

    @Test fun buildQuery_parksOnlyIsSeparateSmallQuery() {
        val q = OsmDiscovery.buildQuery(26.1, -80.2, 10000, parksOnly = true)
        assertTrue(q.contains("leisure\"=\"park\"") || q.contains("[\"leisure\"=\"park\"]") ||
            q.contains("\"leisure\"=\"park\""))
        assertFalse(q.contains("artwork"))
        assertTrue(q.endsWith("out center tags 12;"))
    }

    @Test fun isNoise_dropsUnnamedParksButKeepsNamed() {
        val unnamedPark = OsmDiscovery.OsmPlace(
            "node", 1, null, 0.0, 0.0, mapOf("leisure" to "park"))
        val namedPark = OsmDiscovery.OsmPlace(
            "node", 2, "Central Park", 0.0, 0.0, mapOf("leisure" to "park"))
        val unnamedMural = OsmDiscovery.OsmPlace(
            "node", 3, null, 0.0, 0.0,
            mapOf("tourism" to "artwork", "artwork_type" to "mural"))
        assertTrue(OsmDiscovery.isNoise(unnamedPark))
        assertFalse(OsmDiscovery.isNoise(namedPark))
        assertFalse(OsmDiscovery.isNoise(unnamedMural))
    }

    @Test fun dedupePlaces_dropsSameNamedPlaceTwice() {
        fun place(id: Long, type: String, name: String?, lat: Double, lng: Double) =
            OsmDiscovery.OsmPlace(type, id, name, lat, lng, mapOf("leisure" to "park"))
        val places = listOf(
            place(1, "node", "Sailboat Park", 26.1, -80.2),
            place(2, "way", "Sailboat Park", 26.1002, -80.2002), // ~30m, same name
            place(3, "node", "Other Park", 26.5, -80.5),
        )
        val kept = OsmDiscovery.dedupePlaces(places)
        assertEquals(listOf(1L, 3L), kept.map { it.osmId })
    }

    @Test fun kindPriority_muralsRankBeforeParks() {
        val mural = OsmDiscovery.kindFor(
            mapOf("tourism" to "artwork", "artwork_type" to "mural"))!!
        val park = OsmDiscovery.kindFor(mapOf("leisure" to "park"))!!
        assertTrue(mural.priority < park.priority)
    }

    @Test fun haversineM_sane() {
        // 1 degree of latitude ~= 111.2 km.
        val m = OsmDiscovery.haversineM(0.0, 0.0, 1.0, 0.0)
        assertTrue(abs(m - 111200.0) < 1000.0)
    }

    @Test fun wikimediaJunkFilter_skipsSpecimensAndLogos() {
        assertTrue(WikimediaClient.isJunkTitle(
            "File:Technomyrmex difficilis casent0003318 label 1.jpg"))
        assertTrue(WikimediaClient.isJunkTitle("File:Some emblem.svg"))
        assertTrue(WikimediaClient.isJunkTitle("File:City Logo.png"))
        assertFalse(WikimediaClient.isJunkTitle("File:Sunset over the pier.jpg"))
        assertFalse(WikimediaClient.isJunkTitle("File:Downtown skyline at dusk.jpg"))
    }

    @Test fun wikimediaParse_skipsJunkPages() {
        val json = """{"query":{"pages":{
          "1":{"title":"File:Ant casent0001 dorsal 1.jpg",
               "imageinfo":[{"thumburl":"https://x/ant.jpg"}]},
          "2":{"title":"File:Sunset pier.jpg",
               "imageinfo":[{"thumburl":"https://x/pier.jpg"}]}
        }}}"""
        val photos = WikimediaClient.parse(json)
        assertNotNull(photos)
        assertEquals(1, photos.size)
        assertEquals("https://x/pier.jpg", photos[0].url)
    }
}
