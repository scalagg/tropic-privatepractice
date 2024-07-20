package land.esta.plugin.ssb.features.imple;

import lombok.Getter;
import land.esta.plugin.ssb.botevents.Event;
import land.esta.plugin.ssb.botevents.events.BotDamageEvent;
import land.esta.plugin.ssb.bots.Bot;
import land.esta.plugin.ssb.botevents.events.BotAttackEvent;
import land.esta.plugin.ssb.bots.setting.impl.FleeSettings;
import land.esta.plugin.ssb.bots.setting.impl.StrafeSettings;
import land.esta.plugin.ssb.features.Feature;
import land.esta.plugin.ssb.SmartBotsBackend;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Getter
public class StrafeFeature extends Feature {
    private final Map<Bot,Float> strafe = new HashMap<>();

    private final Map<Bot,Integer> ticks = new HashMap<>();
    private final Map<Bot,Integer> combo = new HashMap<>();

    @Override
    public void tickEvent(Bot bot) {
        //Values
        StrafeSettings strafeSettings = bot.getSettingManager().getStrafeSettings();
        boolean inCombo = combo.getOrDefault(bot, 0) > 3;

        // Check if the bot and target are in the same world
        Location botLocation = bot.getMovements().getLocation();
        Location targetLocation = bot.getMovements().getTargetLocate().getTargetEntity().getLocation();
        if (!botLocation.getWorld().equals(targetLocation.getWorld())) {
            return;
        }

        boolean close = botLocation.distance(targetLocation) < 6;
        boolean chance = ThreadLocalRandom.current().nextInt(100) <= bot.getSettingManager().getStrafeSettings().getChance().getIntValue();

        //Enable & Combo & Close check
        if (!strafeSettings.getEnabled().isBooleanValue() || inCombo && !strafeSettings.getStrafeInCombo().isBooleanValue() || !close || bot.getMovements().isEdge()) {
            strafe.put(bot, 0f);
            bot.getMovements().setStrafe(0);
            return;
        }

        if (!chance) {
            bot.getMovements().setStrafe(0);
            return;
        }

        //Defaults + ticking
        if (strafe.getOrDefault(bot, 0f) == 0) strafe.put(bot, 45f);
        ticks.put(bot, ticks.getOrDefault(bot, 0) + 1);

        //Switch check
        if (ticks.get(bot) > strafeSettings.getSwitchSideTicks().getIntValue()) {
            ticks.put(bot, 0);
            strafe.put(bot, strafe.getOrDefault(bot, 0f) * -1f);
            bot.getMovements().setSpeedMultiplier(1 / 1.05);
        }

        bot.getMovements().setStrafe(strafe.get(bot));
    }

    @Override
    public void listen(Event event) {
        //Combo calculation.
        if (event instanceof BotDamageEvent) {
            combo.put(event.bot,combo.getOrDefault(event.bot,0) + 1);
            FleeSettings fleeSettings = event.bot.getSettingManager().getFleeSettings();
            if (combo.get(event.bot) > fleeSettings.getRunFromCombosHits().getIntValue() && fleeSettings.getRunFromCombos().isBooleanValue() && !event.bot.getFleeManager().isRunning()) {
                event.bot.getFleeManager().setRunningFromCombo(true);
                combo.put(event.bot,0);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        event.bot.getFleeManager().setRunningFromCombo(false);
                    }
                }.runTaskLater(SmartBotsBackend.getInstance(), fleeSettings.getRunFromCombosTime().getIntValue());
            }
        }
        if (event instanceof BotAttackEvent) {
            combo.put(event.bot,0);
        }
    }
}
