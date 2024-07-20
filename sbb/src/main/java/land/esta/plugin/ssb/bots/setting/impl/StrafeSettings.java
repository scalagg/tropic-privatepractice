package land.esta.plugin.ssb.bots.setting.impl;

import land.esta.plugin.ssb.bots.setting.Setting;
import land.esta.plugin.ssb.bots.setting.SettingObject;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;

@Getter
@Setter
public class StrafeSettings extends Setting {

    private SettingObject enabled = new SettingObject("Strafe",Material.SUGAR_CANE,false);
    private SettingObject chance = new SettingObject("Chance",Material.GOLDEN_CARROT,100);
    private SettingObject strafeInCombo = new SettingObject("Strafe In Combo",Material.SUGAR,false);
    private SettingObject switchSideTicks = new SettingObject("Switch Side Ticks",Material.TRAP_DOOR,20,1);
    //private SettingObject distancing = new SettingObject("Distancing",Material.DIAMOND_BOOTS,false);
    //private SettingObject distance = new SettingObject("Distance",Material.IRON_BOOTS,0,0.1);

    public StrafeSettings() {
        super(Material.SUGAR_CANE,"&c&lStrafe Settings","Strafe", "strafesettings", 11,true,"&f In this category","&f you are able to","&f modify the bot's","&f strafe settings.","","&aClick to select!");

        addSettings(enabled,chance,strafeInCombo,switchSideTicks);
    }
}
