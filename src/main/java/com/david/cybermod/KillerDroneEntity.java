package com.david.cybermod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class KillerDroneEntity extends Mob {
    @Nullable
    private BlockPos targetPos = null;
    private int lifeTicks = 0;

    public KillerDroneEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setGlowingTag(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.FLYING_SPEED, 0.375)
                .add(Attributes.MOVEMENT_SPEED, 0.375);
    }

    public void findTarget(BlockPos startPos) {
        if (level().isClientSide) return;

        BlockPos closestJammer = null;
        double closestDist = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(startPos.offset(-40, -15, -40), startPos.offset(40, 25, 40))) {
            BlockState state = level().getBlockState(pos);
            if (state.is(ModBlocks.JAMMER_BLOCK.get()) && state.getValue(JammerBlock.ACTIVE)) {
                double dist = pos.distSqr(startPos);
                if (dist < closestDist) {
                    closestDist = dist;
                    closestJammer = pos.immutable();
                }
            }
        }
        this.targetPos = closestJammer;
    }

    @Override
    public void tick() {
        super.tick();
        this.setGlowingTag(true);

        if (!level().isClientSide) {
            lifeTicks++;
            if (lifeTicks > 20 * 60) {
                this.discard();
                return;
            }

            if (targetPos == null || !level().getBlockState(targetPos).is(ModBlocks.JAMMER_BLOCK.get())) {
                findTarget(this.blockPosition());
            }

            if (targetPos != null) {
                Vec3 current = this.position();
                Vec3 targetVec = new Vec3(targetPos.getX() + 0.5, targetPos.getY() + 1.2, targetPos.getZ() + 0.5);
                Vec3 diff = targetVec.subtract(current);
                double distance = diff.length();

                // Скорость ровно 7.5 блоков/сек (0.375 блока за тик)
                if (distance > 1.3) {
                    Vec3 motion = diff.normalize().scale(0.375);
                    this.setDeltaMovement(motion);
                    this.move(MoverType.SELF, motion);
                } else {
                    level().destroyBlock(targetPos, false);
                    level().explode(this, targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5, 1.5f, Level.ExplosionInteraction.NONE);
                    level().playSound(null, targetPos, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.2f, 1.0f);
                    this.discard();
                }
            } else {
                this.setDeltaMovement(Vec3.ZERO);
            }
        } else if (random.nextInt(2) == 0) {
            level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.2, getZ(), 0, -0.05, 0);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (result && !level().isClientSide) {
            level().playSound(null, blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.HOSTILE, 0.8f, 1.8f);
        }
        return result;
    }
}
