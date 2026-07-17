package com.spawnerfinder.mixin;

import com.spawnerfinder.InventoryUI;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public class HandledScreenMixin {
    @Shadow protected int x;
    @Shadow protected int y;
    @Shadow protected int backgroundWidth;
    @Shadow protected int backgroundHeight;

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        HandledScreen self = (HandledScreen)(Object)this;
        InventoryUI.onRender(self, x, y, backgroundWidth, backgroundHeight, context, mouseX, mouseY, delta);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(Click click, boolean unknown, CallbackInfoReturnable<Boolean> cir) {
        if (InventoryUI.onMouseClicked(x, y, backgroundWidth, backgroundHeight, click.x(), click.y(), click.button())) {
            cir.cancel();
            cir.setReturnValue(true);
        }
    }
}
