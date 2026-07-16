package com.example.fdaddon.recipe;

import com.huidu.farmersdelight.api.item.FarmersDelightItems;
import com.huidu.farmersdelight.api.recipe.IngredientMatching;
import com.huidu.farmersdelight.api.recipe.RecipeFiller;
import com.huidu.farmersdelight.api.recipe.ViewableRecipe;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * A Fill button for the recipe book. When a filler is supplied to openRecipeBook, FarmersDelight shows a
 * Fill button that calls fill to move a recipe's ingredients from the player's inventory into the station
 * the book was opened from. Bind one filler to a specific station per open, so Fill and back target that
 * station.
 *
 * This example only CHECKS whether the inventory can satisfy the recipe, using FarmersDelight's
 * order-independent IngredientMatching, and reports the result. A real filler would then remove exactly one
 * matching item per ingredient and place it into the station under the station's lock, staying dupe-safe by
 * removing exactly what it places.
 */
public final class ExampleRecipeFiller implements RecipeFiller {

    @Override
    public boolean fill(Player player, ViewableRecipe recipe) {
        List<ItemStack> required = recipe.inputs();
        if (required == null || required.isEmpty()) {
            return false;
        }
        List<ItemStack> slots = new ArrayList<>();
        for (ItemStack content : player.getInventory().getContents()) {
            if (content != null) {
                slots.add(content);
            }
        }
        // Order-independent assignment: is there a distinct inventory item for each required ingredient?
        // matcher answers "does this slot satisfy this ingredient?"; initialAmount is the slot's stack size.
        boolean canFill = IngredientMatching.matchesIngredients(
                required,
                slots,
                false, // exactSlots=false: extra, unrelated inventory items are allowed
                (slot, ingredient) -> FarmersDelightItems.matchesId(slot, FarmersDelightItems.idOf(ingredient)),
                ItemStack::getAmount);
        if (!canFill) {
            return false;
        }
        // A real filler would now remove one matching item per ingredient and place it into the station.
        return true;
    }

    @Override
    public boolean onBack(Player player) {
        // Reopen the originating station GUI and return true; returning false just lets the book close.
        return false;
    }
}
