# Armor Curve

*Flattens the armor curve — make damage reduction simple, predictable, and fully under your control.*

## Overview

Armor Curve replaces Minecraft's built-in armor calculation with a clean, linear damage reduction curve that you can rewrite entirely. It also adds separately configurable protection enchantment reduction and makes equipment attributes fade as durability drops.

This is a **NeoForge port** of the original Armor Curve mod by Jackiecrazy.

## Features

- **Linear, predictable armor curve** — 1% damage reduction per armor point, another 1% per 2 toughness points, with at least 5% of the damage always getting through.
- **Customizable Protection formula** — 2.5% reduction per protection point (up to 50%), never below 10%.
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
