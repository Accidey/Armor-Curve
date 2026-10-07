![Armor Curve](src/main/resources/logo.png)

# Armor Curve

*Flattens the armor curve — make damage reduction simple, predictable, and fully under your control.*

## Overview

Armor Curve replaces Minecraft's built-in armor calculation with a clean, linear damage reduction curve that you can rewrite entirely. It also adds separately configurable protection enchantment reduction and makes equipment attributes fade as durability drops.

This is a **NeoForge port** of the original Armor Curve mod by Jackiecrazy.

## Features

- **Linear, predictable armor curve** — 1% damage reduction per armor point, another 1% per 2 toughness points, with at least 5% of the damage always getting through.
- **Customizable Protection formula** — 2.5% reduction per protection point (up to 50%), never below 10%.
- **Breach still works** — the weapon's `armor_effectiveness` enchantment effect (vanilla Breach / 破甲 included) is applied on top of the curve through vanilla's own enchantment hook.
- **Durability-based degradation** — Armor attributes scale with remaining durability; the first 25% of wear is free.
- **Live config reload** — save the config file and changes apply immediately, no restart needed.
- **Safe by design** — invalid formulas fall back to safe defaults; failed evaluations leave the original damage untouched.
- **Optional universal degradation** — restrict decay to armor points only, or keep "stealth" attributes intact.

## Installation

1. Install Minecraft **1.21.1** and **NeoForge 21.1.249 or later**.
2. Download the mod `.jar` from the release page.
3. Place the `.jar` into the `mods` folder of your game directory.
4. Launch the game.

## Configuration

All formulas live in `config/armorcurve-common.toml`.

```
# Armor damage reduction
damage*MAX(0.05, 1-(armor+toughness/2)/100)

# Secondary damage reduction (disabled by default)
damage

# Protection enchantment reduction
damage*MAX(0.1, 1-2.5*enchant/100)

# Durability degradation
MIN(1, remaining/(MAX(max,1)*0.75))
```

- Set a formula to `damage` to skip that reduction step.
- Set the degradation formula to `1` to disable durability decay.
- Variables: `damage`, `armor`, `toughness`, `enchant`, `remaining`, `max`.
- See [MOD_INTRO.md](MOD_INTRO.md) for the full configuration guide.

## Requirements

- Minecraft **1.21.1**
- NeoForge **21.1.249 or later**
- Java 21

## Credits & License

- Original mod by Jackiecrazy: [CurseForge](https://www.curseforge.com/minecraft/mc-mods/armor-curve) · [Modrinth](https://modrinth.com/mod/armor-curve)
- This NeoForge port: xulai
- Special thanks to the people behind EvalEx
- License: GPL-3.0 (see [LICENSE](LICENSE) and [NOTICE](NOTICE))
- Report issues: [GitHub Issues](https://github.com/Jackiecrazy/ArmorCurve/issues)

---

![Armor Curve](src/main/resources/logo.png)

# Armor Curve

*将原版护甲曲线拉平——让减伤变得简单、可预测，并且完全由你掌控。*

## 模组简介

Armor Curve 用一条简洁、线性的减伤曲线替换了 Minecraft 原版的护甲计算机制，并且这条曲线完全可以由你改写。它还提供了独立可配置的保护附魔减伤，并让装备属性随耐久损耗而衰减。

本模组是 Jackiecrazy 原版 Armor Curve 的 **NeoForge 移植版**。

## 功能特性

- **线性、可预期的护甲曲线** —— 每 1 点护甲减伤 1%，每 2 点韧性再额外减伤 1%，任何伤害至少保留 5%。
- **可自定义的保护附魔公式** —— 每 1 点保护减伤 2.5%（最多 50%），伤害永远不会低于 10%。
- **破甲附魔照常生效** —— 武器上的 `armor_effectiveness` 附魔效果（含原版“破甲”）会在曲线算完之后继续结算，走的是原版同一个附魔钩子。
- **耐久衰减机制** —— 装备属性随剩余耐久缩放，前 25% 的磨损完全免费。
- **配置实时生效** —— 保存配置文件后立即生效，无需重启游戏。
- **安全设计** —— 无效公式自动回退到安全默认值；求值失败时本次伤害保持原值。
- **可选的全面衰减** —— 可只衰减护甲值，名称包含 "stealth" 的属性始终不受影响。

## 安装方法

1. 安装 Minecraft **1.21.1** 与 **NeoForge 21.1.249 或更高版本**。
2. 从发布页面下载模组 `.jar` 文件。
3. 将 `.jar` 放入游戏目录的 `mods` 文件夹。
4. 启动游戏。

## 配置说明

所有公式都位于 `config/armorcurve-common.toml`。

```
# 护甲减伤公式
damage*MAX(0.05, 1-(armor+toughness/2)/100)

# 第二道减伤公式（默认关闭）
damage

# 保护附魔减伤公式
damage*MAX(0.1, 1-2.5*enchant/100)

# 耐久衰减公式
MIN(1, remaining/(MAX(max,1)*0.75))
```

- 将某个公式设为 `damage`，即可跳过该步骤的减伤。
- 将耐久衰减公式设为 `1`，即可关闭耐久衰减。
- 可用变量：`damage`、`armor`、`toughness`、`enchant`、`remaining`、`max`。
- 完整配置指南见 [MOD_INTRO.md](MOD_INTRO.md)。

## 运行环境

- Minecraft **1.21.1**
- NeoForge **21.1.249 或更高版本**
- Java 21

## 作者与许可

- 原版模组作者 Jackiecrazy：[CurseForge](https://www.curseforge.com/minecraft/mc-mods/armor-curve) · [Modrinth](https://modrinth.com/mod/armor-curve)
- 本 NeoForge 移植版：xulai
- 特别感谢 EvalEx 背后的开发者
- 许可证：GPL-3.0（见 [LICENSE](LICENSE) 与 [NOTICE](NOTICE)）
- 问题反馈：[GitHub Issues](https://github.com/Jackiecrazy/ArmorCurve/issues)