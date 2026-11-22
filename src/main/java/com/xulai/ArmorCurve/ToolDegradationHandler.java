package com.xulai.ArmorCurve;

import java.math.BigDecimal;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = ArmorCurve.MODID)
public class ToolDegradationHandler {
    private static final String REMAINING = "remaining";
    private static final String MAX = "max";

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        ItemStack stack = event.getEntity().getMainHandItem();
        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0 || !stack.has(DataComponents.TOOL)) {
            return;
        }
        Formula tool = CurveConfig.formulas().tool();
        if (tool.isOne()) {
            return;
        }
        BigDecimal remaining = BigDecimal.valueOf((long) maxDamage - stack.getDamageValue());
        BigDecimal max = BigDecimal.valueOf(maxDamage);
        double factor;
        try {
            factor = tool.evaluate(REMAINING, remaining, MAX, max).doubleValue();
        } catch (RuntimeException exception) {
            tool.reportFailure(exception);
            return;
        }
        if (!Double.isFinite(factor) || factor == 1.0D) {
            return;
        }
        event.setNewSpeed((float) (event.getNewSpeed() * factor));
    }
}