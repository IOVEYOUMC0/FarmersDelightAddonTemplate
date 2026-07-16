package com.example.fdaddon.recipe;

import com.huidu.farmersdelight.api.recipe.EditableRecipe;
import com.huidu.farmersdelight.api.recipe.NumericField;
import com.huidu.farmersdelight.api.recipe.RecipeEditor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Makes a recipe type editable in-game through FarmersDelight's generic editor GUI. FarmersDelight's GUI
 * calls these methods; the addon only implements them, persisting to its own storage. This example keeps an
 * in-memory store keyed by recipe id, so edits do not survive a restart. A real addon would read and write
 * its own YAML here and reload its recipe manager after a successful save.
 *
 * itemSlotLabels defines how many editable item slots the draft has, one label per slot. numericFields are
 * the editable number buttons. load builds a draft (blank when id is null or blank); save and delete write
 * to storage and return true on success.
 */
public final class ExampleRecipeEditor implements RecipeEditor {

    // Number-field keys, shared between the field definitions and EditableRecipe.number / setNumber storage.
    private static final String COOK_TIME = "cook_time";
    private static final String EXPERIENCE = "experience";

    private final Map<String, EditableRecipe> store = new ConcurrentHashMap<>();

    @Override
    public List<String> itemSlotLabels() {
        // The slot count is this list's size; the draft's item(0..size-1) correspond to these labels.
        return List.of("Ingredient", "Ingredient", "Ingredient");
    }

    @Override
    public List<NumericField> numericFields() {
        // key, button label, step, min, max, decimals (0 = integer). Left-click adds a step, right subtracts.
        return List.of(
                new NumericField(COOK_TIME, "Cook time (ticks)", 20, 20, 100000, 0),
                new NumericField(EXPERIENCE, "Experience", 0.5, 0, 100, 1));
    }

    @Override
    public EditableRecipe load(String id) {
        if (id != null && !id.isBlank()) {
            EditableRecipe saved = store.get(id);
            if (saved != null) {
                return saved;
            }
        }
        // Blank draft. The slot count MUST equal itemSlotLabels().size(); seed sensible number defaults.
        EditableRecipe draft = new EditableRecipe(id, itemSlotLabels().size());
        draft.setNumber(COOK_TIME, 200);
        draft.setNumber(EXPERIENCE, 1.0);
        draft.setResultCount(1);
        return draft;
    }

    @Override
    public boolean save(EditableRecipe draft) {
        if (draft == null || draft.id() == null || draft.id().isBlank() || draft.result() == null) {
            return false; // reject invalid drafts
        }
        // A real addon would write these to its own YAML and reload its manager. The edited values are all
        // available here: draft.result(), draft.resultCount(), draft.number(COOK_TIME, 200),
        // draft.number(EXPERIENCE, 1.0), and draft.item(0..itemSlotCount()-1) (null for an empty slot).
        store.put(draft.id(), draft);
        return true;
    }

    @Override
    public boolean delete(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        return store.remove(id) != null;
    }
}
