package land.esta.plugin.ssb.bots.mechanics;

import lombok.Getter;
import lombok.Setter;
import land.esta.plugin.ssb.botevents.Event;
import land.esta.plugin.ssb.botevents.events.BotAttackLandEvent;
import land.esta.plugin.ssb.bots.Bot;
import land.esta.plugin.ssb.botevents.events.BotAttackEvent;
import land.esta.plugin.ssb.bots.events.Manager;
import land.esta.plugin.ssb.bots.mechanics.movementsmanagement.submovements.TargetLocate;
import land.esta.plugin.ssb.bots.setting.impl.AttackSettings;
import land.esta.plugin.ssb.utils.Nms;
import land.esta.plugin.ssb.utils.objects.AABB;
import land.esta.plugin.ssb.utils.raycast.RayCasting;
import net.minecraft.server.v1_8_R3.PacketPlayOutAnimation;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

@Getter
@Setter
public class AttackingManager extends Manager {
    public AttackingManager(Bot bot) {
        super(bot);
    }

    private boolean attacking = true;
    private boolean blocking = false;
    private int tickCounter;
    private long attacked;
    private boolean landAttack;
    private int attackTicks;

    private boolean hitSelecting;
    private int hitSelectingTicks;
    private boolean projectile;

    @Override
    public void tickPre() {
        if (!landAttack) return;
        landAttack = false;
        Event event = new BotAttackLandEvent(bot,bot.getMovements().getTargetLocate().getTargetEntity());
        bot.getEventListener().run(event); // Additional calling
        callEvent(event);
    }

    @Override
    public void tickPost() {
        attackTicks++;
        if (!attacking) return;
        if (bot.getFleeManager().isActive()) return;
        TargetLocate targetLocate = bot.getMovements().getTargetLocate();
        AttackSettings clickSetting = bot.getSettingManager().getClickSetting();
        int delay = clickSetting.getDelay();
        if (bot.getMovements().isJumpCombo()) delay = 1000 / bot.getSettingManager().getPlaystyleSettings().getJumpComboCPS().getIntValue();
        int clicks = 0;

        tickCounter+=50;

        if (bot.getDamageManager().getDamageTicks() >= bot.getSettingManager().getPlaystyleSettings().getHitSelectTicks().getIntValue()  && bot.getDamageManager().getDamageTicks() < 10) hitSelecting = false;

        //Do not attack if hit selecting
        if (bot.getSettingManager().getPlaystyleSettings().getHitSelect().isBooleanValue() && hitSelectingTicks > bot.getSettingManager().getPlaystyleSettings().getHitSelectDelay().getIntValue() && attackTicks > 20) {
            hitSelecting = true;
            hitSelectingTicks = 0;
        }

        if (hitSelecting) return;
        hitSelectingTicks++;

        while (tickCounter > delay) {
            tickCounter -= delay;
            clicks++;
        }

        for (int i = 0; i < clicks;i ++) {
            double dist = getDistance();
            if (dist == -1) return;
            if (dist >= bot.getSettingManager().getClickSetting().getSwingRange().getDoubleValue()) return;

            swing();
            if (!rayTrace()) return;
            attack(targetLocate.getTargetEntity());
            projectile = false;
        }
    }

    public boolean rayTrace() {
        Location loc = bot.getMovements().getLocation().clone();
        Vector from = loc.toVector();

        double eyeHeight = 1.62;
        loc.setY(loc.getY() + eyeHeight);
        loc.add(loc.getDirection().normalize().multiply(-0.05));

        double offSet = bot.getMovements().getCollision().getXzHitBoxOffset();

        Vector to = from.clone().add(loc.getDirection().normalize().multiply(bot.getSettingManager().getClickSetting().getReach().getDoubleValue() + 0.05));
        AABB collision = getHitBox(bot.getLagCompensatorManager().getClientLocation(), (float) offSet);

        //Block collision check
        Vector bc = traceWithBlocks(loc.toVector(),to.toLocation(loc.getWorld()),0.5);
        double dist = 100;
        if (bc != null) dist = bc.distance(loc.toVector());
        if (dist <= 0.3) dist = 0.3;

        Vector result = RayCasting.hitboxIntersection(loc.toVector(),to,collision);
        if (result == null) return false;

        double reach = result.distance(from);
        boolean attack = dist > reach;

        return reach <= bot.getSettingManager().getClickSetting().getReach().getDoubleValue() && attack;
    }

    public Vector traceWithBlocks(Vector from,Location to,double accurate) {
        Vector dir = to.toVector().subtract(from).normalize();
        double reach = to.toVector().distance(from);
        World world = to.getWorld();
        Vector lastResult = from;

        for (double i = 0;i < reach; i+= accurate) {
            Vector newPos = from.clone().add(dir.clone().multiply(i));

            if (newPos.toLocation(world).getBlock().getType().isSolid()) return lastResult;
            lastResult = newPos;
        }

        return null;
    }

    public AABB getHitBox(Location location, float offSet) {
        Vector targetPos = location.toVector();

        return new AABB(new Vector(targetPos.getX() - offSet,targetPos.getY(),targetPos.getZ() - offSet),new Vector(targetPos.getX() + offSet,targetPos.getY() + 2,targetPos.getZ() + offSet));
    }

    public void swing() {
        globalAction((player, temp) -> Nms.sendPacket(player,new PacketPlayOutAnimation(bot.getNpc(),0)));
    }

    double calculateItemAttackDamage(ItemStack item) {
        //Item has enchantments
        if (item == null) return 1;
        //return damage
        double attackDamage = 1;

        //SWORD
        if (item.getType().equals(Material.DIAMOND_SWORD)) {
            attackDamage += 7;
        } else if (item.getType().equals(Material.IRON_SWORD)) {
            attackDamage += 6;
        } else if (item.getType().equals(Material.STONE_SWORD)) {
            attackDamage += 5;
        } else if (item.getType().equals(Material.GOLD_SWORD)) {
            attackDamage += 4;
        } else if (item.getType().equals(Material.WOOD_SWORD)) {
            attackDamage += 4;
        }

        //AXE
        else if (item.getType().equals(Material.DIAMOND_AXE)) {
            attackDamage += 6;
        } else if (item.getType().equals(Material.IRON_AXE)) {
            attackDamage += 5;
        } else if (item.getType().equals(Material.STONE_AXE)) {
            attackDamage += 4;
        } else if (item.getType().equals(Material.GOLD_AXE)) {
            attackDamage += 3;
        } else if (item.getType().equals(Material.WOOD_AXE)) {
            attackDamage += 3;
        }

        //PICKAXE
        else if (item.getType().equals(Material.DIAMOND_PICKAXE)) {
            attackDamage += 5;
        } else if (item.getType().equals(Material.IRON_PICKAXE)) {
            attackDamage += 4;
        } else if (item.getType().equals(Material.STONE_PICKAXE)) {
            attackDamage += 3;
        } else if (item.getType().equals(Material.GOLD_PICKAXE)) {
            attackDamage += 2;
        } else if (item.getType().equals(Material.WOOD_PICKAXE)) {
            attackDamage += 2;
        }

        //SPADE
        else if (item.getType().equals(Material.DIAMOND_SPADE)) {
            attackDamage = 5.5;
        } else if (item.getType().equals(Material.IRON_SPADE)) {
            attackDamage = 4.5;
        } else if (item.getType().equals(Material.STONE_SPADE)) {
            attackDamage = 3.5;
        } else if (item.getType().equals(Material.GOLD_SPADE)) {
            attackDamage = 2.5;
        } else if (item.getType().equals(Material.WOOD_SPADE)) {
            attackDamage = 2.5;
        }

        //Player has strength effect
        int strenghteffectLVL = bot.getEffectsManager().getLevel(PotionEffectType.INCREASE_DAMAGE);
        attackDamage += 3 * strenghteffectLVL;

        //Get sharpness enchantment level
        int sharpnessLVL = item.getEnchantmentLevel(Enchantment.DAMAGE_ALL);

        //Player has sharpness on item
        attackDamage += sharpnessLVL * 1.25;

        //Player has armour
        return attackDamage;
    }

    public void attack(Entity player) {
        bot.getMovements().attack();
        int damage = (int) calculateItemAttackDamage(bot.getInventoryManager().getItemInMainHand());
        ((Damageable) player).damage(damage,bot.getNpc().getBukkitEntity());
        callEvent(new BotAttackEvent(bot));
        this.attacked = System.currentTimeMillis();
    }
}
