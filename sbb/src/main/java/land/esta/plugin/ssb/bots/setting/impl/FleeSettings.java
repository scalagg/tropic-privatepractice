package land.esta.plugin.ssb.bots.setting.impl;

import land.esta.plugin.ssb.bots.setting.Setting;
import land.esta.plugin.ssb.bots.setting.SettingObject;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;

@Getter
@Setter
public class FleeSettings extends Setting {

    private SettingObject calculatedDistance = new SettingObject("Calculated Distance", Material.WATCH,10.0);
    private SettingObject runFromCombos = new SettingObject("Run From Combos", Material.DIAMOND_BOOTS,false);
    private SettingObject runFromCombosHits = new SettingObject("Run From Combo Hits", Material.DIAMOND_SWORD,3,1);
    private SettingObject runFromCombosTime = new SettingObject("Run From Combo Ticks", Material.WATCH,20,1);
    private SettingObject runningRotationSpeed = new SettingObject("Run Rotation Speed",Material.WEB,40,1);

    public FleeSettings() {
        super(Material.FEATHER,"&c&lFlee Settings","Flee", "fleesettings",22,false,"&f In this category","&f you are able to","&f modify the bot's","&f flee settings.","","&aClick to select!");
        addSettings(calculatedDistance,runFromCombos,runFromCombosHits,runFromCombosTime,runningRotationSpeed);
    }
}
