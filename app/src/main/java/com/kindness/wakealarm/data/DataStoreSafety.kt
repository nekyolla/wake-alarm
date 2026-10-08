package com.kindness.wakealarm.data

import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import java.io.IOException

/** A corrupted preferences file is replaced by defaults instead of crashing every reader. */
internal fun resetOnCorruption(): ReplaceFileCorruptionHandler<Preferences> =
    ReplaceFileCorruptionHandler { emptyPreferences() }

/** A failed disk read falls back to defaults so the UI and the alarm path keep working. */
internal fun Flow<Preferences>.orDefaultsOnIoError(): Flow<Preferences> =
    catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
