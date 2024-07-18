package land.esta.plugin.ssb.bots.mechanics.movementsmanagement.submovements;

import land.esta.plugin.ssb.SmartBotsBackend;
import lombok.Getter;
import lombok.Setter;
import land.esta.plugin.ssb.botevents.events.RotationEvent;
import land.esta.plugin.ssb.bots.Bot;
import land.esta.plugin.ssb.bots.events.Manager;
import land.esta.plugin.ssb.bots.mechanics.movementsmanagement.MovementsMain;
import land.esta.plugin.ssb.utils.MathUtils;
import land.esta.plugin.ssb.utils.WorldUtils;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.Random;

@Getter
@Setter
public class Rotations extends Manager {
    public Rotations(Bot bot) {
        super(bot);
    }

    private double JitterRotation = bot.getSettingManager().getRotationSetting().getJitterRotation().getDoubleValue();
    private double rotationSpeedRandom = bot.getSettingManager().getRotationSetting().getSpeedRandom().getDoubleValue();

    private Location targetPos = null;

    private float lastYaw;

    @Override
    public void tickPre() {
        float speed = bot.getSettingManager().getRotationSetting().getSpeed().getIntValue();
        if (bot.getFleeManager().getOffTicks() < 5) {
            speed = bot.getSettingManager().getFleeSettings().getRunningRotationSpeed().getIntValue();
        }

        float yaw = bot.getMovements().getLocation().getYaw();
        float pitch = bot.getMovements().getLocation().getPitch();

        MovementsMain movements = bot.getMovements();
        Location tPos = movements.getTargetLocate().getTarget();

        //Flips
        double flipYaw = 0;
        double flipPitch = 0;

        //Flip check
        boolean bool1 = bot.getAttackingManager().getAttackTicks() <= bot.getSettingManager().getPlaystyleSettings().getFlipLong().getIntValue();
        boolean bool2 = bot.getSettingManager().getPlaystyleSettings().getFlipSync().isBooleanValue();
        boolean bool3 = bot.getMovements().isWtap();
        boolean bool4 = SmartBotsBackend.getInstance().getDataManager().getPlayer(bot.getMovements().getTargetLocate().getTargetEntity()).getBotCombo() > 3; //If necessary could be made into a setting
        boolean flip = false;

        if (bool2 && bool3 && bool4) flip = true;
        if (!bool2 && bool1 && bool4) flip = true;

        if (bot.getSettingManager().getPlaystyleSettings().getFlips().isBooleanValue() && flip) {
            //Flip chance
            if (MathUtils.getRandom(0,100) <= bot.getSettingManager().getPlaystyleSettings().getFlipChance().getIntValue()) {
                double size = 1;
                size = generateSize();

                flipYaw = bot.getSettingManager().getPlaystyleSettings().getFlipYaw().getIntValue() * size;
                flipPitch = bot.getSettingManager().getPlaystyleSettings().getFlipPitch().getIntValue() * size;
            }
        }

        Vector aimBot = WorldUtils.calculateDirectionDifference(movements.getLocation(),tPos);
        this.lastYaw = yaw;
        yaw = aimAssist(yaw, (float) (aimBot.getX() + flipYaw),speed) + (float) MathUtils.getRandom(-JitterRotation, JitterRotation);
        pitch = aimAssistPitch(pitch,(float) (aimBot.getZ() + flipPitch),speed) + (float) MathUtils.getRandom(-JitterRotation, JitterRotation);
        if (Math.abs(pitch) > 90) pitch = 90 * pitch / Math.abs(pitch);

        callEventAsync(new RotationEvent(bot,yaw,pitch,bot.getMovements().getLocation().getYaw(),bot.getMovements().getLocation().getPitch()));
        movements.rotate(yaw,pitch);
    }

    public int generateSize() {
        Random random = new Random();
        int size = random.nextInt(2);
        if (size == 0) return -1;
        return 1;
    }

    public float aimAssistPitch(float before,float after,float s) {
        double delta = after - before;

        double speed = s + (float) MathUtils.getRandom(-rotationSpeedRandom, rotationSpeedRandom);

        if (Math.abs(delta) <= speed) return after;
        return  ((float) (before + (delta / Math.abs(delta) * speed)));
    }

    public float aimAssist(float before,float after,float s) {
        double deltaYaw;

        double speed = s + (float) MathUtils.getRandom(-rotationSpeedRandom, rotationSpeedRandom);

        double original = after - before;
        float var1 = (after + 360) - before;
        float var2 = after - (before + 360);
        float var3 = (after + 360) - (before + 360);

        deltaYaw = Math.min(min(Math.abs(var1),Math.abs(var2),Math.abs(var3)),Math.abs(original));
        if (deltaYaw == Math.abs(var1)) deltaYaw = var1;
        if (deltaYaw == Math.abs(var2)) deltaYaw = var2;
        if (deltaYaw == Math.abs(var3)) deltaYaw = var3;
        if (deltaYaw == Math.abs(original)) deltaYaw = original;


        if (Math.abs(deltaYaw) > speed) return ((float) (before + (deltaYaw / Math.abs(deltaYaw) * speed)));
        return after;
    }

    public double min(double var1,double var2,double var3) {
        double var4 = Math.min(var1,var2);
        return Math.min(var3,var4);
    }
}
