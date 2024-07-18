package gg.tropic.practice.editor

import org.bukkit.Bukkit
import java.io.File

/**
 * @author GrowlyX
 * @since 7/18/2024
 */
class Containers
{
    val pluginsDirectory = File(
        Bukkit.getWorldContainer(),
        "plugins"
    )

    val worldEditDirectory = File(
        pluginsDirectory,
        "WorldEdit"
    )

    val schematicsDirectory = File(
        worldEditDirectory,
        "schematics"
    )
}
