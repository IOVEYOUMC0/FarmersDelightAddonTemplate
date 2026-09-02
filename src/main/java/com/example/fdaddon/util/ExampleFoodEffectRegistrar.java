package com.example.fdaddon.util;

import com.huidu.farmersdelight.api.effect.FarmersDelightFoodEffects;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Config-driven registration of FarmersDelight's Comfort + Nourishment food effects. Reads two lists from
 * a config section and registers each item-id → duration-ticks mapping with FD.
 *
 * apply(ConfigurationSection) replaces this registrar's managed entries, so config reloads leave no
 * stale registrations.
 *
 * Expected config shape (under your plugin's config.yml):
 * food-effects:
 *   comfort:
 *     - id: "minecraft:golden_apple"
 *       duration: 600
 *   nourishment:
 *     - id: "minecraft:cooked_beef"
 *       duration: 300
 *
 * Mirrors the production pattern in BrewinAndChewin's FoodEffectRegistrar.
 */
public final class ExampleFoodEffectRegistrar {

    private final Set<String> registeredComfort = new HashSet<>();
    private final Set<String> registeredNourishment = new HashSet<>();

    /**
     * Replace managed registrations from section. Pass a null or empty section to clear everything.
     */
    public void apply(ConfigurationSection section) {
        clear();
        if (section == null) return;
        loadList(section, "comfort", true);
        loadList(section, "nourishment", false);
    }

    /** Remove every food effect managed by this registrar. Idempotent. */
    public void clear() {
        for (String id : registeredComfort) FarmersDelightFoodEffects.unregisterComfortFood(id);
        for (String id : registeredNourishment) FarmersDelightFoodEffects.unregisterNourishmentFood(id);
        registeredComfort.clear();
        registeredNourishment.clear();
    }

    private void loadList(ConfigurationSection root, String key, boolean comfort) {
        List<?> raw = root.getList(key);
        if (raw == null) return;
        for (Object entry : raw) {
            if (!(entry instanceof java.util.Map<?, ?> map)) continue;
            Object idObj = map.get("id");
            Object durObj = map.get("duration");
            if (!(idObj instanceof String id) || id.isBlank()) continue;
            int duration = durObj instanceof Number n ? n.intValue() : 0;
            if (duration <= 0) continue;
            if (comfort) {
                FarmersDelightFoodEffects.registerComfortFood(id, duration);
                registeredComfort.add(id);
            } else {
                FarmersDelightFoodEffects.registerNourishmentFood(id, duration);
                registeredNourishment.add(id);
            }
        }
    }
}
