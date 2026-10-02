package com.xulai.ArmorCurve;

import java.math.BigDecimal;
import java.util.Map;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

@EventBusSubscriber(modid = ArmorCurve.MODID)
public class CurveConfig {
    private static final String DEFAULT_ARMOR_FORMULA = "damage*MAX(0.05, 1-(armor+toughness/2)/100)";
    private static final String DEFAULT_TOUGHNESS_FORMULA = "damage";
    private static final String DEFAULT_ENCHANTMENT_FORMULA = "damage*MAX(0.1, 1-2.5*enchant/100)";
    private static final String DEFAULT_DEGRADATION_FORMULA = "MIN(1, remaining/(MAX(max,1)*0.75))";
    private static final BigDecimal PROBE_DAMAGE = BigDecimal.valueOf(8D);
    private static final BigDecimal PROBE_ARMOR = BigDecimal.valueOf(12D);
    private static final BigDecimal PROBE_TOUGHNESS = BigDecimal.valueOf(3D);
    private static final BigDecimal PROBE_ENCHANT = BigDecimal.valueOf(7D);
    private static final BigDecimal PROBE_REMAINING = BigDecimal.valueOf(50L);
    private static final BigDecimal PROBE_MAX = BigDecimal.valueOf(100L);
    private static final Map<String, BigDecimal> ARMOR_PROBE = Map.of("damage", PROBE_DAMAGE, "armor", PROBE_ARMOR, "toughness", PROBE_TOUGHNESS);
    private static final Map<String, BigDecimal> ENCHANT_PROBE = Map.of("damage", PROBE_DAMAGE, "enchant", PROBE_ENCHANT);
    private static final Map<String, BigDecimal> DEGRADATION_PROBE = Map.of("remaining", PROBE_REMAINING, "max", PROBE_MAX);

    public static final ModConfigSpec SPEC;
    private static final CurveConfig CONFIG;
    private static volatile Formulas formulas = Formulas.defaults();

    private final ModConfigSpec.ConfigValue<String> firstFormula;
    private final ModConfigSpec.ConfigValue<String> secondFormula;
    private final ModConfigSpec.ConfigValue<String> enchantmentFormula;
    private final ModConfigSpec.ConfigValue<String> degradationFormula;
    private final ModConfigSpec.BooleanValue universalDegradation;

    static {
        final Pair<CurveConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(CurveConfig::new);
        CONFIG = specPair.getLeft();
        SPEC = specPair.getRight();
    }

    private CurveConfig(ModConfigSpec.Builder builder) {
        firstFormula = builder.comment(
                "",
                " 第一道减伤公式：把护甲和韧性换算成线性百分比减伤。",
                " 可用变量：damage（本次伤害）、armor（护甲）、toughness（护甲韧性）。",
                " 默认公式的含义：每 1 点护甲减伤 1%，每 2 点韧性再多减 1%，无论如何至少留下 5% 伤害。",
                " 用 MAX 兜底是因为一旦系数变成 0 或负数，完全免伤不说，负伤害还会反过来给自己叠加吸收（金心）。",
                " 例：20 护甲 + 8 韧性 = 减 24%；60 护甲 + 40 韧性 = 减 80%。",
                " 特殊值：填 \"damage\" 表示这一步不做任何修改，直接进入下一道公式。",
                " 改完直接保存文件即可实时生效，不需要重启游戏。",
                "",
                " First damage reduction formula: armor and toughness become a flat percentage reduction.",
                " Valid variables: damage, armor, toughness.",
                " Default: 1% less damage per armor point, 1% more per 2 toughness points, and at least 5% of the damage always gets through.",
                " The MAX guard matters: at 0 the target is immune, and a negative result would grant absorption hearts instead of damage.",
                " Example: 20 armor + 8 toughness = 24% reduced, 60 armor + 40 toughness = 80% reduced.",
                " Set to \"damage\" to leave the damage untouched at this step.",
                " Edits are picked up live: save the file, no restart required.").define("first_damage_reduction_formula", DEFAULT_ARMOR_FORMULA);

        secondFormula = builder.comment(
                "",
                " 第二道减伤公式，紧跟在第一道之后，输入是第一道算完的结果。",
                " 原本用于实现“护甲韧性防猝死”保底，可用变量与第一道公式相同。",
                " 默认关闭：减伤全部交给第一道那条线性曲线，避免两条曲线叠加把伤害压成 0。",
                " 想要在这里再加一层，可以写类似 damage*MAX(0.5, 1-toughness/40) 的式子。",
                " 特殊值：填 \"damage\" 表示这一步不做任何修改。",
                "",
                " Second damage reduction formula, chained right after the first one and fed with its result.",
                " Originally meant for the toughness sudden-death protection; same variables as the first formula.",
                " Disabled by default so that only the first curve reduces damage, which keeps the two curves from stacking down to zero.",
                " To add a second layer here, use something like damage*MAX(0.5, 1-toughness/40).",
                " Set to \"damage\" to leave the damage untouched at this step.").define("second_damage_reduction_formula", DEFAULT_TOUGHNESS_FORMULA);

        enchantmentFormula = builder.comment(
                "",
                " 保护类附魔的减伤公式，在护甲减伤之后结算。",
                " 可用变量：damage（护甲减伤后剩下的伤害）、enchant（保护附魔的减伤点数，原版一般为 4×等级，最高 20）。",
                " 默认公式的含义：每 1 点保护减伤 2.5%，最多减掉 50%，并且始终保留 10% 伤害。",
                " 注意方向：enchant 越大应该是受伤越小，写成 damage*enchant/40 那种比例会把保护变成负担。",
                " 特殊值：填 \"damage\" 表示附魔完全不参与减伤。",
                "",
                " Protection enchantment damage reduction formula, applied after armor reduction.",
                " Valid variables: damage (what is left after armor), enchant (protection points, usually 4 x level and capped at 20).",
                " Default: 2.5% less damage per protection point, up to 50% reduced, and never less than 10% of the damage.",
                " Watch the direction: more enchant has to mean less damage, so a bare damage*enchant/40 would turn protection into a penalty.",
                " Set to \"damage\" to let protection enchantments do nothing.").define("enchantment_damage_reduction_formula", DEFAULT_ENCHANTMENT_FORMULA);

        degradationFormula = builder.comment(
                "",
                " 护甲耐久衰减公式：装备提供的属性乘上该公式的结果，耐久越低属性越弱。",
                " 可用变量：remaining（剩余耐久）、max（耐久上限）。",
                " 默认公式的含义：前 25% 的磨损完全不惩罚（MIN 把系数封顶在 1，满耐久也不会有额外加成），",
                " 之后随剩余耐久线性下滑，快碎时属性趋近于 0。",
                " 例：耐久上限 528 的钻甲衫，剩余 396 以上属性完整；剩一半时只有 2/3 的护甲；剩 132 时只剩 1/3。",
                " 特殊值：填 \"1\" 表示关闭耐久衰减。",
                "",
                " Armor degradation formula: equipment attribute modifiers are multiplied by its result.",
                " Valid variables: remaining (durability left), max (maximum durability).",
                " Default: the first 25% of wear is free, and MIN caps the factor at 1 so full durability grants no bonus either.",
                " After that the stats fall off linearly and approach 0 right before the item breaks.",
                " Example: a 528 durability diamond chestplate keeps full stats above 396 remaining, 2/3 at half, 1/3 at 132.",
                " Set to \"1\" to disable durability degradation.").define("armor_degradation_formula", DEFAULT_DEGRADATION_FORMULA);

        universalDegradation = builder.comment(
                "",
                " 设为 false 时只有护甲值随耐久衰减，韧性等其他装备属性保持原值。",
                " 名称中包含 stealth 的属性（例如 Project: War Dance 的潜行加成）无论如何都不会衰减。",
                " 该开关作用于装备贡献的全部属性修饰符，因此附魔给装备的属性（若有）也会一起衰减。",
                "",
                " Set to false to only cause the armor value to degrade, leaving toughness and other modifiers untouched.",
                " Attributes whose name contains stealth, such as the Project: War Dance stealth bonus, never degrade either way.",
                " It covers every attribute modifier the equipment contributes, so enchantment-granted attributes decay as well.").define("universal_armor_degradation", true);
    }

    public static Formulas formulas() {
        return formulas;
    }

    @SubscribeEvent
    public static void onModConfigEvent(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        if (event instanceof ModConfigEvent.Loading || event instanceof ModConfigEvent.Reloading) {
            bake();
        }
    }

    private static void bake() {
        formulas = new Formulas(
                Formula.compile(CONFIG.firstFormula.get(), "damage", ARMOR_PROBE),
                Formula.compile(CONFIG.secondFormula.get(), "damage", ARMOR_PROBE),
                Formula.compile(CONFIG.enchantmentFormula.get(), "damage", ENCHANT_PROBE),
                Formula.compile(CONFIG.degradationFormula.get(), "1", DEGRADATION_PROBE),
                CONFIG.universalDegradation.get());
    }

    public record Formulas(Formula armor, Formula toughness, Formula enchantment, Formula degradation, boolean degradeAll) {
        static Formulas defaults() {
            return new Formulas(
                    Formula.compile(DEFAULT_ARMOR_FORMULA, "damage", ARMOR_PROBE),
                    Formula.compile(DEFAULT_TOUGHNESS_FORMULA, "damage", ARMOR_PROBE),
                    Formula.compile(DEFAULT_ENCHANTMENT_FORMULA, "damage", ENCHANT_PROBE),
                    Formula.compile(DEFAULT_DEGRADATION_FORMULA, "1", DEGRADATION_PROBE),
                    true);
        }
    }
}
