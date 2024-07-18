package land.esta.plugin.ssb.bots.mechanics;

import land.esta.plugin.ssb.SmartBotsBackend;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import land.esta.plugin.ssb.botevents.events.BotArmorBreakEvent;
import land.esta.plugin.ssb.botevents.events.BotRequiredToEatEvent;
import land.esta.plugin.ssb.botevents.events.EatingCancelEvent;
import land.esta.plugin.ssb.botevents.events.FinishEatingEvent;
import land.esta.plugin.ssb.bots.Bot;
import land.esta.plugin.ssb.bots.events.Manager;
import land.esta.plugin.ssb.utils.Durability;
import land.esta.plugin.ssb.utils.Nms;
import land.esta.plugin.ssb.utils.objects.BooleanRunnable;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

@Getter
@Setter
public class InventoryManager extends Manager {

    public Map<Integer, ItemStack> slots;
    private int lines = 4;
    private ItemStack helmet;
    private ItemStack chestPlate;
    private ItemStack leggings;
    private ItemStack boots;

    private Bot bot;
    private int mainHand = 1;
    private ItemStack lastItem;

    private boolean requiredToEat;
    private long gappleDelay;
    private net.minecraft.server.v1_8_R3.ItemStack[] armorStacks = new net.minecraft.server.v1_8_R3.ItemStack[4];
    private  net.minecraft.server.v1_8_R3.ItemStack[] itemStacks = new net.minecraft.server.v1_8_R3.ItemStack[36];

    public void updateArmorStacks() {
        armorStacks = new net.minecraft.server.v1_8_R3.ItemStack[4];
        armorStacks[0] = (convertToNms(helmet));
        armorStacks[1] = (convertToNms(chestPlate));
        armorStacks[2] = (convertToNms(leggings));
        armorStacks[3] = (convertToNms(boots));
    }

    public void updateItemStacks() {
        itemStacks = new net.minecraft.server.v1_8_R3.ItemStack[36];

        if (slots == null) return;
        for (Integer slot : slots.keySet()) {
            itemStacks[slot - 1] = CraftItemStack.asNMSCopy((slots.get(slot)));
        }
    }

    public void breakArmor(int index) {
        //Bukkit.broadcastMessage("DEBUG: Break " + index);

        if (index == 1) {
            boots = new ItemStack(Material.AIR);
        } else if (index == 2) {
            leggings = new ItemStack(Material.AIR);
        } else if (index == 3) {
            chestPlate = new ItemStack(Material.AIR);
        } else if (index == 4) {
            helmet = new ItemStack(Material.AIR);
        }

        updateArmorContents(new ItemStack(Material.AIR), index);

        callEventAsync(new BotArmorBreakEvent(bot, index));
    }

    public void updateMainHandNMS() {
        PlayerInventory inv = bot.getNpc().inventory;
        inv.itemInHandIndex = mainHand - 1;
        inv.setItem(inv.itemInHandIndex,CraftItemStack.asNMSCopy(getItemInMainHand()));
    }

    // New version for damageArmor()
    public void changeArmorDurability(double damage) {
        List<ItemStack> armors = new ArrayList<>();
        armors.add(boots);
        armors.add(leggings);
        armors.add(chestPlate);
        armors.add(helmet);

        int index = 1;
        for (ItemStack armor : armors) {
            if (armor == null || armor.getType() == Material.AIR) {
                index++;
                continue;
            }

            short newDurability = (short) (armor.getDurability() + Durability.getDurabilityLost(armor, damage));
            armor.setDurability(newDurability);

            if (armor.getType().getMaxDurability() < newDurability) {
                breakArmor(index);
            }
            index++;
        }
    }

    public void changeArmorDurability(int index, double damage) {
        ItemStack piece = index == 1 ? boots : index == 2 ? leggings : index == 3 ? chestPlate : index == 4 ? helmet : null;

        if (piece == null || piece.getType() == Material.AIR) {
            return;
        }

        short newDurability = (short) (piece.getDurability() + Durability.getDurabilityLost(piece, damage));
        piece.setDurability(newDurability);

        if (piece.getType().getMaxDurability() < newDurability) breakArmor(index);
    }

    public net.minecraft.server.v1_8_R3.ItemStack convertToNms(ItemStack stack) {
        return CraftItemStack.asNMSCopy(stack);
    }


    public void setMainHand(int hand) {
        mainHand = hand;
        itemUpdate();
        updateItemStacks();
        updateMainHandNMS();
    }

    public InventoryManager(Bot bot) {
        super(bot);
        slots = new HashMap<>();
        this.bot = bot;
    }

    public ItemStack getItemInMainHand() {
        return slots.get(mainHand);
    }

    public void setItem(int slot,ItemStack item) {
        if (slot > lines * 9) return;
        if (item == null) return;
        if (item.getType().equals(Material.AIR)) return;
        slots.put(slot,item);

        itemUpdate();
        updateItemStacks();
    }

    public void addItem(ItemStack item) {
        for (int slot = 1; slot <= lines * 9; lines++) {
            if (!slots.containsKey(slot)) {
                setItem(slot,item);
                break;
            }
        }

        itemUpdate();
    }

    public void removeItem(int slot) {
        ItemStack stack = slots.get(slot);
        if (stack == null) return;

        //Removes one
        if (stack.getAmount() > 1) {
            stack.setAmount(stack.getAmount() - 1);
            return;
        }

        //Removes all if there is only 1
        slots.put(slot,null);
        itemUpdate();
        updateItemStacks();
    }

    public void deleteItem(int slot) {
        slots.put(slot,null);
        itemUpdate();
        updateItemStacks();
    }

    public void applyItemList(List<ItemStack> items) {
        int counter = 0;

        for (ItemStack item : items) {
            if (item == null) {
                setItem(counter + 1,null);
            } else {
                setItem(counter + 1,item.clone());
            }
            counter++;
        }
    }

    public List<ItemStack> hasItem(Material material) {
        List<ItemStack> list = new ArrayList<>();
        slots.values().forEach(value -> {
            if (value != null && value.getType() == (material)) list.add(value);
        });

        return list;
    }

    public List<ItemStack> hasArmor(int index) {
        List<ItemStack> list = new ArrayList<>();
        slots.values().forEach(value -> {
            if (value != null && hasArmor(index,value.getType())) list.add(value);
        });

        return list;
    }

    public boolean hasArmor(int index,Material material) {
        if (index == 1) return isBoots(material);
        if (index == 2) return isChestplate(material);
        if (index == 3) return isLeggings(material);
        if (index == 4) return isHelmet(material);
        return false;
    }

    public void addArmor(int index, ItemStack stack) {
        if (index == 1) updateBoots(stack);
        if (index == 2) updateLeggings(stack);
        if (index == 3) updateChestPlate(stack);
        if (index == 4) updateHelmet(stack);
    }

    public boolean isBoots(Material material) {
        if (material == null) return false;
        if (material == Material.CHAINMAIL_BOOTS) return true;
        if (material == Material.DIAMOND_BOOTS) return true;
        if (material == Material.IRON_BOOTS) return true;
        if (material == Material.GOLD_BOOTS) return true;
        if (material == Material.LEATHER_BOOTS) return true;
        return false;
    }

    public boolean isChestplate(Material material) {
        if (material == null) return false;
        if (material == Material.CHAINMAIL_CHESTPLATE) return true;
        if (material == Material.DIAMOND_CHESTPLATE) return true;
        if (material == Material.IRON_CHESTPLATE) return true;
        if (material == Material.GOLD_CHESTPLATE) return true;
        if (material == Material.LEATHER_CHESTPLATE) return true;
        return false;
    }

    public boolean isLeggings(Material material) {
        if (material == null) return false;
        if (material == Material.CHAINMAIL_LEGGINGS) return true;
        if (material == Material.DIAMOND_LEGGINGS) return true;
        if (material == Material.IRON_LEGGINGS) return true;
        if (material == Material.GOLD_LEGGINGS) return true;
        if (material == Material.LEATHER_LEGGINGS) return true;
        return false;
    }

    public boolean isHelmet(Material material) {
        if (material == null) return false;
        if (material == Material.CHAINMAIL_HELMET) return true;
        if (material == Material.DIAMOND_HELMET) return true;
        if (material == Material.IRON_HELMET) return true;
        if (material == Material.GOLD_HELMET) return true;
        if (material == Material.LEATHER_HELMET) return true;
        return false;
    }

    //public boolean isArmor(Material material) {
      //if(isHelmet(material)) return true;
      //if(isChestplate(material)) return true;
      //if(isLeggings(material)) return true;
      //if(isBoots(material)) return true;
    //}

    public List<ItemStack> hasItem(int material) {
        List<ItemStack> list = new ArrayList<>();
        slots.values().forEach(value -> {
            if (value != null &&value.getTypeId() == (material)) list.add(value);
        });

        return list;
    }

    public void applyInv(org.bukkit.inventory.Inventory inventory) {
        for (int slot = 0; slot < lines * 9; lines++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null) continue;

            setItem(slot + 1,stack);
        }

        itemUpdate();
    }

    public void itemUpdate() {
        ItemStack updated = getItemInMainHand();
        ItemStack last = this.lastItem;
        this.lastItem = updated;

        if (last != updated) {
            updateItemPacket(updated);
        }
    }

    public void updateHelmet(ItemStack helmet) {
        this.helmet = helmet;
        if (helmet != null) updateItemPacket(helmet,1);
        updateArmorStacks();
    }

    public void updateChestPlate(ItemStack chestPlate) {
        this.chestPlate = chestPlate;
        if (chestPlate != null) updateItemPacket(chestPlate,2);
        updateArmorStacks();
    }

    public void updateLeggings(ItemStack leggings) {
        this.leggings = leggings;
        if (leggings != null) updateItemPacket(leggings,3);
        updateArmorStacks();
    }

    public void updateBoots(ItemStack boots) {
        this.boots = boots;
        if (boots != null) updateItemPacket(boots,4);
        updateArmorStacks();
    }

    public void eat(ItemStack item) {
        if (eating) return;
        //Eat pots
        if (item.getType().equals(Material.POTION)) {
            Collection<PotionEffect> effects = Potion.fromItemStack(item).getEffects();

            if (Potion.fromItemStack(item).isSplash()) return;
            if (bot.getEffectsManager().hasEffects(effects)) return;

            eatEffects(effects,item,true);
            return;
        }

        //Eat gapples
        if (item.getType().equals(Material.GOLDEN_APPLE)) {
            Collection<PotionEffect> effects = new ArrayList<>();
            if (item.getData().getData() == 1) {
                //Enchanted golden apple
                effects.add(new PotionEffect(PotionEffectType.REGENERATION,20 * 30,5));
                effects.add(new PotionEffect(PotionEffectType.ABSORPTION,20 * 120,1));
                effects.add(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE,20 * 60 * 5,1));
                effects.add(new PotionEffect(PotionEffectType.FIRE_RESISTANCE,20 * 60 * 5,1));
            } else {
                //Normal gapple
                effects.add(new PotionEffect(PotionEffectType.ABSORPTION,20 * 120,1));
                effects.add(new PotionEffect(PotionEffectType.REGENERATION,20 * 5,2));
            }

            eatEffects(effects, item, false);
        }
    }

    @Override
    public void tickPre() {
        PlayerInventory inventory = bot.getNpc().inventory;
        inventory.armor = armorStacks;
        inventory.items = itemStacks;
        inventory.itemInHandIndex = getMainHand() - 1;

        if (requiredToEat) {
            callEventAsync(new BotRequiredToEatEvent(bot));
        }
    }

    public BooleanRunnable switchSlowTemp(ItemStack item) {
        //Hold the item
        int slot = getItemSlot(item);
        setMainHand(slot);

        return (a) -> {
            setMainHand(1);
            if (a) removeItem(slot);
        };
    }

    private boolean allowToEat = true;
    private boolean eating;

    public void cancelEating() {
        allowToEat = false;
    }

    public void allowEating() {
        allowToEat = true;
        requiredToEat = false;
    }

    public void eatEffects(Collection<PotionEffect> effects,ItemStack item,boolean drink) {
        bot.getMovements().setUse(true);
        BooleanRunnable f1 = switchSlowTemp(item);
        eating = true;
        BooleanRunnable f3 = (a) -> {
            f1.run(a);
            if (a) bot.getEffectsManager().addEffects(effects);
            if (!drink) {
                bot.getHealthManager().addHealth(7);
            }
            bot.getMovements().setUse(false);
            eating = false;
            callEventAsync(new FinishEatingEvent(bot,item));
        };
        eatEffect(f3,item,drink);
    }

    public void eatEffect(BooleanRunnable done,ItemStack item,boolean drink) {
        new BukkitRunnable() {
            int ticks = 0;
            final int max = 32;
            final int soundTicks = 4;
            @SneakyThrows
            public void run() {
                if (!bot.isAlive()) {
                    // Cancel eating if the bot died.
                    this.cancel();
                    return;
                }

                if (ticks % soundTicks == 0) runEffect(item,drink);

                if (ticks++ > max || !allowToEat) {
                    this.cancel();
                    done.run(allowToEat);
                    if (!allowToEat) {
                        callEventAsync(new EatingCancelEvent(bot));
                        allowToEat = true;
                    }
                }
            }
        }.runTaskTimer(SmartBotsBackend.getInstance(),0,1);
    }

    public void runEffect(ItemStack item,boolean drink) {
        //TODO run effect
        bot.getViewsManager().getPlayers().forEach(player -> {
            Sound sound = Sound.EAT;
            if (drink) sound = Sound.DRINK;
            Random random = new Random();
            Location loc = bot.getMovements().getLocation();

            if (drink) player.playSound(bot.getMovements().getLocation(), sound,0.5F, random.nextFloat() * 0.1F + 0.9F);
            if (!drink) player.playSound(bot.getMovements().getLocation(), sound, 0.5F + 0.5F * random.nextInt(2), (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);

            //Eating effects
            if (!drink) {
                Item.getById(item.getTypeId());
                PacketPlayOutWorldParticles particlePack = new PacketPlayOutWorldParticles(EnumParticle.ITEM_CRACK, true, (float) loc.getX(), (float) loc.getY(), (float) loc.getZ(), 0, 0, 0, 1, 0, item.getTypeId(), item.getTypeId());
                ((CraftPlayer) player).getHandle().playerConnection.sendPacket(particlePack);
            }
        });
    }

    public int getItemSlot(ItemStack item) {
        for (int key : getSlots().keySet()) {
            ItemStack stack = slots.get(key);
            if (stack != null && stack.equals(item))  return key;
        }

        return 1;
    }

    @Override
    public void addPlayerToList(Player player) {
        updateItemPacket(getItemInMainHand());
        if (helmet != null) updateItemPacket(helmet,1);
        if (chestPlate != null) updateItemPacket(chestPlate,2);
        if (leggings != null) updateItemPacket(leggings,3);
        if (boots != null) updateItemPacket(boots,4);
    }

    public void updateItemPacket(ItemStack updated) {
        updateItemPacket(updated,0);
    }

    public void updateItemPacket(ItemStack updated, int slot) {
        PacketPlayOutEntityEquipment entityEquipment = new PacketPlayOutEntityEquipment(bot.getNpc().getId(), slot, CraftItemStack.asNMSCopy(updated));

        globalAction((player, temp) -> Nms.sendPacket(player, entityEquipment));
    }

    public void updateArmorContents(ItemStack to, int i) {
        if (i == 1) boots = to;
        if (i == 2) leggings = to;
        if (i == 3) chestPlate = to;
        if (i == 4) helmet = to;

        List<ItemStack> armorContents = new ArrayList<>();
        armorContents.add(boots);
        armorContents.add(leggings);
        armorContents.add(chestPlate);
        armorContents.add(helmet);
        ItemStack[] tempArray = new ItemStack[4];

        bot.getNpc().getBukkitEntity().getInventory().setArmorContents(armorContents.toArray(tempArray));

        PacketPlayOutEntityEquipment entityEquipment = new PacketPlayOutEntityEquipment(bot.getNpc().getId(), i, CraftItemStack.asNMSCopy(to));
        globalAction((player, temp) -> Nms.sendPacket(player, entityEquipment));
        updateArmorStacks();
    }

    public void updateArmorPacket(int slot) {
        PacketPlayOutEntityEquipment entityEquipment = new PacketPlayOutEntityEquipment(bot.getNpc().getId(), slot, armorStacks[slot]);

        globalAction((player, temp) -> Nms.sendPacket(player,entityEquipment));
    }

    public org.bukkit.inventory.Inventory toBukkitInv(org.bukkit.inventory.PlayerInventory inventory) {
        inventory.clear();

        for (Integer slot : slots.keySet()) {
            inventory.setItem(slot - 1,slots.get(slot));
        }

        return inventory;
    }
}
