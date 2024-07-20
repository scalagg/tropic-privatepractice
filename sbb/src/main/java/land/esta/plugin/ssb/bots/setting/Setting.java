package land.esta.plugin.ssb.bots.setting;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.*;

@Getter
@Setter
public class Setting implements ConfigurationSerializable {

    private final ArrayList<SettingObject> settings;
    private final Material material;
    private final String fancyName;
    private final String name;
    private final  String permissionName;
    private int id = 0;
    private int slot;
    private List<String> description;
    private boolean important;
    private List<Integer> empty;

    private static final HashMap<String,Setting> defaults = new HashMap<>();

    public SettingObject getSettingByName(String name) {
        for (SettingObject sb : settings) {
            if (Objects.equals(sb.getName(), name)) return sb;
        }
        return null;
    }

    public Setting(Material material, String fancyName, String name, String permissionName, int slot,boolean important, String... strings) {
        this(material, fancyName,name, permissionName, slot,important,Arrays.asList(strings));
    }

    public Setting(Material material, String fancyName,String name, String permissionName, int slot,boolean important, List<String> strings) {
        settings = new ArrayList<>();
        this.material = material;
        this.fancyName = fancyName;
        this.name = name;
        this.permissionName = permissionName;
        this.slot = slot;
        this.description = strings;
        this.important = important;
        this.empty = new ArrayList<>();

        defaults.put(fancyName,this);
    }

    public void addSetting(SettingObject object) {
        settings.add(object);
    }

    public void addSettings(SettingObject... objects) {
        int index = 0;
        for (SettingObject object : objects) {
            if (object == null) {
                empty.add(index);
                continue;
            }

            addSetting(object);
            index++;
        }
    }

    public Setting setMaterialId(int id) {
        this.id = id;
        return this;
    }

    public void addSettings(ArrayList<SettingObject> objects) {
        for (SettingObject object : objects) {
            addSetting(object);
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> m = new HashMap<String, Object>();
        settings.forEach((setting) -> {
            m.put(setting.getName(),setting.getObject());
        });
        m.put("name", fancyName);

        return m;
    }

    public Setting clone() {
        Setting setting = new Setting(material, fancyName,name, permissionName, slot,important,description);
        setting.addSettings(settings);

        return setting;
    }

    public String toDatabaseString() {
        //Bukkit.broadcastMessage("Before: " + name);
        String[] words = name.replaceAll("\\([^)]*\\)", "").split(" ");
        String databaseStr = words[0].toLowerCase();
        for (int i = 1; i < words.length; i++) {
            databaseStr += words[i];
        }
        //Bukkit.broadcastMessage("After: " + databaseStr);
        return databaseStr;
    }
}
