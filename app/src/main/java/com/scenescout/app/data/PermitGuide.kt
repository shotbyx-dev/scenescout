package com.scenescout.app.data

/**
 * Permit guidance. There is no single US-wide filming-permit API, so v1 ships
 * with a curated table of major film offices plus a safe default. The app
 * surfaces the level + a link; the user confirms with the local office.
 */
object PermitGuide {

    data class Jurisdiction(
        val match: String, // substring matched against "city, state"
        val level: PermitLevel,
        val summary: String,
        val authorityName: String,
        val authorityUrl: String,
    )

    private val table = listOf(
        Jurisdiction(
            "los angeles, ca", PermitLevel.REQUIRED,
            "LA requires a FilmLA permit for almost any filming on public property, including run-and-gun with a tripod.",
            "FilmLA", "https://www.filmla.com",
        ),
        Jurisdiction(
            "new york, ny", PermitLevel.REQUIRED,
            "NYC requires an MOME permit when you use equipment beyond a handheld camera, or block sidewalks/streets.",
            "Mayor's Office of Media & Entertainment", "https://www.nyc.gov/mome",
        ),
        Jurisdiction(
            "miami, fl", PermitLevel.SIMPLE,
            "Miami-Dade: small crews can often get a free/low-cost permit via the county film office; always check for parks and beaches.",
            "Miami-Dade Film Office", "https://www.filmiamidade.com",
        ),
        Jurisdiction(
            "atlanta, ga", PermitLevel.SIMPLE,
            "Atlanta: free film permit application through the Mayor's Office of Film & Entertainment for public property.",
            "Atlanta Mayor's Office of Film", "https://www.atlanta.gov/film-entertainment",
        ),
        Jurisdiction(
            "austin, tx", PermitLevel.SIMPLE,
            "Austin: free permit via the Austin Film Commission for most public locations; some parks need separate approval.",
            "Austin Film Commission", "https://www.austinfilmcommission.com",
        ),
        Jurisdiction(
            "national park", PermitLevel.REQUIRED,
            "US National Parks require a commercial filming permit from the NPS — apply weeks ahead.",
            "National Park Service", "https://www.nps.gov",
        ),
    )

    fun lookup(cityState: String): PermitInfo {
        val key = cityState.lowercase()
        val hit = table.firstOrNull { key.contains(it.match) }
        return if (hit == null) {
            PermitInfo.unknown()
        } else {
            PermitInfo(hit.level, hit.summary, hit.authorityName, hit.authorityUrl)
        }
    }

    /** All curated jurisdictions, for the in-app reference list. */
    fun all(): List<Jurisdiction> = table
}
