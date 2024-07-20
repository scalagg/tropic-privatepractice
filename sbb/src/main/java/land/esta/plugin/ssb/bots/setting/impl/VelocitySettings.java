package land.esta.plugin.ssb.bots.setting.impl;

import land.esta.plugin.ssb.bots.setting.Setting;
import land.esta.plugin.ssb.bots.setting.SettingObject;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;

@Getter
@Setter
public class VelocitySettings extends Setting {

    private SettingObject horizontal = new SettingObject("Horizontal", Material.ANVIL,0.47,0.01);
    private SettingObject vertical = new SettingObject("Vertical", Material.ANVIL,0.35,0.01);
    private SettingObject dependentRange = new SettingObject("Dependent Range",Material.BLAZE_ROD,3.2,0.1);
    private SettingObject dependentRangeMultiplier = new SettingObject("Dependent Range Multiplier",Material.BLAZE_POWDER,0.15,0.01);
    private SettingObject motionFriction = new SettingObject("Motion Friction",Material.GOLD_BLOCK,1.0,0.02);
    private SettingObject sprintMultiplier = new SettingObject("Sprint Multiplier",Material.DIAMOND_HOE,1.2,0.1);
    private SettingObject groundMultiplier = new SettingObject("Ground Multiplier",Material.DIAMOND_SPADE,1.1,0.1);

    public VelocitySettings() {
        super(Material.SLIME_BALL,"&c&lVelocity Settings","Velocity", "velocitysettings",24,false,"&f In this category","&f you are able to","&f modify the bot's","&f velocity settings.","","&aClick to select!");

        addSettings(horizontal,vertical,dependentRange,dependentRangeMultiplier,motionFriction,sprintMultiplier,groundMultiplier);

    }
}
