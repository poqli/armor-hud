package ru.berdinskiybear.armorhud.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ru.berdinskiybear.armorhud.ArmorHudMod;
import ru.berdinskiybear.armorhud.config.ArmorHudConfig;
import java.util.ArrayList;
import java.util.List;

@Mixin(BossBarHud.class)
public class BossbarHudMixin {

    @Shadow @Final private MinecraftClient client;

    private final List<ItemStack> armorHud_armorItems = new ArrayList<>(4);

    @ModifyVariable(method = "render", at = @At(value = "STORE", ordinal = 0), ordinal = 1)
    public int calculateOffset(int offset) {
        ArmorHudConfig currentConfig = this.armorHud_getCurrentArmorHudConfig();
        if (currentConfig.isEnabled() && currentConfig.isPushBossbars()) {
            int add = 0;
            if (currentConfig.getAnchor() == ArmorHudConfig.Anchor.Top_Center) {
                int amount = 0;
                PlayerEntity playerEntity = this.getCameraPlayer();
                if (playerEntity != null) {
                    this.armorHud_armorItems.clear();
                    for (ItemStack itemStack : playerEntity.getInventory().armor) {
                        if (!itemStack.isEmpty())
                            amount++;
                        if (!itemStack.isEmpty() || currentConfig.getSlotsShown() != ArmorHudConfig.SlotsShown.Equipped)
                            this.armorHud_armorItems.add(itemStack);
                    }

                    if (!(amount == 0 && currentConfig.getSlotsShown() != ArmorHudConfig.SlotsShown.Always)) {
                        if (currentConfig.getOrientation() == ArmorHudConfig.Orientation.Vertical) {
                            if (currentConfig.getSlotsShown() == ArmorHudConfig.SlotsShown.Equipped)
                                add += 22 + 20 * (amount - 1) + currentConfig.getOffsetY();
                            else
                                add += 82 + currentConfig.getOffsetY();
                        }
                        else
                            add += 22 + currentConfig.getOffsetY();
                        if (currentConfig.isWarningShown() && this.armorHud_armorItems.stream().anyMatch((ItemStack itemStack) -> {
                            if (itemStack.isDamageable()) {
                                final int damage = itemStack.getDamage();
                                final int maxDamage = itemStack.getMaxDamage();
                                return ((1.0F - ((float) damage) / ((float) maxDamage) <= currentConfig.getMinDurabilityPercentage()) || (maxDamage - damage <= currentConfig.getMinDurabilityValue()));
                            }
                            return false;
                        })) {
                            if (currentConfig.getOrientation() == ArmorHudConfig.Orientation.Horizontal) {
                                    add += 2 + 8;
                                if (currentConfig.getWarningIconBobbingIntervalMs() != 0.0F) {
                                    add += 7;
                                }
                            }
                        }
                    }
                }
            }
            return offset + Math.max(add, 0);
        } else
            return offset;
    }

    /**
     * This function determines which config is supposed to be current. Usually the loaded config is considered current
     * but if config screen is open then the preview config is used as current.
     *
     * @return Current config
     */
    private ArmorHudConfig armorHud_getCurrentArmorHudConfig() {
        return this.client.currentScreen != null && this.client.currentScreen.getTitle() == ArmorHudMod.CONFIG_SCREEN_NAME ? ArmorHudMod.previewConfig : ArmorHudMod.getCurrentConfig();
    }

    private PlayerEntity getCameraPlayer() {
        return !(this.client.getCameraEntity() instanceof PlayerEntity) ? null : (PlayerEntity) this.client.getCameraEntity();
    }
}