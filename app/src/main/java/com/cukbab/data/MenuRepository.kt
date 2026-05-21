package com.cukbab.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import java.time.LocalDate
import java.time.temporal.WeekFields
import androidx.core.content.edit

typealias MenuData = Map<String, Map<String, String>>

// Use a public named class to help R8 preserve the generic signature
class MenuDataTypeToken : TypeToken<Map<String, Map<String, String>>>()

interface MenuService {
    @Headers("Cache-Control: no-cache")
    @GET("CUKbab/CUK_Menu/refs/heads/main/latest.json")
    suspend fun getLatestMenu(): Map<String, Map<String, String>>

    @GET("CUKbab/CUK_Menu/refs/heads/main/menus/{year}/{week}/menu.json")
    suspend fun getArchivedMenu(
        @retrofit2.http.Path("year") year: Int,
        @retrofit2.http.Path("week") week: Int
    ): Map<String, Map<String, String>>

    @GET("CUKbab/CUK/refs/heads/main/announcements.json")
    suspend fun getAnnouncements(): List<Announcement>
}

object RetrofitClient {
    private const val BASE_URL = "https://raw.githubusercontent.com/"

    val menuService: MenuService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MenuService::class.java)
    }
}

object MenuRepository {
    private const val PREFS_NAME = "menu_cache"
    private const val KEY_MENU_JSON_PREFIX = "cached_menu_json_"
    private val gson = Gson()
    private val weekFields = WeekFields.ISO

    suspend fun getMenu(context: Context, date: LocalDate, forceRefresh: Boolean = false): MenuData {
        val cacheKey = getCacheKey(date)

        if (!forceRefresh) {
            getCachedMenu(context, cacheKey)?.let { return it }
        }

        val dateString = date.toString()

        return try {
            // 1. Try to fetch from latest.json first
            var rawData = withContext(Dispatchers.IO) {
                RetrofitClient.menuService.getLatestMenu()
            }

            // 2. Check if latest.json actually contains the requested date
            val hasDate = rawData.values.any { it.containsKey(dateString) }

            if (!hasDate) {
                // 3. Fallback to archive if it's not in latest.json
                rawData = fetchArchivedMenu(date)
            }

            val menuData = processMenuData(rawData)

            // 4. Cache the result if it contains the data we need
            if (menuData.values.any { it.containsKey(dateString) }) {
                cacheMenu(context, cacheKey, menuData)
            }

            menuData
        } catch (e: Exception) {
            // Fallback to cache if network fails
            getCachedMenu(context, cacheKey) ?: throw e
        }
    }

    private suspend fun fetchArchivedMenu(date: LocalDate): MenuData {
        val weekNumber = date.get(weekFields.weekOfWeekBasedYear())
        val year = date.year
        return withContext(Dispatchers.IO) {
            RetrofitClient.menuService.getArchivedMenu(year, weekNumber)
        }
    }

    private fun processMenuData(data: MenuData): MenuData {
        return data.mapValues { (_, menuMap) ->
            menuMap.mapValues { (_, menuText) ->
                menuText.replace("\\n", "\n").trim()
            }
        }
    }

    fun isCached(context: Context, date: LocalDate): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .contains(getCacheKey(date))
    }

    private fun isSameWeek(d1: LocalDate, d2: LocalDate): Boolean {
        return d1.year == d2.year && 
               d1.get(weekFields.weekOfWeekBasedYear()) == d2.get(weekFields.weekOfWeekBasedYear())
    }

    private fun getCacheKey(date: LocalDate): String {
        val weekNumber = date.get(weekFields.weekOfWeekBasedYear())
        return "$KEY_MENU_JSON_PREFIX${date.year}_$weekNumber"
    }

    private fun cacheMenu(context: Context, key: String, data: MenuData) {
        val json = gson.toJson(data)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(key, json)
        }
    }

    private fun getCachedMenu(context: Context, key: String): MenuData? {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(key, null) ?: return null

        return try {
            val type = MenuDataTypeToken().type
            val rawData: MenuData = gson.fromJson(json, type)
            processMenuData(rawData)
        } catch (e: Exception) {
            null
        }
    }
}
