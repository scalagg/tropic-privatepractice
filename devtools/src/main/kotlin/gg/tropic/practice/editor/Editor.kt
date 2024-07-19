package gg.tropic.practice.editor

import com.sk89q.worldedit.Vector
import gg.scala.lemon.hotbar.HotbarPreset
import gg.scala.lemon.hotbar.HotbarPresetHandler
import gg.scala.lemon.hotbar.entry.impl.DynamicHotbarPresetEntry
import gg.tropic.practice.map.Map
import gg.tropic.practice.map.MapManageServices
import gg.tropic.practice.map.MapService
import gg.tropic.practice.map.metadata.impl.MapSpawnMetadata
import gg.tropic.practice.map.utilities.MapMetadataScanUtilities
import gg.tropic.practice.utilities.WorldEditUtils
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.terminable.composite.CompositeTerminable
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Constants
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.bukkit.prompt.InputPrompt
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import java.io.File
import java.util.UUID

/**
 * @author GrowlyX
 * @since 7/18/2024
 */
class Editor(private val player: Player) : CompositeTerminable by CompositeTerminable.create()
{
    companion object
    {
        val instances = mutableMapOf<UUID, Editor>()
    }

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

        instances[player.uniqueId] = this

        with {
            instances.remove(player.uniqueId)
        }

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
        visit(schematics.first())
    }

    fun visit(schematic: File)
    {
        val previousInstance = visiting

        val newWorld = EditorGenerator.createNewEmptyWorld()
        WorldEditUtils.paste(
            newWorld, schematic, Vector(0, 32, 0)
        )

        val instance = EditorInstance(schematic, newWorld)
        instance.bindWith(this)

        player.gameMode = GameMode.CREATIVE
        player.allowFlight = true
        player.isFlying = true
        player.teleport(
            Location(
                newWorld, 0.0, 64.0, 0.0, -65.0f, 0.0f
            )
        )

        player.sendMessage("${CC.GREEN}Visiting ${CC.WHITE}${schematic.nameWithoutExtension}${CC.GREEN}...")
        player.playSound(player.location, Sound.NOTE_PLING, 1.0f, 1.0f)

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
                    .withText("${CC.B_GOLD}Enter the Slime world name! ${CC.GRAY}(do not prefix with Map!)")
                    .acceptInput { _, newWorldName ->
                        prepareSlimeWorld(newWorldName)
                    }
                    .start(player)
            }
        })

        hotbarPreset.addSlot(1, DynamicHotbarPresetEntry().apply {
            onBuild = {
                ItemBuilder.of(Material.ENDER_PORTAL_FRAME)
                    .glow()
                    .name("${CC.U_AQUA}Develop Map ${CC.GRAY}(Right Click)")
                    .build()
            }

            onClick = context@{
                if (visiting == null)
                {
                    player.sendMessage("${CC.RED}You are not visiting!")
                    return@context
                }

                val bounds = visiting!!.world.loadedChunks.toList().toBounds()
                player.sendMessage("${CC.GREEN}Prepared bounds!")

                InputPrompt()
                    .withText("${CC.B_GOLD}Enter the map name! ${CC.GRAY}(do not prefix with Map!) (Capitalize!)")
                    .acceptInput { _, newMapName ->
                        prepareSlimeWorld(newMapName)

                        val metadata = MapMetadataScanUtilities.buildMetadataFor(bounds, visiting!!.world)
                        val availableSpawnMetadata = metadata.metadata.filterIsInstance<MapSpawnMetadata>()
                        if (availableSpawnMetadata.size >= 2)
                        {
                            player.sendMessage("${CC.B_GRAY}(!)${CC.GRAY} Created a metadata copy! We're now going to build the map data model...")

                            val map = Map(
                                name = newMapName.lowercase(),
                                bounds = bounds,
                                metadata = metadata,
                                displayName = newMapName.capitalize(),
                                associatedSlimeTemplate = "Map${newMapName.capitalize()}"
                            )

                            with(MapService.cached()) {
                                maps[map.name] = map
                                MapService.sync(this)

                                player.playSound(player.location, Sound.FIREWORK_LAUNCH, 1.0f, 1.0f)
                                player.sendMessage("${CC.B_GREEN}(!)${CC.GREEN} Successfully created map ${CC.YELLOW}${map.name}${CC.GREEN}!")
                            }
                        } else
                        {
                            player.sendMessage("${CC.B_RED}(!) Failed to create a metadata copy! We found no spawn locations in the map. Please try again.")
                        }
                    }
                    .start(player)
            }
        })

        hotbarPreset.addSlot(2, DynamicHotbarPresetEntry().apply {
            onBuild = {
                ItemBuilder.of(Material.EMPTY_MAP)
                    .glow()
                    .name("${CC.B_WHITE}(DEV) ${CC.PINK}Minimum ${CC.GRAY}(Right Click)")
                    .build()
            }

            onClick = context@{
                if (visiting == null)
                {
                    player.sendMessage("${CC.RED}You are not visiting!")
                    return@context
                }

                val bounds = visiting!!.world.loadedChunks.toList().toBounds()
                player.sendMessage("${CC.GREEN}Teleported!")
                player.teleport(
                    bounds.lowerLeft
                        .toLocation(visiting!!.world)
                        .add(0.0, 64.0, 0.0)
                )
            }
        })

        hotbarPreset.addSlot(3, DynamicHotbarPresetEntry().apply {
            onBuild = {
                ItemBuilder.of(Material.EMPTY_MAP)
                    .glow()
                    .name("${CC.B_WHITE}(DEV) ${CC.DARK_PURPLE}Maximum ${CC.GRAY}(Right Click)")
                    .build()
            }

            onClick = context@{
                if (visiting == null)
                {
                    player.sendMessage("${CC.RED}You are not visiting!")
                    return@context
                }

                val bounds = visiting!!.world.loadedChunks.toList().toBounds()
                player.sendMessage("${CC.GREEN}Teleported!")
                player.teleport(
                    bounds.upperRight
                        .toLocation(visiting!!.world)
                        .add(0.0, 64.0, 0.0)
                )
            }
        })

        hotbarPreset.applyToPlayer(player)
        HotbarPresetHandler.startTrackingHotbar(
            editorID,
            hotbarPreset
        )
    }

    private fun prepareSlimeWorld(newWorldName: String)
    {
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
            "Map${newWorldName.capitalize()}",
            MapManageServices.loader
        )

        player.playSound(player.location, Sound.LEVEL_UP, 1.0f, 1.0f)
        player.sendMessage("${CC.GREEN}Imported!")

        visiting?.closeAndReportException()
        if (visiting != null)
        {
            visit(visiting!!.schematic)
        }
    }

    override fun close()
    {
        player.inventory.clear()
        player.updateInventory()

        HotbarPresetHandler.forget(editorID)
    }
}
