package com.suslimc.threnody.entity;

import com.suslimc.threnody.ThrenodyMod;
import com.suslimc.threnody.config.ThrenodyConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.Nullable;

public class ThrenodyEntity extends Monster {
    private static final EntityDataAccessor<Byte> DATA_STAGE =
            SynchedEntityData.defineId(ThrenodyEntity.class, EntityDataSerializers.BYTE);
    private static final TagKey<net.minecraft.world.level.block.Block> BREAKABLE_BLOCKS =
            TagKey.create(
                    net.minecraft.core.registries.Registries.BLOCK,
                    ResourceLocation.fromNamespaceAndPath(ThrenodyMod.MODID, "breakable_by_threnody")
            );
    private static final int STAGE_DURATION_TICKS = 1_200;
    private static final double[] HEALTH_BY_STAGE = {20.0D, 24.0D, 28.0D, 34.0D, 40.0D, 50.0D};
    private static final double[] SPEED_BY_STAGE = {0.23D, 0.27D, 0.25D, 0.24D, 0.30D, 0.33D};
    private static final double[] DAMAGE_BY_STAGE = {3.0D, 4.0D, 5.0D, 7.0D, 8.0D, 10.0D};

    private int stageTicks;

    public ThrenodyEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 12;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_STAGE, (byte) Stage.STALKER.getId());
    }

    public Stage getStage() {
        return Stage.fromId(this.entityData.get(DATA_STAGE));
    }

    public void setStage(Stage stage) {
        if (stage == getStage()) {
            return;
        }
        this.entityData.set(DATA_STAGE, (byte) stage.getId());
        refreshDimensions();
        applyStageAttributes(stage, true);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, true));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HEALTH_BY_STAGE[0])
                .add(Attributes.MOVEMENT_SPEED, SPEED_BY_STAGE[0])
                .add(Attributes.ATTACK_DAMAGE, DAMAGE_BY_STAGE[0])
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.15D);
    }

    public static boolean checkThrenodySpawnRules(
            EntityType<ThrenodyEntity> type,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        if (!ThrenodyConfig.COMMON.spawnEnabled.get()
                || level.getDifficulty() == Difficulty.PEACEFUL
                || level.getLevel().isDay()
                || level.getMaxLocalRawBrightness(pos) > ThrenodyConfig.COMMON.spawnLightThreshold.get()) {
            return false;
        }
        return Monster.checkMonsterSpawnRules(type, level, spawnType, pos, random);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty,
            MobSpawnType spawnType,
            @Nullable SpawnGroupData spawnData,
            @Nullable CompoundTag dataTag
    ) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData, dataTag);
        applyStageAttributes(getStage(), false);
        return result;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            spawnStageParticle();
            return;
        }

        if (this.level().isDay() && this.level().dimensionType().hasSkyLight()) {
            ((ServerLevel) this.level()).sendParticles(
                    ParticleTypes.LARGE_SMOKE,
                    getX(),
                    getY() + getBbHeight() * 0.5D,
                    getZ(),
                    12,
                    0.3D,
                    0.6D,
                    0.3D,
                    0.02D
            );
            discard();
            return;
        }

        advanceStage();
        chooseRememberedTarget();
        applyStageMovement();
        tryBreakBlockingBlock();
    }

    private void advanceStage() {
        if (++stageTicks < STAGE_DURATION_TICKS || getStage() == Stage.ESCHATON) {
            return;
        }

        stageTicks = 0;
        setStage(Stage.fromId(getStage().getId() + 1));
        this.level().broadcastEntityEvent(this, (byte) 60);
    }

    private void chooseRememberedTarget() {
        if (tickCount % 100 != 0 && getTarget() != null && getTarget().isAlive()) {
            return;
        }

        ServerPlayer rememberedTarget = PlayerMemory.findPreferredTarget((ServerLevel) level(), this);
        if (rememberedTarget != null) {
            setTarget(rememberedTarget);
        }
    }

    private void applyStageMovement() {
        if ((getStage() == Stage.CRAWLER || getStage() == Stage.ESCHATON) && this.horizontalCollision) {
            Vec3 movement = getDeltaMovement();
            setDeltaMovement(movement.x, Math.max(0.2D, movement.y), movement.z);
        }
    }

    private void tryBreakBlockingBlock() {
        if ((getStage() != Stage.BREAKER && getStage() != Stage.ESCHATON)
                || ThrenodyConfig.COMMON.disableBlockBreaking.get()
                || tickCount % 20 != 0
                || !ForgeEventFactory.getMobGriefingEvent(this.level(), this)) {
            return;
        }

        BlockPos targetPos = blockPosition().relative(getDirection());
        BlockState state = this.level().getBlockState(targetPos);
        if (state.is(BREAKABLE_BLOCKS) && state.getDestroySpeed(this.level(), targetPos) >= 0.0F) {
            this.level().destroyBlock(targetPos, true, this);
        }
    }

    private void spawnStageParticle() {
        int interval = Math.max(3, 14 - getStage().getId() * 2);
        if (this.random.nextInt(interval) == 0) {
            this.level().addParticle(
                    getStage() == Stage.ESCHATON ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SMOKE,
                    getRandomX(0.7D),
                    getRandomY(),
                    getRandomZ(0.7D),
                    0.0D,
                    0.01D,
                    0.0D
            );
        }
    }

    private void applyStageAttributes(Stage stage, boolean preserveHealthRatio) {
        float healthRatio = getMaxHealth() > 0.0F ? getHealth() / getMaxHealth() : 1.0F;
        setBaseValue(Attributes.MAX_HEALTH, HEALTH_BY_STAGE[stage.getId()]);
        setBaseValue(Attributes.MOVEMENT_SPEED, SPEED_BY_STAGE[stage.getId()]);
        setBaseValue(Attributes.ATTACK_DAMAGE, DAMAGE_BY_STAGE[stage.getId()]);
        if (preserveHealthRatio) {
            setHealth(Math.max(1.0F, getMaxHealth() * healthRatio));
        }
    }

    private void setBaseValue(net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        if (!super.doHurtTarget(target)) {
            return false;
        }

        if (target instanceof Player player) {
            int stage = getStage().getId();
            if (stage >= Stage.CRAWLER.getId()) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30 + stage * 10, stage / 3));
            }
            if (stage >= Stage.HUNTMASTER.getId()) {
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
            }
            if (player instanceof ServerPlayer serverPlayer) {
                PlayerMemory.recordEncounter(serverPlayer, blockPosition(), 3 + stage);
            }
        }
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FIRE)) {
            amount *= getStage() == Stage.CRAWLER ? 1.5F : 1.2F;
        } else if (getStage() == Stage.BREAKER && source.getDirectEntity() instanceof Player) {
            amount *= 0.75F;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && source.getEntity() instanceof ServerPlayer player) {
            PlayerMemory.recordEncounter(player, blockPosition(), 1 + getStage().getId());
        }
        return hurt;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        if (getStage() == Stage.SQUEEZE) {
            return EntityDimensions.scalable(0.55F, 0.85F);
        }
        return super.getDimensions(pose);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putByte("ThrenodyStage", (byte) getStage().getId());
        compound.putInt("ThrenodyStageTicks", stageTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        setStage(Stage.fromId(compound.getByte("ThrenodyStage")));
        stageTicks = Math.max(0, compound.getInt("ThrenodyStageTicks"));
        applyStageAttributes(getStage(), false);
    }
}
