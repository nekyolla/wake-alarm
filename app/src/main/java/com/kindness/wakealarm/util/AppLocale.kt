package com.kindness.wakealarm.util

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * In-app language choice: follow the system, Indonesian or English.
 *
 * Android 13+ handles per-app languages natively ([LocaleManager]), including the system
 * "App languages" screen. Older versions store the choice here and wrap contexts with [wrap].
 */
object AppLocale {

    enum class Option(val tag: String?) {
        SYSTEM(null),
        INDONESIAN("id"),
        ENGLISH("en")
    }

    private const val PREFS = "app_locale"
    private const val KEY_TAG = "tag"

    fun current(context: Context): Option {
        val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
            if (locales.isEmpty) null else locales[0].language
        } else {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TAG, null)
        }
        return Option.entries.firstOrNull { it.tag != null && it.tag == tag } ?: Option.SYSTEM
    }

    /**
     * Applies [option]. On Android 13+ the system recreates visible activities by itself;
     * on older versions the caller must recreate its activity.
     */
    fun set(context: Context, option: Option) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                option.tag?.let { LocaleList.forLanguageTags(it) } ?: LocaleList.getEmptyLocaleList()
        } else {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_TAG, option.tag).apply()
        }
    }

    /**
     * Returns a context whose resources use the chosen language. A no-op on Android 13+, where the
     * system already applies the per-app language to every component.
     */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val tag = base.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TAG, null) ?: return base
        val locale = Locale.forLanguageTag(tag)
        val config = Configuration(base.resources.configuration)
        config.setLocales(LocaleList(locale))
        return base.createConfigurationContext(config)
    }
}
