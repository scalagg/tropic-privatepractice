package land.esta.plugin.ssb.bots.mechanics;

import ga.strikepractice.fights.botduel.BotDuel;
import land.esta.plugin.ssb.SmartBotsBackend;
import lombok.Getter;
import lombok.Setter;
import land.esta.plugin.ssb.botevents.events.BotDamageEvent;
import land.esta.plugin.ssb.bots.Bot;
import land.esta.plugin.ssb.bots.events.Manager;
import land.esta.plugin.ssb.utils.Color;
import land.esta.plugin.ssb.utils.FileManager;
import land.esta.plugin.ssb.utils.Nms;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

@Getter
@Setter
public class DamageManager extends Manager {
    public DamageManager(Bot bot) {
        super(bot);
    }

    private int damageTicks;
    private long last;
    private boolean noDamage;
    private boolean invaluable;
    private boolean combo;

    private boolean allowGlobalAttacks;

    @Override
    public void tickPre() {
        damageTicks++;
    }

    public void damage(DamageSource source, float damage) {
        if (invaluable) return;
        //Attack delay
        long attackDelay = 475;
        if (combo) attackDelay = 75;
        if (System.currentTimeMillis() - last <= attackDelay) return;

        BotDamageEvent event = new BotDamageEvent(bot, source);
        bot.getEventListener().run(event); // Additional calling
        //todo Improve the additional calling
        callEvent(event);

        if (event.isCanceled()) return;
        last = System.currentTimeMillis();
        damageTicks = 0;

        //Damage
        if (source.i().getBukkitEntity() instanceof Player) {
            Player player = (Player) source.i().getBukkitEntity();
            playerAttack(player,damage);
        } else if (source.i().getBukkitEntity() instanceof Projectile){
            Projectile projectile = (Projectile) source.i().getBukkitEntity();
            if (projectile.getType().equals(EntityType.FISHING_HOOK)) {
                bot.getNpc().getBukkitEntity().setPassenger(projectile);
            } else
                projectile.remove();

            projectileAttack(projectile,damage,source);
        }
    }

    public void projectileAttack(Projectile projectile, float damage, DamageSource source) {
        Vector direction = projectile.getVelocity().setY(0).normalize();
        double v = 0.35;
        double h = 0.6;
        direction.multiply(h).setY(v);
        bot.getVelocityCalculator().setVelocity(direction);

        // Store health before damage
        double healthBefore = bot.getHealthManager().getHealth();
        bot.getDamageCalculator().damage(source, damage);

        damageEffect();
        if (projectile.getType().equals(EntityType.ARROW)) {
            playSound();
            Player attacker = (Player) projectile.getShooter();
            playProjectileSound(attacker);

            // Calculate and format health after damage
            double healthAfter = bot.getHealthManager().getHealth();
            String healthMessage = String.format("%.1f❤", healthAfter / 2);

            // Calculate and format damage
            double damageDealt = healthBefore - healthAfter;
            String damageMessage = String.format(" &f(&4-%.1f❤&f)", damageDealt / 2);

            String botName = bot.getNpc().getName();
            attacker.sendMessage(Color.code("&c" + botName +  " &f➡ &c" + healthMessage + damageMessage));
        } else playSound();

        //Calls
        if (SmartBotsBackend.getInstance().getBukkitEvents().isProjectileHitEvent()) {
            Bukkit.getServer().getPluginManager().callEvent(new ProjectileHitEvent(projectile));
        }
    }

    public void playerAttack(Player attacker, float damage) {
        if (!attacker.getUniqueId().equals(bot.getMovements().getTargetLocate().getTargetEntity().getUniqueId()) && !allowGlobalAttacks) return;

        velocity(attacker);
        damageEffect();
        playSound();
        sprintReset(attacker);
        if (!noDamage) removeHealth(damage,attacker);
    }

    public void removeHealth(Player attacker) {
        float swordAttackDamage = (float) Nms.getPlayer(attacker).getAttributeInstance(GenericAttributes.ATTACK_DAMAGE).getValue();
        float sharp = attacker.getItemInHand().getEnchantmentLevel(Enchantment.DAMAGE_ALL);
        swordAttackDamage += sharp * 1.25;
        bot.getDamageCalculator().damage(DamageSource.playerAttack(Nms.getPlayer(attacker)),swordAttackDamage);
    }

    public void removeHealth(float damage,Player attacker) {
        bot.getDamageCalculator().damage(DamageSource.playerAttack(Nms.getPlayer(attacker)),damage);
    }

    public void sprintReset(Player attacker) {
        if (Nms.getPlayer(attacker).isSprinting()) {
            Nms.getPlayer(attacker).setSprinting(false);
        }
    }

    public void playSwingEffect(Player attacker) {
        int type = 4; //4 If server allows diff particle depends on sprint
        if (Nms.getPlayer(attacker).isSprinting() || !FileManager.getOrDefault(SmartBotsBackend.getInstance(),"serversettings.changeParticleDependsOnSprint",false)) type = 5;
        int finalType = type;
        globalAction((player, temp) -> Nms.sendPacket(player,new PacketPlayOutAnimation(bot.getNpc(), finalType)));
    }

    public void damageEffect() {
        globalAction((player, temp) -> Nms.sendPacket(player,new PacketPlayOutAnimation(bot.getNpc(),1)));
    }

    public void velocity(Player attacker) {
        boolean sprint = Nms.getPlayer(attacker).isSprinting();
        bot.getVelocityCalculator().setSprinting(sprint);
        bot.getVelocityCalculator().attack(Nms.getPlayer(attacker));
    }

    public void playProjectileSound(Player shooter) {
        shooter.playSound(bot.getMovements().getLocation(), Sound.ORB_PICKUP,10F,0.5f);
    }

    public void playSound() {
        globalAction((player, temp) -> player.playSound(bot.getMovements().getLocation(), Sound.HURT_FLESH,100F,1));
    }

}
