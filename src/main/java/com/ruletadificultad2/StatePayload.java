package com.ruletadificultad2;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Servidor -> cliente: estado de los cambios. Si abrir es true, el cliente abre el menú. */
public record StatePayload(int mask, boolean abrir) implements CustomPayload {

    public static final Id<StatePayload> ID = new Id<>(Identifier.of(RuletaDificultad2Mod.MOD_ID, "estado"));

    public static final PacketCodec<RegistryByteBuf, StatePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, StatePayload::mask,
            PacketCodecs.BOOL, StatePayload::abrir,
            StatePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
