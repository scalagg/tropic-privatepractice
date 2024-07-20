package gg.tropic.practice.games.tasks.lifecycle

import gg.tropic.practice.games.GameImpl
import gg.tropic.practice.games.GameService
import gg.tropic.practice.games.enterSpectatorAfterLifeEnd
import gg.tropic.practice.games.prepareForNewLife
import gg.tropic.practice.kit.feature.FeatureFlag
import me.lucko.helper.Schedulers
import me.lucko.helper.scheduler.Task
import net.evilblock.cubed.util.CC
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.title.Title
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

/**
 * @author GrowlyX
 * @since 7/15/2024
 */
class PlayerRespawnTask(
    private val player: Player,
    private val game: GameImpl
) : Runnable, AutoCloseable
{
    companion object
    {
        fun GameImpl.startDelayedRespawn(player: Player) = with(PlayerRespawnTask(player, this)) {
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

    private var task by Delegates.notNull<Task>()
    private var tick = game
        .flagMetaData(FeatureFlag.TimeUserSpectatesAfterDeath, "value")
        ?.toIntOrNull() ?: 3

    init
    {
        player.allowFlight = true
        player.isFlying = true

        game.enterSpectatorAfterLifeEnd(player)
    }

    override fun run()
    {
        if (tick <= 0)
        {
            game.prepareForNewLife(player)

            GameService.audiences.player(player).clearTitle()

            player.sendMessage("${CC.GREEN}You have respawned!")
            player.playSound(player.location, Sound.NOTE_PLING, 1.0f, 1.0f)

            task.closeAndReportException()
            return
        }

        val respawnMessage = "${CC.SEC}Respawning in ${CC.PRI}$tick${CC.SEC}..."
        GameService.audiences.player(player).apply {
            showTitle(
                Title.title(
                    Component
                        .text("YOU DIED!")
                        .color(NamedTextColor.RED),
                    LegacyComponentSerializer
                        .legacySection()
                        .deserialize(respawnMessage),
                    Title.Times.times(
                        Duration.ofMillis(1L),
                        Duration.ofMillis(1500L),
                        Duration.ofMillis(1L)
                    )
                )
            )
        }

        player.sendMessage(respawnMessage)
        player.playSound(player.location, Sound.NOTE_STICKS, 1.0f, 1.0f)

        tick -= 1
    }

    override fun close()
    {

    }
}
