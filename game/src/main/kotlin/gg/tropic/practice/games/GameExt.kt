package gg.tropic.practice.games

import dev.iiahmed.disguise.Disguise
import gg.scala.basics.plugin.disguise.DisguiseService
import gg.tropic.practice.games.GameService.plugin
import gg.tropic.practice.kit.feature.FeatureFlag
import gg.tropic.practice.profile.PracticeProfile
import gg.tropic.practice.queue.QueueType
import gg.tropic.practice.resetAttributes
import gg.tropic.practice.statistics.KitStatistics
import net.evilblock.cubed.nametag.NametagHandler
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Constants
import net.evilblock.cubed.visibility.VisibilityHandler
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.scoreboard.DisplaySlot

/**
 * @author GrowlyX
 * @since 10/25/2023
 */
fun PracticeProfile.useKitStatistics(
    game: GameImpl, block: KitStatistics.() -> Unit
)
{
    when (game.expectationModel.queueType)
    {
        QueueType.Casual -> block(
            getCasualStatsFor(game.kit)
        )
        QueueType.Ranked -> block(
            getRankedStatsFor(game.kit)
        )
        null -> {}
    }
}

fun GameImpl.resetPlayerForFight(player: Player, noUpdateVisibility: Boolean = false)
{
    if (!noUpdateVisibility)
    {
        VisibilityHandler.update(player)
        NametagHandler.reloadPlayer(player)
    }

    player.apply {
        isFlying = false
        allowFlight = false
        resetAttributes()
    }

    if (flag(FeatureFlag.HeartsBelowNameTag))
    {
        val objective = player.scoreboard
            .getObjective("commonsHealth")
            ?: player.scoreboard
                .registerNewObjective(
                    "commonsHealth", "health"
                )

        objective.displaySlot = DisplaySlot.BELOW_NAME
        objective.displayName = "${CC.D_RED}${Constants.HEART_SYMBOL}"
    }

    if (flag(FeatureFlag.EntityDisguise))
    {
        val type = flagMetaData(FeatureFlag.EntityDisguise, "type")
            ?: "PLAYER"

        DisguiseService.provider().disguise(
            player,
            Disguise
                .builder()
                .setEntityType(
                    EntityType.valueOf(type)
                )
                .build()
        )
    }
}

fun GameImpl.enterSpectatorAfterLifeEnd(player: Player)
{
    player.apply {
        setMetadata(
            "spectator", FixedMetadataValue(plugin, true)
        )

        VisibilityHandler.update(player)
        NametagHandler.reloadPlayer(player)

        if (flag(FeatureFlag.EntityDisguise))
        {
            DisguiseService.provider().undisguise(player)
        }

        resetAttributes(editFlightAttributes = false)
        scoreboard
            .getObjective("commonsHealth")
            ?.apply {
                displaySlot = null
            }
    }
}

fun GameImpl.prepareForNewLife(player: Player, noUpdateVisibility: Boolean = false)
{
    if (!noUpdateVisibility)
    {
        player.removeMetadata("spectator", plugin)
    }

    teleportToSpawnLocation(player)
    resetPlayerForFight(player)
    applyPreSelectedLoadouts(player)
}

fun GameImpl.teleportToSpawnLocation(player: Player)
{
    val spawnLocation = map
        .findSpawnLocationMatchingTeam(
            getTeamOf(player).side
        )!!
        .toLocation(arenaWorld!!)

    if (flag(FeatureFlag.ClearIllegalBlocksOnRespawn))
    {
        val block = arenaWorld!!.getBlockAt(spawnLocation)
        block.type = Material.AIR

        block.getRelative(BlockFace.UP).apply {
            type = Material.AIR
        }
    }

    player.teleport(spawnLocation)
}

fun GameImpl.applyPreSelectedLoadouts(player: Player)
{
    loadout(player).apply(player)
}
