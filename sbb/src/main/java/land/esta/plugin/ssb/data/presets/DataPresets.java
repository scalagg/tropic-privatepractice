package land.esta.plugin.ssb.data.presets;

import com.mongodb.client.MongoCursor;
import land.esta.plugin.ssb.SmartBotsBackend;
import lombok.Getter;
import lombok.Setter;
import land.esta.plugin.ssb.bots.setting.SettingManager;
import land.esta.plugin.ssb.utils.Color;
import land.esta.plugin.ssb.utils.gui.GlobalGui;
import land.esta.plugin.ssb.utils.gui.imple.SettingGui;
import org.bson.Document;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Getter
@Setter
public class DataPresets {

    //Data
    private final List<SettingGui> presets;
    private final List<GlobalGui> guis = new ArrayList<>();
    private final String name;
    private final int theme = 15;

    //The max amount of presets
    private final int maxSize = 9 * 5;
    private PresetType type;
    private int count = 0;

    //OnCreate
    public DataPresets(String name, PresetType type) {
        presets = new ArrayList<>();
        this.name = name;
        this.type = type;

        loadPresets();
        createGuis();
    }

    //Loads all the presets from the config
    public void loadPresets() {
        try (MongoCursor<Document> iterator = SmartBotsBackend.getInstance().getMongoHandler().getPresetCollection(type).find().iterator()) {
            while (iterator.hasNext()) {
                count++;
                Document document = iterator.next();
                loadPreset(SettingManager.loadSettings(type, document.getString("presetName")));
            }
        }
    }

    public void createGuis() {
        double toPages = count / 36.0;
        String[] split = (toPages + "").split("\\.", 2);
        int pages = Integer.parseInt(split[0].replaceAll("\\.", ""));

        guis.clear();
        guis.add(new GlobalGui(type.name().toLowerCase() + " - 1", maxSize));
        System.out.println("Loaded Page - 1 for " + type.name().toLowerCase());
        for (int i = 1; i < pages + 1; i++) {
            System.out.println("Loaded Page - " + (i + 1) + " for " + type.name().toLowerCase());
            guis.add(new GlobalGui(type.name().toLowerCase() + " - " + (i + 1), maxSize));
        }

        loadGui();
    }

    //Loads the presets' admin gui
    public void loadGui() { // 42
        for (int c = 0; c < guis.size(); c++) {
            GlobalGui gui = guis.get(c);
            int finalC = c;

            gui.setGui(() -> {
                gui.reset();

                double toPage2 = count / 36.0;
                String[] split2 = (toPage2 + "").split("\\.", 2);
                int page2 = Integer.parseInt(split2[0].replaceAll("\\.", ""));

                // Loops for each preset
                int slot = 0;
                // for (SettingGui preset : presets) {
                for (int i = finalC * 36; i < finalC * 36 + (presets.size() - finalC * 36); i++) {
                    SettingGui preset = presets.get(i);
                    // Create an item for the presets
                    SettingManager manager = preset.getManager();
                    String name = preset.getManager().getName();

                    List<String> lore = new ArrayList<>();
                    lore.add("&4&m--*------------------------*--");
                    lore.add("&c&lPreset:");
                    lore.add("&e" + name);
                    lore.add("&c&lMain:");
                    lore.add(" &a[+] &fLeft Click &a(To open)");
                    lore.add(" &c[-] &fRight Click &c(To delete)");
                    lore.add("&4&m--*------------------------*--");

                    gui.createGuiItemUsingName(manager.getSkullItem("crafticat", true), slot, "&c&l" + manager.getBotName().replace("%player%", "crafticat"), (player, pressedSlot) -> {
                        // Opens an edit gui for the settings.
                        String newName = "";
                        for (SettingGui p : presets) {
                            if (p.getManager().getName().equals(gui.getGui().getItem(pressedSlot).getItemMeta().getLore().get(2).replaceAll(Color.code("&e"), ""))) {
                                p.getMainGui().minOpen(player);
                                newName = p.getManager().getName();
                            }
                        }
                        player.sendMessage(Color.code("&fYou are now editing &c&l" + newName.replaceAll(Color.code("&e"), "")));
                    }, (player, pressedSlot) -> {
                        SettingGui settingGui = null;
                        for (SettingGui p : presets) {
                            if (p.getManager().getName().equals(gui.getGui().getItem(pressedSlot).getItemMeta().getLore().get(2).replaceAll(Color.code("&e"), ""))) {
                                settingGui = p;
                                SmartBotsBackend.getInstance().getMongoHandler().deletePreset(type, name);
                                player.sendMessage(Color.code("&fDeleted preset &c" + manager.getBotName().replace("%player%", "crafticat")));
                            }
                        }

                        if (settingGui != null) {
                            presets.remove(settingGui);
                            gui.minOpen(player);
                        }
                    }, lore);

                    if (slot++ >= maxSize - 10) {
                        // guis.add(new GlobalGui(type.name().toLowerCase() + " - " + (finalC + 1), maxSize));
                        break;
                    }
                }

                if (guis.size() - 1 > finalC) {
                    gui.createGuiItem(new ItemStack(Material.ARROW), maxSize - 3, "&cNext Page", (player, temp) -> {
                        guis.get(finalC + 1).minOpen(player);
                    });
                }

                if (finalC > 0) {
                    gui.createGuiItem(new ItemStack(Material.ARROW), maxSize - 7, "&cPrevious Page", (player, temp) -> {
                        guis.get(finalC - 1).minOpen(player);
                    });
                }

                gui.setBackGroundColor(theme);

                if (!gui.getGui().getItem(maxSize - 10).getType().equals(Material.SKULL_ITEM)) {
                    gui.createGuiItem(new ItemStack(Material.STONE), maxSize - 1, "&a&lCreate New Preset", (player, temp) -> {
                        SettingGui preset = createPreset();
                        player.sendMessage(Color.code("&fYou have successfully created a new preset named &c&l" + preset.getManager().getName()));
                        gui.minOpen(player);
                        recreateGuis(gui, player, finalC);
                    });
                }
            });
        }
    }

    public void recreateGuis(GlobalGui gui, Player player, int c) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (gui.getGui().getItem(maxSize - 10).getType().equals(Material.SKULL_ITEM) && c + 1 == guis.size()) {
                    // presets.clear();
                    guis.clear();

                    count = presets.size();

                    // loadPresets();
                    createGuis();
                    gui.minOpen(player);
//                    player.closeInventory();
//                    player.sendMessage(Color.code("&aPlease wait 10 seconds before opening " + type.name().toLowerCase() + "admin menu again"));
                }
            }
        }.runTaskLaterAsynchronously(SmartBotsBackend.getInstance(), 40);
    }

    //Picks a random preset
    public SettingGui getPreset() {
        if (presets.isEmpty()) return null;
        Random random = new Random();
        int index = random.nextInt(presets.size());
        return presets.get(index);
    }

    //Creates a default preset
    public SettingGui createPreset() {
        String presetName = "default";
        int counter = 0;

        List<String> names = presetsNames();

        while (names.contains(presetName)) {
            counter++;
            presetName = presetName + counter;
        }
        //Bukkit.broadcastMessage("path " + path);
        String finalPresetName = presetName;
        new BukkitRunnable() {
            @Override
            public void run() {
                SmartBotsBackend.getInstance().getMongoHandler().createBotDoc(type, finalPresetName);
            }
        }.runTaskAsynchronously(SmartBotsBackend.getInstance());
        SettingGui preset = new SettingGui(SettingManager.loadSettings(type, presetName),null, type);
        preset.getManager().setName(presetName);
        return addPreset(preset);
    }

    // Loads a preset
    public SettingGui loadPreset(SettingManager manager) {
        SettingGui preset = new SettingGui(manager,null, type);
        return addPreset(preset);
    }

    //If a present with that name exists
    public boolean findPreset(String name) {
        for (SettingGui preset : presets) {
            //If a preset was found
            if (preset.getManager().getName().equals(name)) return true;
        }

        //No presets were found
        return false;
    }

    public List<String> presetsNames() {
        List<String> result = new ArrayList<>();
        for (SettingGui preset : presets) {
            //adding the preset name
            result.add(preset.getManager().getName());
        }

        //returning the result
        return result;
    }

    public SettingGui addPreset(SettingGui preset) {
        presets.add(preset);
        return preset;
    }
}
