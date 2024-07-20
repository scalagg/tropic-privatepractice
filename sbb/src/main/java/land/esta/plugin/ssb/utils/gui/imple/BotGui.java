package land.esta.plugin.ssb.utils.gui.imple;

import com.mongodb.client.MongoCursor;
import land.esta.plugin.ssb.SmartBotsBackend;
import land.esta.plugin.ssb.bot.profile.BotSettingProfile;
import land.esta.plugin.ssb.bot.profile.BotSettingProfileService;
import lombok.Getter;
import lombok.Setter;
import land.esta.plugin.ssb.bots.setting.Setting;
import land.esta.plugin.ssb.bots.setting.SettingManager;
import land.esta.plugin.ssb.bots.setting.SettingObject;
import land.esta.plugin.ssb.data.DataPlayer;
import land.esta.plugin.ssb.utils.Color;
import land.esta.plugin.ssb.utils.MathUtils;
import land.esta.plugin.ssb.utils.gui.PlayerGui;
import org.bson.Document;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Getter
@Setter
public class BotGui {
    private PlayerGui mainGui;
    private DataPlayer data;
    private SettingManager manager;
    private HashMap<Setting,PlayerGui> guis;
    private PlayerGui profilesGui;
    private int theme = 15;
    private final String message = "&aYou have modified &e%setting% &ato &e%value%&a.";
    private int maxProfiles;

    private String defaultPath = "data.%uuid%";
    private boolean loaded;
    private List<SettingManager> profiles;

    public BotGui(DataPlayer dataPlayer) {
        new BukkitRunnable() {
            @Override
            public void run() {
                manager = SettingManager.loadSettings(dataPlayer.getPlayer().getUniqueId());
                mainGui = new PlayerGui("&c&lBot Settings", (int) MathUtils.floor(manager.getSettings().size() * 2,9,false) + 18,dataPlayer);
                data = dataPlayer;
                maxProfiles = 9;
                profilesGui = new PlayerGui("&c&lBot Profiles",maxProfiles,dataPlayer);
                guis = new HashMap<>();

                profiles = new ArrayList<>();
                createSubGuis();
                createProfiles();
                createMain(dataPlayer.getPlayer());
                loaded = true;
            }
        }.runTaskAsynchronously(SmartBotsBackend.getInstance());
    }

    public void saveSettings() {
        BotSettingProfile profile = BotSettingProfileService.INSTANCE.find(data.getPlayer().getUniqueId());

        if (profile != null) {
            profile.update(manager.getSettings());
        }
    }

    public void resetSettings() {}

    public void createSubGuis() {
        manager.getSettings().forEach(setting -> {
            PlayerGui gui;
            if (setting.getName().equalsIgnoreCase("PlayStyle")) gui = new PlayerGui(setting.getFancyName(), 9 * 5, data);
            else gui = new PlayerGui(setting.getFancyName(), (int) MathUtils.floor(setting.getSettings().size() + 2,9,false) + 9 * 2, data);

            gui.setGui(() -> {
                int slot = 10;
                int index = 0;
                for (SettingObject so : setting.getSettings()) {
                    if (setting.getEmpty().contains(index)) {
                        gui.setBackGroundColorSlot(theme,slot);
                        index++;
                        slot+=1;
                        if ((slot - 8) % 9 == 0) slot += 2;
                    }

                    gui.createGuiItem(so.getItem(),slot,"&c&l"+so.getName(), (player, pressedSlot) -> {
                        if (so.isHasPerm() && !player.hasPermission("botgui." + setting.getPermissionName() + "." + so.getName())) {
                            sendNoPerms(player);
                        }
                        if (so.getObject() instanceof Boolean) {
                            so.setObjectUpdate(!so.isBooleanValue());
                            player.sendMessage(Color.code(message.replace("%setting%", so.getName()).replace("%value%",so.isBooleanValue() + "")));
                        }

                        if (so.getObject() instanceof Integer) {
                            so.setObjectUpdate((int) Math.max(so.getOriginalIntValue() + so.getUpdateBy(),0));
                            player.sendMessage(Color.code(message.replace("%setting%", so.getName()).replace("%value%",so.getOriginalIntValue() + "")));
                        }

                        if (so.getObject() instanceof Double) {
                            so.setObjectUpdate(Math.max(so.getOriginalDouble() + so.getUpdateBy(),0));
                            player.sendMessage(Color.code(message.replace("%setting%", so.getName()).replace("%value%",MathUtils.stringOfDouble(so.getOriginalDouble(),3) + "")));
                        }

                        player.playSound(player.getLocation(), Sound.CLICK, 1.0F, 1.0F);
                        gui.minOpen();
                    },(player, temp) -> {
                        if (so.isHasPerm() && !player.hasPermission("botgui." + setting.getPermissionName() + "." + so.getName())) {
                            sendNoPerms(player);
                        }
                        if (so.getObject() instanceof Boolean) {
                            so.setObjectUpdate(!so.isBooleanValue());
                            player.sendMessage(Color.code(message.replace("%setting%", so.getName()).replace("%value%",so.isBooleanValue() + "")));
                        }

                        if (so.getObject() instanceof Integer) {
                            so.setObjectUpdate((int) Math.max(so.getOriginalIntValue() - so.getUpdateBy(),0));
                            player.sendMessage(Color.code(message.replace("%setting%", so.getName()).replace("%value%",so.getOriginalIntValue() + "")));
                        }

                        if (so.getObject() instanceof Double) {
                            so.setObjectUpdate(Math.max(so.getOriginalDouble() - so.getUpdateBy(),0));
                            player.sendMessage(Color.code(message.replace("%setting%", so.getName()).replace("%value%",MathUtils.stringOfDouble(so.getOriginalDouble(),3) + "")));
                        }

                        player.playSound(player.getLocation(), Sound.CLICK, 1.0F, 1.0F);
                        gui.minOpen();
                    },(player, temp) -> {
                        if (so.isHasPerm() && !player.hasPermission("botgui." + setting.getPermissionName() + "." + so.getName())) {
                            sendNoPerms(player);
                        }
                        if (so.getObject() instanceof Boolean) {
                           return;
                        }

                        if (so.getObject() instanceof Integer) {
                            so.setRandomizerUpdate((int) Math.max(so.getRandomizerInt() + so.getUpdateBy(),0));
                            player.sendMessage(Color.code(message.replace("%setting%", so.getName() + " Randomizer").replace("%value%",so.getRandomizerInt() + "")));
                        }

                        if (so.getObject() instanceof Double) {
                            so.setRandomizerUpdate(Math.max(so.getRandomizer() + so.getUpdateBy(),0));
                            player.sendMessage(Color.code(message.replace("%setting%", so.getName() + " Randomizer").replace("%value%",so.getRandomizer() + "")));
                        }

                        player.playSound(player.getLocation(), Sound.CLICK, 1.0F, 1.0F);
                        gui.minOpen();
                    },(player, temp) -> {
                        if (so.isHasPerm() && !player.hasPermission("botgui." + setting.getPermissionName() + "." + so.getName())) {
                            sendNoPerms(player);
                        }
                        if (so.getObject() instanceof Boolean) {
                            return;
                        }

                        if (so.getObject() instanceof Integer) {
                            so.setRandomizerUpdate((int) Math.max(so.getRandomizerInt() - so.getUpdateBy(),0));
                            player.sendMessage(Color.code(message.replace("%setting%", so.getName() + " Randomizer").replace("%value%",so.getRandomizerInt() + "")));
                        }

                        if (so.getObject() instanceof Double) {
                            so.setRandomizerUpdate(Math.max(so.getRandomizer() - so.getUpdateBy(),0));
                            player.sendMessage(Color.code(message.replace("%setting%", so.getName() + " Randomizer").replace("%value%",so.getRandomizer() + "")));
                        }

                        player.playSound(player.getLocation(), Sound.CLICK, 1.0F, 1.0F);
                        gui.minOpen();
                    }, so.getDescription());

                    index++;
                    slot += 1;
                    if ((slot - 8) % 9 == 0) slot += 2;
                }

                gui.createGuiItem(new ItemStack(Material.CARPET),0,"&c&lLast Page", (player, pressedSlot) -> mainGui.minOpen());

                gui.setBackGroundColor(theme);
            });
            guis.put(setting,gui);
        });
    }

    public void createMain(Player p) {
        mainGui.setGui(() -> {
            mainGui.reset();
            int slot = 0;

            for (Setting setting : manager.getSettings()) {
                mainGui.createGuiItem(new ItemStack(setting.getMaterial(),1, (short) setting.getId()),setting.getSlot(), setting.getFancyName(),(player, temp) -> {
                    if (data.isUpdatingGui()) {
                        player.sendMessage(Color.code("&cPlease wait for the menu to update!"));
                    } else {
                        if (player.hasPermission("botgui." + setting.getPermissionName())) {
                            PlayerGui gui = guis.get(setting);
                            if (gui == null) return;
                            gui.minOpen();
                        } else if (setting.getPermissionName().equalsIgnoreCase("velocitysettings")) {
                            player.sendMessage(Color.code("&cYou need &5SIMP &crank to use this."));
                            player.sendMessage(Color.code("&cStore link: &estore.esta.land"));
                            player.closeInventory();
                        } else if (setting.getPermissionName().equalsIgnoreCase("playstylesettings") || (setting.getPermissionName().equalsIgnoreCase("fleesettings") || (setting.getPermissionName().equalsIgnoreCase("wtapsettings") || (setting.getPermissionName().equalsIgnoreCase("rodsettings"))))) {
                            player.sendMessage(Color.code("&cYou need &bMVP &crank to use this."));
                            player.sendMessage(Color.code("&cStore link: &estore.esta.land"));
                            player.closeInventory();
                        } else if (setting.getPermissionName().equalsIgnoreCase("connectionsettings") || (setting.getPermissionName().equalsIgnoreCase("potionsettings") || (setting.getPermissionName().equalsIgnoreCase("gapplesettings")))) {
                            player.sendMessage(Color.code("&cYou need &9VIP+ &crank to use this."));
                            player.sendMessage(Color.code("&cStore link: &estore.esta.land"));
                            player.closeInventory();
                        } else {
                            player.sendMessage(Color.code("&cYou need &3VIP &crank to use this."));
                            player.sendMessage(Color.code("&cStore link: &estore.esta.land"));
                            player.closeInventory();
                        }
                    }
                }, setting.getDescription(), p.hasPermission("botgui." + setting.getPermissionName()));
                slot+=2;
            }

            //Name
            slot = (int) MathUtils.floor(manager.getSettings().size() * 2,9,false) + 6 + 9;
            mainGui.createGuiItemUsingName(manager.getSkullItem(data.getPlayer().getName(),false),slot,"&cChange Bot Name",(player, pressedSlot) -> {
                if (player.hasPermission("botgui.name")) {
                    DataPlayer data = SmartBotsBackend.getInstance().getDataManager().getPlayer(player);
                    data.setChatUpdate((name) -> {
                        manager.setBotName(data.getPlayer().getUniqueId(), Color.code(name));
                        player.sendMessage(Color.code("&aYou have changed the Bot Name to &e" + name + "&a."));

                        return true;
                    });

                    player.sendMessage(Color.code("&aType a valid player username."));
                    player.sendMessage(Color.code("&7(You may find search around on https://namemc.com)"));
                    player.closeInventory();
                } else {
                    player.sendMessage(Color.code("&cYou need &3VIP &crank to use this."));
                    player.sendMessage(Color.code("&cStore link: &estore.esta.land"));
                    player.closeInventory();
                }
            });

            //Skin
            slot = (int) MathUtils.floor(manager.getSettings().size() * 2,9,false) + 7 + 9;
            mainGui.createGuiItemUsingName(manager.getSkullItem(data.getPlayer().getName(),true),slot,"&cChange Bot Skin",(player, pressedSlot) -> {
                if (player.hasPermission("botgui.skin")) {
                    DataPlayer data = SmartBotsBackend.getInstance().getDataManager().getPlayer(player);
                    data.setChatUpdate((name) -> {
                        manager.setSkin(data.getPlayer().getUniqueId(), name);
                        player.sendMessage(Color.code("&aYou have changed the Bot Skin to &e" + name + "&a."));
                        mainGui.openGui();
                        return true;
                    });

                    player.sendMessage(Color.code("&aType a valid player username."));
                    player.sendMessage(Color.code("&7(You may find search around on https://namemc.com)"));
                    player.closeInventory();
                } else {
                    player.sendMessage(Color.code("&cYou need &3VIP &crank to use this."));
                    player.sendMessage(Color.code("&cStore link: &estore.esta.land"));
                    player.closeInventory();
                }
            });

            //Save
            slot = (int) MathUtils.floor(manager.getSettings().size() * 2,9,false) + 1 + 9;
            mainGui.createGuiItem(new ItemStack(Material.WOOL,1,(short) 5),slot,"&a&lSave",(player, pressedSlot) -> {
                DataPlayer data = SmartBotsBackend.getInstance().getDataManager().getPlayer(player);
                if (!data.isUpdatingGui()) {
                    if (!data.isSaving()) {
//                        data.getGui().saveSettings();
                        player.sendMessage(Color.code("&aYou have successfully saved your settings."));
                    } else player.sendMessage(Color.code("&7Please wait for your presets to save..."));
                } else {
                    player.sendMessage(Color.code("&cPlease wait for the menu to update!"));
                }
            });

            //Load
            slot = (int) MathUtils.floor(manager.getSettings().size() * 2,9,false) + 2 + 9;
            mainGui.createGuiItem(new ItemStack(Material.NAME_TAG,1),slot,"&e&lLoad Presets",(player, pressedSlot) -> {
                if (!data.isSaving()) {
                    profilesGui.minOpen();
                    player.sendMessage(Color.code("&cThis feature is currently disabled."));
                } else player.sendMessage(Color.code("&7Please wait for your presets to save..."));
            });

            //Reset
            slot = (int) MathUtils.floor(manager.getSettings().size() * 2,9,false) + 4 + 9;
            mainGui.createGuiItem(new ItemStack(Material.WOOL,1,(short) 14),slot,"&c&lReset",(player, pressedSlot) -> {
                DataPlayer data = SmartBotsBackend.getInstance().getDataManager().getPlayer(player);
                data.getGui().getManager().resetSettings(data);
                manager.setSkin(data.getPlayer().getUniqueId(), "Xecivity");
                manager.setBotName(data.getPlayer().getUniqueId(), Color.code("Bot"));
                player.sendMessage(Color.code("&aYou have successfully reset your settings."));
            });
            mainGui.setBackGroundColor(theme);
        });
    }

    public void sendNoPerms(Player player) {
        player.sendMessage(Color.code("&cYou need &3VIP &crank to use this."));
        player.sendMessage(Color.code("&cStore link: &estore.esta.land"));
    }

    public void createProfiles() {
        profilesGui.setGui(() -> {
            profilesGui.reset();

            List<SettingManager> managers = profiles;
            while (managers.size() > maxProfiles) {
                managers.remove(0);
            }

            int slot = 0;
            for (SettingManager settingManager : managers) {
                profilesGui.createGuiItemUsingName(manager.getSkullItem("crafticat",true),slot,manager.getBotName().replace("%player%","crafticat"),(player, pressedSlot) -> {
                    this.manager.update(settingManager);
                    resetSettings();
                    player.sendMessage(Color.code("&aYou've switched to &e#" + manager.getBotName().replace("%player%","crafticat") + "&a profile."));
                }, (player, pressedSlot) -> {
                    //SmartBotsBackend.getInstance().getSqlInfo().deletePreset(manager.getFoundPath());
                    player.sendMessage(Color.code("&fDeleted preset &c" + manager.getBotName().replace("%player%","crafticat")));
                    profiles.remove(settingManager);
                    player.closeInventory();
                }, manager.getDescription());
                slot++;
            }

            profilesGui.setBackGroundColor(15);
        });
    }
}
