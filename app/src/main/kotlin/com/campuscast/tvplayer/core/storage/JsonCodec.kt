package com.campuscast.tvplayer.core.storage

import kotlinx.serialization.json.Json

val appJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    isLenient = true
    encodeDefaults = true
}
