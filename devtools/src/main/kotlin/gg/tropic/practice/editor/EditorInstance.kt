package gg.tropic.practice.editor

import me.lucko.helper.terminable.composite.CompositeTerminable
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
): CompositeTerminable by CompositeTerminable.create()
{
    init
    {
        with {
            if (Bukkit.getWorld(world.name) == null)
            {
                return@with
            }

            Bukkit.unloadWorld(world, false)
        }
    }
}
