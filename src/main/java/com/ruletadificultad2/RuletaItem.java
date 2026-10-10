package com.ruletadificultad2;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** La ruleta multicolor. Con clic derecho abre el menú, pero solo para admins. */
public class RuletaItem extends Item {

    public RuletaItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient && user instanceof ServerPlayerEntity player) {
            if (RuletaDificultad2Mod.esAdmin(player)) {
                int mask = DificultadState.get(player.getServer()).mask();
                ServerPlayNetworking.send(player, new StatePayload(mask, true));
            } else {
                player.sendMessage(Text.literal("Solo los admins pueden usar la ruleta."), true);
            }
        }

        return TypedActionResult.success(stack, world.isClient);
    }
}
