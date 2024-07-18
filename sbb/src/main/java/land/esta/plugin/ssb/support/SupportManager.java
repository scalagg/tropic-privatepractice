package land.esta.plugin.ssb.support;

import land.esta.plugin.ssb.SmartBotsBackend;
import lombok.Getter;
import land.esta.plugin.ssb.support.papi.BotPingPlaceHolder;
import land.esta.plugin.ssb.support.papi.PlaceHolderApiSupport;
import land.esta.plugin.ssb.support.strikepractice.StrikePracticeSupport;
import land.esta.plugin.ssb.utils.Color;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.ArrayList;

@Getter
public class SupportManager implements Listener {

    private final ArrayList<String> messages = new ArrayList<>();
    private final ArrayList<PluginSupport> pluginSupports = new ArrayList<>();

    public SupportManager() {
        addSupport(new StrikePracticeSupport("aiPractice"));
        addSupport(new PlaceHolderApiSupport("PlaceHolderAPI"));
        Bukkit.getServer().getPluginManager().registerEvents(this, SmartBotsBackend.getInstance());
    }

    @EventHandler
    public void joinMessage(PlayerJoinEvent e) {
        messages.forEach(message -> {
            e.getPlayer().sendMessage(message);
        });
    }

    public void addSupport(PluginSupport plugin) {
        if (Bukkit.getServer().getPluginManager().isPluginEnabled(plugin.getName())) {
            pluginSupports.add(plugin.enable());
        } else {
            Bukkit.getLogger().info(Color.code("&f"+ SmartBotsBackend.getInstance().getPluginName()+" were unable to add support for &B&l" + plugin.getName()));
        }
    }
}
