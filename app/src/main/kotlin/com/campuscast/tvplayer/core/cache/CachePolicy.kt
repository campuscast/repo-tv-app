package com.campuscast.tvplayer.core.cache

data class CacheCleanupDecision(
    val canCleanup: Boolean,
    val keepFiles: Set<String>,
    val removeFiles: Set<String>,
)

object CachePolicy {
    fun buildCleanupDecision(
        existingFiles: Set<String>,
        expectedFiles: Set<String>,
        missingAssets: Int,
    ): CacheCleanupDecision {
        val canCleanup = missingAssets == 0
        val remove = if (canCleanup) existingFiles - expectedFiles else emptySet()
        return CacheCleanupDecision(
            canCleanup = canCleanup,
            keepFiles = expectedFiles,
            removeFiles = remove,
        )
    }
}
