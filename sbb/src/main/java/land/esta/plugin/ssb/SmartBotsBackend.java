package land.esta.plugin.ssb;

import gg.scala.commons.ExtendedScalaPlugin;
import gg.scala.commons.annotations.container.ContainerDisable;
import gg.scala.commons.annotations.container.ContainerEnable;
import gg.scala.commons.core.plugin.*;
import land.esta.plugin.ssb.utils.BukkitEvents;
import land.esta.plugin.ssb.utils.PacketListener;
import lombok.Getter;
import land.esta.plugin.ssb.botevents.BotEvent;
import land.esta.plugin.ssb.botevents.Event;
import land.esta.plugin.ssb.botevents.EventManager;
import land.esta.plugin.ssb.bots.Bot;
import land.esta.plugin.ssb.bots.BotManager;
import land.esta.plugin.ssb.data.DataManager;
import land.esta.plugin.ssb.data.presets.PresetManager;
import land.esta.plugin.ssb.features.FeatureManager;
import land.esta.plugin.ssb.support.SupportManager;
import land.esta.plugin.ssb.thread.ThreadSystem;
import land.esta.plugin.ssb.utils.config.ConfigManager;
import land.esta.plugin.ssb.utils.db.Mongo;
import land.esta.plugin.ssb.utils.db.MongoHandler;
import land.esta.plugin.ssb.utils.settings.DifficultiesSettings;
import org.bukkit.Bukkit;

@Plugin(
        name = "EstaPractice-SmartBots",
        version = "%remote%/%branch%/%id%"
)
@PluginAuthorComposite({
        @PluginAuthor("crafticat"),
        @PluginAuthor("GrowlyX")
})
@PluginWebsite("https://esta.land")
@PluginDependencyComposite({
        @PluginDependency("scala-commons"),
        @PluginDependency("Lemon"),
        @PluginDependency("EstaPractice")
})
public final class SmartBotsBackend extends ExtendedScalaPlugin {

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

    @ContainerEnable
    public void containerEnable() {
        // Plugin startup logic
        instance = this;

        new ConfigManager(); //Loading the config manager
        loadMongo();

        botManager = new BotManager();
        eventManager = new EventManager();
        supportManager = new SupportManager();
        featureManager = new FeatureManager();
        presetManager = new PresetManager();
        bukkitEvents = new BukkitEvents();
        difficultiesSettings = new DifficultiesSettings();
        threadSystem = new ThreadSystem();

        registerListeners();
    }

    public void loadMongo() {
        mongo = new Mongo();
        mongoHandler = new MongoHandler();
    }

    public void registerListeners() {
        Bukkit.getServer().getPluginManager().registerEvents(new PacketListener(),this);
        Bukkit.getServer().getPluginManager().registerEvents(dataManager = new DataManager(), this);
    }

    @ContainerDisable
    public void disable() {
        for (Bot bot : getBotManager().getBots()) {
            bot.silentKill();
        }

        instance = null;
    }

    public void callEvent(Event event) {
        for (BotEvent events : SmartBotsBackend.getInstance().getEventManager().getEvents()) {
            events.call(event);
        }
    }
}
