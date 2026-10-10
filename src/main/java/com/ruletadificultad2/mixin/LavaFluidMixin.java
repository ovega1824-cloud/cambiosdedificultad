package com.ruletadificultad2.mixin;

import com.ruletadificultad2.DificultadLogic;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.fluid.LavaFluid;
import net.minecraft.world.WorldView;

/** Cambio 15: la lava se esparce igual que el agua (misma distancia y misma velocidad). */
@Mixin(LavaFluid.class)
public class LavaFluidMixin {

    @Inject(method = "getFlowSpeed", at = @At("HEAD"), cancellable = true, require = 0)
    private void ruletadificultad2$distancia(WorldView world, CallbackInfoReturnable<Integer> cir) {
        if (DificultadLogic.lavaComoAgua) {
            cir.setReturnValue(4);
        }
    }

    @Inject(method = "getLevelDecreasePerBlock", at = @At("HEAD"), cancellable = true, require = 0)
    private void ruletadificultad2$decaimiento(WorldView world, CallbackInfoReturnable<Integer> cir) {
        if (DificultadLogic.lavaComoAgua) {
            cir.setReturnValue(1);
        }
    }

    @Inject(method = "getTickRate", at = @At("HEAD"), cancellable = true, require = 0)
    private void ruletadificultad2$velocidad(WorldView world, CallbackInfoReturnable<Integer> cir) {
        if (DificultadLogic.lavaComoAgua) {
            cir.setReturnValue(5);
        }
    }
}
