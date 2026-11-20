package com.padelpro.app.data.content

import android.content.Context
import com.padelpro.app.domain.academy.AcademyContentParser
import com.padelpro.app.domain.academy.ContentPack
import java.io.IOException

interface AcademyContentProvider {
    fun load(): ContentPack
}

class AssetAcademyContentLoader(
    context: Context
) : AcademyContentProvider {

    private val appContext = context.applicationContext

    @Volatile
    private var cachedPack: ContentPack? = null

    override fun load(): ContentPack {
        cachedPack?.let { return it }
        return synchronized(this) {
            cachedPack?.let { return@synchronized it }
            val pack = AcademyContentParser.parse(
                versionJson = readAssetText(PATH_VERSION),
                coursesJson = readAssetText(PATH_COURSES),
                rulebookJson = readAssetText(PATH_RULEBOOK),
                achievementsJson = readAssetText(PATH_ACHIEVEMENTS)
            )
            cachedPack = pack
            pack
        }
    }

    private fun readAssetText(path: String): String {
        return try {
            appContext.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: IOException) {
            throw IllegalStateException("Failed to load bundled Academy asset: $path", e)
        }
    }

    companion object {
        const val PATH_VERSION = "content/content_version.json"
        const val PATH_COURSES = "content/courses.json"
        const val PATH_RULEBOOK = "content/rulebook.json"
        const val PATH_ACHIEVEMENTS = "content/achievements.json"
    }
}
