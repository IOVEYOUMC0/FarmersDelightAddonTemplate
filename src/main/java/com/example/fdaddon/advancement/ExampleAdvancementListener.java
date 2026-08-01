package com.example.fdaddon.advancement;

import com.huidu.farmersdelight.api.advancement.FarmersDelightAdvancements;
import com.huidu.farmersdelight.api.event.FarmersDelightProduceEvent;
import com.huidu.farmersdelight.api.item.FarmersDelightItems;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Awards example advancements based on player actions. Two trigger paths:
 *   - FarmersDelightProduceEvent — fired by FarmersDelight (and any addon, e.g. BrewinAndChewin's
 *         keg) when a station hands a produced item to a player. The simplest hook for "the player got X".
 *   - PlayerItemConsumeEvent — fired when the player finishes eating/drinking an item. Used for
 *         the multiTask criteria: each soup variant ticks one criterion.
 *
 * On PlayerJoinEvent we proactively grant the root + show the tab so it appears in the player's
 * advancement screen immediately (otherwise UAA-managed tabs only appear after the first grant).
 */
public final class ExampleAdvancementListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!FarmersDelightAdvancements.isAvailable()) return;
        Player player = event.getPlayer();
        if (!ExampleAdvancements.has(player, ExampleAdvancements.ROOT)) {
            ExampleAdvancements.award(player, ExampleAdvancements.ROOT);
        }
        FarmersDelightAdvancements.showTab(ExampleAdvancements.TAB_ID, player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onProduce(FarmersDelightProduceEvent event) {
        if (!FarmersDelightAdvancements.isAvailable()) return;
        Player player = event.getPlayerId() == null ? null : Bukkit.getPlayer(event.getPlayerId());
        if (player == null) return;
        String id = FarmersDelightItems.idOf(event.getResult());
        if ("minecraft:rabbit_stew".equals(id)) {
            ExampleAdvancements.award(player, ExampleAdvancements.FIRST_STEW);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (!FarmersDelightAdvancements.isAvailable()) return;
        Player player = event.getPlayer();
        String id = FarmersDelightItems.idOf(event.getItem());
        if (id == null) return;

        // The multiTask criteria are the unprefixed material names — match by id suffix.
        for (String criterion : ExampleAdvancements.SAMPLER_PLATTER_CRITERIA) {
            if (("minecraft:" + criterion).equals(id)) {
                ExampleAdvancements.awardCriterion(player, ExampleAdvancements.SAMPLER_PLATTER, criterion);
                return;
            }
        }
    }
}
