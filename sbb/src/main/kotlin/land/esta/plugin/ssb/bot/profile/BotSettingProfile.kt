package land.esta.plugin.ssb.bot.profile

import gg.scala.store.controller.DataStoreObjectControllerCache
import gg.scala.store.storage.storable.IDataStoreObject
import land.esta.plugin.ssb.bot.setting.BotSetting
import land.esta.plugin.ssb.bots.setting.Setting
import java.util.*


data class BotSettingProfile(
    override val identifier: UUID
) : IDataStoreObject
{
    var skin: String? = null
    var name: String? = null
    val settings: MutableMap<String, BotSetting> = mutableMapOf()

    init
    {
        // Give them the default botsetting and use their identifier as the setting id
        settings["default"] = BotSetting(identifier.toString())
    }

    fun update(toUpdate: MutableList<Setting>)
    {
        val botSetting = settings["default"]!!

        toUpdate.forEach { setting ->
            setting.settings.forEach { ob ->
                botSetting.settings.putIfAbsent(setting.name, mutableMapOf())
                botSetting.settings[setting.name]!![ob.name] = ob
            }
        }

        save()
    }

    fun save() = DataStoreObjectControllerCache
        .findNotNull<BotSettingProfile>()
        .save(this)

}