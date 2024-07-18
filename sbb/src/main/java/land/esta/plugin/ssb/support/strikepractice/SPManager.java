package land.esta.plugin.ssb.support.strikepractice;

import ga.strikepractice.StrikePractice;
import ga.strikepractice.battlekit.BattleKit;
import ga.strikepractice.fights.BotFight;
import ga.strikepractice.fights.Fight;
import ga.strikepractice.fights.botduel.BotDuel;
import ga.strikepractice.fights.duel.BestOf;
import ga.strikepractice.npc.CitizensNPC;
import ga.strikepractice.utils.PlayerUtil;
import lombok.Getter;
import land.esta.plugin.ssb.botevents.events.BotAttackLandEvent;
import land.esta.plugin.ssb.botevents.events.BotDamageEvent;
import land.esta.plugin.ssb.bots.Bot;
import land.esta.plugin.ssb.bots.mechanics.InventoryManager;
import land.esta.plugin.ssb.bots.setting.Setting;
import land.esta.plugin.ssb.bots.setting.SettingManager;
import land.esta.plugin.ssb.bots.setting.SettingObject;
import land.esta.plugin.ssb.data.DataPlayer;
import land.esta.plugin.ssb.SmartBotsBackend;
import land.esta.plugin.ssb.utils.Color;
import land.esta.plugin.ssb.utils.RandomUtil;
import land.esta.plugin.ssb.utils.gui.imple.BotGui;
import land.esta.plugin.ssb.utils.gui.imple.SettingGui;
import net.crackpixel.winstreaks.StrikeWinstreaks;
import net.crackpixel.winstreaks.managers.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Random;

@Getter
public class SPManager implements Listener {

    private final StrikePracticeSupport support;

    public SPManager(StrikePracticeSupport support) {
        this.support = support;
        Bukkit.getServer().getPluginManager().registerEvents(this, SmartBotsBackend.getInstance());
    }

    public Bot spawn(Location loc, List<ItemStack> inv, ItemStack helmet, ItemStack chestPlate, ItemStack leggings, ItemStack boots, Collection<PotionEffect> effects, boolean combo, Player player, boolean noDamage, BotDuel fight, int... tier) throws IOException {
        DataPlayer data = SmartBotsBackend.getInstance().getDataManager().getPlayer(player);
        try {
            SettingManager settings = getSettings(data, fight.getDifficulty().toString(), fight.getKit().getName(), tier);

            if (settings == null) {
                StrikePractice.getAPI().cancelFight(player, Color.code("&cError: An error has occured, unable to locate Bot Settings."));
                return null;
            }

            if (settings.isGameRandomizer()) {
                for (Setting setting : settings.getSettings()) {
                    for (SettingObject object : setting.getSettings()) {
                        if (object.getMin() <= 0 || object.getMax() <= 0) continue;

                        if (object.getObject() instanceof Boolean) {
                            object.setObject(RandomUtil.randInt(1, 3) == 1 && RandomUtil.randInt(1, 2) != -1);
                        }
                        else if (object.getObject() instanceof Double) {
                            if ((Double) object.getObject() % 1 == 0) if (RandomUtil.randInt((int) object.getMin(), (int) object.getMax()) != -1) object.setObject(RandomUtil.randInt((int) object.getMin(), (int) object.getMax()));
                            else if (RandomUtil.randDouble(object.getMin(), object.getMax()) != -1) object.setObject(RandomUtil.randDouble(object.getMin(), object.getMax()));
                        } else {
                            if (RandomUtil.randInt((int) object.getMin(), (int) object.getMax()) != -1) object.setObject(RandomUtil.randInt((int) object.getMin(), (int) object.getMax()));
                        }
                    }
                }
            }

            String name = settings.getBotName().replace("%player%", player.getName());
            if (name.length() > 16) {
                name = name.substring(0, 16);
            }
            Bot bot = new Bot(loc,Color.code(name) ,settings.getSkin().replace("%player%",player.getName()),player,settings, fight.getKit()); // ADDED: Applies the bot skin and name

            if (fight.getKit().getName().contains("sumo")) {
                bot.getMovements().setEdgeDetector(true);
            }
            bot.getDamageManager().setAllowGlobalAttacks(false);
            // bot.lightning();
            bot.getMovements().getCollision().setWaterEnterEvent(bot::silentKill);
            bot.getDamageManager().setNoDamage(noDamage);
            bot.getMovements().setMove(false);

            if (combo) bot.getDamageManager().setCombo(combo);

            //Boxing support
            if (noDamage) {
                data.setBotHitCounter(0);
                data.setHitCounter(0);
                data.setCombo(0);
                data.setBotCombo(0);
                bot.setEventListener((event -> {
                    if (event instanceof BotDamageEvent) {
                        if (((BotDamageEvent) event).getSource() != null && ((BotDamageEvent) event).getSource().getEntity() != null &&  Bukkit.getServer().getPlayer(((BotDamageEvent) event).getSource().getEntity().getName()).equals(player)) {
                            data.hit();
                            fight.getStatistics(player).setHits(data.getHitCounter());

                            if (data.getHitCounter() >= 100) {
                                bot.kill();
                            }
                        } else {
                            BotDamageEvent e = (BotDamageEvent) event;
                            e.setCanceled(true);
                        }
                    }
                    if (event instanceof BotAttackLandEvent) {
                        data.botHit();
                        if (data.getBotHitCounter() >= 100) {
                            player.setHealth(0);
                        }
                    }
                }));
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (bot.isAlive()) bot.getMovements().setMove(true);
                }
            }.runTaskLater(SmartBotsBackend.getInstance(),6 * 20);

            InventoryManager inventoryManager = bot.getInventoryManager();
            inventoryManager.applyItemList(inv);
            if (helmet != null) {
                // inventoryManager.updateHelmet(helmet.clone());
                inventoryManager.updateArmorContents(helmet.clone(), 4);
            }
            if (chestPlate != null) {
                // inventoryManager.updateChestPlate(chestPlate.clone());
                inventoryManager.updateArmorContents(chestPlate.clone(), 3);
            }
            if (leggings != null) {
                // inventoryManager.updateLeggings(leggings.clone());
                inventoryManager.updateArmorContents(leggings.clone(), 2);
            }
            if (boots != null) {
                // inventoryManager.updateBoots(boots.clone());
                inventoryManager.updateArmorContents(boots.clone(), 1);
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    bot.getEffectsManager().addEffects(effects);
                }
            }.runTaskLater(SmartBotsBackend.getInstance(),1);
            //TODO combo mode support.
            return bot;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }


    public void kill(int id) {
        Bot bot = SmartBotsBackend.getInstance().getBotManager().getNpcFromId(id);
        if (bot != null && bot.isAlive()) bot.silentKill();
    }

    public SettingManager getSettings(DataPlayer data, String level, String kitName, int... tier) {
        //Unranked / ranked queue
        if (level.equals("UNRANKED")) return SmartBotsBackend.getInstance().getPresetManager().getUnranked().getPreset().getManager();
        if (level.equals("RANKED")) {
            int elo = getElo(data.getPlayer(), kitName);

            SettingGui closedPreset = null;
            int best = 1000;

            for (SettingGui preset : SmartBotsBackend.getInstance().getPresetManager().getRanked().getPresets()) {
                int diff = Math.abs(preset.getManager().getElo() - elo);
                if (diff < best) {
                    closedPreset = preset;
                    best = diff;
                }
            }

            if (closedPreset != null)
                return closedPreset.getManager();
        }
        if (level.equals("TOURNAMENT")) {
            for (SettingGui preset : SmartBotsBackend.getInstance().getPresetManager().getTournament().getPresets()) {
                if (preset.getManager().getTier() == tier[0]) return preset.getManager();
            }
        }

        //Custom
        BotGui gui = data.getGui();
        if (level.equals("HACKER")) {
            if (!gui.isLoaded()) {
                data.getPlayer().sendMessage(Color.code("&aFetching settings, please a moment..."));
                return null;
            }
            return data.getGui().getManager();
        }
        //Easy,Normal,Hard,Expert,Nightmare
        return SmartBotsBackend.getInstance().getDifficultiesSettings().getData(level);
    }

    public int getElo(Player player, String kitName) {
        //player.setMetadata("StrikePracticeBotElo", new FixedMetadataValue(this.plugin, elo));

        DataManager dataManager = StrikeWinstreaks.getInstance().getDataManager();

        int playerKitElo = dataManager.getPlayerData().get(player.getUniqueId()).getElo(kitName);

        Random random = new Random();

        int elo1 = playerKitElo - 100;
        int elo2 = playerKitElo + 100;

        return random.nextInt(elo2 - elo1) + elo1;
    }
}
