package land.esta.plugin.ssb.bots.setting.impl;

import land.esta.plugin.ssb.bots.setting.Setting;
import land.esta.plugin.ssb.bots.setting.SettingObject;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;

@Getter
@Setter
public class AttackSettings extends Setting {

    private SettingObject reach = new SettingObject("Reach",Material.DIAMOND_SWORD,2.7,0.1);
    private SettingObject cps = new SettingObject("CPS", Material.DIAMOND_CHESTPLATE,6);
    private SettingObject swingRange = new SettingObject("Swing Range",Material.IRON_SWORD,6);
    private SettingObject playerHitBox = new SettingObject("Player Hitbox",Material.IRON_CHESTPLATE,0.3,0.1);
    private SettingObject advancedReduce = new SettingObject("Reduce",Material.STICK,false);

    public int getDelay() {
        return cps.getIntValue() > 0 ? 1000 / cps.getIntValue() : 1000;
    }

    public AttackSettings() {
        super(Material.DIAMOND_SWORD,"&c&lAttack Settings","Attack", "attacksettings", 10,true,"&f In this category","&f you are able to","&f modify the bot's","&f attack settings.","","&aClick to select!");
        addSettings(reach,cps,swingRange,advancedReduce,playerHitBox);
    }
}
