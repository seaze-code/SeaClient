package com.seaclient.menu.hud;

import com.seaclient.menu.optimize.SeaClientOptimizer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Tracks real-time Hypixel Bedwars & PvP combat state:
 * - Reach distance (eye-to-hitbox closest point)
 * - Consecutive hit Combo counter
 * - Target HUD entity locking (crosshair + last attacked player)
 * - ToggleSprint state & execution
 * - Bedwars inventory resource & utility item scanning
 * - Bedwars Generator Upgrade Timer & AutoGG trigger
 */
public final class SeaClientPvpTracker {

    private static LivingEntity lastAttackedTarget = null;
    private static long lastAttackTimeMs = 0L;
    private static double lastReachDistance = 0.0;
    private static int comboCount = 0;
    private static int lastTargetId = -1;
    private static int prevLocalHurtTime = 0;

    // Bedwars match timer & AutoGG
    private static long matchStartTimeMs = 0L;
    private static long lastAutoGgSentMs = 0L;
    private static long pendingAutoGgSendAtMs = 0L;

    // Cached inventory counts updated each tick
    public static int ironCount = 0;
    public static int goldCount = 0;
    public static int diamondCount = 0;
    public static int emeraldCount = 0;

    public static int blocksCount = 0;
    public static int gappleCount = 0;
    public static int fireballCount = 0;
    public static int tntCount = 0;
    public static int pearlCount = 0;
    public static boolean holdingBlocks = false;

    private SeaClientPvpTracker() {
    }

    public static void onClientTick(Minecraft mc) {
        if (mc == null || mc.player == null || mc.level == null) {
            lastAttackedTarget = null;
            comboCount = 0;
            return;
        }
        LocalPlayer player = mc.player;
        SeaClientOptimizer opt = SeaClientOptimizer.getInstance();

        // 1. ToggleSprint execution
        if (opt.toggleSprintEnabled && mc.options != null) {
            if (mc.options.keyUp.isDown()
                    && !player.isShiftKeyDown()
                    && !player.isUsingItem()
                    && !player.horizontalCollision) {
                player.setSprinting(true);
            }
        }

        // 2. Reset combo if local player takes damage or 4.5s passes without attacking
        long now = System.currentTimeMillis();
        if (player.hurtTime > prevLocalHurtTime) {
            comboCount = 0;
        }
        prevLocalHurtTime = player.hurtTime;

        if (now - lastAttackTimeMs > 4500L) {
            comboCount = 0;
        }
        if (lastAttackedTarget != null && (!lastAttackedTarget.isAlive() || now - lastAttackTimeMs > opt.targetHudTimeoutMs)) {
            lastAttackedTarget = null;
        }

        // 3. Scan inventory for Bedwars resources & combat utilities
        scanInventory(player);

        // 4. Execute pending AutoGG if scheduled
        if (pendingAutoGgSendAtMs > 0L && now >= pendingAutoGgSendAtMs) {
            pendingAutoGgSendAtMs = 0L;
            if (mc.getConnection() != null && opt.autoGgEnabled && !"Off".equalsIgnoreCase(opt.autoGgMessage)) {
                mc.getConnection().sendChat(opt.autoGgMessage);
            }
        }
    }

    private static void scanInventory(LocalPlayer player) {
        int iron = 0, gold = 0, dia = 0, em = 0;
        int blocks = 0, gapples = 0, fireballs = 0, tnt = 0, pearls = 0;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            Item item = stack.getItem();
            int count = stack.getCount();

            if (item == Items.IRON_INGOT) iron += count;
            else if (item == Items.GOLD_INGOT) gold += count;
            else if (item == Items.DIAMOND) dia += count;
            else if (item == Items.EMERALD) em += count;
            else if (item == Items.GOLDEN_APPLE || item == Items.ENCHANTED_GOLDEN_APPLE) gapples += count;
            else if (item == Items.FIRE_CHARGE) fireballs += count;
            else if (item == Items.TNT) tnt += count;
            else if (item == Items.ENDER_PEARL) pearls += count;
            else if (item instanceof BlockItem) {
                blocks += count;
            }
        }

        ironCount = iron;
        goldCount = gold;
        diamondCount = dia;
        emeraldCount = em;
        blocksCount = blocks;
        gappleCount = gapples;
        fireballCount = fireballs;
        tntCount = tnt;
        pearlCount = pearls;

        ItemStack mainHand = player.getMainHandItem();
        holdingBlocks = !mainHand.isEmpty() && mainHand.getItem() instanceof BlockItem && mainHand.getItem() != Items.TNT;
    }

    public static void onAttack(Player attacker, Entity target) {
        if (attacker == null || target == null || !(target instanceof LivingEntity living)) {
            return;
        }
        long now = System.currentTimeMillis();
        Vec3 eye = attacker.getEyePosition();
        AABB box = target.getBoundingBox();
        double cx = Math.clamp(eye.x, box.minX, box.maxX);
        double cy = Math.clamp(eye.y, box.minY, box.maxY);
        double cz = Math.clamp(eye.z, box.minZ, box.maxZ);
        double dist = eye.distanceTo(new Vec3(cx, cy, cz));

        lastReachDistance = Math.min(6.0, dist);
        lastAttackTimeMs = now;
        lastAttackedTarget = living;

        if (target.getId() == lastTargetId) {
            if (living.hurtTime <= 3) {
                comboCount++;
            }
        } else {
            lastTargetId = target.getId();
            comboCount = 1;
        }
    }

    public static LivingEntity getActiveTarget(Minecraft mc) {
        if (mc != null && mc.crosshairPickEntity instanceof LivingEntity crosshairLiving && crosshairLiving.isAlive()) {
            return crosshairLiving;
        }
        if (lastAttackedTarget != null && lastAttackedTarget.isAlive()
                && System.currentTimeMillis() - lastAttackTimeMs <= SeaClientOptimizer.getInstance().targetHudTimeoutMs) {
            return lastAttackedTarget;
        }
        return null;
    }

    public static double getLastReachDistance() {
        if (System.currentTimeMillis() - lastAttackTimeMs > 5000L) {
            return 0.0;
        }
        return lastReachDistance;
    }

    public static int getComboCount() {
        return comboCount;
    }

    public static void onChatMessage(String rawText) {
        if (rawText == null || rawText.isEmpty()) {
            return;
        }
        SeaClientOptimizer opt = SeaClientOptimizer.getInstance();
        long now = System.currentTimeMillis();

        // Detect Bedwars match start
        if (rawText.contains("Protect your bed and destroy the enemy beds")
                || rawText.contains("The game starts in 1 second!")) {
            matchStartTimeMs = now;
        }

        // Detect Hypixel match end for AutoGG
        if (opt.autoGgEnabled && now - lastAutoGgSentMs > 8000L) {
            if (rawText.contains("1st Killer -")
                    || rawText.contains("Winning Team -")
                    || rawText.contains("Victory!")
                    || rawText.contains("Reward Summary")
                    || rawText.contains("1st Place -")) {
                lastAutoGgSentMs = now;
                pendingAutoGgSendAtMs = now + 450L;
            }
        }
    }

    public static void resetBedwarsTimer() {
        matchStartTimeMs = System.currentTimeMillis();
    }

    public static String getBedwarsNextEventString() {
        if (matchStartTimeMs <= 0L) {
            matchStartTimeMs = System.currentTimeMillis();
        }
        long elapsedSec = Math.max(0L, (System.currentTimeMillis() - matchStartTimeMs) / 1000L);
        int[] thresholds = {360, 720, 1080, 1440, 1800};
        String[] names = {"Diamond II", "Emerald II", "Diamond III", "Emerald III", "Bed Gone"};

        for (int i = 0; i < thresholds.length; i++) {
            if (elapsedSec < thresholds[i]) {
                long rem = thresholds[i] - elapsedSec;
                return String.format("%s in %d:%02d", names[i], rem / 60, rem % 60);
            }
        }
        return String.format("Match %d:%02d", elapsedSec / 60, elapsedSec % 60);
    }

    public static void sendHypixelCommand(String commandNoSlash) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.getConnection() != null) {
            mc.getConnection().sendCommand(commandNoSlash);
        }
    }
}
