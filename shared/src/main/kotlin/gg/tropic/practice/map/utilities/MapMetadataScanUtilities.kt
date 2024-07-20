package gg.tropic.practice.map.utilities

import gg.tropic.practice.map.metadata.AbstractMapMetadata
import gg.tropic.practice.map.metadata.anonymous.Bounds
import gg.tropic.practice.map.metadata.sign.parseIntoMetadata
import org.bukkit.World
import org.bukkit.block.Sign
import org.bukkit.util.Vector

/**
 * @author GrowlyX
 * @since 9/21/2023
 */
object MapMetadataScanUtilities
{
    fun buildMetadataFor(bounds: Bounds, world: World): MapMetadata
    {
        val scheduledRemoval = mutableListOf<Vector>()
        val blocks = bounds.getTileEntities(world)

        val modelMappings = blocks
            .filterIsInstance<Sign>()
            .mapNotNull {
                val metadata = it.lines.toList()
                    .parseIntoMetadata(it.location)

                metadata
            }
            .onEach {
                scheduledRemoval += it.location.block.location.toVector()
            }
            .groupBy { "${it.metaType}${it.id}" }

        val metadata = mutableListOf<AbstractMapMetadata>()
        for (modelMapping in modelMappings)
        {
            val model = modelMapping.value.first()
            val scanner = model.scanner

            val scannedMetadata = runCatching {
                scanner.scan(
                    modelMapping.key.removePrefix(model.metaType),
                    modelMapping.value
                )
            }.getOrNull()

            if (scannedMetadata != null)
            {
                metadata += scannedMetadata
            }
        }

        return MapMetadata(
            metadataSignLocations = scheduledRemoval,
            metadata = metadata
        )
    }
}
