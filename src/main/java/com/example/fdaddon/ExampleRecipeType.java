package com.example.fdaddon;

import com.huidu.farmersdelight.api.item.FarmersDelightItems;
import com.huidu.farmersdelight.api.recipe.RecipeType;
import com.huidu.farmersdelight.api.recipe.ViewableRecipe;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * A minimal {@link RecipeType} so this addon's recipes show in FarmersDelight's generic recipe book.
 * A RecipeType is just a titled, icon'd list of {@link ViewableRecipe}s — FD renders the menu/list/detail.
 * For an editable type, also override {@code editor()} (see api.recipe.RecipeEditor).
 */
public final class ExampleRecipeType implements RecipeType {

    @Override
    public String id() {
        return "fdaddon:example";
    }

    @Override
    public Component title() {
        return Component.text("Example Recipes", NamedTextColor.GOLD);
    }

    @Override
    public ItemStack icon() {
        // Any ItemStack works (vanilla or a CraftEngine custom item via FarmersDelightItems.create).
        return FarmersDelightItems.create("minecraft:rabbit_stew");
    }

    @Override
    public List<ViewableRecipe> recipes() {
        // Build these from your own data/config. One hard-coded example shown here.
        return List.of(new ExampleViewableRecipe());
    }

    /** One viewable recipe: inputs on the left, a result, and optional info lines. */
    private static final class ExampleViewableRecipe implements ViewableRecipe {
        @Override
        public String id() {
            return "example_stew";
        }

        @Override
        public List<ItemStack> inputs() {
            return List.of(
                    FarmersDelightItems.create("minecraft:carrot"),
                    FarmersDelightItems.create("minecraft:potato"),
                    FarmersDelightItems.create("minecraft:bowl"));
        }

        @Override
        public ItemStack result() {
            return FarmersDelightItems.create("minecraft:rabbit_stew");
        }

        @Override
        public List<Component> infoLines(Player viewer) {
            return List.of(Component.text("Cook in a cooking pot.", NamedTextColor.GRAY));
        }
    }
}
