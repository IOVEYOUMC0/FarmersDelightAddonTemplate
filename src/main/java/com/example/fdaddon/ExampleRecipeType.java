package com.example.fdaddon;

import com.example.fdaddon.recipe.ExampleRecipeEditor;
import com.example.fdaddon.util.AddonLang;
import com.huidu.farmersdelight.api.item.FarmersDelightItems;
import com.huidu.farmersdelight.api.recipe.JumpTarget;
import com.huidu.farmersdelight.api.recipe.RecipeBookLayout;
import com.huidu.farmersdelight.api.recipe.RecipeEditor;
import com.huidu.farmersdelight.api.recipe.RecipeType;
import com.huidu.farmersdelight.api.recipe.ViewableRecipe;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A RecipeType so this addon's recipes show in a recipe book. A RecipeType is just a titled, icon'd
 * list of ViewableRecipes.
 *
 * Two ways to render it:
 * - Shared book (do nothing extra): FD shows your type in its generic recipe-book-gui.
 *       Simple, but if several addons register types they share one category menu.
 * - Independent book (override listLayout()/detailLayout()): you supply your
 *       OWN page layout (title, grid, decorations) and FD renders an independent book just for your type —
 *       open it with openRecipeBook(player, "fdaddon:example", filler). This example does that.
 * For an editable type, also override editor() (see api.recipe.RecipeEditor).
 */
public final class ExampleRecipeType implements RecipeType {

    // A single stable editor instance so its in-memory drafts persist across editor opens.
    private final ExampleRecipeEditor editor = new ExampleRecipeEditor();

    @Override
    public String id() {
        return "fdaddon:example";
    }

    @Override
    public Component title() {
        return text("fdaddon.example_recipe.title");
    }

    @Override
    public ItemStack icon() {
        // Any ItemStack works (vanilla or a CraftEngine custom item via FarmersDelightItems.create).
        ItemStack icon = FarmersDelightItems.create("minecraft:rabbit_stew");
        return icon != null ? icon : new ItemStack(Material.RABBIT_STEW);
    }

    @Override
    public List<ViewableRecipe> recipes() {
        // Build these from your own data/config. One hard-coded example shown here.
        return List.of(new ExampleViewableRecipe());
    }

    @Override
    public RecipeEditor editor() {
        // Returning an editor makes this type editable in-game through FarmersDelight's generic recipe
        // editor GUI. Return null (the default) for a read-only type.
        return editor;
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
                text("fdaddon.example_recipe.list_title"),
                6,
                List.of("RRRRRRRRR",
                        "RRRRRRRRR",
                        "RRRRRRRRR",
                        "RRRRRRRRR",
                        "RRRRRRRRR",
                        "PXXXBXXXN"),
                Map.of('R', "recipe", 'P', "prev_page", 'N', "next_page", 'B', "back", 'X', "background"),
                Map.of("background", filler(Material.GRAY_STAINED_GLASS_PANE),
                        "prev_page", named(Material.ARROW, text("fdaddon.example_recipe.prev_page")),
                        "next_page", named(Material.ARROW, text("fdaddon.example_recipe.next_page")),
                        "back", named(Material.BARRIER, text("fdaddon.example_recipe.close"))));
    }

    @Override
    public RecipeBookLayout detailLayout() {
        // One recipe: 'I' ingredients, 'R' result, 'T' a CUSTOM role filled from displaySlots() below,
        // plus back/fill buttons. Any legend char that isn't a known/dynamic role is static decoration.
        return new Layout(
                text("fdaddon.example_recipe.detail_title"),
                6,
                List.of("XXXXXXXXX",
                        "XIIIXTXRX",
                        "XXXXXXXXX",
                        "XXXXXXXXX",
                        "XXXXXXXXX",
                        "XXXXBXFXX"),
                Map.of('I', "ingredient", 'R', "result", 'T', "tool", 'F', "fill", 'B', "back", 'X', "background"),
                Map.of("background", filler(Material.GRAY_STAINED_GLASS_PANE),
                        "fill", named(Material.HOPPER, text("fdaddon.example_recipe.fill")),
                        "back", named(Material.ARROW, text("fdaddon.example_recipe.back"))));
    }

    /** One viewable recipe: inputs, a result, optional info lines, and optional custom display slots. */
    private static final class ExampleViewableRecipe implements ViewableRecipe {
        @Override
        public String id() {
            return "example_stew";
        }

        @Override
        public List<ItemStack> inputs() {
            List<ItemStack> items = new ArrayList<>();
            ItemStack carrot = FarmersDelightItems.create("minecraft:carrot");
            if (carrot != null) items.add(carrot);
            ItemStack potato = FarmersDelightItems.create("minecraft:potato");
            if (potato != null) items.add(potato);
            ItemStack bowl = FarmersDelightItems.create("minecraft:bowl");
            if (bowl != null) items.add(bowl);
            return items;
        }

        @Override
        public ItemStack result() {
            return FarmersDelightItems.create("minecraft:rabbit_stew");
        }

        @Override
        public List<Component> infoLines(Player viewer) {
            return List.of(text("fdaddon.example_recipe.description"));
        }

        @Override
        public Map<String, List<ItemStack>> displaySlots() {
            // Extra items keyed by a custom role; the detail layout's matching legend slots ('T'->"tool")
            // get filled with them. Use this for things the flat inputs/result can't express (e.g. the keg
            // addon uses "fluid"/"return" roles for its fermenting recipes).
            return Map.of("tool", List.of(FarmersDelightItems.create("minecraft:wooden_axe")));
        }

        @Override
        public Map<String, JumpTarget> jumpTargets() {
            // Make the 'tool' detail slot clickable: clicking it opens another recipe's detail page. A real
            // addon points this at the recipe that PRODUCES that item; here it self-references for the demo.
            // A role absent from this map (the default empty map) is simply not clickable.
            return Map.of("tool", new JumpTarget("fdaddon:example", "example_stew"));
        }
    }

    // ── Tiny helpers to build decoration/button items (a real addon would read these from its gui.yml) ──

    /** A plain data RecipeBookLayout; FD's default methods derive slot lookups from layout+legend. */
    private record Layout(Component title, int rows, List<String> layout,
                          Map<Character, String> legend, Map<String, ItemStack> decorations)
            implements RecipeBookLayout {
    }

    /**
     * Player-visible text from this addon's language layer, so the book follows the server locale like every
     * other message. MiniMessage tags in the yml value are parsed here.
     */
    private static Component text(String key) {
        return MiniMessage.miniMessage().deserialize(AddonLang.get(key))
                .decoration(TextDecoration.ITALIC, false);
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

    private static ItemStack named(Material material, Component name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }
}
