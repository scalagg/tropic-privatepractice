package land.esta.plugin.ssb.bots.mechanics;

import lombok.Getter;
import lombok.Setter;
import land.esta.plugin.ssb.bots.Bot;
import land.esta.plugin.ssb.bots.events.Manager;
import land.esta.plugin.ssb.bots.setting.VelocitySettings;
import land.esta.plugin.ssb.utils.MathUtils;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.MathHelper;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.Random;

@Getter
@Setter
public class VelocityCalculator extends Manager {

    public VelocityCalculator(Bot bot) {
        super(bot);
    }

    private boolean velocityReplace;
    private Vector velocity = null;
    private boolean damage;
    private EntityPlayer attacker;
    private boolean sprinting;
    private boolean pre;
    private boolean next;

    public void attack(EntityPlayer attacker) {
        if (pre) next = true;
        damage = true;
        this.attacker = attacker;
        velocityReplace = true;
    }

    public boolean isVelocityReplace() {
        return velocityReplace;
    }

    public Vector getReplacedVelocity(Vector motion) {
        if (velocity == null) return getVelocityBot(attacker, bot, sprinting, motion);
        Vector clonedVelocity = velocity.clone();
        velocity = null;
        return clonedVelocity;
    }

    public Vector getVelocityBot(EntityPlayer attacker, Bot bot, boolean sprinting, Vector motion) {
        Vector dir = MathUtils.getDirection(attacker.yaw);

        VelocitySettings velocitySettings = settingManager.getVelocitySettings();

        double h = velocitySettings.getHorizontal().getDoubleValue();
        double reach = velocitySettings.getDependentRange().getDoubleValue();
        double distMultiplier = velocitySettings.getDependentRangeMultiplier().getDoubleValue();
        double v = velocitySettings.getVertical().getDoubleValue();
        double motionMultiplier = velocitySettings.getMotionFriction().getDoubleValue();
        double extraH = 0;
        Location botLocation = bot.getMovements().getLocation();
        Location attackerLocation = attacker.getBukkitEntity().getLocation();

        // Check if bot and attacker are in the same world before calculating distance
        if (botLocation.getWorld().equals(attackerLocation.getWorld())) {
            extraH = Math.min(Math.max((reach - (botLocation.distance(attackerLocation) - 0.3)) * distMultiplier, 0), 0.4);
        }

        if (sprinting) h *= velocitySettings.getSprintMultiplier().getDoubleValue();

    // Calculate the distance between the bot and the attacker
    double distance = bot.getMovements().getLocation().distance(attacker.getBukkitEntity().getLocation());

    // Calculate the reduction percentage (max 2%)
    double reductionPercentage = Math.min(2, distance * 1.0); // 1% reduction per block, max 2%

    // Adjust the horizontal velocity components by the reduction percentage
    h *= (1 - reductionPercentage / 100.0);

        if (bot.getMovements().isOnGround()) h *= velocitySettings.getGroundMultiplier().getDoubleValue();
        h += extraH;
        h -= velocitySettings.getHorizontal().getDoubleValue();

        Vector velocity = dir.clone().multiply(new Vector(h, 1, h));
        motion.multiply(-motionMultiplier);
        motion.setY(0);
        velocity.setY(v);

        if (bot.getDamageManager().isCombo()) {
            // Bukkit.broadcastMessage("Combo!");
            double horizontal = 0.4;
            double vertical = 0.3;

            double yD = bot.getMovements().getLocation().getY() - attacker.locY;
            if (yD + vertical > 2.5) {
                horizontal = 0.25;
                vertical = 2.5 - yD;
            }

            velocity = dir.clone().multiply(horizontal).setY(vertical);
        }
        velocity.add(motion);

        double distanceX = bot.getMovements().getLocation().getX() - attacker.locX;

        double distanceZ;

        Random random = new Random();
        for (distanceZ = bot.getMovements().getLocation().getZ() - attacker.locZ; distanceX * distanceX + distanceZ * distanceZ < 1.0E-4D; distanceZ = (random.nextDouble() - random.nextDouble()) * 0.01D) {
            distanceX = (random.nextDouble() - random.nextDouble()) * 0.01D;
        }

        if (!bot.getDamageManager().isCombo()) {
            velocity.add(getBasedVelocity(distanceX, distanceZ, velocitySettings.getHorizontal().getDoubleValue()));
        }

        return velocity;
    }

    public Vector getBasedVelocity(double d0, double d1, double horizontal) {
        float magnitude = MathHelper.sqrt(d0 * d0 + d1 * d1);

        double x = d0 / (double) magnitude * horizontal;
        double z = d1 / (double) magnitude * horizontal;

        return new Vector(x, 0, z);
    }

    public void setVelocity(Vector velocity) {
        velocityReplace = true;
        this.velocity = velocity;
    }
}
