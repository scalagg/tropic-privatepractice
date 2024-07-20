package land.esta.plugin.ssb.bots.setting.impl;

import land.esta.plugin.ssb.bots.setting.Setting;
import land.esta.plugin.ssb.bots.setting.SettingObject;
import lombok.Getter;
import org.bukkit.Material;

@Getter
public class PearlSettings extends Setting {

    //TODO add pearl chance.
    private SettingObject pearlDelay = new SettingObject("Pearl Delay",Material.ENDER_PEARL,600,5);
    private SettingObject angleDifference = new SettingObject("Angle Difference",Material.NETHER_STAR,130,5);
    private SettingObject pearlThrowYawOffSet = new SettingObject("Pearl Throw YawOffSet",Material.NAME_TAG,10,1);
    private SettingObject pearlingTicks = new SettingObject("Pearling Ticks",Material.WATCH,10,1);

    public PearlSettings() {
        super(Material.ENDER_PEARL,"&c&lPearl Settings","Pearl", "pearlsettings",12,false,"&f In this category","&f you are able to","&f modify the bot's","&f pearl settings.","","&aClick to select!");
        addSettings(angleDifference,pearlThrowYawOffSet,pearlDelay,pearlingTicks);
    }
}
