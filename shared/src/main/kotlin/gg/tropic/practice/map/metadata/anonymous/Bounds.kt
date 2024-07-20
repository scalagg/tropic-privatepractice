package gg.tropic.practice.map.metadata.anonymous

import org.bukkit.Chunk
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockState

/**
 * @author GrowlyX
 * @since 9/23/2023
 */
data class Bounds(
    val lowerLeft: Position,
    val upperRight: Position
)
{
    fun getTileEntities(world: World): List<BlockState>
    {
        return world.loadedChunks
            .flatMap {
                it.tileEntities.toList()
            }
    }

    fun getChunks(world: World): List<Chunk>
    {
        val chunks = mutableListOf<Chunk>()
        val minX = lowerLeft.x.toInt()
        val maxX = upperRight.x.toInt()
        val minZ = lowerLeft.z.toInt()
        val maxZ = upperRight.z.toInt()

        for (x in (if (maxX > minX) minX..maxX else maxX..minX))
        {
            for (z in (if (maxZ > minZ) minZ..maxZ else maxZ..minZ))
            {
                if (!world.isChunkLoaded(x, z))
                    world.loadChunk(x, z, false)

                chunks.add(
                    world.getChunkAt(x, z)
                )
            }
        }

        return chunks
    }

    fun utility(world: World) = iterator<Block?> {
        val minX = minOf(lowerLeft.x, upperRight.x).toInt()
        val maxX = maxOf(lowerLeft.x, upperRight.x).toInt()

        val minY = minOf(lowerLeft.y, upperRight.y).toInt()
        val maxY = maxOf(lowerLeft.y, upperRight.y).toInt()

        val minZ = minOf(lowerLeft.z, upperRight.z).toInt()
        val maxZ = maxOf(lowerLeft.z, upperRight.z).toInt()

        for (x in minX..maxX)
        {
            for (y in minY..maxY)
            {
                for (z in minZ..maxZ)
                {
                    val block = world.getBlockAt(x, y, z)
                    yield(block)
                }
            }
        }
    }

    operator fun contains(position: Position): Boolean
    {
        return position.x >= lowerLeft.x &&
            position.x <= upperRight.x &&
            position.z >= lowerLeft.z &&
            position.z <= upperRight.z
    }
}
