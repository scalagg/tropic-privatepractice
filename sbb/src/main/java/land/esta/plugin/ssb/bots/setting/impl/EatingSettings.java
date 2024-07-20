package land.esta.plugin.ssb.bots.setting.impl;

import land.esta.plugin.ssb.bots.setting.Setting;
import land.esta.plugin.ssb.bots.setting.SettingObject;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;

@Getter
@Setter
public class EatingSettings extends Setting {

    private SettingObject startEatingTimer = new SettingObject("Start Eating Timer",Material.GOLDEN_APPLE,40,2);
    private SettingObject startEatingDistance = new SettingObject("Start Eating Distance",Material.MUSHROOM_SOUP,3.0,1);
    private SettingObject eatingGappleHealth = new SettingObject("Eating Gapple Health",Material.GOLDEN_APPLE,15,1);
    private SettingObject cancelEatingDistance = new SettingObject("Jump Eating Distance", Material.BARRIER,4.5,0.1);
    private SettingObject preGap = new SettingObject("Pre Gappling", Material.GOLDEN_CARROT,true);

    public EatingSettings() {
        super(Material.GOLDEN_APPLE,"&c&lGapple Settings","Gapple", "gapplesettings", 14,false,"&f In this category","&f you are able to","&f modify the bot's","&f gapple settings.","","&aClick to select!");
        addSettings(preGap,startEatingDistance,startEatingTimer,eatingGappleHealth,cancelEatingDistance);
    }
}
