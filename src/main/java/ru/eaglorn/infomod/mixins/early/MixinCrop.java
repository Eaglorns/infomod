package ru.eaglorn.infomod.mixins.early;

import ic2.core.crop.TileEntityCrop;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntityCrop.class)
public class MixinCrop {
    @Inject(
        method = "tick",
        at = @At(
            value = "JUMP",
            opcode = Opcodes.IFNULL,
            ordinal = 0
        ),
        remap = false,
        cancellable = true
    )
    private void skipCropNullCheck(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = {"generateWeed"}, at = {@At("HEAD")}, remap = false, cancellable = true)
    public void returnBeforeGenerate(CallbackInfo ci) {
        ci.cancel();
    }
}
