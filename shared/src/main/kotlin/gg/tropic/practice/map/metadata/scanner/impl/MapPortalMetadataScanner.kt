package gg.tropic.practice.map.metadata.scanner.impl

import gg.tropic.practice.map.metadata.anonymous.toPosition
import gg.tropic.practice.map.metadata.impl.MapPortalMetadata
import gg.tropic.practice.map.metadata.impl.MapZoneMetadata
import gg.tropic.practice.map.metadata.scanner.AbstractMapMetadataScanner
import gg.tropic.practice.map.metadata.sign.MapSignMetadataModel

/**
 * @author GrowlyX
 * @since 11/10/2022
 */
object MapPortalMetadataScanner : AbstractMapMetadataScanner<MapPortalMetadata>()
{
    override val type = "portal"

    override fun scan(
        id: String,
        models: List<MapSignMetadataModel>
    ): MapPortalMetadata?
    {
        if (models.size != 2)
        {
            return null
        }

        return MapPortalMetadata(
            id,
            models[0].location.toPosition(),
            models[1].location.toPosition()
        )
    }
}
