package land.esta.plugin.ssb.bot.setting

import gg.scala.commons.persist.datasync.DataSyncKeys
import gg.scala.commons.persist.datasync.DataSyncService
import gg.scala.commons.persist.datasync.DataSyncSource
import gg.scala.flavor.service.Service
import gg.tropic.practice.namespace
import gg.tropic.practice.suffixWhenDev
import net.kyori.adventure.key.Key

@Service
object BotSettingService: DataSyncService<BotSettingContainer>()
{

    fun getOrCreate(identifier: String): BotSetting = cached().map[identifier] ?: BotSetting(identifier)

    object BotSettingKeys : DataSyncKeys
    {
        override fun newStore() = "mi-practice-bot-settings"

        override fun store() = Key.key(namespace(), "bot-settings")
        override fun sync() = Key.key(namespace().suffixWhenDev(), "bsync")
    }

    override fun locatedIn() = DataSyncSource.Mongo

    override fun keys() = BotSettingKeys
    override fun type() = BotSettingContainer::class.java
}