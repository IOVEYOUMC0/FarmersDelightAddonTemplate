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
 * Awards example advancements on produced-item and item-consumption events.
 * Joining grants the root so the advancement tab is visible before any task is completed.
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
