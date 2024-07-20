package land.esta.plugin.ssb.bots.setting.impl;

import land.esta.plugin.ssb.bots.setting.Setting;
import land.esta.plugin.ssb.bots.setting.SettingObject;
import lombok.Getter;
import org.bukkit.Material;

@Getter
public class RodSettings extends Setting {
    private final SettingObject enabled = new SettingObject("Rod Enabled",Material.FISHING_ROD,false);
    private final SettingObject minRodDistance = new SettingObject("Minimum Rod Throwing Distance",Material.FISHING_ROD,5.0,0.1);
    private final SettingObject maxRodDistance = new SettingObject("Maximum Rod Throwing Distance",Material.FISHING_ROD,7.5,0.1);
    private final SettingObject rodDuration = new SettingObject("Rod Duration (MS)",Material.FISHING_ROD,750, 10);
    private final SettingObject rodWhileInCombo = new SettingObject("Throw rod while in combo",Material.FISHING_ROD,false);

    public RodSettings() {
        super(Material.FISHING_ROD,"&c&lRod Settings","Rod", "rodsettings",20,false,"&f In this category","&f you are able to","&f modify the bot's","&f rod settings.","","&aClick to select!");
        addSettings(enabled,minRodDistance,maxRodDistance,rodDuration,rodWhileInCombo);
    }
}
