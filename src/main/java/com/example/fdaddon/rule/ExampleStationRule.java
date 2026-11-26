package com.example.fdaddon.rule;

import com.huidu.farmersdelight.api.block.FarmersDelightBlocks;
import com.huidu.farmersdelight.api.content.ContentId;
import com.huidu.farmersdelight.api.rule.StationRule;
import org.bukkit.block.Block;

/** Example custom station: the template's CE example block doubles as a recipe-book station. */
public final class ExampleStationRule implements StationRule {

    public static final ContentId ID = ContentId.parse("fdaddon:example_block");
    public static final ExampleStationRule INSTANCE = new ExampleStationRule();
    private static final ContentId RECIPE_TYPE = ContentId.parse("fdaddon:example");

    private ExampleStationRule() {
    }

    @Override
    public ContentId id() {
        return ID;
    }

    @Override
    public boolean matches(Block block) {
        ContentId blockId = FarmersDelightBlocks.blockId(block);
        return blockId != null && blockId.equals(ID);
    }

    @Override
    public String recipeTypeId() {
        return RECIPE_TYPE.value();
    }
}
