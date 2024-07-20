package land.esta.plugin.ssb.bots.setting.impl;

import land.esta.plugin.ssb.bots.setting.Setting;
import land.esta.plugin.ssb.bots.setting.SettingObject;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;

@Getter
@Setter
public class PotionSetting extends Setting {

    //Health potion
    private SettingObject fleeHealth = new SettingObject("Flee Health", Material.APPLE,4,1);
    private SettingObject agroPotion = new SettingObject("Agro Potion",Material.DIAMOND_SWORD,false);
    private SettingObject firstPotionTick = new SettingObject("First Potion Tick", Material.WATCH,9,1);
    private SettingObject secondPotionTick = new SettingObject("Second Potion Tick", Material.WATCH,12,1);
    private SettingObject finishPotionTick = new SettingObject("Finish Potion Tick", Material.WATCH,22,1);
    private SettingObject tickOffset = new SettingObject("Tick Offset", Material.DIODE,1,1);
    private SettingObject secondPotion = new SettingObject("Second Potion",Material.POTION, true);
    private SettingObject cancelPotionDistance = new SettingObject("Cancel Potion Distance",Material.HOPPER,3.0,0.1);

    public PotionSetting() {
        super(Material.POTION,"&c&lPotion Settings","Potion", "potionsettings",15,false,"&f In this category","&f you are able to","&f modify the bot's","&f potion settings.","","&aClick to select!");

        setId(16428);
        addSettings(agroPotion,fleeHealth,firstPotionTick,secondPotionTick,finishPotionTick,tickOffset,cancelPotionDistance,secondPotion);
    }
}
