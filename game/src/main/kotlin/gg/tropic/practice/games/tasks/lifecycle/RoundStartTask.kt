package gg.tropic.practice.games.tasks.lifecycle

import gg.tropic.practice.PracticeGame
import gg.tropic.practice.games.*
import gg.tropic.practice.games.GameService.plugin
import gg.tropic.practice.games.team.GameTeam
import gg.tropic.practice.kit.feature.FeatureFlag
import gg.tropic.practice.serializable.Message
import gg.tropic.practice.utilities.toRBTeamSide
import me.lucko.helper.Helper
import me.lucko.helper.Schedulers
import me.lucko.helper.scheduler.Task
import me.lucko.helper.terminable.composite.CompositeTerminable
import net.evilblock.cubed.nametag.NametagHandler
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.visibility.VisibilityHandler
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.title.Title
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.metadata.FixedMetadataValue
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

/**
 * @author GrowlyX
 * @since 7/17/2024
 */
class RoundStartTask(
    private val winner: GameTeam,
    private val game: GameImpl
) : Runnable, AutoCloseable
{
    companion object
    {
        fun GameImpl.startNewRound(winner: GameTeam) = with(RoundStartTask(winner, this)) {
            task = Schedulers.sync()
                .runRepeating(
                    this,
                    0L, TimeUnit.SECONDS,
                    1L, TimeUnit.SECONDS
                )

            game.with(this)
            task.bindWith(game)
        }
    }

    private var rbTeamSide = winner.side.toRBTeamSide()

    private var task by Delegates.notNull<Task>()
    private var tick = game
        .flagMetaData(FeatureFlag.CountDownTimeBeforeRoundStart, "value")
        ?.toIntOrNull() ?: 3

    init
    {
        game.multiRoundGame.switchingRounds = true

        game.multiRoundGame.terminable.closeAndReportException()
        game.multiRoundGame.terminable = CompositeTerminable.create()

        winner.gameLifecycleArbitraryObjectiveProgress += 1

        game.cleanupWorld()
        game.toBukkitPlayers().filterNotNull()
            .forEach {
                it.removeMetadata("spectator", plugin)
            }

        game.toBukkitPlayers().filterNotNull()
            .forEach {
                VisibilityHandler.update(it)
            }

        game.toBukkitPlayers().filterNotNull()
            .forEach { player ->
                // enter a "light" spectator mode, where they can't interact with anything yet
                game.prepareForNewLife(player, true)

                player.setMetadata(
                    "spectator",
                    FixedMetadataValue(plugin, "light")
                )

                player.setMetadata(
                    "light-spectator",
                    FixedMetadataValue(plugin, "true")
                )
            }

        val winnerDisplay = if (winner.players.size == 1)
            winner.toBukkitPlayers().firstOrNull()?.name else rbTeamSide.display

        val opponent = game.getOpponent(winner)
        val primary = "${rbTeamSide.format(winnerDisplay ?: "???")} ${CC.SEC}scored!"
        val subText = " ${CC.I}${rbTeamSide.format(winner.gameLifecycleArbitraryObjectiveProgress.toString())}${CC.R} ${CC.GRAY}- ${CC.I}${
            opponent!!.side.toRBTeamSide().format(opponent.gameLifecycleArbitraryObjectiveProgress.toString())
        }"

        game.sendMessage("", primary, "${CC.I}$subText", "")
        game.audiences {
            it.showTitle(
                Title.title(
                    LegacyComponentSerializer.legacySection()
                        .deserialize(primary),
                    LegacyComponentSerializer.legacySection()
                        .deserialize(subText),
                    Title.Times.times(
                        Duration.ofMillis(1L),
                        Duration.ofMillis(1500L),
                        Duration.ofMillis(500L)
                    )
                )
            )
        }
    }

    override fun run()
    {
        if (tick <= 0)
        {
            game.multiRoundGame.switchingRounds = false
            game.sendMessage("${CC.GREEN}Round started!")
            game.playSound(Sound.FIREWORK_BLAST, 1.0f)

            game.toBukkitPlayers().filterNotNull()
                .forEach {
                    it.removeMetadata("spectator", plugin)
                    it.removeMetadata("light-spectator", plugin)

                    VisibilityHandler.update(it)
                    NametagHandler.reloadPlayer(it)
                }

            game.audiences {
                it.clearTitle()
                it.sendActionBar { Component.empty() }
            }

            task.closeAndReportException()
            return
        }

        val respawnMessage = "${CC.SEC}Round starts in ${CC.PRI}$tick${CC.SEC}..."
        game.sendMessage(respawnMessage)
        game.audiences {
            it.sendActionBar(
                Component.text { text ->
                    text.append(
                        Component
                            .text("Starting in ")
                            .color(NamedTextColor.GREEN)
                    )

                    text.append(
                        Component
                            .text("$tick")
                            .color(NamedTextColor.WHITE)
                            .decorate(TextDecoration.BOLD)
                    )

                    text.append(
                        Component
                            .text("...")
                            .color(NamedTextColor.GREEN)
                    )
                }
            )
        }

        game.playSound(Sound.NOTE_STICKS, 1.0f)
        tick -= 1
    }

    override fun close()
    {

    }
}
