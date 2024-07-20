package land.esta.plugin.ssb.bots.setting.impl;

import land.esta.plugin.ssb.bots.setting.Setting;
import land.esta.plugin.ssb.bots.setting.SettingObject;
import lombok.Getter;
import org.bukkit.Material;

@Getter
public class PlaystyleSettings extends Setting {
    private final SettingObject hitSelect = new SettingObject("Hit Select",Material.STICK,false);
    private final SettingObject hitSelectTicks = new SettingObject("Hit Select Ticks",Material.STICK,5);

    private final SettingObject hitSelectDelay = new SettingObject("Hit Select Delay",Material.STICK,5);

    private final SettingObject jumpComboCPS = new SettingObject("Jump Combo CPS",Material.STICK,4);

    private final SettingObject flips = new SettingObject("Flicks (Recommended to have high rotation speed)",Material.SLIME_BALL,false);
    private final SettingObject flipYaw = new SettingObject("Flicks Yaw (360, 180, 90...)",Material.SLIME_BALL,35,5);
    private final SettingObject flipPitch = new SettingObject("Flicks Pitch (25, 15, 0...)",Material.SLIME_BALL,5,2);
    private final SettingObject flipLong = new SettingObject("Flicks Long",Material.SLIME_BALL,5);
    private final SettingObject flipChance = new SettingObject("Flicks Chance",Material.SLIME_BALL,75,5);

    private final SettingObject flipSync = new SettingObject("Flicks Sync w/ WTaps",Material.SLIME_BALL,true);

    private final SettingObject alwaysJump = new SettingObject("Jump",Material.SADDLE,false);

    private final SettingObject jumpWhileCombo = new SettingObject("Jump While Comboed",Material.SADDLE,false);

    private final SettingObject jumpWhileComboHits = new SettingObject("Jump While Comboed Hits",Material.SADDLE,4);

    private final SettingObject jumpCombo = new SettingObject("Jump Combo",Material.SADDLE,false);

    private final SettingObject jumpComboHits = new SettingObject("Jump Combo Hits",Material.SADDLE,4);

    public PlaystyleSettings() {
        super(Material.ENDER_PEARL,"&c&lPlayStyle Settings","PlayStyle", "playstylesettings",23,true,"&f In this category","&f you are able to","&f modify the bot's","&f playstyle settings.","","&aClick to select!");
        addSettings(hitSelect,hitSelectTicks,hitSelectDelay,flips,flipYaw,flipPitch,flipLong,flipChance,flipSync,alwaysJump,jumpWhileCombo,jumpWhileComboHits,jumpCombo,jumpComboCPS,jumpComboHits);
    }
}
