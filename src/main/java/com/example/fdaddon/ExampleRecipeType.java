package com.example.fdaddon;

import com.huidu.farmersdelight.api.item.FarmersDelightItems;
import com.huidu.farmersdelight.api.recipe.RecipeBookLayout;
import com.huidu.farmersdelight.api.recipe.RecipeType;
import com.huidu.farmersdelight.api.recipe.ViewableRecipe;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;

/**
 * A {@link RecipeType} so this addon's recipes show in a recipe book. A RecipeType is just a titled, icon'd
 * list of {@link ViewableRecipe}s.
 *
 * <p>Two ways to render it:
 * <ul>
 *   <li><b>Shared book</b> (do nothing extra): FD shows your type in its generic {@code recipe-book-gui}.
 *       Simple, but if several addons register types they share one category menu.</li>
 *   <li><b>Independent book</b> (override {@link #listLayout()}/{@link #detailLayout()}): you supply your
 *       OWN page layout (title, grid, decorations) and FD renders an independent book just for your type —
 *       open it with {@code openRecipeBook(player, "fdaddon:example", filler)}. This example does that.</li>
 * </ul>
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

    // ── Optional: give this type its OWN independent recipe book ─────────────────────────────────
    // Return null from these (or don't override them) to fall back to FD's shared book. When non-null,
    // FD renders YOUR layout. Build them from your own gui.yml so server owners can edit them; titles may
    // contain CraftEngine <image:ns:id>/<shift:N> tags for custom-texture backgrounds (resolve them
    // yourself via CraftEngine before passing the Component — FD uses the title as-is).

    @Override
    public RecipeBookLayout listLayout() {
        // A paginated grid of recipes + page/back buttons. 'R' = recipe slot (filled by FD from recipes()).
        return new Layout(
                Component.text("Example Recipes", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false),
                6,
                List.of("RRRRRRRRR",
                        "RRRRRRRRR",
                        "RRRRRRRRR",
                        "RRRRRRRRR",
                        "RRRRRRRRR",
                        "PXXXBXXXN"),
                Map.of('R', "recipe", 'P', "prev_page", 'N', "next_page", 'B', "back", 'X', "background"),
                Map.of("background", filler(Material.GRAY_STAINED_GLASS_PANE),
                        "prev_page", named(Material.ARROW, "Previous"),
                        "next_page", named(Material.ARROW, "Next"),
                        "back", named(Material.BARRIER, "Close")));
    }

    @Override
    public RecipeBookLayout detailLayout() {
        // One recipe: 'I' ingredients, 'R' result, 'T' a CUSTOM role filled from displaySlots() below,
        // plus back/fill buttons. Any legend char that isn't a known/dynamic role is static decoration.
        return new Layout(
                Component.text("Example Recipe", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false),
                6,
                List.of("XXXXXXXXX",
                        "XIIIXTXRX",
                        "XXXXXXXXX",
                        "XXXXXXXXX",
                        "XXXXXXXXX",
                        "XXXXBXFXX"),
                Map.of('I', "ingredient", 'R', "result", 'T', "tool", 'F', "fill", 'B', "back", 'X', "background"),
                Map.of("background", filler(Material.GRAY_STAINED_GLASS_PANE),
                        "fill", named(Material.HOPPER, "Fill ingredients"),
                        "back", named(Material.ARROW, "Back")));
    }

    /** One viewable recipe: inputs, a result, optional info lines, and optional custom display slots. */
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
            return List.of(Component.text("Cook in a cooking pot.", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }

        @Override
        public Map<String, List<ItemStack>> displaySlots() {
            // Extra items keyed by a custom role; the detail layout's matching legend slots ('T'->"tool")
            // get filled with them. Use this for things the flat inputs/result can't express (e.g. the keg
            // addon uses "fluid"/"return" roles for its fermenting recipes).
            return Map.of("tool", List.of(FarmersDelightItems.create("minecraft:wooden_axe")));
        }
    }

    // ── Tiny helpers to build decoration/button items (a real addon would read these from its gui.yml) ──

    /** A plain data {@link RecipeBookLayout}; FD's default methods derive slot lookups from layout+legend. */
    private record Layout(Component title, int rows, List<String> layout,
                          Map<Character, String> legend, Map<String, ItemStack> decorations)
            implements RecipeBookLayout {
    }

    private static ItemStack filler(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            meta.setHideTooltip(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack named(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }
}
