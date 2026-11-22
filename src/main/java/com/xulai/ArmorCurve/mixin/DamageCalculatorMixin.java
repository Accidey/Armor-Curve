package com.xulai.ArmorCurve.mixin;

import com.xulai.ArmorCurve.CurveConfig;
import com.xulai.ArmorCurve.Formula;
import java.math.BigDecimal;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CombatRules.class)
public class DamageCalculatorMixin {
    private static final String DAMAGE = "damage";
    private static final String ARMOR = "armor";
    private static final String TOUGHNESS = "toughness";
    private static final String ENCHANT = "enchant";

    @Inject(method = "getDamageAfterAbsorb(Lnet/minecraft/world/entity/LivingEntity;FLnet/minecraft/world/damagesource/DamageSource;FF)F", at = @At("HEAD"), cancellable = true)
    private static void armorCurve$applyArmorFormulas(LivingEntity target, float damage, DamageSource source, float armor, float toughness, CallbackInfoReturnable<Float> info) {
        if (!Float.isFinite(damage) || !Float.isFinite(armor) || !Float.isFinite(toughness)) {
            return;
        }
        CurveConfig.Formulas formulas = CurveConfig.formulas();
        BigDecimal input = BigDecimal.valueOf(damage);
        BigDecimal armorValue = BigDecimal.valueOf(armor);
        BigDecimal toughnessValue = BigDecimal.valueOf(toughness);
        Formula first = formulas.armor();
        Formula second = formulas.toughness();
        BigDecimal reduced = armorCurve$apply(first, input, () -> first.evaluate(DAMAGE, input, ARMOR, armorValue, TOUGHNESS, toughnessValue));
        float result = armorCurve$apply(second, reduced, () -> second.evaluate(DAMAGE, reduced, ARMOR, armorValue, TOUGHNESS, toughnessValue)).floatValue();
        if (Float.isFinite(result)) {
            info.setReturnValue(armorCurve$applyWeaponEffects(target, damage, source, result));
        }
    }

    @Inject(method = "getDamageAfterMagicAbsorb(FF)F", at = @At("HEAD"), cancellable = true)
    private static void armorCurve$applyEnchantmentFormula(float damage, float protection, CallbackInfoReturnable<Float> info) {
        if (!Float.isFinite(damage) || !Float.isFinite(protection)) {
            return;
        }
        Formula formula = CurveConfig.formulas().enchantment();
        BigDecimal input = BigDecimal.valueOf(damage);
        BigDecimal enchantValue = BigDecimal.valueOf(protection);
        float result = armorCurve$apply(formula, input, () -> formula.evaluate(DAMAGE, input, ENCHANT, enchantValue)).floatValue();
        if (Float.isFinite(result)) {
            info.setReturnValue(result);
        }
    }

    private static float armorCurve$applyWeaponEffects(LivingEntity target, float damage, DamageSource source, float reduced) {
        ItemStack weapon = source.getWeaponItem();
        if (weapon == null || weapon.isEmpty() || damage <= 0.0F || !(target.level() instanceof ServerLevel level)) {
            return reduced;
        }
        float before = Mth.clamp(1.0F - reduced / damage, 0.0F, 1.0F);
        float after = Mth.clamp(EnchantmentHelper.modifyArmorEffectiveness(level, weapon, target, source, before), 0.0F, 1.0F);
        return after == before ? reduced : reduced + damage * (before - after);
    }

    private static BigDecimal armorCurve$apply(Formula formula, BigDecimal damage, Supplier<BigDecimal> evaluation) {
        if (formula.passthrough(DAMAGE)) {
            return damage;
        }
        try {
            BigDecimal result = evaluation.get();
            if (Double.isFinite(result.doubleValue())) {
                return result;
            }
            formula.reportFailure(new ArithmeticException("Result " + result.toPlainString() + " is not a finite number."));
        } catch (RuntimeException exception) {
            formula.reportFailure(exception);
        }
        return damage;
    }
}
