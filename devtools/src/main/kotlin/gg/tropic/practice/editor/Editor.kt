package gg.tropic.practice.editor

import com.sk89q.worldedit.Vector
import gg.scala.lemon.hotbar.HotbarPreset
import gg.scala.lemon.hotbar.HotbarPresetHandler
import gg.scala.lemon.hotbar.entry.impl.DynamicHotbarPresetEntry
import gg.scala.lemon.hotbar.entry.impl.StaticHotbarPresetEntry
import gg.tropic.practice.map.Map
import gg.tropic.practice.map.MapManageServices
import gg.tropic.practice.map.MapService
import gg.tropic.practice.map.metadata.impl.MapSpawnMetadata
import gg.tropic.practice.map.utilities.MapMetadataScanUtilities
import gg.tropic.practice.utilities.WorldEditUtils
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import me.lucko.helper.terminable.composite.CompositeTerminable
import net.evilblock.cubed.menu.Button
import net.evilblock.cubed.menu.pagination.PaginatedMenu
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Constants
import net.evilblock.cubed.util.bukkit.ItemBuilder
import net.evilblock.cubed.util.bukkit.Tasks
import net.evilblock.cubed.util.bukkit.prompt.InputPrompt
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.*
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntitySpawnEvent
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
                if (it.player.uniqueId == player.uniqueId)
                {
                    closeAndReportException()
                }
            }
            .bindWith(this)

        instances[player.uniqueId] = this

        with {
            instances.remove(player.uniqueId)
        }

        Schedulers
            .sync()
            .runRepeating({ task ->
                if (isClosed)
                {
                    task.closeAndReportException()
                    return@runRepeating
                }

                val audiences = MapManageServices.audiences.player(player)
                val availableSchematics = schematics.size

                audiences.sendActionBar(
                    LegacyComponentSerializer
                        .legacySection()
                        .deserialize(
                            "${CC.BD_RED}Editor ${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ${
                                "${CC.D_RED}Visiting: ${CC.WHITE}${visiting?.schematic?.nameWithoutExtension ?: "N/A"}${
                                    if (visiting != null) "${CC.D_GRAY} (${schematics.indexOf(visiting?.schematic) + 1}/${schematics.size})" else ""
                                }"
                            } ${CC.GRAY}${Constants.THIN_VERTICAL_LINE} ${
                                "${CC.D_RED}Schematics: ${CC.WHITE}$availableSchematics"
                            }"
                        )
                )
            }, 0L, 10L)
            .bindWith(this@Editor)

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
                visit(schematics.getOrNull(index - 1) ?: schematics.last())
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
                visit(schematics.getOrNull(index + 1) ?: schematics.first())
            }
        })

        hotbarPreset.addSlot(7, DynamicHotbarPresetEntry().apply {
            onBuild = {
                ItemBuilder.of(Material.NAME_TAG)
                    .glow()
                    .name("${CC.YELLOW}Search ${CC.GRAY}(Right Click)")
                    .build()
            }

            onClick = context@{
                InputPrompt()
                    .withText("${CC.B_GOLD}Enter the schematic name!")
                    .acceptInput { _, schematic ->
                        val file = schematics.firstOrNull { it.nameWithoutExtension == schematic }
                            ?: return@acceptInput run {
                                player.sendMessage("${CC.RED}No schematic with that name!")
                            }

                        visit(file)
                    }
                    .start(player)
            }
        })

        hotbarPreset.addSlot(6, DynamicHotbarPresetEntry().apply {
            onBuild = {
                ItemBuilder.of(Material.PAINTING)
                    .glow()
                    .name("${CC.GOLD}All Schematics ${CC.GRAY}(Right Click)")
                    .build()
            }

            onClick = context@{
                object : PaginatedMenu()
                {
                    override fun getPrePaginatedTitle(player: Player) = "All schematics..."
                    override fun getAllPagesButtons(player: Player): kotlin.collections.Map<Int, Button>
                    {
                        val mappings = mutableMapOf<Int, Button>()
                        schematics.forEach {
                            mappings[mappings.size] = ItemBuilder
                                .of(Material.MAP)
                                .name("${CC.WHITE}${it.nameWithoutExtension}")
                                .addToLore(
                                    "${CC.GREEN}Click to visit!"
                                )
                                .toButton { _, _ ->
                                    visit(it)
                                }
                        }

                        return mappings
                    }
                }.openMenu(player)
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

        hotbarPreset.addSlot(2, StaticHotbarPresetEntry(
            ItemBuilder.of(Material.SIGN)
                .name("${CC.WHITE}Metadata Sign")
        ))

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
                        Tasks.delayed(10L) {
                            val metadata = MapMetadataScanUtilities.buildMetadataFor(bounds, visiting!!.world)

                            val availableSpawnMetadata = metadata.metadata.filterIsInstance<MapSpawnMetadata>()
                            if (availableSpawnMetadata.size >= 2)
                            {
                                player.sendMessage("${CC.B_GRAY}(!)${CC.GRAY} Created a metadata copy! We're now going to build the map data model & store the map in MongoDB...")
                                prepareSlimeWorld(newMapName)

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

        val oldSchematic = visiting?.schematic
        visiting?.closeAndReportException()
        visiting = null

        if (oldSchematic != null)
        {
            visit(oldSchematic)
        }
    }

    override fun close()
    {
        val audiences = MapManageServices.audiences.player(player)
        audiences.sendActionBar(Component.empty())

        player.inventory.clear()
        player.updateInventory()

        player.teleport(Location(
            Bukkit.getWorld("world"),
            0.0, 100.0, 0.0
        ))

        HotbarPresetHandler.forget(editorID)
    }
}
