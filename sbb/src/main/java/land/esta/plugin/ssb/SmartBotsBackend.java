package land.esta.plugin.ssb;

import land.esta.plugin.ssb.utils.BukkitEvents;
import land.esta.plugin.ssb.utils.Color;
import land.esta.plugin.ssb.utils.Command;
import land.esta.plugin.ssb.utils.PacketListener;
import lombok.Getter;
import land.esta.plugin.ssb.botevents.BotEvent;
import land.esta.plugin.ssb.botevents.Event;
import land.esta.plugin.ssb.botevents.EventManager;
import land.esta.plugin.ssb.bots.Bot;
import land.esta.plugin.ssb.bots.BotManager;
import land.esta.plugin.ssb.data.DataManager;
import land.esta.plugin.ssb.data.DataPlayer;
import land.esta.plugin.ssb.data.presets.PresetManager;
import land.esta.plugin.ssb.features.FeatureManager;
import land.esta.plugin.ssb.support.SupportManager;
import land.esta.plugin.ssb.thread.ThreadSystem;
import land.esta.plugin.ssb.utils.config.ConfigManager;
import land.esta.plugin.ssb.utils.db.Mongo;
import land.esta.plugin.ssb.utils.db.MongoHandler;
import land.esta.plugin.ssb.utils.gui.imple.BotGui;
import land.esta.plugin.ssb.utils.settings.DifficultiesSettings;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class SmartBotsBackend extends JavaPlugin {

    @Getter
    private static SmartBotsBackend instance;

    @Getter
    DifficultiesSettings difficultiesSettings;

    @Getter
    private BotManager botManager;

    @Getter
    private DataManager dataManager;

    @Getter
    private EventManager eventManager;

    @Getter
    private FeatureManager featureManager;

    @Getter
    private String pluginName;

    @Getter
    private PresetManager presetManager;

    @Getter
    private BukkitEvents bukkitEvents;

    @Getter
    private SupportManager supportManager;

    @Getter
    private ThreadSystem threadSystem;

    @Getter
    private Mongo mongo = null;

    @Getter
    private MongoHandler mongoHandler;

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        pluginName = "OptimizedBotsV2";
        new ConfigManager(); //Loading the config manager
        saveDefaultConfig();
        loadSQL();
        botManager = new BotManager();
        eventManager = new EventManager();
        supportManager = new SupportManager();
        featureManager = new FeatureManager();
        presetManager = new PresetManager();
        bukkitEvents = new BukkitEvents();
        difficultiesSettings = new DifficultiesSettings();
        threadSystem = new ThreadSystem();
        botGuiRegister();

        registerListeners();

        Bukkit.getLogger().info(Color.code("&b&LLoading "+ pluginName +" &7by crafticat."));
    }

    public void loadSQL() {
        mongo = new Mongo();
        if (mongo.isEnabled()) {
//            try {
//                sqlManager.connect();
//                Bukkit.getLogger().info(Color.code("&aDatabase is connected"));
//            } catch (ClassNotFoundException | SQLException ignore) {
//                Bukkit.getLogger().info(Color.code("&cDatabase is not connected"));
//            }
            mongoHandler = new MongoHandler();
        }
    }

    public void botGuiRegister() {
        Command command = new Command("botgui","Bots setting gui","/botgui",null);

        command.setOnCommand((player, temp) -> {
            if (player.hasPermission("botgui.use")) {
                DataPlayer data = SmartBotsBackend.getInstance().dataManager.getPlayer(player);
                BotGui gui = data.getGui();
                if (gui.isLoaded()) {
                    gui.getMainGui().openGui();
                } else {
                    player.sendMessage(Color.code("&cError: The menu has not loaded, please try again later."));
                }
            } else {
                player.sendMessage(Color.code("&cYou need &3VIP &crank to use this."));
                player.sendMessage(Color.code("&cStore link: &estore.esta.land"));
            }
        });
    }

    public void registerListeners() {
        Bukkit.getServer().getPluginManager().registerEvents(new PacketListener(),this);
        Bukkit.getServer().getPluginManager().registerEvents(dataManager = new DataManager(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic

        for (Bot bot : getBotManager().getBots()) {
            bot.silentKill();
        }

        if (mongo != null) mongo.shutdown();

        instance = null;
    }

    public void callEvent(Event event) {
        for (BotEvent events : SmartBotsBackend.getInstance().getEventManager().getEvents()) {
            events.call(event);
        }
    }
}
