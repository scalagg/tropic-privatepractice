package land.esta.plugin.ssb.utils;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.*;

@Getter
public class MathUtils {
    public static Random random = new Random();

    public static Vector getDirection(float yaw) {
        Vector vector = new Vector();
        double rotY = (double) 0;
        vector.setY(-Math.sin(Math.toRadians(rotY)));
        double xz = Math.cos(Math.toRadians(rotY));
        vector.setX(-xz * Math.sin(Math.toRadians((double) yaw)));
        vector.setZ(xz * Math.cos(Math.toRadians((double) yaw)));
        return vector.normalize();
    }

    public static Double getRelativeBlockHeight(Material material) {
        switch (material) {
            case ACACIA_FENCE:
            case ACACIA_FENCE_GATE:
            case BIRCH_FENCE:
            case BIRCH_FENCE_GATE:
            case DARK_OAK_FENCE:
            case DARK_OAK_FENCE_GATE:
            case FENCE:
            case FENCE_GATE:
            case IRON_FENCE:
            case JUNGLE_FENCE:
            case JUNGLE_FENCE_GATE:
            case NETHER_FENCE:
            case SPRUCE_FENCE:
            case SPRUCE_FENCE_GATE:
            case COBBLE_WALL:
                return 0.5;
            case SOIL:
            case CACTUS:
                return 0.9375;
            case SOUL_SAND:
            case CHEST:
            case ENDER_CHEST:
            case TRAPPED_CHEST:
                return 0.875;
            case ENCHANTMENT_TABLE:
                return 0.75;
            case BED_BLOCK:
                return 0.5625;
            case SKULL:
                return 0.25;
            case WATER_LILY:
                return 0.09375;
            default:
                return 0.0625;
        }
    }

    public static String stringOfDouble(Double var0,int element) {
        double var1 = floor(var0,Math.pow(0.1,element - 1),false);
        String var2 = Double.toString(var1);
        boolean point = false;
        int values = 0;
        String builder = "";

        for (String string : var2.split("")) {
            if (point) values++;
            if (values > element) return builder;
            if (string.equals(".")) point = true;
            builder = builder + string;
        }
        return var2;
    }

    public static List<Block> blocksFromTwoPoints(Location loc1, Location loc2) {

        List<Block> blocks = new ArrayList<Block>();

        int topBlockX = (Math.max(loc1.getBlockX(), loc2.getBlockX()));
        int bottomBlockX = (Math.min(loc1.getBlockX(), loc2.getBlockX()));

        int topBlockY = (Math.max(loc1.getBlockY(), loc2.getBlockY()));
        int bottomBlockY = (Math.min(loc1.getBlockY(), loc2.getBlockY()));

        int topBlockZ = (Math.max(loc1.getBlockZ(), loc2.getBlockZ()));
        int bottomBlockZ = (Math.min(loc1.getBlockZ(), loc2.getBlockZ()));

        for(int x = bottomBlockX; x <= topBlockX; x++) {
            for(int z = bottomBlockZ; z <= topBlockZ; z++) {
                for(int y = bottomBlockY; y <= topBlockY; y++) {
                    Block block = loc1.getWorld().getBlockAt(x, y, z);

                    blocks.add(block);
                }
            }
        }

        return blocks;
    }
    public static double floor(double number, double by, boolean down) {
        double var1 = number / by;
        if (down) {
            var1 = Math.floor(var1);
        } else var1  = Math.ceil(var1);
        var1 *= by;

        return var1;
    }

    public static double getRandom(double min,double max) {
        min = min * 100;
        max = max * 100;

        for(int i = (int) min; i <=max; i++) {
            double getRandomValue = ((int) (random.nextDouble() * (max - min)) + min) / 100;
            return getRandomValue;
        }
        return 0;
    }
}
