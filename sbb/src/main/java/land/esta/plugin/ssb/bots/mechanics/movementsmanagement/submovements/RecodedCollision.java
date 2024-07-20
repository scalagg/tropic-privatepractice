package land.esta.plugin.ssb.bots.mechanics.movementsmanagement.submovements;

import land.esta.plugin.ssb.SmartBotsBackend;
import lombok.Getter;
import land.esta.plugin.ssb.bots.Bot;
import land.esta.plugin.ssb.bots.events.Manager;
import land.esta.plugin.ssb.bots.setting.impl.PlaystyleSettings;
import land.esta.plugin.ssb.utils.MathUtils;
import land.esta.plugin.ssb.utils.objects.AABB;
import net.minecraft.server.v1_8_R3.AxisAlignedBB;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.IBlockData;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class RecodedCollision extends Manager {

    public RecodedCollision(Bot bot) {
        super(bot);
    }

    private int last;
    private boolean jump;

    @Override
    public void tickPre() {
        if (bot.getMovements().isJumpCombo() && bot.getMovements().isOnGround()) bot.getMovements().setJumpCombo(false);
    }

    public Location cast(Location from, Location to) {
        Vector move = to.toVector().subtract(from.toVector());
        Vector clonedTo = to.toVector();
        World world = from.getWorld();

        AABB hitbox = createAABB(from.toVector(),to.toVector());
        hitbox.getMin().subtract(new Vector(0.3,0,0.3));
        hitbox.getMax().add(new Vector(0.3,1.8,0.3));

        List<AABB> collisions = getCollision(hitbox,world);

        // Y check
        Vector min = from.toVector();
        Vector max = to.toVector();

        if (max.getY() < min.getY()) {
            double temp = max.getY();
            max.setY(min.getY());
            min.setY(temp);
        }

        min.setX(min.getX() - 0.29);
        min.setZ(min.getZ() - 0.29);

        List<AB> yCollisions = new ArrayList<>();

        for (AABB box : collisions) {
            if (box.isColliding(createBox(min,new Vector(0.58,1.8 + max.getY() - min.getY(),0.58)))) {
                yCollisions.add(new AB(box.getMin().getY(),box.getMax().getY()));
            }
        }
        double y = collisionCheck(to.getY(),from.getY(),yCollisions,1.8,0);
        to.setY(y);
        from.setY(y);

        // Z check
        List<AB> zCollisions = new ArrayList<>();
        max = to.toVector();
        min = from.toVector();
        if (max.getZ() < min.getZ()) {
            double temp = max.getZ();
            max.setZ(min.getZ());
            min.setZ(temp);
        }

        min.setX(min.getX() - 0.3);
        min.setZ(min.getZ() - 0.3);

        for (AABB box : collisions) {
            //new Vector(0.6,1.8,max.getZ() - min.getZ() + 0.6)
            if (box.isColliding(createBox(min,new Vector(0.6,1.8,max.getZ() - min.getZ() + 0.6)))) zCollisions.add(new AB(box.getMin().getZ(),box.getMax().getZ()));
        }
        double z = collisionCheck(to.getZ(),from.getZ(),zCollisions,0.3,0.3);
        to.setZ(z);
        from.setZ(z);

        // X check
        List<AB> xCollisions = new ArrayList<>();
        max = to.toVector();
        min = from.toVector();
        if (max.getX() < min.getX()) {
            double temp = max.getX();
            max.setX(min.getX());
            min.setX(temp);
        }

        min.setX(min.getX() - 0.3);
        min.setZ(min.getZ() - 0.3);

        for (AABB box : collisions) {
            if (box.isColliding(createBox(min,new Vector(0.6 + max.getX() - min.getX(),1.8,0.6)))) xCollisions.add(new AB(box.getMin().getX(),box.getMax().getX()));
        }
        double x = collisionCheck(to.getX(),from.getX(),xCollisions,0.3,0.3);
        to.setX(x);
        from.setX(x);

        //Jumping
        PlaystyleSettings styleManager = bot.getSettingManager().getPlaystyleSettings();
        if (bot.getSettingManager().getPlaystyleSettings().getAlwaysJump().isBooleanValue()) {
            //Jump when combed
            if (styleManager.getJumpWhileCombo().isBooleanValue() && SmartBotsBackend.getInstance().getFeatureManager().getStrafeFeature().getCombo().getOrDefault(bot,0) >= styleManager.getJumpWhileComboHits().getIntValue()) {
                bot.getMovements().setJump(true);
            }

            //Jump when attacking
            int combo = SmartBotsBackend.getInstance().getDataManager().getPlayer(bot.getMovements().getTargetLocate().getTargetEntity()).getBotCombo();
            int required = styleManager.getJumpComboHits().getIntValue() + 1;
            if (styleManager.getJumpCombo().isBooleanValue() && combo > required && combo % (required - 1) == 0 && last != combo) {
                jump = true;
            }

            if (jump && bot.getMovements().isSprint() && bot.getMovements().isMove() && !bot.getMovements().isUse() && bot.getMovements().getAttackMultiplier() == 1) {
                bot.getMovements().setJump(true);
                bot.getMovements().setJumpCombo(true);
                //Bukkit.broadcastMessage("jump combo");
                jump = false;
            }

            last = combo;
        } else if (to.toVector().distance(clonedTo.clone().setY(to.getY())) > 0.05) {
            Location loc = getBot().getNpc().getBukkitEntity().getLocation();
            Vector dir = loc.getDirection().normalize();
            loc.add(dir);
            // if (shouldJump(clonedTo.setY(to.getY()).toLocation(world).getBlock())) {
            if (shouldJump(loc.getBlock())) {
                bot.getMovements().setJump(true);
                // Bukkit.broadcastMessage("jump");
            }
        }

        return to;
    }

    public boolean shouldJump(Block original) {
        if (!original.getType().isSolid()) return false;

        Location start = original.getLocation().clone().add(new Vector(0,1,0));
        Location stop = original.getLocation().clone().add(new Vector(0,3,0));

        for (Block block : MathUtils.blocksFromTwoPoints(start,stop)) {
            if (block.getType().isSolid()) return false;
        }

        return true;
    }

    public AABB createAABB(Vector from,Vector to) {
        Vector max2 = new Vector();
        Vector min2 = new Vector();

        if (to.getX() > from.getX()) {
            max2.setX(to.getX());
            min2.setX(from.getX());
        } else {
            max2.setX(from.getX());
            min2.setX(to.getX());
        }

        if (to.getY() > from.getY()) {
            max2.setY(to.getY());
            min2.setY(from.getY());
        } else {
            max2.setY(from.getY());
            min2.setY(to.getY());
        }

        if (to.getZ() > from.getZ()) {
            max2.setZ(to.getZ());
            min2.setZ(from.getZ());
        } else {
            max2.setZ(from.getZ());
            min2.setZ(to.getZ());
        }

        return new AABB(min2,max2);
    }

    public List<AABB> getBoxes(List<AABB> boxes,Location from,Vector expend) {
        Vector min = from.toVector();
        Vector max = from.toVector().add(expend);
        AABB aabb = new AABB(min,max);

        List<AABB> result = new ArrayList<>();
        for (AABB box : boxes) {
            if (aabb.isColliding(box)) result.add(box);
        }

        return result;
    }

    public List<AABB> getCollision(AABB hitbox,World world) {
        List<Block> blocks = MathUtils.blocksFromTwoPoints(hitbox.getMin().toLocation(world),hitbox.getMax().toLocation(world));
        return getCollision(blocks);
    }

    public double collisionCheck(double to,double from,List<AB> boxes,double hitbox,double hitbox2) {
        double y;

        if (to > from) {
            y = to + hitbox;

            for (AB box : boxes) {
                double min = box.getMin();
                double max = box.getMax();

                if (max > from && max <= to + hitbox) {
                    y = Math.min(y,max);
                }
                if (min > from && min <= to + hitbox) {
                    y = Math.min(y,min);
                }
            }

            y = Math.max(from,y - hitbox);
        } else {
            y = to - hitbox2;

            for (AB box : boxes) {
                double min = box.getMin();
                double max = box.getMax();

                if (max <= from && max > to - hitbox2) {
                    y = Math.max(max,y);
                }
                if (min <= from && min > to - hitbox2) {
                    y = Math.max(min,y);
                }
            }

            y = Math.min(from,y + hitbox2);
        }

        return y;
    }

    public List<AABB> getCollision(List<Block> blocks) {
        List<AABB> result = new ArrayList<>();

        for (Block block : blocks) {
            Location blockLoc = block.getLocation(); //location of your Bukkit block.
            BlockPosition.MutableBlockPosition bPos = new BlockPosition.MutableBlockPosition();
            bPos.c(blockLoc.getBlockX(), blockLoc.getBlockY(), blockLoc.getBlockZ());
            IBlockData data = ((CraftWorld) block.getWorld()).getHandle().getType(bPos);
            net.minecraft.server.v1_8_R3.Block b = data.getBlock();
            b.updateShape(((CraftWorld) block.getWorld()).getHandle(), bPos);

            List<AxisAlignedBB> bbs = new ArrayList<>();
//This AxisAlignedBB is the shape of a 1x1x1 cuboid. This will define the bounds of the block.
            AxisAlignedBB cube = AxisAlignedBB.a(blockLoc.getBlockX(), blockLoc.getBlockY(), blockLoc.getBlockZ(), blockLoc.getBlockX() + 1, blockLoc.getBlockY() + 1, blockLoc.getBlockZ() + 1);
            b.a(((CraftWorld) blockLoc.getWorld()).getHandle(), bPos, data, cube, bbs, null);
//Check your bbs List. It should contain something.

            for (AxisAlignedBB collsion : bbs) {
                AABB aabb = new AABB(new Vector(collsion.a,collsion.b,collsion.c),new Vector(collsion.d,collsion.e,collsion.f));
                result.add(aabb);
            }
        }

        return result;
    }

    public AABB createBox(Vector from,Vector expend) {
        return new AABB(from,from.clone().add(expend));
    }

    @Getter
    public class AB {
        private final double min;
        private final double max;

        public AB(double min,double max) {
            this.min = min;
            this.max = max;
        }
    }
}
