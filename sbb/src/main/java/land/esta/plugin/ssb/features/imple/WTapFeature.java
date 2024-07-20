package land.esta.plugin.ssb.features.imple;

import land.esta.plugin.ssb.botevents.Event;
import land.esta.plugin.ssb.botevents.events.BotAttackLandEvent;
import land.esta.plugin.ssb.bots.Bot;
import land.esta.plugin.ssb.bots.setting.impl.WTapSetting;
import land.esta.plugin.ssb.features.Feature;
import land.esta.plugin.ssb.SmartBotsBackend;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ThreadLocalRandom;

public class WTapFeature extends Feature {

    @Override
    public void listen(Event event) {
        if (event instanceof BotAttackLandEvent) {
            BotAttackLandEvent e = (BotAttackLandEvent) event;
            Bot bot = e.bot;
            int random = (ThreadLocalRandom.current().nextInt(100));
            WTapSetting wTapSetting = bot.getSettingManager().getWTapSetting();

            bot.getMovements().setAttackSprint(false);
            if (!wTapSetting.getEnabled().isBooleanValue()) return;
            if (random > wTapSetting.getChance().getIntValue()) return;

            final long tapLong;
            if (SmartBotsBackend.getInstance().getDataManager().getPlayer(e.getPlayer()).getBotCombo() > 3) {
                tapLong = wTapSetting.getComboTapLong().getIntValue();
            } else {
                tapLong = wTapSetting.getTapLong().getIntValue();
            }
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!bot.isAlive()) return;


                    if (bot.getSettingManager().getWTapSetting().getBlockingHitting().isBooleanValue()) {
                        //Block hitting handling
                        bot.getPacketsManager().sendBlockHittingPackets(true);
                        bot.getMovements().setUse(true);
                    } else {
                        //W tap handing
                        bot.getMovements().setMove(false);
                    }
                    bot.getMovements().setWtap(true);

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (!bot.isAlive()) return;

                            if (bot.getSettingManager().getWTapSetting().getBlockingHitting().isBooleanValue()) {
                                //Block hitting handling
                                bot.getPacketsManager().sendBlockHittingPackets(false);
                                bot.getMovements().setUse(false);
                            } else {
                                //W tap handing
                                bot.getMovements().setMove(true);
                            }

                            bot.getMovements().setWtap(false);
                        }
                    }.runTaskLater(SmartBotsBackend.getInstance(),tapLong);
                }
            }.runTaskLater(SmartBotsBackend.getInstance(),wTapSetting.getTapStart().getIntValue());
        }
    }
}
