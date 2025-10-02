package ru.eaglorn.infomod.mixins.late;

import ic2.core.crop.TileEntityCrop;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntityCrop.class)
public class MixinCrop {
    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lic2/core/crop/TileEntityCrop;generateWeed()V"
        ),
        remap = false
    )
    private void redirectGenerateWeed(TileEntityCrop instance) {
    }

    @Inject(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Random;nextInt(I)I",
            ordinal = 0
        ),
        remap = false,
        cancellable = true
    )
    private void removeWeedCreation(CallbackInfo ci) {
        ci.cancel();
    }
}
