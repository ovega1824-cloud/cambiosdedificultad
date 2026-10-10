package com.ruletadificultad2;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class RuletaDificultad2Mod implements ModInitializer {

    public static final String MOD_ID = "ruletadificultad2";

    public static final Item RULETA = Registry.register(
            Registries.ITEM,
            Identifier.of(MOD_ID, "ruleta_dificultad_2"),
            new RuletaItem(new Item.Settings().maxCount(1))
    );

    /** Solo los admins (operadores con nivel de permiso 2 o más) pueden usar la ruleta. */
    public static boolean esAdmin(ServerPlayerEntity player) {
        return player.hasPermissionLevel(2);
    }

    @Override
    public void onInitialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(RULETA));

        PayloadTypeRegistry.playS2C().register(StatePayload.ID, StatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(TogglePayload.ID, TogglePayload.CODEC);

        // El cliente pide activar o desactivar un cambio. El servidor revisa que sea admin.
        ServerPlayNetworking.registerGlobalReceiver(TogglePayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if (!esAdmin(player)) {
                return;
            }
            int id = payload.id();
            if (Dificultades.obtener(id) == null) {
                return;
            }

            MinecraftServer server = context.server();
            DificultadState state = DificultadState.get(server);
            state.set(id, payload.activar());
            DificultadLogic.aplicarAlCambiar(server, state);

            // Se activa en silencio: solo se actualiza el estado del menú, sin mensajes en el chat.
            ServerPlayNetworking.send(player, new StatePayload(state.mask(), false));
        });

        DificultadLogic.registrar();
    }
}
