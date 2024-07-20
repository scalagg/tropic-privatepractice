package land.esta.plugin.ssb.utils.gui.imple;

import land.esta.plugin.ssb.SmartBotsBackend;
import land.esta.plugin.ssb.bots.setting.Setting;
import land.esta.plugin.ssb.bots.setting.SettingManager;
import land.esta.plugin.ssb.bots.setting.SettingObject;
import land.esta.plugin.ssb.data.DataPlayer;
import land.esta.plugin.ssb.data.presets.PresetType;
import land.esta.plugin.ssb.utils.Color;
import land.esta.plugin.ssb.utils.MathUtils;
import land.esta.plugin.ssb.utils.gui.GlobalGui;
import land.esta.plugin.ssb.utils.gui.PlayerGui;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Getter
public class SettingGui {

    private final SettingManager manager;
    private final HashMap<Setting, GlobalGui> guis;
    private final int theme = 15;
    private final GlobalGui mainGui;
    private List<GlobalGui> gameRandomizerGuis = new ArrayList<>();

    private final String message = "&aYou have modified &e%setting% &ato &e%value%&a.";

    // Manager -> The setting which will be displayed in the gui
    // Data -> The player who sees the gui
    // Profiles -> Set to null to disable
    public SettingGui(SettingManager manager, PlayerGui profiles, PresetType type) {
        this.manager = manager;
        this.guis = new HashMap<>();
        this.mainGui = new GlobalGui("&c&lBot Settings", (int) MathUtils.floor(manager.getSettings().size() * 2, 9, false) + 18);

        createSubGuis();
        createMain(profiles, type);
    }

    public void createSubGuis() {
        manager.getSettings().forEach(setting -> {
            GlobalGui gui;
            if (setting.getName().equalsIgnoreCase("PlaySettings")) gui = new GlobalGui(setting.getFancyName(), 9 * 5);
            else
                gui = new GlobalGui(setting.getFancyName(), (int) MathUtils.floor(setting.getSettings().size() + 2, 9, false) + 9 * 2);

            NumberFormat numberFormat = NumberFormat.getInstance();
            numberFormat.setMaximumFractionDigits(1);
            numberFormat.setMinimumFractionDigits(0);

            gui.setGui(() -> {
                int slot = 10;
                int index = 0;
                for (SettingObject so : setting.getSettings()) {
                    final GlobalGui g = new GlobalGui("Game Randomizer " + so.getName(), 27);

                    if (so.getObject() instanceof Double) {
                        g.setGui(() -> {
                            List<String> loreMin = new ArrayList<>();
                            loreMin.add("&7Left click to add - 0.1");
                            loreMin.add("&7Right click to subtract - 0.1");
                            loreMin.add("");
                            loreMin.add("&7Shift Left Click to add - 1");
                            loreMin.add("&7Shift Right Click to subtract 1");
                            loreMin.add("");
                            loreMin.add("&7Current Value: " + numberFormat.format(so.getMin()));

                            List<String> loreMax = new ArrayList<>();
                            loreMax.add("&7Left click to add - 0.1");
                            loreMax.add("&7Right click to subtract - 0.1");
                            loreMax.add("");
                            loreMax.add("&7Shift Left Click to add - 1");
                            loreMax.add("&7Shift Right Click to subtract 1");
                            loreMax.add("");
                            loreMax.add("&7Current Value: " + numberFormat.format(so.getMax()));

                            // 11, 15
                            g.createGuiItem(new ItemStack(Material.BONE), 11, "&eMinimum", (player, pressedSlot) -> {
                                so.setMinUpdate(so.getMin() + 0.1);

                                g.minOpen(player);
                            }, (player, pressedSlot) -> {
                                if (so.getMin() > 0) {
                                    so.setMinUpdate(so.getMin() - 0.1);

                                    g.minOpen(player);
                                }
                            }, (player, pressedSlot) -> {
                                so.setMinUpdate(so.getMin() + 1);

                                g.minOpen(player);
                            }, (player, pressedSlot) -> {
                                if (so.getMin() > 9) {
                                    so.setMinUpdate(so.getMin() - 1);

                                    g.minOpen(player);
                                }
                            }, loreMin);

                            g.createGuiItem(new ItemStack(Material.BLAZE_ROD), 15, "&eMaximum", (player, pressedSlot) -> {
                                so.setMaxUpdate(so.getMax() + 0.1);

                                g.minOpen(player);
                            }, (player, pressedSlot) -> {
                                if (so.getMax() > 0) {
                                    so.setMaxUpdate(so.getMax() - 0.1);

                                    g.minOpen(player);
                                }
                            }, (player, pressedSlot) -> {
                                so.setMaxUpdate(so.getMax() + 1);

                                g.minOpen(player);
                            }, (player, pressedSlot) -> {
                                if (so.getMax() > 9) {
                                    so.setMaxUpdate(so.getMax() - 1);

                                    g.minOpen(player);
                                }
                            }, loreMax);

                            g.createGuiItem(new ItemStack(Material.CARPET), 0, "&cGo Back", ((player, pressedSlot) -> {
                                GlobalGui globalGui = guis.get(setting);
                                if (globalGui == null) return;

                                globalGui.minOpen(player);
                            }));

                            g.setBackGroundColor(15);
                        });
                    } else {
                        g.setGui(() -> {
                            List<String> loreMin = new ArrayList<>();
                            loreMin.add("&7Left click to add - 1");
                            loreMin.add("&7Right click to subtract - 1");
                            loreMin.add("");
                            loreMin.add("&7Shift Left Click to add - 10");
                            loreMin.add("&7Shift Right Click to subtract 10");
                            loreMin.add("");
                            loreMin.add("&7Current Value: " + numberFormat.format(so.getMin()));

                            List<String> loreMax = new ArrayList<>();
                            loreMax.add("&7Left click to add - 1");
                            loreMax.add("&7Right click to subtract - 1");
                            loreMax.add("");
                            loreMax.add("&7Shift Left Click to add - 10");
                            loreMax.add("&7Shift Right Click to subtract 10");
                            loreMax.add("");
                            loreMax.add("&7Current Value: " + numberFormat.format(so.getMax()));

                            // 11, 15
                            g.createGuiItem(new ItemStack(Material.BONE), 11, "&eMinimum", (player, pressedSlot) -> {
                                so.setMinUpdate(so.getMin() + 1);

                                g.minOpen(player);
                            }, (player, pressedSlot) -> {
                                if (so.getMin() > 0) {
                                    so.setMinUpdate(so.getMin() - 1);

                                    g.minOpen(player);
                                }
                            }, (player, pressedSlot) -> {
                                so.setMinUpdate(so.getMin() + 10);

                                g.minOpen(player);
                            }, (player, pressedSlot) -> {
                                if (so.getMin() > 9) {
                                    so.setMinUpdate(so.getMin() - 10);

                                    g.minOpen(player);
                                }
                            }, loreMin);

                            g.createGuiItem(new ItemStack(Material.BLAZE_ROD), 15, "&eMaximum", (player, pressedSlot) -> {
                                so.setMaxUpdate(so.getMax() + 1);

                                g.minOpen(player);
                            }, (player, pressedSlot) -> {
                                if (so.getMax() > 0) {
                                    so.setMaxUpdate(so.getMax() - 1);

                                    g.minOpen(player);
                                }
                            }, (player, pressedSlot) -> {
                                so.setMaxUpdate(so.getMax() + 10);

                                g.minOpen(player);
                            }, (player, pressedSlot) -> {
                                if (so.getMax() > 9) {
                                    so.setMaxUpdate(so.getMax() - 10);

                                    g.minOpen(player);
                                }
                            }, loreMax);

                            g.createGuiItem(new ItemStack(Material.CARPET), 0, "&cGo Back", ((player, pressedSlot) -> {
                                GlobalGui globalGui = guis.get(setting);
                                if (globalGui == null) return;

                                globalGui.minOpen(player);
                            }));

                            g.setBackGroundColor(15);
                        });
                    }

                    gameRandomizerGuis.add(g);

                    if (setting.getEmpty().contains(index)) {
                        gui.setBackGroundColorSlot(theme, slot);
                        index++;
                        slot += 1;
                        if ((slot - 8) % 9 == 0) slot += 2;
                    }

                    gui.createGuiItem(so.getItem(), slot, "&c&l" + so.getName(), (player, pressedSlot) -> {
                        if (so.getObject() instanceof Boolean) {
                            so.setObjectUpdate(!so.isBooleanValue());
                            player.sendMessage(Color.code(message.replace("%setting%", so.getName()).replace("%value%", so.isBooleanValue() + "")));
                        }

                        if (so.getObject() instanceof Integer) {
                            so.setObjectUpdate((int) Math.max(so.getOriginalIntValue() + so.getUpdateBy(), 0));
                            player.sendMessage(Color.code(message.replace("%setting%", so.getName()).replace("%value%", so.getOriginalIntValue() + "")));
                        }

                        if (so.getObject() instanceof Double) {
                            so.setObjectUpdate(Math.max(so.getOriginalDouble() + so.getUpdateBy(), 0));
                            player.sendMessage(Color.code(message.replace("%setting%", so.getName()).replace("%value%", MathUtils.stringOfDouble(so.getOriginalDouble(), 3) + "")));
                        }

                        gui.minOpen(player);
                    }, (player, pressedSlot) -> {
                        if (so.getObject() instanceof Boolean) {
                            so.setObjectUpdate(!so.isBooleanValue());
                            player.sendMessage(Color.code(message.replace("%setting%", so.getName()).replace("%value%", so.isBooleanValue() + "")));
                        }

                        if (so.getObject() instanceof Integer) {
                            so.setObjectUpdate((int) Math.max(so.getOriginalIntValue() - so.getUpdateBy(), 0));
                            player.sendMessage(Color.code(message.replace("%setting%", so.getName()).replace("%value%", so.getOriginalIntValue() + "")));
                        }

                        if (so.getObject() instanceof Double) {
                            so.setObjectUpdate(Math.max(so.getOriginalDouble() - so.getUpdateBy(), 0));
                            player.sendMessage(Color.code(message.replace("%setting%", so.getName()).replace("%value%", MathUtils.stringOfDouble(so.getOriginalDouble(), 3) + "")));
                        }

                        gui.minOpen(player);
                    }, (player, pressedSlot) -> {
                        if (so.getObject() instanceof Boolean) {
                            return;
                        }

                        if (so.getObject() instanceof Integer) {
                            so.setRandomizerUpdate((int) Math.max(so.getRandomizerInt() + so.getUpdateBy(), 0));
                            player.sendMessage(Color.code(message.replace("%setting%", so.getName() + " Randomizer").replace("%value%", so.getRandomizerInt() + "")));
                        }

                        if (so.getObject() instanceof Double) {
                            so.setRandomizerUpdate(Math.max(so.getRandomizer() + so.getUpdateBy(), 0));
                            player.sendMessage(Color.code(message.replace("%setting%", so.getName() + " Randomizer").replace("%value%", so.getRandomizer() + "")));
                        }

                        gui.minOpen(player);
                    }, (player, pressedSlot) -> {
                        if (so.getObject() instanceof Boolean) {
                            return;
                        }

                        if (so.getObject() instanceof Integer) {
                            so.setRandomizerUpdate((int) Math.max(so.getRandomizerInt() - so.getUpdateBy(), 0));
                            player.sendMessage(Color.code(message.replace("%setting%", so.getName() + " Randomizer").replace("%value%", so.getRandomizerInt() + "")));
                        }

                        if (so.getObject() instanceof Double) {
                            so.setRandomizerUpdate(Math.max(so.getRandomizer() - so.getUpdateBy(), 0));
                            player.sendMessage(Color.code(message.replace("%setting%", so.getName() + " Randomizer").replace("%value%", so.getRandomizer() + "")));
                        }

                        gui.minOpen(player);
                    }, (player, pressedSlot) -> {
                        if (manager.isGameRandomizer()) {
                            for (GlobalGui menu : gameRandomizerGuis) {
                                if (menu.getTitle().contains(so.getName())) {
                                    menu.minOpen(player);
                                }
                            }
                        }
                    }, so.getDescription());

                    index++;
                    slot += 1;
                    if ((slot - 8) % 9 == 0) slot += 2;
                }

                gui.createGuiItem(new ItemStack(Material.CARPET), 0, "&cPrevious Page", mainGui::minOpen);

                gui.setBackGroundColor(theme);
            });
            guis.put(setting, gui);
        });
    }

    public void createMain(PlayerGui profiles, PresetType type) {
        mainGui.setGui(() -> {
            mainGui.reset();
            int slot = 0;

            for (Setting setting : manager.getSettings()) {
                mainGui.createGuiItem(new ItemStack(setting.getMaterial(), 1, (short) setting.getId()), setting.getSlot(), setting.getFancyName(), (player, pressedSlot) -> {
                    GlobalGui gui = guis.get(setting);
                    if (gui == null) return;
                    gui.minOpen(player);
                }, setting.getDescription(), setting.isImportant());
                slot += 2;
            }

            //Name
            slot = (int) MathUtils.floor(manager.getSettings().size() * 2, 9, false) + 6 + 9;
            mainGui.createGuiItemUsingName(manager.getSkullItem("Xecivity", false), slot, "&cChange Bot Name", (player, pressedSlot) -> {
                DataPlayer data = SmartBotsBackend.getInstance().getDataManager().getPlayer(player);
                data.setChatUpdate((name) -> {
                    switch (type) {
                        case UNRANKED:
                        case RANKED:
                        case TOURNAMENT:
                            manager.setBotName(type, name);
                            break;
                        case DEFAULT:
                            manager.setBotName(SmartBotsBackend.getInstance().getDifficultiesSettings().getData(manager), name);
                            break;
                    }
                    player.sendMessage(Color.code("&aYou have changed the Bot Name to &e" + name + "&a."));

                    return true;
                });

                player.sendMessage(Color.code("&aType a valid player username."));
                player.sendMessage(Color.code("&7(You may find search around on https://namemc.com)"));
                player.closeInventory();
            });

            //Skin
            slot = (int) MathUtils.floor(manager.getSettings().size() * 2, 9, false) + 7 + 9;
            mainGui.createGuiItemUsingName(manager.getSkullItem("Xecivity", true), slot, "&cChange Bot Skin", (player, pressedSlot) -> {
                DataPlayer data = SmartBotsBackend.getInstance().getDataManager().getPlayer(player);
                data.setChatUpdate((name) -> {
                    switch (type) {
                        case UNRANKED:
                        case RANKED:
                        case TOURNAMENT:
                            manager.setSkin(type, name);
                            break;
                        case DEFAULT:
                            manager.setSkin(SmartBotsBackend.getInstance().getDifficultiesSettings().getData(manager), name);
                            break;
                    }
                    player.sendMessage(Color.code("&aYou have changed the Bot Skin to &e" + name + "&a."));

                    return true;
                });

                player.sendMessage(Color.code("&aType a valid player username."));
                player.sendMessage(Color.code("&7(You may find search around on https://namemc.com)"));
                player.closeInventory();
            });

            //Save
            slot = (int) MathUtils.floor(manager.getSettings().size() * 2, 9, false) + 1 + 9;
            mainGui.createGuiItem(new ItemStack(Material.WOOL, 1, (short) 5), slot, "&a&lSave", (player, pressedSlot) -> {
                DataPlayer data = SmartBotsBackend.getInstance().getDataManager().getPlayer(player);
                data.getGui().saveSettings();
                player.closeInventory();
                player.sendMessage(Color.code("&aYou have successfully saved your settings."));
            });

            if (profiles != null) {
                //Load
                slot = (int) MathUtils.floor(manager.getSettings().size() * 2, 9, false) + 2 + 9;
                mainGui.createGuiItem(new ItemStack(Material.NAME_TAG, 1), slot, "&e&lLoad Presets", (player, pressedSlot) -> {
                    profiles.minOpen();
                });
            }

            //Reset
            slot = (int) MathUtils.floor(manager.getSettings().size() * 2, 9, false) + 4 + 9;
            mainGui.createGuiItem(new ItemStack(Material.WOOL, 1, (short) 14), slot, "&c&lReset", (player, pressedSlot) -> {
                DataPlayer data = SmartBotsBackend.getInstance().getDataManager().getPlayer(player);
                switch (type) {
                    case UNRANKED:
                    case RANKED:
                    case TOURNAMENT:
                        data.getGui().getManager().resetSettings(data, type);
                        break;
                    case DEFAULT:
                        data.getGui().getManager().resetSettings(data, SmartBotsBackend.getInstance().getDifficultiesSettings().getData(manager));
                        break;
                }
                player.sendMessage(Color.code("&aYou have successfully reset your settings."));
            });

            if (type == PresetType.UNRANKED) {
                mainGui.createGuiItem(new ItemStack(Material.NOTE_BLOCK), 31, "&b&lGame Randomizer", (player, pressedSlot) -> {
                    manager.setGameRandomizer(!manager.isGameRandomizer());
                    player.sendMessage(Color.code("&aGame randomizer toggled to: " + manager.isGameRandomizer()));

                    mainGui.minOpen(player);
                }, "&7Current state: " + manager.isGameRandomizer());
            }
            if (type == PresetType.RANKED) {
                mainGui.createGuiItem(new ItemStack(Material.ENCHANTMENT_TABLE), 31, "&4&lELO", (player, pressedSlot) -> {
                    DataPlayer dataPlayer = SmartBotsBackend.getInstance().getDataManager().getPlayer(player);
                    dataPlayer.setChatUpdate(typed -> {
                        try {
                            int number = Integer.parseInt(typed);
                            manager.setElo(number);
                            // Updated
                            player.sendMessage(Color.code("&aELO Changed to: " + typed));
                            return true;
                        } catch (Exception e) {
                            player.sendMessage(Color.code("&cYou should type a number"));
                        }
                        return false;
                    });
                    player.sendMessage(Color.code("&aType the amount of ELO you want: "));
                    player.closeInventory();
                }, "&cCurrent ELO: " + manager.getElo());
            }
            if (type == PresetType.TOURNAMENT) {
                mainGui.createGuiItem(new ItemStack(Material.ANVIL), 31, "&c&lTier", (player, pressedSlot) -> {
                    DataPlayer dataPlayer = SmartBotsBackend.getInstance().getDataManager().getPlayer(player);
                    dataPlayer.setChatUpdate(typed -> {
                        try {
                            int number = Integer.parseInt(typed);
                            manager.setTier(number);
                            // Updated
                            player.sendMessage(Color.code("&aTier changed to: " + typed));
                            return true;
                        } catch (Exception e) {
                            player.sendMessage(Color.code("&cYou should type a number"));
                        }
                        return false;
                    });
                    player.sendMessage(Color.code("&aType the tier number: "));
                    player.closeInventory();
                }, "&7Current tier: " + manager.getTier());
            }

            mainGui.setBackGroundColor(theme);
        });
    }
}
