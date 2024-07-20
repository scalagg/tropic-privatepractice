package land.esta.plugin.ssb.features.imple;

import land.esta.plugin.ssb.botevents.Event;
import land.esta.plugin.ssb.botevents.events.FinalMoveEvent;
import land.esta.plugin.ssb.bots.Bot;
import land.esta.plugin.ssb.bots.setting.impl.RodSettings;
import land.esta.plugin.ssb.features.Feature;
import land.esta.plugin.ssb.SmartBotsBackend;
import land.esta.plugin.ssb.npc.nms.FixedEntityFishingHook;
import net.minecraft.server.v1_8_R3.*;
import org.apache.commons.lang.Validate;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RodFeature extends Feature {

    //TODO remove the bot from the map after the bot dies! to prevent any memory leaks
    private final Map<Bot, Integer> prevSlot = new HashMap<>();
    private final Map<Bot, Long> lastSwitch = new HashMap<>();

    @Override
    public void listen(Event event) {
        if (event instanceof FinalMoveEvent) {
            FinalMoveEvent e = (FinalMoveEvent) event;
            try {
                double distance = e.getTo().distance(e.bot.getMovements().getTargetLocate().getTarget());
                RodSettings settings = e.bot.getSettingManager().getRodSettings();
                ItemStack mainHand = e.bot.getInventoryManager().getItemInMainHand();

                if (!settings.getEnabled().isBooleanValue()) return;

                if (distance >= settings.getMinRodDistance().getDoubleValue() && distance <= settings.getMaxRodDistance().getDoubleValue() && !e.bot.getFleeManager().isActive()) {
                    if (mainHand == null) return;
                    if (mainHand.getType().equals(Material.FISHING_ROD)) return;
                    if (!settings.getRodWhileInCombo().isBooleanValue() && SmartBotsBackend.getInstance().getFeatureManager().getStrafeFeature().getCombo().getOrDefault(e.bot, 0) > 2 && e.bot.getDamageManager().getDamageTicks() < 10) return;
                    List<ItemStack> rods = e.bot.getInventoryManager().hasItem(Material.FISHING_ROD);
                    if (rods.isEmpty()) return;

                    //Hold the fishingRod
                    prevSlot.put(e.bot, e.bot.getInventoryManager().getMainHand());
                    e.bot.getInventoryManager().setMainHand(e.bot.getInventoryManager().getItemSlot(rods.get(0)));
                    e.bot.getNpc().dead = false;
                    lastSwitch.put(e.bot,System.currentTimeMillis());
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            EntityHuman npc = e.bot.getNpc();
                            launchProjectile(npc.world, null, npc);

                            e.bot.getAttackingManager().setProjectile(true); // Special velocity for the projectile
                        }
                    }.runTask(SmartBotsBackend.getInstance());
                } else {
                    if (System.currentTimeMillis() - lastSwitch.getOrDefault(e.bot, 0L) < settings.getRodDuration().getIntValue()) return;
                    if (mainHand == null) return;
                    if (!mainHand.getType().equals(Material.FISHING_ROD)) return;
                    e.bot.getInventoryManager().setMainHand(prevSlot.getOrDefault(e.bot,0));
                }

            } catch (IllegalArgumentException ignore) {

            }
        }
    }

    public FixedEntityFishingHook launchProjectile(World world, Vector velocity, EntityHuman human) {
        FixedEntityFishingHook launch = new FixedEntityFishingHook(world, human);

        Validate.notNull(launch, "Projectile not supported");
        if (velocity != null) {
            ((Projectile) launch.getBukkitEntity()).setVelocity(velocity);
        }

        world.addEntity(launch);
        return launch;
    }
}
