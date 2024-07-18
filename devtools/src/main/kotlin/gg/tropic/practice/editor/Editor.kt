package gg.tropic.practice.editor

import com.sk89q.worldedit.Vector
import gg.scala.lemon.hotbar.HotbarPreset
import gg.scala.lemon.hotbar.HotbarPresetHandler
import gg.scala.lemon.hotbar.entry.impl.DynamicHotbarPresetEntry
import gg.tropic.practice.map.MapManageServices
import gg.tropic.practice.utilities.WorldEditUtils
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.terminable.composite.CompositeTerminable
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Constants
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.bukkit.prompt.InputPrompt
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import java.io.File

/**
 * @author GrowlyX
 * @since 7/18/2024
 */
class Editor(private val player: Player) : CompositeTerminable by CompositeTerminable.create()
{
    private val containers = Containers()
    private val editorID = "editor-${player.uniqueId}"

    private var visiting: EditorInstance? = null
    private val schematics = containers.schematicsDirectory.listFiles()
        ?.toList()?.filterNotNull() ?: emptyList()

    fun initialize()
    {
        if (schematics.isEmpty())
        {
            player.sendMessage("${CC.RED}There are no schematics to edit!")
            return
        }

        Events
            .subscribe(PlayerQuitEvent::class.java)
            .handler {
                closeAndReportException()
            }
            .bindWith(this)

        Schedulers
            .sync()
            .runRepeating({ _ ->
                val audiences = MapManageServices.audiences.player(player)
                val availableSchematics = schematics.size

                audiences.sendActionBar(
                    LegacyComponentSerializer
                        .legacySection()
                        .deserialize(
                            "${CC.GREEN}Visiting: ${CC.WHITE}${visiting?.schematic?.nameWithoutExtension ?: "N/A"} ${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ${
                                "${CC.GREEN}Schematics: ${CC.WHITE}$availableSchematics"
                            }"
                        )
                )
            }, 0L, 10L)
            .bindWith(this)

        populateInventoryHotbar()
    }

    fun visit(schematic: File)
    {
        val previousInstance = visiting

        val newWorld = EditorGenerator.createNewEmptyWorld()
        WorldEditUtils.paste(
            newWorld, schematic, Vector(0, 62, 0)
        )

        val instance = EditorInstance(schematic, newWorld)
        instance.bindWith(this)

        player.allowFlight = true
        player.isFlying = true

        player.sendMessage("${CC.GREEN}Visiting ${CC.WHITE}${schematic.nameWithoutExtension}${CC.GREEN}...")
        player.playSound(player.location, Sound.NOTE_PLING, 1.0f, 1.0f)
        player.teleport(
            Location(
                newWorld, 0.0, 64.0, 0.0
            )
        )

        visiting = instance
        previousInstance?.closeAndReportException()
    }

    private fun populateInventoryHotbar()
    {
        val hotbarPreset = HotbarPreset()
        hotbarPreset.addSlot(0, DynamicHotbarPresetEntry().apply {
            onBuild = {
                ItemBuilder.of(Material.MELON)
                    .glow()
                    .name("${CC.RED}Visit Previous ${CC.GRAY}(Right Click)")
                    .build()
            }

            onClick = context@{
                if (visiting == null)
                {
                    player.sendMessage("${CC.RED}You are not visiting!")
                    return@context
                }

                val index = schematics.indexOf(visiting!!.schematic)
                if (index <= 0)
                {
                    player.sendMessage("${CC.RED}This is the first schematic!")
                    return@context
                }

                visit(schematics[index - 1])
            }
        })

        hotbarPreset.addSlot(8, DynamicHotbarPresetEntry().apply {
            onBuild = {
                ItemBuilder.of(Material.SPECKLED_MELON)
                    .glow()
                    .name("${CC.GREEN}Visit Next ${CC.GRAY}(Right Click)")
                    .build()
            }

            onClick = context@{
                if (visiting == null)
                {
                    player.sendMessage("${CC.RED}You are not visiting!")
                    return@context
                }

                val index = schematics.indexOf(visiting!!.schematic)
                if (index == schematics.size - 1)
                {
                    player.sendMessage("${CC.RED}This is the last schematic!")
                    return@context
                }

                visit(schematics[index + 1])
            }
        })

        hotbarPreset.addSlot(7, DynamicHotbarPresetEntry().apply {
            onBuild = {
                ItemBuilder.of(Material.NAME_TAG)
                    .glow()
                    .name("${CC.YELLOW}Rename Current ${CC.GRAY}(Right Click)")
                    .build()
            }

            onClick = context@{
                if (visiting == null)
                {
                    player.sendMessage("${CC.RED}You are not visiting!")
                    return@context
                }

                player.sendMessage("${CC.RED}Not yet implemented!")
            }
        })

        hotbarPreset.addSlot(4, DynamicHotbarPresetEntry().apply {
            onBuild = {
                ItemBuilder.of(Material.SLIME_BALL)
                    .glow()
                    .name("${CC.GOLD}Convert to Slime ${CC.GRAY}(Right Click)")
                    .build()
            }

            onClick = context@{
                if (visiting == null)
                {
                    player.sendMessage("${CC.RED}You are not visiting!")
                    return@context
                }

                InputPrompt()
                    .withText("${CC.B_GOLD}Enter a Slime world name!")
                    .acceptInput { _, newWorldName ->
                        player.sendMessage("${CC.GOLD}Preparing world...")
                        player.teleport(
                            Location(
                                Bukkit.getWorld("world"),
                                0.0, 100.0, 0.0
                            )
                        )

                        val worldName = visiting!!.world.name
                        Bukkit.unloadWorld(visiting!!.world, true)

                        player.sendMessage("${CC.GREEN}Importing into SWM -> MongoDB...")
                        MapManageServices.slimePlugin.importWorld(
                            File(Bukkit.getWorldContainer(), worldName),
                            newWorldName,
                            MapManageServices.loader
                        )

                        player.playSound(player.location, Sound.LEVEL_UP, 1.0f, 1.0f)
                        player.sendMessage("${CC.GREEN}Imported! You are no longer visiting.")

                        visiting?.closeAndReportException()
                    }
                    .start(player)
            }
        })

        hotbarPreset.applyToPlayer(player)
        HotbarPresetHandler.startTrackingHotbar(
            editorID,
            hotbarPreset
        )
    }

    override fun close()
    {
        HotbarPresetHandler.forget(editorID)
    }
}
