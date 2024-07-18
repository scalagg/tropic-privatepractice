package gg.tropic.practice.kit.feature

/**
 * @author GrowlyX
 * @since 9/17/2023
 */
enum class FeatureFlag(
    val schema: MutableMap<String, String> = mutableMapOf(),
    val incompatibleWith: () -> Set<FeatureFlag> = { emptySet() },
    val requires: Set<FeatureFlag> = setOf(),
    val availableLifecycles: Set<GameLifecycle> = GameLifecycle.entries.toSet()
)
{
    Ranked,
    QueueSizes(
        // another example: 3:Ranked,2:Casual,10:Ranked+Casual
        schema = mutableMapOf("sizes" to "1:Casual+Ranked")
    ),
    HeartsBelowNameTag,
    ExplodePlacedBlocks,
    DoNotTakeDamage(
        schema = mutableMapOf("doDamageTick" to "false")
    ),
    DoNotDropInventoryOnDeath,
    DoNotTakeHunger,
    DoNotRemoveArmor,
    RequiresBuildMap,
    HeadshotMultiplier(
        schema = mutableMapOf(
            "multiplier" to "1.0"
        )
    ),
    DeathBelowYAxis(
        schema = mutableMapOf(
            "level" to "95"
        )
    ),
    BuildLimit(
        schema = mutableMapOf(
            "blocks" to "16"
        )
    ),
    PlaceBlocks,
    BreakAllBlocks(
        incompatibleWith = { setOf(BreakPlacedBlocks, BreakSpecificBlockTypes) },
        requires = setOf(RequiresBuildMap)
    ),
    BreakPlacedBlocks(
        incompatibleWith = { setOf(BreakAllBlocks) },
        requires = setOf(RequiresBuildMap)
    ),
    BreakSpecificBlockTypes(
        incompatibleWith = { setOf(BreakAllBlocks) },
        requires = setOf(RequiresBuildMap),
        schema = mutableMapOf(
            // Default for bridges LMAO
            "types" to "STAINED_CLAY:11,STAINDED_CLAY:14"
        )
    ),
    EnderPearlCooldown(
        schema = mutableMapOf("duration" to "15")
    ),
    FrozenOnGameStart,
    NewlyCreated,
    MenuOrderWeight(
        schema = mutableMapOf("weight" to "0")
    ),
    ExpirePlacedBlocksAfterNSeconds(
        schema = mutableMapOf("time" to "10"),
        requires = setOf(PlaceBlocks)
    ),
    DeathOnLiquidInteraction,
    EntityDisguise(
        schema = mutableMapOf("type" to "IRON_GOLEM")
    ),
    RedBlueTeams,

    // multi-round
    CountDownTimeBeforeRoundStart(
        schema = mutableMapOf("value" to "5")
    ),

    // ImmediateRespawnOnDeath may be implied in game logic!
    ImmediateRespawnOnDeath(
        incompatibleWith = { setOf(StartNewRoundOnDeath) },
        availableLifecycles = setOf(
            GameLifecycle.RoundBound,
            GameLifecycle.ObjectivePlusSoulBound,
            GameLifecycle.ObjectiveBound
        )
    ),
    StartNewRoundOnDeath(
        incompatibleWith = { setOf(ImmediateRespawnOnDeath, SpectateAfterDeath) },
        availableLifecycles = setOf(
            GameLifecycle.RoundBound
        )
    ),
    RoundsRequiredToCompleteGame(
        schema = mutableMapOf("value" to "2"),
        availableLifecycles = setOf(
            GameLifecycle.RoundBound
        )
    ),

    SpectateAfterDeath(
        incompatibleWith = { setOf(ImmediateRespawnOnDeath) },
        availableLifecycles = setOf(
            GameLifecycle.RoundBound,
            GameLifecycle.ObjectivePlusSoulBound,
            GameLifecycle.ObjectiveBound
        )
    ),
    TimeUserSpectatesAfterDeath(
        schema = mutableMapOf("value" to "3"),
        requires = setOf(SpectateAfterDeath),
        availableLifecycles = setOf(
            GameLifecycle.RoundBound,
            GameLifecycle.ObjectivePlusSoulBound,
            GameLifecycle.ObjectiveBound
        )
    ),

    WinWhenNHitsReached(
        schema = mutableMapOf("hits" to "100"),
        availableLifecycles = setOf(
            GameLifecycle.ObjectiveBound,
            GameLifecycle.ObjectivePlusSoulBound
        )
    ),

    FrozenOnRoundStart(
        // game start freeze is implied with round start
        incompatibleWith = { setOf(FrozenOnGameStart) },
        availableLifecycles = setOf(
            GameLifecycle.RoundBound,
            GameLifecycle.ObjectivePlusSoulBound,
            GameLifecycle.ObjectiveBound
        )
    ),
    StartNewRoundOnPortalEnter(
        incompatibleWith = { setOf(StartNewRoundOnDeath) },
        availableLifecycles = setOf(
            GameLifecycle.RoundBound
        )
    ),
    RemovePlacedBlocksOnRoundStart(
        availableLifecycles = setOf(
            GameLifecycle.RoundBound
        )
    )
}
