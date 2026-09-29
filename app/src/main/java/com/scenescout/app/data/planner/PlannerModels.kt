package com.scenescout.app.data.planner

import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/**
 * Everything a videographer plans for shoot day: the treatment, the shot
 * list, the day schedule, and the gear checklist. Pure Kotlin — the JSON
 * round-trip is unit-tested, persistence lives in [PlannerStore].
 */
data class ShotItem(
    val id: String = UUID.randomUUID().toString(),
    val description: String,
    val locationName: String = "",
    val lens: String = "",
    val done: Boolean = false,
)

data class ScheduleBlock(
    val id: String = UUID.randomUUID().toString(),
    /** "HH:mm" 24h — sorts lexicographically. */
    val time: String,
    val title: String,
    val note: String = "",
)

data class GearItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val packed: Boolean = false,
)

data class ShootProject(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val client: String = "",
    /** "yyyy-MM-dd". */
    val shootDate: String = "",
    /** Treatment / concept notes. */
    val treatment: String = "",
    val shots: List<ShotItem> = emptyList(),
    val schedule: List<ScheduleBlock> = emptyList(),
    val gear: List<GearItem> = emptyList(),
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("client", client)
        put("shootDate", shootDate)
        put("treatment", treatment)
        put("shots", JSONArray(shots.map {
            JSONObject().apply {
                put("id", it.id); put("description", it.description)
                put("locationName", it.locationName); put("lens", it.lens)
                put("done", it.done)
            }
        }))
        put("schedule", JSONArray(schedule.map {
            JSONObject().apply {
                put("id", it.id); put("time", it.time)
                put("title", it.title); put("note", it.note)
            }
        }))
        put("gear", JSONArray(gear.map {
            JSONObject().apply {
                put("id", it.id); put("name", it.name); put("packed", it.packed)
            }
        }))
    }

    companion object {
        fun fromJson(o: JSONObject): ShootProject = ShootProject(
            id = o.optString("id", UUID.randomUUID().toString()),
            title = o.optString("title"),
            client = o.optString("client"),
            shootDate = o.optString("shootDate"),
            treatment = o.optString("treatment"),
            shots = o.optJSONArray("shots")?.asObjects()?.map {
                ShotItem(
                    id = it.optString("id", UUID.randomUUID().toString()),
                    description = it.optString("description"),
                    locationName = it.optString("locationName"),
                    lens = it.optString("lens"),
                    done = it.optBoolean("done"),
                )
            }.orEmpty(),
            schedule = o.optJSONArray("schedule")?.asObjects()?.map {
                ScheduleBlock(
                    id = it.optString("id", UUID.randomUUID().toString()),
                    time = it.optString("time"),
                    title = it.optString("title"),
                    note = it.optString("note"),
                )
            }.orEmpty(),
            gear = o.optJSONArray("gear")?.asObjects()?.map {
                GearItem(
                    id = it.optString("id", UUID.randomUUID().toString()),
                    name = it.optString("name"),
                    packed = it.optBoolean("packed"),
                )
            }.orEmpty(),
        )

        fun listToJson(projects: List<ShootProject>): String =
            JSONArray(projects.map { it.toJson() }).toString()

        fun listFromJson(raw: String): List<ShootProject> =
            try {
                JSONArray(raw).asObjects().map { fromJson(it) }
            } catch (e: Exception) {
                emptyList() // Corrupt file — start fresh, never crash.
            }
    }
}

private fun JSONArray.asObjects(): List<JSONObject> =
    // Skip corrupt elements instead of throwing: one bad entry must never
    // wipe the user's whole planner file on load.
    (0 until length()).mapNotNull { optJSONObject(it) }
