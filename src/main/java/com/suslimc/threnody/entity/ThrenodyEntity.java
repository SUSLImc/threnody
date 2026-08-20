package com.suslimc.threnody.entity;

import com.suslimc.threnody.ThrenodyMod;
import com.suslimc.threnody.ai.ThrenodyMeleeGoal;
import com.suslimc.threnody.ai.ThrenodyStalkGoal;
import com.suslimc.threnody.config.ThrenodyConfig;
import com.suslimc.threnody.registration.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A patient, towering stalker. It keeps its distance, holds perfectly still while it is being
 * watched, and only closes the gap when nobody is looking. Once it stops pretending it does not
 * stop, and once it has killed it is gone.
 */
public class ThrenodyEntity extends Monster implements GeoEntity {
    public static final String ACTION_CONTROLLER = "action";
    public static final String ANIM_ATTACK = "attack";
    public static final String ANIM_SHRIEK = "shriek";
    public static final String ANIM_VANISH = "vanish";

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.threnody.idle");
    private static final RawAnimation STARE_ANIM = RawAnimation.begin().thenLoop("animation.threnody.stare");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("animation.threnody.walk");
    private static final RawAnimation CHASE_ANIM = RawAnimation.begin().thenLoop("animation.threnody.chase");
    private static final RawAnimation CRAWL_ANIM = RawAnimation.begin().thenLoop("animation.threnody.crawl");
    private static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("animation.threnody.attack");
    private static final RawAnimation SHRIEK_ANIM = RawAnimation.begin().thenPlay("animation.threnody.shriek");
    private static final RawAnimation VANISH_ANIM = RawAnimation.begin().thenPlay("animation.threnody.vanish");

    private static final EntityDataAccessor<Byte> DATA_STAGE =
            SynchedEntityData.defineId(ThrenodyEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_STATE =
            SynchedEntityData.defineId(ThrenodyEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_WATCHED =
            SynchedEntityData.defineId(ThrenodyEntity.class, EntityDataSerializers.BOOLEAN);

    private static final TagKey<Block> BREAKABLE_BLOCKS = TagKey.create(
            net.minecraft.core.registries.Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(ThrenodyMod.MODID, "breakable_by_threnody")
    );

    private static final int STAGE_DURATION_TICKS = 2_400;
    private static final int VANISH_DURATION_TICKS = 20;
    private static final double[] HEALTH_BY_STAGE = {40.0D, 48.0D, 56.0D, 68.0D, 80.0D, 100.0D};
    private static final double[] SPEED_BY_STAGE = {0.24D, 0.27D, 0.26D, 0.26D, 0.31D, 0.34D};
    private static final double[] DAMAGE_BY_STAGE = {6.0D, 7.0D, 8.0D, 10.0D, 12.0D, 15.0D};

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    private int stageTicks;
    private int lurkTicks;
    private int observedTicks;
    private int vanishTicks = -1;
    private int heartbeatCooldown;
    private int ambientCooldown = 200;

    public ThrenodyEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 20;
    }

    // ── Synched state ───────────────────────────────────────────────

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_STAGE, (byte) Stage.STALKER.getId());
        this.entityData.define(DATA_STATE, (byte) ThrenodyState.LURKING.getId());
        this.entityData.define(DATA_WATCHED, false);
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

    public ThrenodyState getState() {
        return ThrenodyState.fromId(this.entityData.get(DATA_STATE));
    }

    private void setState(ThrenodyState state) {
        this.entityData.set(DATA_STATE, (byte) state.getId());
    }

    public boolean isWatched() {
        return this.entityData.get(DATA_WATCHED);
    }

    public boolean isHunting() {
        return getState() == ThrenodyState.HUNTING;
    }

    public boolean isVanishing() {
        return getState() == ThrenodyState.VANISHING;
    }

    /** While frozen it must not drift, turn, or be pushed around. */
    public boolean isFrozen() {
        return isWatched() && !isHunting() && ThrenodyConfig.COMMON.freezeWhenWatched.get();
    }

    // ── Setup ───────────────────────────────────────────────────────

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ThrenodyMeleeGoal(this));
        this.goalSelector.addGoal(2, new ThrenodyStalkGoal(this));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 40.0F, 1.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isPrey));
    }

    private boolean isPrey(LivingEntity candidate) {
        return candidate instanceof Player player && !player.isCreative() && !player.isSpectator();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HEALTH_BY_STAGE[0])
                .add(Attributes.MOVEMENT_SPEED, SPEED_BY_STAGE[0])
                .add(Attributes.ATTACK_DAMAGE, DAMAGE_BY_STAGE[0])
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.85D)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0D);
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
            DifficultyInstance difficulty,
            MobSpawnType spawnType,
            @Nullable SpawnGroupData spawnData,
            @Nullable CompoundTag dataTag
    ) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData, dataTag);
        applyStageAttributes(getStage(), false);
        return result;
    }

    // ── Core loop ───────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            spawnAmbientParticles();
            return;
        }

        if (isVanishing()) {
            tickVanish();
            return;
        }

        if (this.level().isDay() && this.level().dimensionType().hasSkyLight()) {
            beginVanish();
            return;
        }

        updateObservation();

        if (isFrozen()) {
            holdStill();
        }

        advanceStage();
        chooseTarget();
        escalateIfDue();
        applyStagePressure();
        tryBreakBlockingBlock();
        tickAtmosphere();
    }

    /** Freezing is what makes it feel alive: it simply refuses to move while you look at it. */
    private void holdStill() {
        getNavigation().stop();
        setDeltaMovement(0.0D, Math.min(0.0D, getDeltaMovement().y), 0.0D);
        setSpeed(0.0F);
        setXxa(0.0F);
        setZza(0.0F);
    }

    private void updateObservation() {
        Player watcher = nearestWatcher();
        boolean watched = watcher != null;
        this.entityData.set(DATA_WATCHED, watched);

        if (!watched) {
            observedTicks = Math.max(0, observedTicks - 2);
            return;
        }

        if (isHunting()) {
            return;
        }

        observedTicks++;
        getLookControl().setLookAt(watcher, 30.0F, 30.0F);

        int limit = ThrenodyConfig.COMMON.secondsWatchedBeforeVanish.get() * 20;
        if (observedTicks > limit) {
            beginVanish();
        }
    }

    /** Returns the closest player currently looking in this entity's direction with clear line of sight. */
    private @Nullable Player nearestWatcher() {
        Player closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (Player player : this.level().players()) {
            if (player.isSpectator() || !player.isAlive() || player.isCreative()) {
                continue;
            }
            double distance = player.distanceToSqr(this);
            if (distance > 4_096.0D || distance >= closestDistance || !isLookedAtBy(player)) {
                continue;
            }
            closest = player;
            closestDistance = distance;
        }
        return closest;
    }

    private boolean isLookedAtBy(Player player) {
        Vec3 view = player.getViewVector(1.0F).normalize();
        Vec3 toEntity = new Vec3(getX() - player.getX(), getEyeY() - player.getEyeY(), getZ() - player.getZ());
        double distance = toEntity.length();
        if (distance < 1.0E-4D) {
            return true;
        }
        double alignment = view.dot(toEntity.normalize());
        return alignment > 0.88D && player.hasLineOfSight(this);
    }

    private void chooseTarget() {
        if (tickCount % 40 != 0 && getTarget() != null && getTarget().isAlive()) {
            return;
        }
        ServerPlayer remembered = PlayerMemory.findPreferredTarget((ServerLevel) level(), this);
        if (remembered != null) {
            setTarget(remembered);
        }
    }

    private void escalateIfDue() {
        if (isHunting()) {
            return;
        }

        lurkTicks++;
        LivingEntity target = getTarget();
        boolean cornered = target != null && target.distanceToSqr(this) < 25.0D;
        boolean patienceSpent = lurkTicks > ThrenodyConfig.COMMON.secondsBeforeHunt.get() * 20;

        if (cornered || patienceSpent) {
            beginHunt();
        }
    }

    public void beginHunt() {
        if (isHunting() || isVanishing()) {
            return;
        }
        setState(ThrenodyState.HUNTING);
        observedTicks = 0;
        triggerAnim(ACTION_CONTROLLER, ANIM_SHRIEK);
        playThrenodySound(ModSounds.SHRIEK.get(), 1.6F, 1.0F);

        if (getTarget() instanceof ServerPlayer player) {
            player.playNotifySound(ModSounds.SEEN.get(), SoundSource.HOSTILE, 0.9F, 1.0F);
        }
    }

    private void applyStagePressure() {
        if (!isHunting() || !ThrenodyConfig.COMMON.darknessWhenClose.get() || tickCount % 40 != 0) {
            return;
        }
        if (getTarget() instanceof ServerPlayer player && player.distanceToSqr(this) < 100.0D) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0, false, false, true));
        }
    }

    private void advanceStage() {
        if (++stageTicks < STAGE_DURATION_TICKS || getStage() == Stage.ESCHATON) {
            return;
        }
        stageTicks = 0;
        setStage(Stage.fromId(getStage().getId() + 1));
    }

    private void tryBreakBlockingBlock() {
        if (!isHunting()
                || (getStage() != Stage.BREAKER && getStage() != Stage.ESCHATON)
                || ThrenodyConfig.COMMON.disableBlockBreaking.get()
                || tickCount % 20 != 0
                || !ForgeEventFactory.getMobGriefingEvent(this.level(), this)) {
            return;
        }

        BlockPos targetPos = blockPosition().above().relative(getDirection());
        BlockState state = this.level().getBlockState(targetPos);
        if (state.is(BREAKABLE_BLOCKS) && state.getDestroySpeed(this.level(), targetPos) >= 0.0F) {
            this.level().destroyBlock(targetPos, false, this);
        }
    }

    // ── Vanishing ───────────────────────────────────────────────────

    /** Starts the disappearance. It is never killed by daylight or by finishing a hunt; it simply leaves. */
    public void beginVanish() {
        if (isVanishing()) {
            return;
        }
        setState(ThrenodyState.VANISHING);
        setTarget(null);
        getNavigation().stop();
        vanishTicks = VANISH_DURATION_TICKS;
        triggerAnim(ACTION_CONTROLLER, ANIM_VANISH);
        playThrenodySound(ModSounds.VANISH.get(), 1.2F, 1.0F);
    }

    private void tickVanish() {
        holdStill();
        setInvulnerable(true);

        if (--vanishTicks > 0) {
            return;
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.LARGE_SMOKE,
                    getX(), getY() + getBbHeight() * 0.5D, getZ(),
                    26, 0.35D, 0.9D, 0.35D, 0.02D
            );
            serverLevel.sendParticles(
                    ParticleTypes.SMOKE,
                    getX(), getY(), getZ(),
                    18, 0.4D, 0.1D, 0.4D, 0.01D
            );
        }
        discard();
    }

    // ── Atmosphere ──────────────────────────────────────────────────

    private void tickAtmosphere() {
        LivingEntity target = getTarget();
        if (!(target instanceof ServerPlayer player)) {
            return;
        }
        double distance = Math.sqrt(player.distanceToSqr(this));

        if (ThrenodyConfig.COMMON.heartbeat.get() && distance < 26.0D && --heartbeatCooldown <= 0) {
            float closeness = (float) (1.0D - Math.min(distance, 26.0D) / 26.0D);
            heartbeatCooldown = (int) (46 - 26 * closeness);
            player.playNotifySound(
                    ModSounds.HEARTBEAT.get(),
                    SoundSource.HOSTILE,
                    0.25F + closeness * 0.55F,
                    0.86F + closeness * 0.3F
            );
        }

        if (!ThrenodyConfig.COMMON.ambientSounds.get() || --ambientCooldown > 0) {
            return;
        }
        ambientCooldown = 260 + this.random.nextInt(420);

        if (isHunting()) {
            playThrenodySound(ModSounds.CHASE.get(), 1.1F, 1.0F);
            return;
        }

        SoundEvent cue = switch (this.random.nextInt(3)) {
            case 0 -> ModSounds.WHISPER.get();
            case 1 -> ModSounds.KNOCK.get();
            default -> ModSounds.DRONE.get();
        };
        playThrenodySound(cue, 0.85F, 0.94F + this.random.nextFloat() * 0.12F);
    }

    private void playThrenodySound(SoundEvent sound, float volume, float pitch) {
        this.level().playSound(null, getX(), getY(), getZ(), sound, SoundSource.HOSTILE, volume, pitch);
    }

    private void spawnAmbientParticles() {
        if (this.random.nextInt(isHunting() ? 6 : 22) != 0) {
            return;
        }
        this.level().addParticle(
                ParticleTypes.SMOKE,
                getRandomX(0.55D),
                getY() + this.random.nextDouble() * getBbHeight() * 0.65D,
                getRandomZ(0.55D),
                0.0D, 0.005D, 0.0D
        );
    }

    // ── Combat ──────────────────────────────────────────────────────

    @Override
    public boolean doHurtTarget(Entity target) {
        triggerAnim(ACTION_CONTROLLER, ANIM_ATTACK);
        if (!super.doHurtTarget(target)) {
            return false;
        }

        if (target instanceof Player player) {
            int stage = getStage().getId();
            if (stage >= Stage.CRAWLER.getId()) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40 + stage * 10, stage / 3));
            }
            if (player instanceof ServerPlayer serverPlayer) {
                PlayerMemory.recordEncounter(serverPlayer, blockPosition(), 3 + stage);
            }
            if (!player.isAlive() && ThrenodyConfig.COMMON.vanishAfterKill.get()) {
                beginVanish();
            }
        }
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isVanishing()) {
            return false;
        }
        if (source.is(DamageTypeTags.IS_FIRE)) {
            amount *= 1.35F;
        }

        boolean hurt = super.hurt(source, amount);
        if (hurt && source.getEntity() instanceof ServerPlayer player) {
            PlayerMemory.recordEncounter(player, blockPosition(), 2 + getStage().getId());
            beginHunt();
        }
        return hurt;
    }

    @Override
    public boolean isPushable() {
        return !isFrozen() && super.isPushable();
    }

    @Override
    protected void pushEntities() {
        if (!isFrozen()) {
            super.pushEntities();
        }
    }

    @Override
    public void push(double x, double y, double z) {
        if (!isFrozen()) {
            super.push(x, y, z);
        }
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || isFrozen() || isVanishing();
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

    private void setBaseValue(Attribute attribute, double value) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        if (getStage() == Stage.SQUEEZE) {
            return EntityDimensions.scalable(0.6F, 1.45F);
        }
        return super.getDimensions(pose);
    }

    // ── Sound ───────────────────────────────────────────────────────

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.SHRIEK.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.VANISH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (!isFrozen()) {
            playSound(ModSounds.STEP.get(), isHunting() ? 0.5F : 0.22F, 0.92F + this.random.nextFloat() * 0.16F);
        }
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    // ── Persistence ─────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putByte("ThrenodyStage", (byte) getStage().getId());
        compound.putByte("ThrenodyState", (byte) getState().getId());
        compound.putInt("ThrenodyStageTicks", stageTicks);
        compound.putInt("ThrenodyLurkTicks", lurkTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        setStage(Stage.fromId(compound.getByte("ThrenodyStage")));
        setState(ThrenodyState.fromId(compound.getByte("ThrenodyState")));
        stageTicks = Math.max(0, compound.getInt("ThrenodyStageTicks"));
        lurkTicks = Math.max(0, compound.getInt("ThrenodyLurkTicks"));
        if (isVanishing()) {
            setState(ThrenodyState.LURKING);
        }
        applyStageAttributes(getStage(), false);
    }

    // ── Animation ───────────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 8, this::movementAnimation));
        controllers.add(new AnimationController<>(this, ACTION_CONTROLLER, 2, state -> PlayState.STOP)
                .triggerableAnim(ANIM_ATTACK, ATTACK_ANIM)
                .triggerableAnim(ANIM_SHRIEK, SHRIEK_ANIM)
                .triggerableAnim(ANIM_VANISH, VANISH_ANIM));
    }

    private PlayState movementAnimation(AnimationState<ThrenodyEntity> state) {
        AnimationController<ThrenodyEntity> controller = state.getController();

        if (isWatched() && !isHunting()) {
            controller.setAnimation(STARE_ANIM);
        } else if (getStage() == Stage.SQUEEZE && state.isMoving()) {
            controller.setAnimation(CRAWL_ANIM);
        } else if (state.isMoving()) {
            controller.setAnimation(isHunting() ? CHASE_ANIM : WALK_ANIM);
        } else {
            controller.setAnimation(IDLE_ANIM);
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
