package gg.tropic.practice.games

import gg.scala.aware.AwareBuilder
import gg.scala.aware.codec.codecs.interpretation.AwareMessageCodec
import gg.scala.aware.message.AwareMessage
import gg.scala.basics.plugin.profile.BasicsProfileService
import gg.scala.basics.plugin.settings.defaults.values.StateSettingValue
import gg.scala.basics.plugin.shutdown.ServerShutdownEvent
import gg.scala.commons.agnostic.sync.ServerSync
import gg.scala.flavor.inject.Inject
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.lemon.redirection.aggregate.ServerAggregateHandler
import gg.scala.lemon.util.QuickAccess
import gg.scala.lemon.util.QuickAccess.username
import gg.scala.staff.anticheat.AnticheatCheck
import gg.scala.staff.anticheat.AnticheatFeature
import gg.tropic.game.extensions.cosmetics.CosmeticLocalConfig
import gg.tropic.game.extensions.cosmetics.CosmeticRegistry
import gg.tropic.game.extensions.cosmetics.killeffects.KillEffectCosmeticCategory
import gg.tropic.game.extensions.cosmetics.killeffects.cosmetics.KillEffect
import gg.tropic.game.extensions.cosmetics.killeffects.cosmetics.LightningKillEffect
import gg.tropic.game.extensions.cosmetics.messagebundles.KillMessageBundleCosmeticCategory
import gg.tropic.game.extensions.cosmetics.messagebundles.cosmetics.MessageBundle
import gg.tropic.practice.PracticeGame
import gg.tropic.practice.games.features.FireballFeature
import gg.tropic.practice.games.tasks.lifecycle.PlayerRespawnTask.Companion.startDelayedRespawn
import gg.tropic.practice.games.tasks.lifecycle.RoundStartTask.Companion.startNewRound
import gg.tropic.practice.games.team.GameTeamSide
import gg.tropic.practice.kit.feature.FeatureFlag
import gg.tropic.practice.kit.feature.GameLifecycle
import gg.tropic.practice.profile.PracticeProfileService
import gg.tropic.practice.services.GameManagerService
import gg.tropic.practice.settings.DuelsSettingCategory
import gg.tropic.practice.suffixWhenDev
import gg.tropic.practice.utilities.toRBTeamSide
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap
import me.lucko.helper.Events
import me.lucko.helper.Schedulers
import net.evilblock.cubed.nametag.NametagHandler
import net.evilblock.cubed.util.CC
import net.evilblock.cubed.util.bukkit.Constants.HEART_SYMBOL
import net.evilblock.cubed.util.bukkit.EventUtils
import net.evilblock.cubed.util.bukkit.Tasks
import net.evilblock.cubed.visibility.VisibilityHandler
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.block.BlockFace
import org.bukkit.entity.*
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.*
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.player.*
import org.bukkit.inventory.ItemStack
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min


/**
 * @author GrowlyX
 * @since 8/4/2022
 */
@Service
object GameService
{
    @Inject
    lateinit var plugin: PracticeGame

    @Inject
    lateinit var audiences: BukkitAudiences

    @Inject
    lateinit var redirector: ServerAggregateHandler

    private val communicationLayer by lazy {
        AwareBuilder
            .of<AwareMessage>("practice:communications".suffixWhenDev())
            .codec(AwareMessageCodec)
            .logger(plugin.logger)
            .build()
    }

    val gameMappings = mutableMapOf<UUID, GameImpl>()

    val playerToGameMappings = Object2ReferenceOpenHashMap<UUID, GameImpl>(Bukkit.getMaxPlayers(), 0.75F)
    val spectatorToGameMappings = Object2ReferenceOpenHashMap<UUID, GameImpl>(Bukkit.getMaxPlayers(), 0.75F)

    private val ensureCauseDenied = listOf(
        DamageCause.BLOCK_EXPLOSION,
        DamageCause.FIRE,
        DamageCause.LAVA,
        DamageCause.STARVATION,
        DamageCause.LIGHTNING
    )

    @Configure
    fun configure()
    {
        CosmeticLocalConfig.enableCosmeticResources = false

        plugin.registerListener(FireballFeature())

        Events
            .subscribe(ServerShutdownEvent::class.java)
            .handler {
                gameMappings.values.forEach { game ->
                    game.complete(null, "Server rebooting")
                }
            }

        AnticheatFeature.configureAlertFilter {

            /**
             * (game.expectationModel.queueType == null ||
             *                 game.expectationModel.queueType == QueueType.Casual) &&
             *                 game.expectationModel.queueId != "tournament"
             */
            return@configureAlertFilter it.type == AnticheatCheck.DOUBLE_CLICK || it.type == AnticheatCheck.AUTO_CLICKER
        }

        communicationLayer.listen("terminate") {
            val matchID = retrieve<UUID>("matchID")
            val terminator = retrieveNullable<UUID>("terminator")
            val reason = retrieveNullable<String>("reason")
                ?: "Ended by an administrator"
            val game = gameMappings[matchID]
                ?: return@listen

            game.complete(null, reason = reason)

            QuickAccess.sendGlobalBroadcast(
                "${CC.L_PURPLE}[P] ${CC.D_PURPLE}[${
                    ServerSync.getLocalGameServer().id
                }] ${CC.L_PURPLE}Match ${CC.WHITE}${
                    matchID.toString().substring(0..5)
                }${CC.L_PURPLE} was terminated by ${CC.B_WHITE}${
                    terminator?.username() ?: "Console"
                }${CC.L_PURPLE} for ${CC.WHITE}${
                    reason
                }${CC.L_PURPLE}.",
                permission = "practice.admin"
            )
        }
        communicationLayer.connect()

        GameManagerService.bindToStatusService {
            GameStatus(
                games = gameMappings.values
                    .map {
                        val players = it.toBukkitPlayers()
                            .filterNotNull()

                        val majoritySpectatorsEnabled = players
                            .mapNotNull(BasicsProfileService::find)
                            .count { profile ->
                                profile.setting(
                                    "${DuelsSettingCategory.DUEL_SETTING_PREFIX}:allow-spectators",
                                    StateSettingValue.ENABLED
                                ) == StateSettingValue.ENABLED
                            }

                        GameReference(
                            uniqueId = it.identifier,
                            mapID = it.map.name,
                            state = it.state,
                            players = it.toPlayers(),
                            spectators = it.arenaWorld?.players
                                ?.filter { player ->
                                    player.hasMetadata("spectator")
                                }
                                ?.map(Player::getUniqueId)
                                ?: listOf(),
                            kitID = it.kit.id,
                            replicationID = it.arenaWorldName!!,
                            server = ServerSync.local.id,
                            queueId = it.expectationModel.queueId,
                            majorityAllowsSpectators = players.isEmpty() ||
                                (majoritySpectatorsEnabled / players.size) >= 0.50
                        )
                    }
            )
        }

        fun Player.sendHealthHUD(damaged: Player)
        {
            audiences.player(this).apply {
                sendActionBar {
                    Component.text { text ->
                        text.append(Component.text("${damaged.name} ")
                            .color(NamedTextColor.YELLOW))

                        val hearts = damaged.health / 2.0
                        val fullHearts = floor(hearts).toInt()

                        repeat(fullHearts) {
                            text.append(Component.text(HEART_SYMBOL)
                                .color(NamedTextColor.DARK_RED))
                        }

                        if (hearts.toInt().toDouble() != hearts)
                        {
                            text.append(Component.text(HEART_SYMBOL)
                                .color(NamedTextColor.RED))
                        } else
                        {
                            text.append(Component.text(HEART_SYMBOL)
                                .color(NamedTextColor.GRAY))
                        }

                        val usedHearts = floor((damaged.maxHealth - damaged.health) / 2.0)
                        repeat(usedHearts.toInt()) {
                            text.append(Component.text(HEART_SYMBOL)
                                .color(NamedTextColor.GRAY))
                        }
                    }
                }
            }
        }

        fun Player.killedBy(): Entity?
        {
            if (
                lastDamageCause != null &&
                !lastDamageCause.isCancelled &&
                lastDamageCause is EntityDamageByEntityEvent
            )
            {
                val damager = (lastDamageCause as EntityDamageByEntityEvent).damager

                if (damager is Projectile)
                {
                    val shooter = damager.shooter

                    if (shooter != null)
                    {
                        return shooter as Entity
                    }
                }

                return damager
            }

            return null
        }

        fun runFinalDeathEffectsFor(
            player: Player, target: Player, game: GameImpl
        )
        {
            val killerCosmetic = CosmeticRegistry
                .getAllEquipped(
                    KillEffectCosmeticCategory,
                    player
                )
                .randomOrNull()
                ?: LightningKillEffect

            val killEffectCosmetic = killerCosmetic as KillEffect
            killEffectCosmetic.applyTo(
                game.toBukkitPlayers().filterNotNull(),
                player, target
            )

            val configuration = killEffectCosmetic.serveConfiguration(player)
            if (configuration.flight != false)
            {
                player.allowFlight = true
                player.isFlying = true
            }

            target.allowFlight = true
            target.isFlying = true

            if (configuration.clearInventory != false)
            {
                game.takeSnapshot(player)

                player.inventory.clear()
                player.updateInventory()
            }
        }

        fun getMessageBundlePhrase(player: Player): String
        {
            val bundle = CosmeticRegistry
                .findRelatedTo(KillMessageBundleCosmeticCategory)
                .filter { like ->
                    like.equipped(player)
                }
                .filterIsInstance<MessageBundle>()
                .flatMap { bundle -> bundle.phrases }

            if (bundle.isEmpty())
            {
                return "slain"
            }

            return "${CC.GOLD}${bundle.random()}${CC.GRAY}"
        }

        data class GameRemovalEvent(
            val drops: MutableList<ItemStack>,
            val shouldRespawn: Boolean = true,
            val killerPlayerOverride: Player? = null,
            val alternativeDeath: Boolean = false,
            val alternativeDeathMethod: String = "",
        )

        fun Player.gracefullyRemoveFromGame(event: GameRemovalEvent)
        {
            val game = byPlayer(this)
                ?: return kotlin.run {
                    event.drops.clear()
                }

            game.takeSnapshot(this)

            health = maxHealth
            foodLevel = 20
            saturation = 20.0F

            scoreboard
                .getObjective("commonsHealth")
                ?.apply {
                    displaySlot = null
                }

            if (event.shouldRespawn)
            {
                spigot().respawn()
            }

            setMetadata(
                "spectator", FixedMetadataValue(plugin, true)
            )

            if (game.flag(FeatureFlag.DoNotDropInventoryOnDeath))
            {
                event.drops.clear()
            } else
            {
                event.drops.removeIf { stack ->
                    stack.type == Material.POTION || stack.type == Material.BOWL
                }
            }

            val team = game.getTeamOf(this)
            val noAlive = if (team.players.size > 1)
                team.nonSpectators().isEmpty() else true

            if (!noAlive)
            {
                game.toBukkitPlayers()
                    .filterNotNull()
                    .forEach { other ->
                        VisibilityHandler.update(other)
                        NametagHandler.reloadPlayer(this, other)
                    }
            }

            val killerPlayer = event.killerPlayerOverride ?: killedBy()
            val killer = if (killerPlayer !is Player || killerPlayer == this)
            {
                game.getOpponent(this)
            } else
            {
                game.getTeamOf(killerPlayer)
            }

            val killerName = if (game.flag(FeatureFlag.RedBlueTeams))
                killer?.side?.toRBTeamSide()?.format(killerPlayer?.name ?: "???") ?: "???"
            else "${CC.GREEN}${killerPlayer?.name ?: "???"}"

            val killedName = if (game.flag(FeatureFlag.RedBlueTeams))
                team.side.toRBTeamSide().format(name) ?: "???"
            else "${CC.RED}$name"

            val deathMessage = "${CC.RED}$killedName${CC.GRAY} ${
                if (event.alternativeDeath && killerPlayer == null) "fell into the void" else "was ${
                    if (killerPlayer == null || killerPlayer == this) "killed" else "${
                        getMessageBundlePhrase(killerPlayer as Player)
                    } by ${CC.GREEN}$killerName${CC.GRAY}"
                }"
            }!"

            if (killerPlayer is Player)
            {
                with(PracticeProfileService.find(killerPlayer)) {
                    if (this != null)
                    {
                        useKitStatistics(game) {
                            kills += 1
                        }

                        globalStatistics.userKilledOpponent().apply()
                        save()
                    }
                }

                audiences.player(killerPlayer).apply {
                    sendActionBar(Component.text {
                        it.append(
                            Component
                                .text("KILL! ")
                                .color(NamedTextColor.RED)
                                .decorate(TextDecoration.BOLD)
                        )

                        it.append(
                            Component
                                .text(player.name)
                                .color(NamedTextColor.YELLOW)
                        )
                    })
                }
            }

            with(PracticeProfileService.find(this)) {
                if (this != null)
                {
                    useKitStatistics(game) {
                        deaths += 1
                    }

                    globalStatistics.userWasKilled().apply()
                    save()
                }
            }

            when (game.lifecycle())
            {
                GameLifecycle.SoulBound ->
                {
                    if (killerPlayer is Player)
                    {
                        runFinalDeathEffectsFor(killerPlayer, this, game)
                    }

                    game.sendMessage(deathMessage)

                    if (noAlive)
                    {
                        game.complete(killer)
                    }
                }
                GameLifecycle.RoundBound ->
                {
                    if (
                        (game.flag(FeatureFlag.StartNewRoundOnDeath)) ||
                        (event.alternativeDeath && game.flag(FeatureFlag.StartNewRoundOnPortalEnter))
                    )
                    {
                        val requiredRounds = game
                            .flagMetaData(FeatureFlag.RoundsRequiredToCompleteGame, "value")
                            ?.toIntOrNull() ?: 2

                        game.sendMessage(deathMessage)
                        if (team.gameLifecycleArbitraryObjectiveProgress + 1 >= requiredRounds)
                        {
                            game.complete(team)
                            return
                        }

                        game.multiRoundGame.roundNumber += 1
                        Tasks.delayed(1L) {
                            game.startNewRound(team)
                        }
                        return
                    }

                    game.sendMessage(deathMessage)
                    if (game.flag(FeatureFlag.SpectateAfterDeath))
                    {
                        game.startDelayedRespawn(player)
                        return
                    }

                    game.prepareForNewLife(player)
                }
                GameLifecycle.ObjectiveBound ->
                {
                    game.sendMessage(deathMessage)

                    if (game.flag(FeatureFlag.SpectateAfterDeath))
                    {
                        game.startDelayedRespawn(player)
                        return
                    }

                    game.prepareForNewLife(player)
                }
                GameLifecycle.ObjectivePlusSoulBound ->
                {
                    val opposingTeam = game.getOpponent(team)!!
                    if (opposingTeam.gameLifecycleObjectiveMet())
                    {
                        if (killerPlayer is Player)
                        {
                            runFinalDeathEffectsFor(killerPlayer, this, game)
                        }

                        game.sendMessage("$deathMessage ${CC.B_AQUA}FINAL KILL!")

                        if (noAlive)
                        {
                            game.complete(killer)
                        }
                        return
                    }

                    game.sendMessage(deathMessage)
                    if (game.flag(FeatureFlag.SpectateAfterDeath))
                    {
                        game.startDelayedRespawn(player)
                        return
                    }

                    game.prepareForNewLife(player)
                }
            }
        }

        Events
            .subscribe(PlayerItemConsumeEvent::class.java)
            .handler {
                val game = byPlayer(it.player)
                    ?: return@handler

                if (!game.state(GameState.Playing))
                {
                    return@handler
                }

                if (
                    !it.player.isDead &&
                    it.player.itemInHand.type == Material.MUSHROOM_SOUP &&
                    it.player.health < 19.0
                )
                {
                    val newHealth = min(it.player.health + 7.0, 20.0)

                    it.player.health = newHealth
                    it.player.itemInHand.type = Material.BOWL
                    it.player.updateInventory()
                }
            }

        fun overridePotionEffect(
            player: Player, effect: PotionEffect
        )
        {
            if (player.hasPotionEffect(effect.type))
            {
                player.removePotionEffect(effect.type)
            }

            player.addPotionEffect(effect)
        }

        Events
            .subscribe(PlayerItemConsumeEvent::class.java)
            .filter {
                it.item.type == Material.POTION
            }
            .handler {
                Tasks.sync {
                    if (it.player.itemInHand.type != Material.GLASS_BOTTLE)
                    {
                        return@sync
                    }

                    it.player.itemInHand = ItemStack(Material.AIR)
                    it.player.updateInventory()
                }
            }
            .bindWith(plugin)

        Events.subscribe(ProjectileLaunchEvent::class.java)
            .handler {
                val fishingHook = it.entity

                if (it.entityType == EntityType.FISHING_HOOK)
                {
                    fishingHook.velocity = fishingHook
                        .velocity.multiply(1.17)
                }
            }
            .bindWith(plugin)

        Events.subscribe(CraftItemEvent::class.java)
            .handler {
                it.isCancelled = true
            }
            .bindWith(plugin)

        Events.subscribe(PlayerItemConsumeEvent::class.java)
            .filter {
                it.item.hasItemMeta() && it.item.itemMeta.displayName.contains("Golden Head")
            }
            .handler {
                it.isCancelled = true
                it.player.playSound(
                    it.player.location, Sound.EAT, 10f, 1f
                )

                it.player.removePotionEffect(PotionEffectType.ABSORPTION)
                it.player.addPotionEffect(
                    PotionEffect(
                        PotionEffectType.ABSORPTION,
                        120 * 20, 0, false, true
                    )
                )

                it.player.removePotionEffect(PotionEffectType.REGENERATION)
                it.player.addPotionEffect(
                    PotionEffect(
                        PotionEffectType.REGENERATION,
                        5 * 20, 2, false, true
                    )
                )

                it.player.removePotionEffect(PotionEffectType.SPEED)
                it.player.addPotionEffect(
                    PotionEffect(
                        PotionEffectType.SPEED,
                        10 * 20, 0, false, true
                    )
                )

                if (it.item.amount == 1)
                {
                    it.player.itemInHand = ItemStack(Material.AIR)
                } else
                {
                    it.item.amount -= 1
                }
            }
            .bindWith(plugin)

        Events.subscribe(PlayerItemConsumeEvent::class.java)
            .filter { it.item.type == Material.GOLDEN_APPLE }
            .handler {
                overridePotionEffect(
                    it.player, PotionEffect(PotionEffectType.REGENERATION, 5 * 20, 2)
                )

                overridePotionEffect(
                    it.player, PotionEffect(PotionEffectType.ABSORPTION, 120 * 20, 0)
                )
            }
            .bindWith(plugin)

        Events
            .subscribe(PlayerDropItemEvent::class.java)
            .handler {
                val game = byPlayer(it.player)
                    ?: return@handler kotlin.run {
                        it.isCancelled = true
                    }

                if (!game.ensurePlaying())
                {
                    it.isCancelled = true
                    return@handler
                }

                if (it.itemDrop.itemStack.type.name.contains("SWORD"))
                {
                    val amountOfSwordsInInventory = it.player.inventory
                        .count { stack -> stack != null && stack.type.name.contains("SWORD") }

                    if (amountOfSwordsInInventory <= 1)
                    {
                        it.isCancelled = true
                        it.player.sendMessage("${CC.RED}You cannot drop your sword!")
                    }
                }

                if (it.itemDrop.itemStack.type == Material.GLASS_BOTTLE)
                {
                    Schedulers.sync().runLater(it.itemDrop::remove, 1L)
                }
            }
            .bindWith(plugin)

        Events.subscribe(PlayerPickupItemEvent::class.java)
            .handler {
                val game = byPlayer(it.player)
                    ?: return@handler kotlin.run {
                        it.isCancelled = true
                    }

                if (
                    !game.ensurePlaying() ||
                    it.player.hasMetadata("spectator")
                )
                {
                    it.isCancelled = true
                }
            }

        Events
            .subscribe(PlayerMoveEvent::class.java)
            .filter(EventUtils::hasPlayerMoved)
            .handler {
                val game = byPlayer(it.player)
                    ?: return@handler

                if (it.player.hasMetadata("light-spectator"))
                {
                    it.player.teleport(it.from)
                    return@handler
                }

                if (it.player.hasMetadata("spectator"))
                {
                    return@handler
                }

                if (game.ensurePlaying())
                {
                    if (
                        game.flag(FeatureFlag.DeathOnLiquidInteraction) &&
                        it.to.block.isLiquid
                    )
                    {
                        it.player.gracefullyRemoveFromGame(
                            event = GameRemovalEvent(
                                drops = mutableListOf(),
                                shouldRespawn = false
                            )
                        )
                        return@handler
                    }

                    val yAxis = game
                        .flagMetaData(
                            FeatureFlag.DeathBelowYAxis, "level"
                        )
                        ?.toIntOrNull()

                    if (yAxis != null && it.to.y < yAxis)
                    {
                        it.player.gracefullyRemoveFromGame(
                            event = GameRemovalEvent(
                                drops = mutableListOf(),
                                shouldRespawn = false
                            )
                        )
                        return@handler
                    }

                    return@handler
                }

                if (
                    game.state(GameState.Starting) &&
                    (game.flag(FeatureFlag.FrozenOnGameStart))
                )
                {
                    it.player.teleport(it.from)
                }
            }
            .bindWith(plugin)

        Events.subscribe(FoodLevelChangeEvent::class.java)
            .handler {
                val game = byPlayer(it.entity as Player)
                    ?: return@handler

                if (!game.ensurePlaying())
                {
                    it.isCancelled = true
                    return@handler
                }

                if (game.flag(FeatureFlag.DoNotTakeHunger))
                {
                    it.isCancelled = true
                }
            }
            .bindWith(plugin)

        Events.subscribe(PlayerDeathEvent::class.java)
            .handler {
                it.deathMessage = null
                it.entity.gracefullyRemoveFromGame(
                    event = GameRemovalEvent(
                        drops = it.drops,
                        shouldRespawn = true
                    )
                )
            }
            .bindWith(plugin)

        Events
            .subscribe(PotionSplashEvent::class.java)
            .handler {
                it.affectedEntities.removeIf { entity ->
                    entity.hasMetadata("spectator")
                }

                val shooter = it.entity.shooter
                if (shooter is Player)
                {
                    val game = byPlayer(shooter)
                        ?: return@handler run {
                            it.isCancelled = true
                        }

                    val counter = game.counter(shooter)
                    val intensity = it.getIntensity(shooter)
                    val effect = it.potion.effects
                        .firstOrNull { effect -> effect.type == PotionEffectType.HEAL }
                        ?: return@handler

                    counter.increment("totalPots")
                    counter.increment(if (intensity <= 0.5) "missedPots" else "hitPots")

                    val amountHealed = (intensity * (4 shl effect.amplifier) + 0.5)
                    if (shooter.health + amountHealed > shooter.maxHealth)
                    {
                        counter.increment(
                            "wastedHeals",
                            (shooter.health + amountHealed) - shooter.maxHealth
                        )
                    }
                }
            }

        Events
            .subscribe(EntityRegainHealthEvent::class.java)
            .filter { it.entity is Player }
            .handler {
                val game = byPlayer(it.entity as Player)
                    ?: return@handler

                if (it.regainReason == EntityRegainHealthEvent.RegainReason.REGEN)
                {
                    game.counter(it.entity as Player)
                        .apply {
                            increment("healthRegained", it.amount)
                        }
                }
            }

        Events
            .subscribe(PlayerQuitEvent::class.java)
            .handler {
                val spectator = bySpectator(it.player.uniqueId)
                    ?: return@handler

                spectator.expectedSpectators -= it.player.uniqueId
            }

        Schedulers
            .async()
            .runRepeating({ _ ->
                gameMappings.values.forEach {
                    if (it.state != GameState.Playing)
                    {
                        return@runRepeating
                    }

                    if (it.toBukkitPlayers().all { player -> player == null })
                    {
                        Tasks.sync {
                            kotlin.runCatching {
                                it.complete(null, reason = "Zero players alive in match")
                            }.onFailure(Throwable::printStackTrace)
                        }
                    }
                }
            }, 0L, 20L)

        Events
            .subscribe(PlayerQuitEvent::class.java)
            .handler {
                val game = byPlayer(it.player)
                    ?: return@handler

                if (game.ensurePlaying())
                {
                    val team = game.getTeamOf(it.player)
                    val noAlive = if (team.players.size > 1)
                        team.nonSpectators().isEmpty() else true

                    game.takeSnapshot(it.player)
                    game.sendMessage(
                        "${CC.RED}${it.player.name}${CC.GRAY} left the game."
                    )

                    if (noAlive)
                    {
                        game.complete(
                            game.getOpponent(it.player)
                        )
                    }
                } else
                {
                    if (
                        game.state(GameState.Starting) ||
                        game.state(GameState.Waiting)
                    )
                    {
                        game.state = GameState.Completed
                        game.closeAndCleanup()
                    }
                }
            }
            .bindWith(plugin)

        Events
            .subscribe(EntityDamageEvent::class.java)
            .filter { it.entity is Player }
            .handler {
                if (it.entity.hasMetadata("spectator"))
                {
                    it.isCancelled = true
                    return@handler
                }

                if (ensureCauseDenied.contains(it.cause))
                {
                    it.isCancelled = true
                    return@handler
                }

                val game = byPlayer(
                    it.entity as Player
                )
                    ?: return@handler

                if (!game.ensurePlaying())
                {
                    it.isCancelled = true
                    return@handler
                }
            }
            .bindWith(plugin)

        Events.subscribe(EntityDamageByEntityEvent::class.java)
            .filter { it.damager is FishHook && ((it.damager as FishHook).shooter) is Player && it.entity is Player }
            .handler { event ->
                val game = byPlayer(
                    event.entity as Player
                )
                    ?: return@handler

                val damagerGame = byPlayer(
                    ((event.damager as FishHook).shooter) as Player
                )
                    ?: return@handler

                if (damagerGame.expectation == game.expectation)
                {
                    val damagerTeam = game
                        .getTeamOf(((event.damager as FishHook).shooter) as Player)

                    val team = game
                        .getTeamOf(event.entity as Player)

                    if (damagerTeam.side == team.side)
                    {
                        event.isCancelled = true
                        return@handler
                    }
                }
            }
            .bindWith(plugin)

        Events.subscribe(EntityDamageByEntityEvent::class.java)
            .filter { it.damager is Arrow && ((it.damager as Arrow).shooter) is Player && it.entity is Player }
            .handler { event ->
                val game = byPlayer(
                    event.entity as Player
                )
                    ?: return@handler

                val damagerGame = byPlayer(
                    ((event.damager as Arrow).shooter) as Player
                )
                    ?: return@handler

                if (damagerGame.expectation == game.expectation)
                {
                    val damagerTeam = game
                        .getTeamOf(((event.damager as Arrow).shooter) as Player)

                    val team = game
                        .getTeamOf(event.entity as Player)

                    if (damagerTeam.side == team.side)
                    {
                        event.isCancelled = true
                        return@handler
                    }
                }

                val entity = event.entity as Player
                val arrow = event.damager as Arrow

                if (arrow.shooter is Player)
                {
                    val shooter = arrow.shooter as Player

                    val minYOffsetForHeadShot = if (entity.isSneaking) 1.25 else 1.5

                    if (arrow.location.y >= entity.location.y + minYOffsetForHeadShot)
                    {
                        game.counter(shooter).increment("headshots")

                        val multiplier = game.kit
                            .featureConfig(FeatureFlag.HeadshotMultiplier, "multiplier")
                            .toDouble()

                        if (multiplier != 1.0)
                        {
                            event.damage *= multiplier
                            shooter.sendMessage("${CC.B_RED}HEADSHOT! ${CC.GREEN}You've hit ${CC.YELLOW}${entity.displayName}${CC.GREEN} with ${CC.BOLD}${multiplier}x${CC.GREEN} the damage.")
                        }
                    }

                    if (entity.name != shooter.name && event.damage != 0.0)
                    {
                        val health = ceil(entity.health - event.finalDamage) / 2.0

                        if (health > 0.0)
                        {
                            shooter.sendHealthHUD(entity)
                            shooter.sendMessage("${entity.displayName}${CC.GREEN} is now at ${CC.RED}$health${HEART_SYMBOL}${CC.GREEN}!")
                        }
                    }
                }
            }
            .bindWith(plugin)

        Events
            .subscribe(PlayerBlockedHitEvent::class.java)
            .handler {
                val game = byPlayer(it.player)
                    ?: return@handler

                game.counter(it.player)
                    .apply {
                        val blockedHitLimit = game.expectationModel.configuration?.blockHitLimit ?: 10
                        if (valueOf("blockedHits") >= blockedHitLimit)
                        {
                            it.isCancelled = true
                            return@handler
                        }

                        increment("blockedHits")
                    }
            }
            .bindWith(plugin)

        Events
            .subscribe(PlayerCriticalHitEvent::class.java)
            .handler {
                val game = byPlayer(it.player)
                    ?: return@handler

                game.counter(it.player)
                    .apply {
                        val criticalHitLimit = game.expectationModel.configuration?.criticalHitLimit
                        if (criticalHitLimit != null)
                        {
                            if (valueOf("criticalHits") >= criticalHitLimit)
                            {
                                it.isCancelled = true
                                return@handler
                            }
                        }

                        increment("criticalHits")
                    }
            }
            .bindWith(plugin)

        Events
            .subscribe(PlayerRunAndEatAttemptEvent::class.java)
            .handler {
                val game = byPlayer(it.player)
                    ?: return@handler

                it.isCancelled = !(game.expectationModel.configuration?.runningAndEating ?: false)
            }
            .bindWith(plugin)

        Events.subscribe(EntityDamageByEntityEvent::class.java)
            .filter {
                it.entity is Player
            }
            .handler {
                val game = byPlayerOrSpectator(it.entity.uniqueId)
                    ?: return@handler run {
                        it.isCancelled = true
                    }

                if (!game.ensurePlaying())
                {
                    it.isCancelled = true
                    return@handler
                }

                if (
                    it.damager.hasMetadata("spectator") ||
                    it.entity.hasMetadata("spectator")
                )
                {
                    it.isCancelled = true
                    return@handler
                }

                if (it.damager !is Player)
                {
                    if (!game.ensurePlaying())
                    {
                        it.isCancelled = true
                    }

                    return@handler
                }

                val damagerGame = byPlayer(
                    it.damager as Player
                )
                    ?: return@handler

                val damagerTeam = game.getTeamOf(it.damager as Player)
                val team = game.getTeamOf(it.entity as Player)

                if (damagerGame.expectation == game.expectation)
                {
                    if (damagerTeam.side == team.side)
                    {
                        it.isCancelled = true
                        it.damager.sendMessage(
                            "${CC.RED}You cannot damage your teammates!"
                        )
                        return@handler
                    }
                }

                val doNotTakeDamage = game
                    .flagMetaData(
                        FeatureFlag.DoNotTakeDamage, "doDamageTick"
                    )
                    ?.toBooleanStrictOrNull()

                if (game.flag(FeatureFlag.WinWhenNHitsReached))
                {
                    damagerTeam.gameLifecycleArbitraryObjectiveProgress += 1
                }

                damagerTeam.playerCombos.compute(
                    it.damager!!.uniqueId
                ) { _, previous ->
                    val computed = (previous ?: 0) + 1
                    val highestCombo = damagerTeam
                        .highestPlayerCombos[it.damager.uniqueId]

                    if (highestCombo == null || highestCombo < computed)
                    {
                        damagerTeam.highestPlayerCombos[it.damager.uniqueId] = computed
                    }

                    computed
                }

                if (game.flag(FeatureFlag.HeartsBelowNameTag))
                {
                    (it.damager as Player).sendHealthHUD(it.entity as Player)
                }

                game
                    .counter(it.entity as Player)
                    .apply {
                        reset("combo")
                    }

                game
                    .counter(it.damager as Player)
                    .apply {
                        increment("totalHits")
                        increment("combo")

                        if (valueOf("combo") > valueOf("highestCombo"))
                        {
                            update("highestCombo", valueOf("combo"))
                        }
                    }

                team.playerCombos.remove(it.entity.uniqueId)

                val winWhenHitsReached = game
                    .flagMetaData(
                        FeatureFlag.WinWhenNHitsReached, "hits"
                    )
                    ?.toIntOrNull()

                if (
                    winWhenHitsReached != null &&
                    damagerTeam.gameLifecycleArbitraryObjectiveProgress >= winWhenHitsReached
                )
                {
                    (it.entity as Player).gracefullyRemoveFromGame(GameRemovalEvent(
                        killerPlayerOverride = it.damager as Player,
                        drops = mutableListOf(),
                        shouldRespawn = false
                    ))
                }

                if (doNotTakeDamage != null)
                {
                    if (doNotTakeDamage)
                    {
                        it.damage = 0.0
                        return@handler
                    }

                    it.isCancelled = true
                }
            }
            .bindWith(plugin)

        Events.subscribe(BlockExplodeEvent::class.java)
            .handler {
                val game = byWorld(it.block.world)
                    ?: return@handler run {
                        it.isCancelled = true
                    }

                if (game.flag(FeatureFlag.ExplodePlacedBlocks))
                {
                    return@handler
                }

                it.blockList()
                    .removeIf { block ->
                        !block.hasMetadata("placed")
                    }
            }
            .bindWith(plugin)

        Events.subscribe(BlockBreakEvent::class.java)
            .handler {
                val game = byPlayer(it.player)
                    ?: return@handler run {
                        it.isCancelled = true
                    }

                if (it.player.hasMetadata("spectator"))
                {
                    it.isCancelled = true
                    return@handler
                }

                if (!game.ensurePlaying())
                {
                    it.isCancelled = true
                    return@handler
                }

                if (game.flag(FeatureFlag.BreakAllBlocks))
                {
                    return@handler
                }

                if (
                    game.flag(FeatureFlag.BreakPlacedBlocks) &&
                    it.block.hasMetadata("placed")
                )
                {
                    return@handler
                }

                if (game.kit.allowedBlockTypeMappings.isPresent)
                {
                    if (
                        game.kit.allowedBlockTypeMappings.get()
                            .any { (type, data) ->
                                it.block.type == type && it.block.data == data.toByte()
                            }
                    )
                    {
                        return@handler
                    }
                }

                it.isCancelled = true
            }
            .bindWith(plugin)

        val validBlockPlace = listOf(
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST
        )

        Events.subscribe(BlockPlaceEvent::class.java)
            .handler {
                val game = byPlayer(it.player)
                    ?: return@handler run {
                        it.isCancelled = true
                    }

                if (it.player.hasMetadata("spectator"))
                {
                    it.isCancelled = true
                    return@handler
                }

                if (!game.ensurePlaying())
                {
                    it.isCancelled = true
                    return@handler
                }

                if (!game.flag(FeatureFlag.PlaceBlocks))
                {
                    it.isCancelled = true
                    return@handler
                }

                val buildLimit = game
                    .flagMetaData(
                        FeatureFlag.BuildLimit,
                        key = "blocks"
                    )
                    ?.toIntOrNull()

                if (buildLimit != null)
                {
                    val maxBuildLimit = (game.map
                        .findSpawnLocationMatchingTeam(GameTeamSide.A)?.y
                        ?: 0.0) + buildLimit

                    if (it.block.y > maxBuildLimit)
                    {
                        it.isCancelled = true
                        it.player.sendMessage(
                            "${CC.RED}You cannot build in this area!"
                        )
                        return@handler
                    }
                }

                val zone = game.map.findZoneContainingEntity(it.player)

                if (zone != null && zone.id.startsWith("restrict"))
                {
                    it.isCancelled = true
                    it.player.sendMessage(
                        "${CC.RED}You cannot build in this area!"
                    )
                    return@handler
                }

                val levelRestrictions = game.map.findMapLevelRestrictions()

                if (levelRestrictions != null)
                {
                    if (it.block.y !in levelRestrictions.range)
                    {
                        if (
                            !levelRestrictions.allowBuildOnBlockSideBlockFaces ||
                            validBlockPlace
                                .any { face ->
                                    it.blockPlaced.getRelative(face)
                                        .hasMetadata("placed")
                                }
                        )
                        {
                            it.isCancelled = true
                            it.player.sendMessage(
                                "${CC.RED}You cannot build in this area!"
                            )
                        }
                        return@handler
                    }
                }

                it.blockPlaced.setMetadata(
                    "placed",
                    FixedMetadataValue(plugin, true)
                )

                if (game.flag(FeatureFlag.RemovePlacedBlocksOnRoundStart))
                {
                    game.placedBlocks += it.blockPlaced.location.toVector()
                }

                val blockExpiration = game
                    .flagMetaData(
                        FeatureFlag.ExpirePlacedBlocksAfterNSeconds,
                        "time"
                    )
                    ?.toIntOrNull()

                if (blockExpiration != null)
                {
                    Schedulers
                        .sync()
                        .runLater({
                            if (it.blockPlaced.hasMetadata("placed"))
                            {
                                it.blockPlaced.type = Material.AIR
                            }
                        }, blockExpiration * 20L)
                }
            }
            .bindWith(plugin)
    }

    fun byPlayer(player: Player) =
        playerToGameMappings[player.uniqueId]

    fun byPlayer(player: UUID) =
        playerToGameMappings[player]

    fun byWorld(world: World) = gameMappings.values.firstOrNull { world.name == it.arenaWorldName }

    fun bySpectator(player: UUID) =
        spectatorToGameMappings[player]

    fun byPlayerOrSpectator(player: UUID) =
        playerToGameMappings[player]
            ?: spectatorToGameMappings[player]

    fun iterativeByPlayerOrSpectator(player: UUID) = gameMappings.values
        .find {
            player in it.toPlayers() || player in it.expectedSpectators
        }
    private inline fun <reified T : PlayerEvent> configureMetadataProvidingEventHandler(incrementing: String)
    {
        Events
            .subscribe(T::class.java)
            .handler {
                val game = byPlayer(it.player)
                    ?: return@handler

                game.counter(it.player)
                    .apply {
                        increment(incrementing)
                    }
            }
    }
}
