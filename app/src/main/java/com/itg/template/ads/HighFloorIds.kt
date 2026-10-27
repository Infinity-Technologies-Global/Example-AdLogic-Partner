package com.itg.template.ads

object HighFloorIds {

    /** Id of [config] when it is enabled and set, otherwise null. */
    fun idOf(config: AdUnitConfig?): String? =
        config?.takeIf { it.isEnable && it.id.isNotBlank() }?.id

    /** Ad unit ids to try in priority order: high floor first, then normal. */
    fun resolve(high: AdUnitConfig?, normal: AdUnitConfig): List<String> =
        listOfNotNull(idOf(high), idOf(normal)).distinct()
}
