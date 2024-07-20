package gg.tropic.practice.games.features

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftFireball
import org.bukkit.entity.Entity
import org.bukkit.entity.Fireball
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.ExplosionPrimeEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.projectiles.ProjectileSource
import org.bukkit.util.Vector
import java.util.*

class FireballFeature : Listener
{
    private val fireballExplosionSize: Double = 3.0
    private val fireballHorizontal: Double = -1.0
    private val fireballVertical: Double = 0.65 * 3

    private val damageSelf: Double = 1.0
    private val damageEnemy: Double = 1.0

    private val cooldowns = mutableMapOf<UUID, Long>()

    @EventHandler
    fun dc(event: PlayerQuitEvent)
    {
        cooldowns.remove(event.player.uniqueId)
    }

    @EventHandler
    fun interact(event: PlayerInteractEvent)
    {
        //check hand
        val inHand = event.item ?: return

        if (event.action == Action.RIGHT_CLICK_BLOCK || event.action == Action.RIGHT_CLICK_AIR)
        {
            if (inHand.type === Material.FIREBALL)
            {
                event.isCancelled = true

                if (System.currentTimeMillis() - cooldowns.getOrDefault(event.player.uniqueId, 0L) > (0.5 * 1000))
                {
                    cooldowns[event.player.uniqueId] = System.currentTimeMillis()

                    var fireball = event.player.launchProjectile(Fireball::class.java)
                    val direction = event.player.eyeLocation.direction

                    fireball = setFireballDirection(fireball, direction)
                    fireball.velocity = fireball.direction.multiply(10)
                    fireball.yield = fireballExplosionSize.toFloat()

                    minusAmount(event.player, inHand, 1)
                }
            }
        }
    }

    fun minusAmount(p: Player, i: ItemStack, amount: Int)
    {
        if (i.amount - amount <= 0)
        {
            p.inventory.removeItem(i)
            return
        }

        i.amount -= amount
        p.updateInventory()
    }

    fun setFireballDirection(fireball: Fireball, vector: Vector): Fireball
    {
        val fb = (fireball as CraftFireball).handle
        fb.dirX = vector.x * 0.1
        fb.dirY = vector.y * 0.1
        fb.dirZ = vector.z * 0.1
        return fb.bukkitEntity as Fireball
    }

    @EventHandler
    fun fireballHit(e: ProjectileHitEvent)
    {
        if (e.entity !is Fireball) return
        val location: Location = e.entity.location

        val projectileSource: ProjectileSource = e.entity.shooter ?: return
        if (projectileSource !is Player) return
        val source: Player = projectileSource

        val vector: Vector = location.toVector()
        val world: World = location.world ?: return
        val nearbyEntities: Collection<Entity> = world.getNearbyEntities(
            location, fireballExplosionSize, fireballExplosionSize, fireballExplosionSize
        )

        for (entity in nearbyEntities)
        {
            if (entity !is Player) continue
            val player: Player = entity

            val playerVector: Vector = player.location.toVector()
            val normalizedVector: Vector = vector.subtract(playerVector).normalize()
            val horizontalVector: Vector = normalizedVector.multiply(fireballHorizontal)
            var y: Double = normalizedVector.y

            if (y < 0) y += 1.5
            y = if (y <= 0.5)
            {
                fireballVertical * 1.5
            } else
            {
                y * fireballVertical * 1.5
            }
            player.velocity = horizontalVector.setY(y)

            if (player == source)
            {
                player.damage(damageSelf) // damage shooter
            } else
            {
                player.damage(damageEnemy) // damage enemies
            }
        }
    }

    @EventHandler
    fun fireballDirectHit(e: EntityDamageByEntityEvent)
    {
        if (e.damager !is Fireball) return
        if (e.entity !is Player) return
        e.isCancelled = true
    }

    @EventHandler
    fun fireballPrime(e: ExplosionPrimeEvent)
    {
        if (e.entity !is Fireball) return
        val shooter: ProjectileSource = (e.entity as Fireball).shooter ?: return
        if (shooter !is Player) return
        e.fire = false
    }
}
