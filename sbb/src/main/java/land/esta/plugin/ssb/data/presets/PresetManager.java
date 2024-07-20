package land.esta.plugin.ssb.data.presets;

import lombok.Getter;
import land.esta.plugin.ssb.bots.setting.SettingManager;
import land.esta.plugin.ssb.utils.Color;
import land.esta.plugin.ssb.utils.Command;
import land.esta.plugin.ssb.utils.gui.imple.SettingGui;

import java.util.List;

@Getter
public class PresetManager {
    private final DataPresets unranked;
    private final DataPresets ranked;
    private final DataPresets tournament;

    public PresetManager() {
        unranked = registerData("unranked", PresetType.UNRANKED);
        ranked = registerData("ranked", PresetType.RANKED);
        tournament = registerData("tournament", PresetType.TOURNAMENT);
    }

    public DataPresets registerData(String name, PresetType type) {
        DataPresets p = new DataPresets(name, type);
        createCMD(p, name);
        return p;
    }

    public void createCMD(DataPresets data, String name) {
        //Admin command
        Command adminCMD = new Command(data.getName() + "admin","Presets admin gui",data.getName() + "admin",null);

        adminCMD.setOnCommand((player, temp) -> {
            if (!player.hasPermission("bot.staff")) {
                player.sendMessage(Color.code("&cYou do not have permission to execute this command."));
                return;
            }
            data.getGuis().get(0).minOpen(player);
        });
    }

    public DataPresets getDataPresets(PresetType type) {
        return switch (type) {
            case UNRANKED -> unranked;
            case RANKED -> ranked;
            case TOURNAMENT -> tournament;
            default -> null;
        };
    }

    public SettingManager getPresetSetting(PresetType type, String name) {
        List<SettingGui> settingGuis = getDataPresets(type).getPresets();
        for (SettingGui settingGui : settingGuis) {
            if (settingGui.getManager().getName().equals(name)) {
                return settingGui.getManager();
            }
        }
        return null;
    }
}
