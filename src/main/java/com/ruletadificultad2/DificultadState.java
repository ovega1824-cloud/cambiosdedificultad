package com.ruletadificultad2;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

/** Guarda en el mundo qué cambios de dificultad están activados (un bit por cambio). */
public class DificultadState extends PersistentState {

    public static final Type<DificultadState> TYPE = new Type<>(
            DificultadState::new,
            DificultadState::fromNbt,
            null
    );

    private int mask;

    public DificultadState() {
    }

    public static DificultadState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        DificultadState state = new DificultadState();
        state.mask = nbt.getInt("mask");
        return state;
    }

    public static DificultadState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, "ruletadificultad2");
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        nbt.putInt("mask", mask);
        return nbt;
    }

    public int mask() {
        return mask;
    }

    public boolean isEnabled(int id) {
        return (mask & (1 << (id - 1))) != 0;
    }

    public void set(int id, boolean enabled) {
        if (enabled) {
            mask |= (1 << (id - 1));
        } else {
            mask &= ~(1 << (id - 1));
        }
        markDirty();
    }
}
