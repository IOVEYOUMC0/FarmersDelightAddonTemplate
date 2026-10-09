package com.example.fdaddon.recipe;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExampleRecipeEditorTest {
    @Test void editingACancelledDraftDoesNotChangeTheSavedRecipe() {
        var editor = new ExampleRecipeEditor();
        var draft = editor.load("example:test");
        draft.setResult(new Stack());
        draft.setNumber("cook_time", 200);
        assertTrue(editor.save(draft));
        draft.setNumber("cook_time", 1);
        var opened = editor.load("example:test");
        assertEquals(200, opened.number("cook_time", 0));
        opened.setNumber("cook_time", 2);
        opened.result().setAmount(9);
        assertEquals(200, editor.load("example:test").number("cook_time", 0));
        assertEquals(1, editor.load("example:test").result().getAmount());
    }
    private static final class Stack extends ItemStack {
        private int amount = 1;
        @Override public Material getType() { return Material.PAPER; }
        @Override public int getAmount() { return amount; }
        @Override public void setAmount(int value) { amount = value; }
        @Override public Stack clone() { Stack copy = new Stack(); copy.amount = amount; return copy; }
    }
}
