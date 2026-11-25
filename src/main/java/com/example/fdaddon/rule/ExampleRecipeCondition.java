package com.example.fdaddon.rule;

import com.huidu.farmersdelight.api.content.ContentId;
import com.huidu.farmersdelight.api.rule.RecipeCondition;
import com.huidu.farmersdelight.api.rule.RecipeContext;
import org.bukkit.GameMode;

/**
 * Example addon recipe condition: spectators cannot trigger FD recipe matches.
 *
 * Conditions are global in phase one, so return true for every recipe the condition does not
 * want to gate. Use RecipeContext.recipeType / RecipeContext.recipeId to scope
 * a condition to your own recipes.
 */
public final class ExampleRecipeCondition implements RecipeCondition {

    public static final ContentId ID = ContentId.parse("fdaddon:no_spectator");
    public static final ExampleRecipeCondition INSTANCE = new ExampleRecipeCondition();

    private ExampleRecipeCondition() {
    }

    @Override
    public ContentId id() {
        return ID;
    }

    @Override
    public boolean test(RecipeContext context) {
        if (context == null || context.player() == null) {
            return true;
        }
        return context.player().getGameMode() != GameMode.SPECTATOR;
    }
}
