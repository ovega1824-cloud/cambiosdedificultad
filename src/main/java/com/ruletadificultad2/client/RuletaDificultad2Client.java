package com.ruletadificultad2.client;

import com.ruletadificultad2.StatePayload;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

public class RuletaDificultad2Client implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(StatePayload.ID, (payload, context) -> {
            MinecraftClient client = context.client();
            if (payload.abrir()) {
                client.setScreen(new RuletaScreen(payload.mask()));
            } else if (client.currentScreen instanceof RuletaScreen pantalla) {
                pantalla.setMask(payload.mask());
            }
        });
    }
}
