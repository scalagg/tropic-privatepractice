package land.esta.plugin.ssb.bots.setting;

import land.esta.plugin.ssb.SmartBotsBackend;
import land.esta.plugin.ssb.bot.profile.BotSettingProfile;
import land.esta.plugin.ssb.bot.profile.BotSettingProfileService;
import land.esta.plugin.ssb.bot.setting.BotSetting;
import land.esta.plugin.ssb.bot.setting.BotSettingService;
import land.esta.plugin.ssb.bots.setting.impl.*;
import lombok.Getter;
import lombok.Setter;
import land.esta.plugin.ssb.data.DataPlayer;
import land.esta.plugin.ssb.data.presets.PresetType;
import land.esta.plugin.ssb.utils.Color;
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
    private ArrayList<Setting> settings;


    private String name = "default";
    private String foundPath;
    private int elo = 0;
    private int tier = 1;
    private boolean gameRandomizer = false;

    private String botName = "&cBot"; //The default would be your self
    private String skin = "Xecivity"; //The default would be your self

    private List<String> description = new ArrayList<>();
    public static SettingManager defaultManager = new SettingManager();

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
                BotSettingProfile profile = BotSettingProfileService.INSTANCE.findOrCreate(data.getPlayer().getUniqueId());
                BotSetting defaultProfile = profile.getSettings().get("default");

                // if they dont even got a fucking profile no need to reset
                if (defaultProfile != null) {
                    defaultProfile.resetWithDefaults();
                    defaultProfile.save();

                    profile.getSettings().put("default", defaultProfile);
                    profile.save();
                }

                data.setUpdatingGui(false);
            }
        }.runTaskAsynchronously(SmartBotsBackend.getInstance());
    }

    public void resetSettings(DataPlayer data, String difficulty) {
        new BukkitRunnable() {
            @Override
            public void run() {
                data.setUpdatingGui(true);
                BotSetting difficultyProfile = BotSettingService.INSTANCE.getOrCreate(difficulty);

                difficultyProfile.resetWithDefaults();
                difficultyProfile.save();

                data.setUpdatingGui(false);
            }
        }.runTaskAsynchronously(SmartBotsBackend.getInstance());
    }

    public void resetSettings(DataPlayer data, PresetType presetType) {
        new BukkitRunnable() {
            @Override
            public void run() {
                data.setUpdatingGui(true);
                BotSetting presetProfile = BotSettingService.INSTANCE.getOrCreate(presetType.name());
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

                presetProfile.resetWithDefaults();
                presetProfile.save();

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
                        settingObject.setObject(sb.getObject());
                    });
                });
            }
        }.runTaskAsynchronously(SmartBotsBackend.getInstance());
    }


    public static SettingManager loadSettings(UUID uuid) {
        SettingManager settingManager = new SettingManager();
        BotSettingProfile profile = BotSettingProfileService.INSTANCE.findOrCreate(uuid);

        String botName = profile.getName();
        String botSkin = profile.getSkin();

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
            setting.getSettings().forEach(object -> {
                SettingObject savedSetting = profile.getSettings().get("default").getSettings().get(setting.getName()).get(object.getName());

                if (savedSetting == null) {
                    savedSetting = (SettingObject) defaultManager.getSettingByName(setting.getFancyName()).getSettingByName(object.getName()).getObject();
                }

                object.setObject(savedSetting);
            });
        });

        return settingManager;
    }

    public static SettingManager loadSettings(String difficulty) {
        SettingManager settingManager = new SettingManager();
        BotSetting settings = BotSettingService.INSTANCE.getOrCreate(difficulty);

        String botName = settings.getName();
        String botSkin = settings.getSkin();

        if (botName.equalsIgnoreCase("&")) {
            settingManager.setBotNameWithoutUpdate(Color.code(botName));
        } else {
            settingManager.setBotNameWithoutUpdate(botName);
        }

        settingManager.setSkinWithoutUpdate(botSkin);

        settingManager.getSettings().forEach(setting -> {
            setting.getSettings().forEach(object -> {
                SettingObject savedSetting = settings.getSettings().get(setting.getName()).get(object.getName());

                if (savedSetting == null) {
                    savedSetting = (SettingObject) defaultManager.getSettingByName(setting.getFancyName()).getSettingByName(object.getName()).getObject();
                }

                object.setObject(savedSetting);
            });
        });

        return settingManager;
    }

    public static SettingManager loadSettings(PresetType presetType, String presetName) {
        SettingManager settingManager = new SettingManager();
        BotSetting settings = BotSettingService.INSTANCE.getOrCreate(presetName);

        String botName = settings.getName();
        String botSkin = settings.getSkin();

        if (botName.equalsIgnoreCase("&")) {
            settingManager.setBotNameWithoutUpdate(Color.code(botName));
        } else {
            settingManager.setBotNameWithoutUpdate(botName);
        }

        settingManager.setSkinWithoutUpdate(botSkin);

        settingManager.getSettings().forEach(setting -> {
            setting.getSettings().forEach(object -> {
                SettingObject savedSetting = settings.getSettings().get(setting.getName()).get(object.getName());

                if (savedSetting == null) {
                    savedSetting = (SettingObject) defaultManager.getSettingByName(setting.getFancyName()).getSettingByName(object.getName()).getObject();
                }

                object.setObject(savedSetting);
            });
        });


        return settingManager;
    }

    public void setBotName(UUID uuid, String botName) {
        this.botName = botName;
        BotSettingProfile playerProfile = BotSettingProfileService.INSTANCE.findOrCreate(uuid);

        playerProfile.setName(botName);
        playerProfile.save();
    }

    public void setBotName(String difficulty, String botName) {
        this.botName = botName;
        BotSetting botSetting = BotSettingService.INSTANCE.getOrCreate(difficulty);

        botSetting.setName(botName);
        botSetting.save();
    }

    public void setBotName(PresetType presetType, String botName) {
        this.botName = botName;
        BotSetting botSetting = BotSettingService.INSTANCE.getOrCreate(presetType.name());

        botSetting.setSkin(botName);
        botSetting.save();
    }

    //We also want to update to config in those 2 methods
    public void setSkin(UUID uuid, String skin) {
        this.skin = skin;
        BotSettingProfile playerProfile = BotSettingProfileService.INSTANCE.findOrCreate(uuid);

        playerProfile.setSkin(skin);
        playerProfile.save();
    }

    public void setSkin(String difficulty, String skin) {
        this.skin = skin;
        BotSetting botSetting = BotSettingService.INSTANCE.getOrCreate(difficulty);

        botSetting.setSkin(skin);
        botSetting.save();
    }

    public void setSkin(PresetType presetType, String skin) {
        this.skin = skin;
        BotSetting botSetting = BotSettingService.INSTANCE.getOrCreate(presetType.name());

        botSetting.setSkin(skin);
        botSetting.save();
    }

    public void setGameRandomizer(boolean gameRandomizer) {
        this.gameRandomizer = gameRandomizer;
        BotSetting botSetting = BotSettingService.INSTANCE.getOrCreate("UNRANKED");

        botSetting.setRandomizer(gameRandomizer);
        botSetting.save();
    }

    public void setElo(int elo) {
        this.elo = elo;
        BotSetting botSetting = BotSettingService.INSTANCE.getOrCreate("RANKED");

        botSetting.setElo(elo);
        botSetting.save();
    }

    public void setTier(int tier) {
        this.tier = tier;
        BotSetting botSetting = BotSettingService.INSTANCE.getOrCreate("TOURNAMENT");

        botSetting.setTier(tier);
        botSetting.save();
    }

    public void setSkinWithoutUpdate(String skin) {
        this.skin = skin;
    }

    public void setBotNameWithoutUpdate(String botName) {
        this.botName = botName;
    }
}
