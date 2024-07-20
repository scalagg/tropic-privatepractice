package land.esta.plugin.ssb.bot.setting

import land.esta.plugin.ssb.bots.setting.SettingManager
import land.esta.plugin.ssb.bots.setting.SettingObject

data class BotSetting(
    val identifier: String
)
{
    var skin: String = "Xecivity"
    var name: String = "Bot"
    var elo: Int = 1000
    var randomizer: Boolean = false
    var tier: Int = 1
    val settings: MutableMap<String, MutableMap<String, SettingObject>> = mutableMapOf()

    init
    {
        val defaults = SettingManager()

        defaults.settings.forEach { setting ->
            setting.settings.forEach { so ->
                settings.putIfAbsent(setting.name, mutableMapOf())

                settings[setting.name]!![so.name] = so
            }
        }
    }

    fun save()
    {
        with(BotSettingService.cached()) {
            this.map[identifier] = this@BotSetting
            BotSettingService.sync(this)
        }
    }

    fun resetWithDefaults()
    {
        val defaults = SettingManager()

        skin = defaults.skin
        name = defaults.botName

        settings.clear()

        defaults.settings.forEach { setting ->
            setting.settings.forEach { obj ->
                settings.putIfAbsent(setting.name, mutableMapOf())

                settings[setting.name]!![obj.name] = obj
            }
        }
    }
}