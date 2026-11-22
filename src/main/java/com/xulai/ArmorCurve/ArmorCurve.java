package com.xulai.ArmorCurve;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(ArmorCurve.MODID)
public class ArmorCurve {
    public static final String MODID = "armorcurve";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ArmorCurve(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, CurveConfig.SPEC);
    }
}
