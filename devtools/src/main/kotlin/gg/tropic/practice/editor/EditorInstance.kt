package gg.tropic.practice.editor

import me.lucko.helper.terminable.Terminable
import org.bukkit.Bukkit
import org.bukkit.World
import java.io.File

/**
 * @author GrowlyX
 * @since 7/18/2024
 */
data class EditorInstance(
    val schematic: File,
    val world: World
): Terminable
{
    override fun close()
    {
        if (Bukkit.getWorld(world.name) == null)
        {
            return
        }

        Bukkit.unloadWorld(world, false)

        File(
            Bukkit.getWorldContainer(),
            world.name
        ).apply {
            deleteRecursively()
        }
    }
}
