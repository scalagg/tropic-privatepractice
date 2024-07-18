package land.esta.plugin.ssb.bots.setting;

import land.esta.plugin.ssb.SmartBotsBackend;
import lombok.Getter;
import lombok.Setter;
import land.esta.plugin.ssb.data.DataPlayer;
import land.esta.plugin.ssb.data.presets.PresetType;
import land.esta.plugin.ssb.utils.Color;
import land.esta.plugin.ssb.utils.FileManager;
import land.esta.plugin.ssb.utils.config.ConfigManager;
import land.esta.plugin.ssb.utils.db.MongoUtil;
import land.esta.plugin.ssb.utils.db.SettingType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class SettingManager {

    private AttackSettings clickSetting;
    private ArrayList<Setting> settings;
    private RotationSetting rotationSetting;
    private ConnectionSettings connectionSettings;
    private FleeSettings fleeSettings;
    private VelocitySettings velocitySettings;
    private PotionSetting potionSetting;
    private EatingSettings eatingSettings;
    private StrafeSettings strafeSettings;
    private PearlSettings pearlSettings;
    private PacketsSettings packetsSettings;
    private PlaystyleSettings playstyleSettings;
    private RodSettings rodSettings;
    private WTapSetting wTapSetting;
    private String name = "default";
    private String foundPath;
    private int elo = 0;
    private int tier = 1;
    private boolean gameRandomizer = false;

    private String botName = "&cBot"; //The default would be your self
    private String skin = "Xecivity"; //The default would be your self

    private List<String> description = new ArrayList<>();
    static SettingManager defaultManager = new SettingManager();

    public SettingManager() {
        settings = new ArrayList<>();

        settings.add(clickSetting = new AttackSettings());
        settings.add(velocitySettings = new VelocitySettings());
        settings.add(playstyleSettings = new PlaystyleSettings());
        settings.add(rotationSetting = new RotationSetting());
        settings.add(potionSetting = new PotionSetting());
        settings.add(fleeSettings = new FleeSettings());
        settings.add(eatingSettings = new EatingSettings());
        settings.add(strafeSettings = new StrafeSettings());
        settings.add(pearlSettings = new PearlSettings());
        settings.add(connectionSettings = new ConnectionSettings());
        packetsSettings = new PacketsSettings();
        settings.add(wTapSetting = new WTapSetting());
        settings.add(rodSettings = new RodSettings());
    }

    public ItemStack getSkullItem(String player,boolean skin) {
        ItemStack stack = new ItemStack(Material.SKULL_ITEM,1,(short) 3);
        SkullMeta meta = (SkullMeta) stack.getItemMeta();

        if (skin) meta.setOwner(this.skin.replace("%player%",player)); //Skin skull
        if (!skin) meta.setOwner(this.botName.replace("%player%",player)); //Name skull

        stack.setItemMeta(meta);

        return stack.clone();
    }

    public Setting getSettingByName(String name) {
        for (Setting setting : settings) {
            if (setting.getFancyName().equals(name)) return setting;
        }
        return null;
    }

    public Setting getSettingByNameRemoveColor(String name) {
        for (Setting setting : settings) {
            if (ChatColor.stripColor(name).equals(name)) return setting;
        }
        return null;
    }

    public void resetSettings(DataPlayer data) {
        new BukkitRunnable() {
            @Override
            public void run() {
                data.setUpdatingGui(true);
                SettingManager defaults = new SettingManager();

                setBotName(data.getPlayer().getUniqueId(), defaults.getBotName());
                setSkin(data.getPlayer().getUniqueId(), defaults.getSkin());

                settings.forEach(setting -> {
                    setting.getSettings().forEach(settingObject -> {
                        SettingObject sb = defaults.getSettingByName(setting.getFancyName()).getSettingByName(settingObject.getName());
                        settingObject.setObjectUpdate(sb.getObject());
                        if (sb.getObject() instanceof Integer || sb.getObject() instanceof Double) {
                            settingObject.setRandomizerUpdate(sb.getRandomizer());
                        }
                    });
                });
                data.setUpdatingGui(false);
            }
        }.runTaskAsynchronously(SmartBotsBackend.getInstance());
    }

    public void resetSettings(DataPlayer data, String difficulty) {
        new BukkitRunnable() {
            @Override
            public void run() {
                data.setUpdatingGui(true);
                SettingManager defaults = new SettingManager();

                setBotName(difficulty, "Bot");
                setSkin(difficulty, "Xecivity");

                settings.forEach(setting -> {
                    setting.getSettings().forEach(settingObject -> {
                        SettingObject sb = defaults.getSettingByName(setting.getFancyName()).getSettingByName(settingObject.getName());
                        settingObject.setObjectUpdate(sb.getObject());
                        if (sb.getObject() instanceof Integer || sb.getObject() instanceof Double) {
                            settingObject.setRandomizerUpdate(sb.getRandomizer());
                        }
                    });
                });
                data.setUpdatingGui(false);
            }
        }.runTaskAsynchronously(SmartBotsBackend.getInstance());
    }

    public void resetSettings(DataPlayer data, PresetType presetType) {
        new BukkitRunnable() {
            @Override
            public void run() {
                data.setUpdatingGui(true);
                SettingManager defaults = new SettingManager();

                setBotName(presetType, "Bot");
                setSkin(presetType, "Xecivity");

                switch (presetType) {
                    case UNRANKED:
                        setGameRandomizer(defaults.isGameRandomizer());
                        break;
                    case RANKED:
                        setElo(defaults.getElo());
                        break;
                    case TOURNAMENT:
                        setTier(defaults.getTier());
                        break;
                }

                settings.forEach(setting -> {
                    setting.getSettings().forEach(settingObject -> {
                        SettingObject sb = defaults.getSettingByName(setting.getFancyName()).getSettingByName(settingObject.getName());
                        settingObject.setObjectUpdate(sb.getObject());
                        if (sb.getObject() instanceof  Integer || sb.getObject() instanceof Double) {
                            settingObject.setRandomizerUpdate(sb.getRandomizer());

                            if (presetType == PresetType.UNRANKED) {
                                settingObject.setMinUpdate(sb.getMin());
                                settingObject.setMaxUpdate(sb.getMax());
                            }
                        }
                    });
                });
                data.setUpdatingGui(false);
            }
        }.runTaskAsynchronously(SmartBotsBackend.getInstance());
    }

    public void update(SettingManager updated) {
        new BukkitRunnable() {
            @Override
            public void run() {
                settings.forEach(setting -> {
                    setting.getSettings().forEach(settingObject -> {
                        SettingObject sb = updated.getSettingByName(setting.getFancyName()).getSettingByName(settingObject.getName());
                        settingObject.setObjectUpdate(sb.getObject());
                    });
                });
            }
        }.runTaskAsynchronously(SmartBotsBackend.getInstance());
    }

    /*
    public void saveSettings(String path,JavaPlugin plugin) {
        if (!SmartBotsBackend.getInstance().getMongo().isEnabled()) {
            // Config loading

            settings.forEach(setting -> {
                setting.getSettings().forEach(sb -> {
                    String p = path + "." + setting.getFancyName() + "." + sb.getName();
                    plugin.getConfig().set(p + ".value", sb.getObject());
                    plugin.getConfig().set(p + ".randomizer", sb.getRandomizer());
                    plugin.saveConfig();
                });
            });
        } else {
            // Sql Loading

            settings.forEach(setting -> {
                setting.getSettings().forEach(sb -> {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            mongoSetting.updateValue(setting.getName(),sb.getName(),sb.getObject(),false);
                            mongoSetting.updateValue(setting.getName(),sb.getName(),sb.getRandomizer(),true);
                            mongoSetting.update();
                        }
                    }.runTaskAsynchronously(SmartBotsBackend.getInstance());
                });
            });
        }
    } */

    public void saveSettings(UUID uuid) {
        settings.forEach(setting -> {
            setting.getSettings().forEach(sb -> {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        MongoUtil.updateSetting(uuid, setting, sb, SettingType.VALUE, sb.getObject());
                        MongoUtil.updateSetting(uuid, setting, sb, SettingType.RANDOMIZER, sb.getRandomizer());
                    }
                }.runTaskAsynchronously(SmartBotsBackend.getInstance());
            });
        });
    }

    public void saveSettings(String difficulty) {
        settings.forEach(setting -> {
            setting.getSettings().forEach(sb -> {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        MongoUtil.updateSetting(difficulty, setting, sb, SettingType.VALUE, sb.getObject());
                        MongoUtil.updateSetting(difficulty, setting, sb, SettingType.RANDOMIZER, sb.getObject());
                    }
                }.runTaskAsynchronously(SmartBotsBackend.getInstance());
            });
        });
    }

    public void saveSettings(PresetType presetType, String presetName) {
        settings.forEach(setting -> {
            setting.getSettings().forEach(sb -> {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        MongoUtil.updateSetting(presetType, presetName, setting, sb, SettingType.VALUE, sb.getObject());
                        if (sb.getObject() instanceof Double || sb.getObject() instanceof  Integer) {
                            MongoUtil.updateSetting(presetType, presetName, setting, sb, SettingType.RANDOMIZER, sb.getRandomizer());
                            if (presetType == PresetType.UNRANKED) {
                                MongoUtil.updateSetting(presetType, presetName, setting, sb, SettingType.MIN, Double.toString(sb.getMin()));
                                MongoUtil.updateSetting(presetType, presetName, setting, sb, SettingType.MAX, Double.toString(sb.getMax()));
                            }
                        }
                    }
                }.runTaskAsynchronously(SmartBotsBackend.getInstance());
            });
        });
    }

    public static SettingManager loadSettings(UUID uuid) {
        SettingManager settingManager = new SettingManager();

        String botName;
        String botSkin;

        botName = (String) SmartBotsBackend.getInstance().getMongoHandler().findField(uuid, "botName");
        botSkin = (String) SmartBotsBackend.getInstance().getMongoHandler().findField(uuid, "botSkin");

        if (botName == null) {
            botName = defaultManager.getBotName();
        }

        if (botSkin == null) {
            botSkin = defaultManager.getSkin();
        }

        if (botName.equalsIgnoreCase("&")) {
            settingManager.setBotNameWithoutUpdate(Color.code(botName));
        } else {
            settingManager.setBotNameWithoutUpdate(botName);
        }
        settingManager.setSkinWithoutUpdate(botSkin);

        settingManager.getSettings().forEach(setting -> {
            setting.getSettings().forEach(sb -> {
                //Defaults
                Object o = MongoUtil.getSetting(uuid, setting, sb, SettingType.VALUE);

                if (o == null) {
                    //Bukkit.broadcastMessage("Using NULL");
                    o = defaultManager.getSettingByName(setting.getFancyName()).getSettingByName(sb.getName()).getObject();
                }

                Double r = null;

                if (o instanceof Double || o instanceof Integer) {
                    r = (Double) MongoUtil.getSetting(uuid, setting, sb,  SettingType.RANDOMIZER);

                    if (r == null) {
                        r = defaultManager.getSettingByName(setting.getFancyName()).getSettingByName(sb.getName()).getRandomizer();
                    }
                }

                //Update
                Object finalO = o;
                sb.setUpdate(() -> {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            //Bukkit.broadcastMessage("Update first, Value - " + finalO);
                            MongoUtil.updateSetting(uuid, setting, sb, SettingType.VALUE, sb.getObject());
                            if (finalO instanceof Double || finalO instanceof Integer) {
                                MongoUtil.updateSetting(uuid, setting, sb, SettingType.RANDOMIZER, sb.getRandomizer());
                            }
                        }
                    }.runTaskAsynchronously(SmartBotsBackend.getInstance());
                });

                //Set
                sb.setObject(o);
                if (o instanceof Integer || o instanceof Double) {
                    sb.setRandomizer(r);
                }
            });
        });

        return settingManager;
    }

    public static SettingManager loadSettings(String difficulty) {
        SettingManager settingManager = new SettingManager();

        String botName;
        String botSkin;

        botName = (String) SmartBotsBackend.getInstance().getMongoHandler().findField(difficulty, "botName");
        botSkin = (String) SmartBotsBackend.getInstance().getMongoHandler().findField(difficulty, "botSkin");

        if (botName == null) {
            botName = defaultManager.getBotName();
        }

        if (botSkin == null) {
            botSkin = defaultManager.getSkin();
        }

        if (botName.equalsIgnoreCase("&")) {
            settingManager.setBotNameWithoutUpdate(Color.code(botName));
        } else {
            settingManager.setBotNameWithoutUpdate(botName);
        }
        settingManager.setSkinWithoutUpdate(botSkin);

        settingManager.getSettings().forEach(setting -> {
            setting.getSettings().forEach(sb -> {
                //Defaults
                Object o = MongoUtil.getSetting(difficulty, setting, sb, SettingType.VALUE);

                if (o == null) {
                    o = defaultManager.getSettingByName(setting.getFancyName()).getSettingByName(sb.getName()).getObject();
                }

                Double r = null;
                if (o instanceof Double || o instanceof Integer) {
                    r = (Double) MongoUtil.getSetting(difficulty, setting, sb,  SettingType.RANDOMIZER);

                    if (r == null) {
                        r = defaultManager.getSettingByName(setting.getFancyName()).getSettingByName(sb.getName()).getRandomizer();
                    }
                }

                //Update
                Object finalO = o;
                sb.setUpdate(() -> {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            MongoUtil.updateSetting(difficulty, setting, sb, SettingType.VALUE, sb.getObject());
                            if (finalO instanceof Double || finalO instanceof Integer) {
                                MongoUtil.updateSetting(difficulty, setting, sb, SettingType.RANDOMIZER, sb.getRandomizer());
                            }
                        }
                    }.runTaskAsynchronously(SmartBotsBackend.getInstance());
                });

                //Set
                sb.setObject(o);
                if (o instanceof Integer || o instanceof Double) {
                    sb.setRandomizer(r);
                }
            });
        });

        return settingManager;
    }

    public static SettingManager loadSettings(PresetType presetType, String presetName) {
        SettingManager settingManager = new SettingManager();

        String botName;
        String botSkin;

        settingManager.setName(presetName);

        switch (presetType) {
            case UNRANKED:
                boolean randomizer = false;

                try {
                    randomizer = Boolean.parseBoolean(SmartBotsBackend.getInstance().getMongoHandler().findField(PresetType.UNRANKED, presetName, "gameRandomizer") + "");
                } catch (Exception ignore) { }

                settingManager.setGameRandomizer(randomizer);
                break;
            case RANKED:
                int elo = defaultManager.getElo();

                try {
                    elo = (Integer) SmartBotsBackend.getInstance().getMongoHandler().findField(PresetType.RANKED, presetName, "elo");
                } catch (Exception ignore) { }
                settingManager.setElo(elo);
                break;
            case TOURNAMENT:
                int tier = defaultManager.getTier();

                try {
                    tier = (Integer) SmartBotsBackend.getInstance().getMongoHandler().findField(PresetType.TOURNAMENT, presetName, "tier");
                } catch (Exception ignore) { }
                settingManager.setTier(tier);
                break;
        }

        botName = (String) SmartBotsBackend.getInstance().getMongoHandler().findField(presetType, presetName, "botName");
        botSkin = (String) SmartBotsBackend.getInstance().getMongoHandler().findField(presetType, presetName, "botSkin");

        if (botName == null) {
            botName = defaultManager.getBotName();
        }

        if (botSkin == null) {
            botSkin = defaultManager.getSkin();
        }

        if (botName.equalsIgnoreCase("&")) {
            settingManager.setBotNameWithoutUpdate(Color.code(botName));
        } else {
            settingManager.setBotNameWithoutUpdate(botName);
        }
        settingManager.setSkinWithoutUpdate(botSkin);

        settingManager.getSettings().forEach(setting -> {
            setting.getSettings().forEach(sb -> {
                //Defaults
                Object o = MongoUtil.getSetting(presetType, presetName, setting, sb,  SettingType.VALUE);

                if (o == null) {
                    o = defaultManager.getSettingByName(setting.getFancyName()).getSettingByName(sb.getName()).getObject();
                }

                Double r = null;
                String min = null;
                String max = null;
                if (o instanceof Double || o instanceof Integer) {
                    r = (Double) MongoUtil.getSetting(presetType, presetName, setting, sb,  SettingType.RANDOMIZER);

                    if (r == null) {
                        r = defaultManager.getSettingByName(setting.getFancyName()).getSettingByName(sb.getName()).getRandomizer();
                    }

                    if (presetType == PresetType.UNRANKED) {
                        min = (String) MongoUtil.getSetting(presetType, presetName, setting, sb,  SettingType.MIN);
                        max = (String) MongoUtil.getSetting(presetType, presetName, setting, sb,  SettingType.MAX);

                        if (min == null) {
                            min = String.valueOf(defaultManager.getSettingByName(setting.getFancyName()).getSettingByName(sb.getName()).getMin());
                        }

                        if (max == null) {
                            max = String.valueOf(defaultManager.getSettingByName(setting.getFancyName()).getSettingByName(sb.getName()).getMax());
                        }
                    }
                }

                //Update
                sb.setUpdate(() -> {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            MongoUtil.updateSetting(presetType, presetName, setting, sb, SettingType.VALUE, sb.getObject());
                            if (sb.getObject() instanceof Double || sb.getObject() instanceof Integer) {
                                MongoUtil.updateSetting(presetType, presetName, setting, sb, SettingType.RANDOMIZER, sb.getRandomizer());
                                if (presetType == PresetType.UNRANKED) {
                                    MongoUtil.updateSetting(presetType, presetName, setting, sb, SettingType.MIN, Double.toString(sb.getMin()));
                                    MongoUtil.updateSetting(presetType, presetName, setting, sb, SettingType.MAX, Double.toString(sb.getMax()));
                                }
                            }
                        }
                    }.runTaskAsynchronously(SmartBotsBackend.getInstance());
                });

                double m = sb.getMin();
                double ma = sb.getMax();

                try {
                    if (min != null) {
                        m = Double.parseDouble(min);
                    }
                } catch (Exception ignore) { }

                try {
                    if (max != null) {
                        ma = Double.parseDouble(max);
                    }
                } catch (Exception ignore) { }

                //Set
                sb.setObject(o);
                if (o instanceof Integer || o instanceof Double) {
                    sb.setRandomizer(r);
                }
                sb.setMin(m);
                sb.setMax(ma);
            });
        });

        return settingManager;
    }

    public void setBotName(UUID uuid, String botName) {
        this.botName = botName;

        if (!SmartBotsBackend.getInstance().getMongo().isEnabled()) {
            //Config updating
            FileManager.update(ConfigManager.data, foundPath + ".BotName", botName);
        } else {
            //SQl updating
            SmartBotsBackend.getInstance().getMongoHandler().updateValue(uuid, "botName", botName);
        }
    }

    public void setBotName(String difficulty, String botName) {
        this.botName = botName;

        if (!SmartBotsBackend.getInstance().getMongo().isEnabled()) {
            //Config updating
            FileManager.update(ConfigManager.data, foundPath + ".BotName", botName);
        } else {
            //SQl updating
            SmartBotsBackend.getInstance().getMongoHandler().updateValue(difficulty, "botName", botName);
        }
    }

    public void setBotName(PresetType presetType, String botName) {
        this.botName = botName;

        if (!SmartBotsBackend.getInstance().getMongo().isEnabled()) {
            //Config updating
            FileManager.update(ConfigManager.data, foundPath + ".BotName", botName);
        } else {
            //SQl updating
            SmartBotsBackend.getInstance().getMongoHandler().updateValue(presetType, name, "botName", botName);
        }
    }

    //We also want to update to config in those 2 methods
    public void setSkin(UUID uuid, String skin) {
        this.skin = skin;

        if (!SmartBotsBackend.getInstance().getMongo().isEnabled()) {
            //Config updating
            FileManager.update(ConfigManager.data, foundPath + ".BotSkin", skin);
        } else {
            //SQl updating
            SmartBotsBackend.getInstance().getMongoHandler().updateValue(uuid, "botSkin", skin);
        }
    }

    public void setSkin(String difficulty, String skin) {
        this.skin = skin;

        if (!SmartBotsBackend.getInstance().getMongo().isEnabled()) {
            //Config updating
            FileManager.update(ConfigManager.data, foundPath + ".BotSkin", skin);
        } else {
            //SQl updating
            SmartBotsBackend.getInstance().getMongoHandler().updateValue(difficulty, "botSkin", skin);
        }
    }

    public void setSkin(PresetType presetType, String skin) {
        this.skin = skin;

        if (!SmartBotsBackend.getInstance().getMongo().isEnabled()) {
            //Config updating
            FileManager.update(ConfigManager.data, foundPath + ".BotSkin", skin);
        } else {
            //SQl updating
            SmartBotsBackend.getInstance().getMongoHandler().updateValue(presetType, name, "botSkin", skin);
        }
    }

    public void setGameRandomizer(boolean gameRandomizer) {
        this.gameRandomizer = gameRandomizer;
        SmartBotsBackend.getInstance().getMongoHandler().updateValue(PresetType.UNRANKED, name, "gameRandomizer", gameRandomizer);
    }

    public void setElo(int elo) {
        this.elo = elo;
        SmartBotsBackend.getInstance().getMongoHandler().updateValue(PresetType.RANKED, name, "elo", elo);
    }

    public void setTier(int tier) {
        this.tier = tier;
        SmartBotsBackend.getInstance().getMongoHandler().updateValue(PresetType.TOURNAMENT, name, "tier", tier);
    }

    public void setSkinWithoutUpdate(String skin) {
        this.skin = skin;
    }

    public void setBotNameWithoutUpdate(String botName) {
        this.botName = botName;
    }
}
