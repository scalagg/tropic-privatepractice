package land.esta.plugin.ssb.bots;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import gg.tropic.practice.kit.Kit;
import land.esta.plugin.ssb.SmartBotsBackend;
import land.esta.plugin.ssb.bots.mechanics.*;
import land.esta.plugin.ssb.bots.setting.SettingManager;
import lombok.Getter;
import lombok.Setter;
import land.esta.plugin.ssb.botevents.events.BotDeathEvent;
import land.esta.plugin.ssb.botevents.events.SpawnEvent;
import land.esta.plugin.ssb.bots.events.MEventManager;
import land.esta.plugin.ssb.bots.mechanics.movementsmanagement.MovementsMain;
import land.esta.plugin.ssb.npc.NPCEntity;
import land.esta.plugin.ssb.npc.NPCNetworkManager;
import land.esta.plugin.ssb.npc.NPCPlayerConnection;
import land.esta.plugin.ssb.utils.Color;
import land.esta.plugin.ssb.utils.Nms;
import land.esta.plugin.ssb.utils.objects.EventRunnable;
import net.minecraft.server.v1_8_R3.EnumProtocolDirection;
import net.minecraft.server.v1_8_R3.MinecraftServer;
import net.minecraft.server.v1_8_R3.PlayerInteractManager;
import net.minecraft.server.v1_8_R3.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

@Getter
@Setter
public class Bot {
    private MovementsMain movements;
    private static Map<String, Property> registeredSkins = new HashMap<>();
    private NPCEntity npc;
    private Kit kit;
    private SettingManager settingManager;
    private PacketsManager packetsManager;
    private ViewsManager viewsManager;
    private EffectsManager effectsManager;
    private VelocityCalculator velocityCalculator;
    private MEventManager MEventManager;
    private DamageManager damageManager;
    private InventoryManager inventoryManager;
    private AttackingManager attackingManager;
    private HealthManager healthManager;
    private LagCompensatorManager lagCompensatorManager;
    private FleeManager fleeManager;
    private EventRunnable eventListener;
    private DamageCalculator damageCalculator;
    private boolean alive = true;
    private boolean killed;

    public Bot(Location loc, String name, String skin, Player p, SettingManager settings, Kit kit) throws IOException, InterruptedException {
        this.kit = kit;
        this.eventListener = (e) -> {};
        //Registering
        registerNPC(name, loc);

        Bukkit.getOnlinePlayers().forEach(player -> {
            player.hidePlayer(npc.getBukkitEntity());
        });

        p.showPlayer(npc.getBukkitEntity());

        registerManagers(settings, loc, p);
        registerSkin(skin, p);

        //Create packets
        packetsManager.sendCreatesPackets();

        loadManagers();
        registerBot();
    }

    private void registerBot() {
        SmartBotsBackend.getInstance().getBotManager().addBot(this);
        movements.callEventAsync(new SpawnEvent(this));
    }

    private void loadManagers() {
        MEventManager.addEvent(movements);
        MEventManager.addEvent(viewsManager);
        MEventManager.addEvent(inventoryManager);
        MEventManager.addEvent(packetsManager);
        MEventManager.addEvent(damageManager = new DamageManager(this));
        MEventManager.addEvent(attackingManager = new AttackingManager(this));
        MEventManager.addEvent(healthManager = new HealthManager(this));
        MEventManager.addEvent(damageCalculator = new DamageCalculator(this));
        MEventManager.addEvent(fleeManager);
        MEventManager.addEvent(effectsManager);
    }

    private void registerManagers(SettingManager settings,Location loc,Player p) {
        if (settings != null) settingManager = settings;
        //Load the SettingsManager first in order for other managers to read from it.
        if (settings == null) settingManager = new SettingManager();
        //Load LagCompensatorManager in order for other managers to read from it.
        lagCompensatorManager = new LagCompensatorManager(this);
        MEventManager = new MEventManager(this);
        effectsManager = new EffectsManager(this);
        velocityCalculator = new VelocityCalculator(this);
        movements = new MovementsMain(this,loc,p);
        packetsManager = new PacketsManager(this);
        inventoryManager = new InventoryManager(this);
        fleeManager = new FleeManager(this);
        List<Player> playerList = new ArrayList<>();
        playerList.add(p);
        viewsManager = new ViewsManager(this,playerList);
    }

    private void registerNPC(String name, Location loc) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), name);
        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
        WorldServer world = ((CraftWorld) loc.getWorld()).getHandle();
        PlayerInteractManager interactManager = new PlayerInteractManager(world);

        npc = new NPCEntity(server, world, profile, interactManager);
        npc.playerConnection = new NPCPlayerConnection(server, new NPCNetworkManager(EnumProtocolDirection.CLIENTBOUND), npc);
        npc.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        npc.setPosition(loc.getX(), loc.getY(), loc.getZ());
        new BukkitRunnable() {
            @Override
            public void run() {
                world.addEntity(npc);
            }
        }.runTask(SmartBotsBackend.getInstance());
        //server.getPlayerList().players.add(npc);
    }

    // TODO: Optimize registering skin (HTTP Open Stream) is tanking performance
    private void registerSkin(String name, Player fallback) throws IOException {
        //If the skin is already registered to the server use it.
        Property property;
        try {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().equalsIgnoreCase(name)) {
                    property = Nms.getPlayer(p).getProfile().getProperties().get("textures").iterator().next();
                    if (property == null) continue;
                    npc.getProfile().getProperties().put("textures", property);
                    return;
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        //If the skin has already been registered to the map use it.
        if (registeredSkins.containsKey(name)) {
            property = registeredSkins.get(name);
            npc.getProfile().getProperties().put("textures", property);
            return;
        }

        // To avoid errors
        // At some points we should remove this warnings
        InputStreamReader reader_0;

        try {
            //If the skin is not registered yet, Register it
            URL url_0 = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
            reader_0 = new InputStreamReader(url_0.openStream());
        } catch (Exception ignore) {
            //Bukkit.broadcastMessage("WARNING: Could not load skin " + name + " using fall back skin");
            property = Nms.getPlayer(fallback).getProfile().getProperties().get("textures").iterator().next();
            if (property == null) {
                //Bukkit.broadcastMessage("WARNING: could not use fall back skin");
                return;
            }
            npc.getProfile().getProperties().put("textures", property);
            return;
        }

        String var1 = "Xecivity";
        try {
            var1 = new JsonParser().parse(reader_0).getAsJsonObject().get("id").getAsString();
        } catch (Exception ignore) {
            fallback.sendMessage(Color.code("&cThis is an invalid skin, there is no player that goes by &e" + name + "&c."));
        }

        InputStreamReader reader_1;

        try {
            URL url_1 = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + var1 + "?unsigned=false");
            reader_1 = new InputStreamReader(url_1.openStream());
        } catch (Exception ignore) {
            property = Nms.getPlayer(fallback).getProfile().getProperties().get("textures").iterator().next();
            if (property == null) {
                //Bukkit.broadcastMessage("WARNING: could not use fall back skin");
                return;
            }
            npc.getProfile().getProperties().put("textures", property);
            return;
        }
        JsonObject textureProperty = new JsonParser().parse(reader_1).getAsJsonObject().get("properties").getAsJsonArray().get(0).getAsJsonObject();
        String texture = textureProperty.get("value").getAsString();
        String signature = textureProperty.get("signature").getAsString();

        property = new Property("textures", texture, signature);
        npc.getProfile().getProperties().put("textures", property);
        registeredSkins.put(name, property);
    }

    public void kill() {
        if (killed) return;
        killed = true;

        //Disables attacking
        attackingManager.setAttacking(false);
        damageManager.setInvaluable(true);

        //Calls the animation with the event
        deathEvent(() -> {
        });
    }

    public void silentKill() {
        alive = false;
        SmartBotsBackend.getInstance().getBotManager().removeBot(this);
        packetsManager.sendRemovePackets();
        npc.getBukkitEntity().remove();
    }

    public void deathEvent(Runnable executeAction) {
        //Animation
        fakeDeath(npc.getBukkitEntity(), movements.getTargetLocate().getTargetEntity(),executeAction);

        //Event
        //Bukkit.getServer().getPluginManager().callEvent(new BotDeathEvent(this));

    }

    public void fakeDeath(Player player, Player killer,Runnable finishAction) {
        movements.setMove(false);

        packetsManager.sendDeathPackets();
        //lightning();

        Bot bot = this;


        new BukkitRunnable() {
            @Override
            public void run() {
                packetsManager.sendRemovePackets();
                SmartBotsBackend.getInstance().getBotManager().removeBot(bot);
                alive = false;
                npc.getBukkitEntity().remove();
                Bukkit.getServer().getPluginManager().callEvent(new BotDeathEvent(bot));
            }
        }.runTaskLater(SmartBotsBackend.getInstance(),14);
    }

    public void lightning() {
        movements.getLocation().getWorld().strikeLightningEffect(movements.getLocation());
    }

    public void tick() {
        velocityCalculator.setPre(true);
        MEventManager.queueUpdate();
        MEventManager.tickPre();
        MEventManager.tickMid();
        MEventManager.tickPost();

        if (!movements.getTargetLocate().getTargetEntity().getWorld().equals(movements.getLocation().getWorld())) return;
        velocityCalculator.setPre(false);
        MEventManager.callEvents();
    }
}
