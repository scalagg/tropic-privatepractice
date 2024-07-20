package land.esta.plugin.ssb.bots.setting;

import lombok.Getter;
import lombok.Setter;
import land.esta.plugin.ssb.utils.MathUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SettingObject {

    private String name;
    private ItemStack item;
    private Object object;
    private double updateBy = 1;
    private double randomizer = 0;
    private boolean hasPerm;
    private double min = 0;
    private double max = 0;

    public void setObjectUpdate(Object object) {
        this.object = object;
    }

    public void setRandomizerUpdate(double randomizer) {
        this.randomizer = randomizer;
    }

    public void setMinUpdate(double value) {
        this.min = value;
    }

    public void setMaxUpdate(double value) {
        this.max = value;
    }

    public List<String> getDescription() {
        List<String> result = new ArrayList<>();
        if (object instanceof Boolean) {
            Boolean bool = (Boolean) object;
            result.add("");
            result.add(bool ? "&a&l│ &fOn" : "&c│ &fOn");
            result.add(bool ? "&c│ &fOff" : "&a&l│ &fOff");
            result.add("");
            result.add("&aClick to change!");
        } else {
            result.add("&c&lSettings:");
            if (object instanceof Double) result.add("&c ● &f" + name + ": &c" + MathUtils.stringOfDouble((double) object,3));
            if (object instanceof Integer) result.add("&c ● &f" + name + ": &c" + object);
            if (object instanceof Double) result.add("&c ● &fRandomizer" + ": &c" + MathUtils.stringOfDouble(randomizer,3) + " &f(&c"+ (MathUtils.stringOfDouble((double) object - randomizer,3)) +" &f◀▶ &c"+ (MathUtils.stringOfDouble((double) object + randomizer,3)) +"&f)");
            if (object instanceof Integer) result.add("&c ● &fRandomizer" + ": &c" + randomizer + " &f(&c"+ ((int) object - randomizer) +" &f◀▶ &c"+ ((int) object + randomizer) +"&f)");

            result.add("");
            result.add("&c&lMain:");
            result.add(" &a▲ &fLeft Click");
            result.add(" &c▼ &fRight Click");
            result.add("");
            result.add("&c&lRandomization:");
            result.add(" &a▲ &fShift Left Click");
            result.add(" &c▼ &fShift Right Click");
        }

        return result;
    }

    public double getDoubleValue() {
        if (object instanceof Integer) return (int) object;
        return (double) object + MathUtils.getRandom(-randomizer,randomizer);
    }

    public double getOriginalDouble() {
        if (object instanceof Integer) return (int) object;
        return (double) object;
    }

    public double getRandomizerInt() {
        return (int) randomizer;
    }

    public int getIntValue() {
        return (int) object + (int) Math.round(MathUtils.getRandom(-randomizer,randomizer));
    }

    public int getOriginalIntValue() {
        return (int) object;
    }

    public boolean isBooleanValue() {
        return (boolean) object;
    }

    public SettingObject(String name, ItemStack item,Object object) {
        this.name = name;
        this.item = item;
        this.object = object;
    }

    public SettingObject(String name, ItemStack item,double doubleValue, double updateBy) {
        this.name = name;
        this.item = item;
        this.object = doubleValue;
        this.updateBy = updateBy;
    }

    public SettingObject(String name, ItemStack item,int intValue, double updateBy) {
        this.name = name;
        this.item = item;
        this.object = intValue;
        this.updateBy = updateBy;
    }

    public SettingObject(String name, ItemStack item,boolean booleanValue) {
        this.name = name;
        this.item = item;
        this.object = booleanValue;
    }

    public SettingObject(String name,Material material,boolean booleanValue) {
        this(name,new ItemStack(material,1),booleanValue);
    }

    public SettingObject(String name,Material material,int intValue) {
        this(name,new ItemStack(material,1),intValue);
    }

    public SettingObject(String name,Material material,double doubleValue) {
        this(name,new ItemStack(material,1),doubleValue);
    }

    public SettingObject(String name,Material material,int intValue,double updateBy) {
        this(name,new ItemStack(material,1),intValue,updateBy);
    }

    public SettingObject(String name,Material material,double doubleValue,double updateBy) {
        this(name,new ItemStack(material,1),doubleValue,updateBy);
    }

    public String toDatabaseString() {
        String[] words = name.replaceAll("\\([^)]*\\)", "").split(" ");
        String databaseStr = words[0].toLowerCase();
        for (int i = 1; i < words.length; i++) {
            databaseStr += words[i].substring(0, 1) + words[i].substring(1).toLowerCase();
        }
        return databaseStr;
    }
}
