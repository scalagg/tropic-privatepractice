package land.esta.plugin.ssb.support;

import land.esta.plugin.ssb.SmartBotsBackend;
import lombok.Getter;
import land.esta.plugin.ssb.utils.Color;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

@Getter
public class PluginSupport implements Listener {

    private final String name;
    protected Object api;

    public PluginSupport(String name) {
        this.name = name;
        this.api = null;
        Bukkit.getServer().getPluginManager().registerEvents(this, SmartBotsBackend.getInstance());
    }

    public PluginSupport enable() {
        Bukkit.getLogger().info(Color.code("&f"+ SmartBotsBackend.getInstance().getPluginName()+" added support for &c&l" + name));
        return this;
    }
}
