package land.esta.plugin.ssb.bots.setting.impl;

import land.esta.plugin.ssb.bots.setting.Setting;
import land.esta.plugin.ssb.bots.setting.SettingObject;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;

@Getter
@Setter
public class RotationSetting extends Setting {

    private SettingObject speed = new SettingObject("Speed", Material.EXP_BOTTLE,18,2);
    private SettingObject jitterRotation = new SettingObject("Jitter Rotation", Material.FLINT,1.0);
    private SettingObject speedRandom = new SettingObject("Speed Random", Material.FIREWORK,1.0);

    public RotationSetting() {
        super(Material.WEB,"&c&lRotation Settings","Rotation", "rotationsettings", 13,true,"&f In this category","&f you are able to","&f modify the bot's","&f rotation settings.","","&aClick to select!");

        addSettings(speed,jitterRotation,speedRandom);
    }
}
