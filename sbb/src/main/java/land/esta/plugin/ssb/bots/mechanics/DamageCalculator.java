package land.esta.plugin.ssb.bots.mechanics;

import com.google.common.base.Function;
import land.esta.plugin.ssb.bots.Bot;
import land.esta.plugin.ssb.bots.events.Manager;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

public class DamageCalculator extends Manager {

    public DamageCalculator(Bot bot) {
        super(bot);
    }

    //Taken from minecraft src
    protected void damage(final DamageSource damagesource, float f) {
        float originalDamage = f;
        InventoryManager manager = bot.getInventoryManager();

        double d = (damagesource == DamageSource.ANVIL || damagesource == DamageSource.FALLING_BLOCK) && manager.getHelmet()  != null ? -(f - f * 0.75D) : -0.0D;
        f += d;

        //Ignore armor (Blocking)
        double d2 = bot.getAttackingManager().isBlocking() && f > 0.0D ? -(f - (1.0D + f) * 0.5D) : -0.0D;
        f += d2;

        double d3 = -(f - (double) applyArmor(damagesource, f,manager));
        f += d3;

        Function<Double, Double> resistance = new Function<Double, Double>() {
            public Double apply(Double f) {
                //!damagesource.isStarvation() &&
                if (bot.getEffectsManager().getLevel(PotionEffectType.DAMAGE_RESISTANCE) != 0 && damagesource != DamageSource.OUT_OF_WORLD) {
                    int i = (bot.getEffectsManager().getLevel(PotionEffectType.DAMAGE_RESISTANCE) + 1) * 5;
                    int j = 25 - i;
                    float f1 = f.floatValue() * (float)j;
                    return -(f - (double)(f1 / 25.0F));
                } else {
                    return -0.0D;
                }
            }
        };
        float resistanceModifier = ((Double)resistance.apply((double)f)).floatValue();
        f += resistanceModifier;

        double d4 = -(f - (double) applyEnchants(damagesource, f, manager));
        f += d4;

        double d5 = -Math.max(f - Math.max(f - (double) bot.getHealthManager().getAbsHearts(), 0.0D), 0.0D);
        float absorptionModifier = (float) d5;

        Random random = new Random();

        if ((damagesource == DamageSource.ANVIL || damagesource == DamageSource.FALLING_BLOCK) && manager.getHelmet() != null) {
            // manager.damageArmor(4,(int)(originalDamage* 4.0D + (double) random.nextFloat() * originalDamage * 2.0D));
            manager.changeArmorDurability(4, (int)(originalDamage* 4.0D + (double) random.nextFloat() * originalDamage * 2.0D));
        }

        float f2;
        //if (!damagesource.ignoresArmor()) {
            f2 = (float)(originalDamage + d2 + d);
            // manager.damageArmors(f2);
            manager.changeArmorDurability(f2);
        //}

        absorptionModifier = (float)(-absorptionModifier);
        bot.getHealthManager().setAbsHearts(Math.max(bot.getHealthManager().getAbsHearts() - absorptionModifier, 0.0F));
        if (f == 0.0F) return;

        bot.getHealthManager().removeHealth((int) f);
    }

    public float applyArmor(DamageSource damagesource, float f, InventoryManager manager) {
       // if (damagesource.ignoresArmor()) {
            int br = br(manager);
            double i = 25.0 - br;
            float f1 = f * (float) i;
            f = f1 / 25.0F;
       // }

        return f;
    }

    public int br(InventoryManager manager) {
        int i = 0;
        ItemStack[] aitemstack = manager.getArmorStacks();

        for (ItemStack itemstack : aitemstack) {
            if (itemstack == null) continue;
            Item item = itemstack.getItem();
            if (item.equals(Item.getById(0))) {
                //Bukkit.broadcastMessage("Is air: " + item);
                continue;
            }

            if (itemstack.getItem() instanceof ItemArmor) {
                int l = ((ItemArmor) itemstack.getItem()).c;
                i += l;
            }
        }

        return i;
    }

    public int br(EntityPlayer player) {
        int i = 0;
        ItemStack[] aitemstack = player.getEquipment();
        int j = aitemstack.length;

        for (ItemStack itemstack : aitemstack) {
            if (itemstack != null && itemstack.getItem() instanceof ItemArmor) {
                int l = ((ItemArmor) itemstack.getItem()).c;
                i += l;
            }
        }

        return i;
    }

    public float applyEnchants(DamageSource damagesource,float f,InventoryManager manager) {
        //if (damagesource.isStarvation()) {
           // return f;
        if (f <= 0.0F) {
            return 0.0F;
        } else {
            int i = EnchantmentManager.a(manager.getArmorStacks(), damagesource);
            if (i > 20) {
                i = 20;
            }

            if (i > 0) {
                int j = 25 - i;
                float f1 = f * (float)j;
                f = f1 / 25.0F;
            }

            return f;
        }
    }
}
