package com.xulai.ArmorCurve.mixin;

import com.xulai.ArmorCurve.CurveConfig;
import com.xulai.ArmorCurve.Formula;
import java.math.BigDecimal;
import java.util.function.BiConsumer;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemStack.class)
public class ArmorDegradationMixin {
    private static final String REMAINING = "remaining";
    private static final String MAX = "max";

    @ModifyVariable(method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V", at = @At("HEAD"), argsOnly = true)
    private BiConsumer<Holder<Attribute>, AttributeModifier> armorCurve$degradeBySlot(BiConsumer<Holder<Attribute>, AttributeModifier> consumer, EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) {
            return consumer;
        }
        return armorCurve$wrap(consumer);
    }

    @ModifyVariable(method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Ljava/util/function/BiConsumer;)V", at = @At("HEAD"), argsOnly = true)
    private BiConsumer<Holder<Attribute>, AttributeModifier> armorCurve$degradeBySlotGroup(BiConsumer<Holder<Attribute>, AttributeModifier> consumer, EquipmentSlotGroup group) {
        if (group == EquipmentSlotGroup.HAND || group == EquipmentSlotGroup.MAINHAND || group == EquipmentSlotGroup.OFFHAND) {
            return consumer;
        }
        return armorCurve$wrap(consumer);
    }

    private BiConsumer<Holder<Attribute>, AttributeModifier> armorCurve$wrap(BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
        ItemStack stack = (ItemStack) (Object) this;
        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) {
            return consumer;
        }
        CurveConfig.Formulas formulas = CurveConfig.formulas();
        Formula degradation = formulas.degradation();
        if (degradation.isOne()) {
            return consumer;
        }
        BigDecimal remaining = BigDecimal.valueOf((long) maxDamage - stack.getDamageValue());
        BigDecimal max = BigDecimal.valueOf(maxDamage);
        double factor;
        try {
            factor = degradation.evaluate(REMAINING, remaining, MAX, max).doubleValue();
        } catch (RuntimeException exception) {
            degradation.reportFailure(exception);
            return consumer;
        }
        if (!Double.isFinite(factor) || factor == 1.0D) {
            return consumer;
        }
        boolean degradeAll = formulas.degradeAll();
        return (attribute, modifier) -> {
            if (armorCurve$degrades(attribute, degradeAll)) {
                consumer.accept(attribute, new AttributeModifier(modifier.id(), modifier.amount() * factor, modifier.operation()));
            } else {
                consumer.accept(attribute, modifier);
            }
        };
    }

    private static boolean armorCurve$degrades(Holder<Attribute> attribute, boolean degradeAll) {
        if (attribute.value().getDescriptionId().contains("stealth")) {
            return false;
        }
        return degradeAll || attribute.is(Attributes.ARMOR);
    }
}
