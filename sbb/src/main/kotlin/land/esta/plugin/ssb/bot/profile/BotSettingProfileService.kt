package land.esta.plugin.ssb.bot.profile

import gg.scala.aware.AwareBuilder
import gg.scala.aware.codec.codecs.interpretation.AwareMessageCodec
import gg.scala.aware.message.AwareMessage
import gg.scala.aware.thread.AwareThreadContext
import gg.scala.commons.persist.ProfileOrchestrator
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.store.controller.DataStoreObjectControllerCache
import gg.scala.store.storage.type.DataStoreStorageType
import gg.tropic.practice.suffixWhenDev
import org.bukkit.Bukkit
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.logging.Logger

@Service
object BotSettingProfileService : ProfileOrchestrator<BotSettingProfile>()
{
    override fun new(uniqueId: UUID) = BotSettingProfile(uniqueId)
    override fun type() = BotSettingProfile::class

    @Configure
    fun configure()
    {
        aware.listen("propagate") {
            val uniqueId = retrieve<UUID>("player")
            Bukkit.getPlayer(uniqueId) ?: return@listen

            with(
                DataStoreObjectControllerCache
                    .findNotNull<BotSettingProfile>()
            ) {
                load(
                    uniqueId,
                    DataStoreStorageType.MONGO
                ).thenAcceptAsync {
                    if (it != null)
                    {
                        localCache().computeIfPresent(uniqueId) { _, _ -> it }
                    }
                }
            }
        }
        aware.connect().toCompletableFuture().join()
    }

    fun findOrCreate(uuid: UUID): BotSettingProfile = find(uuid) ?: new(uuid)

    private val aware by lazy {
        AwareBuilder
            .of<AwareMessage>("practice:bot-setting-profile".suffixWhenDev())
            .codec(AwareMessageCodec)
            .logger(Logger.getAnonymousLogger())
            .build()
    }

    fun sendMessage(packet: String, vararg pairs: Pair<String, Any?>) =
        CompletableFuture.runAsync {
            AwareMessage.of(packet, aware, *pairs)
                .publish(AwareThreadContext.SYNC)
        }

    override fun postLoad(uniqueId: UUID) {}
}
