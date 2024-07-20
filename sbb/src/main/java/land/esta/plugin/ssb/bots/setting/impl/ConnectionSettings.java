package land.esta.plugin.ssb.bots.setting.impl;

import land.esta.plugin.ssb.bots.setting.Setting;
import land.esta.plugin.ssb.bots.setting.SettingObject;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;

@Getter
@Setter
public class ConnectionSettings extends Setting {

    private SettingObject ping = new SettingObject("Ping", Material.STRING,100,5);
    private SettingObject delayedBotLocation = new SettingObject("Delayed Bot Location",Material.WATCH,true);
    private SettingObject connectionPacketDelay = new SettingObject("Rel-Move Simulation Ticks",Material.SPIDER_EYE,3,1);
    public ConnectionSettings() {
        super(Material.DIODE,"&c&lConnection Settings","Connection", "connectionsettings",16,false,"&f In this category","&f you are able to","&f modify the bot's","&f connection settings.","","&aClick to select!");
        addSettings(ping,delayedBotLocation,connectionPacketDelay);
    }
}
