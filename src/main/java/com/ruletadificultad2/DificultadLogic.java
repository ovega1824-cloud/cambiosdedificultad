package com.ruletadificultad2;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.mob.CaveSpiderEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.entity.player.HungerManager;

/** Aquí está lo que hace cada cambio de dificultad. Todo se activa sin mostrar mensajes. */
public final class DificultadLogic {

    /** Lo lee el mixin de la lava (cambio 15). */
    public static volatile boolean lavaComoAgua = false;

    private static final String ID = RuletaDificultad2Mod.MOD_ID;
    private static final Identifier MOD_VIDA_MOBS = Identifier.of(ID, "mobs_doble_vida");
    private static final Identifier MOD_DANO_MOBS = Identifier.of(ID, "mobs_mas_dano");
    private static final Identifier MOD_RANGO_MOBS = Identifier.of(ID, "mobs_rango");
    private static final Identifier MOD_ZOMBI_RAPIDO = Identifier.of(ID, "zombi_rapido");
    private static final Identifier MOD_VIDA_JUGADOR = Identifier.of(ID, "jugador_vida");

    private static final String ETIQUETA_BEBE = "ruletadificultad2_bebe_armado";

    private static final List<EntityType<? extends MobEntity>> MONSTRUOS_NOCHE = List.<EntityType<? extends MobEntity>>of(
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.SPIDER);

    private static final Map<UUID, Float> ULTIMA_SATURACION = new HashMap<>();
    private static final Map<PotionEntity, Vec3d> POCIONES = new HashMap<>();

    private static boolean reentrada = false;
    private static boolean regeneracionDesactivada = false;

    private DificultadLogic() {
    }

    // ------------------------------------------------------------------------------------------
    // Eventos
    // ------------------------------------------------------------------------------------------

    public static void registrar() {
        // 1 (vacas sin leche) y 12 (aldeanos sin armaduras)
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient || !(world instanceof ServerWorld serverWorld)) {
                return ActionResult.PASS;
            }
            DificultadState state = DificultadState.get(serverWorld.getServer());
            ItemStack held = player.getStackInHand(hand);

            if (state.isEnabled(1) && entity instanceof CowEntity
                    && (held.isOf(Items.BUCKET) || held.isOf(Items.BOWL))) {
                return ActionResult.FAIL;
            }
            if (state.isEnabled(12) && entity instanceof VillagerEntity villager) {
                villager.getOffers().removeIf(oferta -> oferta.getSellItem().getItem() instanceof ArmorItem);
            }
            return ActionResult.PASS;
        });

        // 4: puertas, botones y trampillas con 30% de insta kill al activarlas
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient || hand != Hand.MAIN_HAND || !(world instanceof ServerWorld serverWorld)) {
                return ActionResult.PASS;
            }
            if (player.isCreative() || player.isSpectator()) {
                return ActionResult.PASS;
            }
            if (!DificultadState.get(serverWorld.getServer()).isEnabled(4)) {
                return ActionResult.PASS;
            }
            // Si está agachado con algo en la mano, el juego no activa el bloque
            if (player.isSneaking() && (!player.getMainHandStack().isEmpty() || !player.getOffHandStack().isEmpty())) {
                return ActionResult.PASS;
            }
            BlockState state = world.getBlockState(hitResult.getBlockPos());
            Block block = state.getBlock();
            boolean activable = (block instanceof DoorBlock || block instanceof ButtonBlock || block instanceof TrapdoorBlock)
                    && !state.isOf(Blocks.IRON_DOOR) && !state.isOf(Blocks.IRON_TRAPDOOR);
            if (activable && ThreadLocalRandom.current().nextFloat() < 0.3f) {
                player.damage(serverWorld.getDamageSources().genericKill(), Float.MAX_VALUE);
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        // 11 (flechazo mortal) y 22 (doble de daño a jugadores)
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player) || player.isCreative() || player.isSpectator()) {
                return true;
            }
            if (source.isOf(DamageTypes.GENERIC_KILL)) {
                return true;
            }
            DificultadState state = DificultadState.get(player.getServer());

            if (state.isEnabled(11) && source.getSource() instanceof PersistentProjectileEntity
                    && source.getAttacker() instanceof AbstractSkeletonEntity) {
                player.damage(player.getServerWorld().getDamageSources().genericKill(), Float.MAX_VALUE);
                return false;
            }

            if (state.isEnabled(22) && !reentrada) {
                reentrada = true;
                try {
                    player.damage(source, amount * 2f);
                } finally {
                    reentrada = false;
                }
                return false;
            }
            return true;
        });

        // 18: los mobs no sueltan ítems al morir (se borran los ítems que acaban de aparecer)
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof MobEntity) || !(entity.getWorld() instanceof ServerWorld serverWorld)) {
                return;
            }
            if (!DificultadState.get(serverWorld.getServer()).isEnabled(18)) {
                return;
            }
            List<ItemEntity> items = serverWorld.getEntitiesByClass(ItemEntity.class,
                    entity.getBoundingBox().expand(3.0), item -> item.getItemAge() < 3);
            for (ItemEntity item : items) {
                item.discard();
            }
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> aplicarAlCambiar(server, DificultadState.get(server)));

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            lavaComoAgua = false;
            regeneracionDesactivada = false;
            ULTIMA_SATURACION.clear();
            POCIONES.clear();
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            DificultadState state = DificultadState.get(server);
            cadaTick(server, state);
            int ticks = server.getTicks();
            if (ticks % 20 == 0) {
                cadaSegundo(server, state);
            }
            if (ticks % 100 == 0) {
                aparicionesNaturales(server, state);
            }
        });
    }

    /** Cosas que deben pasar justo al activar o desactivar un cambio (también se repite cada segundo). */
    public static void aplicarAlCambiar(MinecraftServer server, DificultadState state) {
        // 15: la lava se esparce como agua (lo usa el mixin)
        lavaComoAgua = state.isEnabled(15);

        // 19: la comida ya no regenera vida (regla natural regeneration)
        boolean sinRegeneracion = state.isEnabled(19);
        if (sinRegeneracion != regeneracionDesactivada) {
            server.getGameRules().get(GameRules.NATURAL_REGENERATION).set(!sinRegeneracion, server);
            regeneracionDesactivada = sinRegeneracion;
        }
    }

    // ------------------------------------------------------------------------------------------
    // Cada tick (solo jugadores y pociones)
    // ------------------------------------------------------------------------------------------

    private static void cadaTick(MinecraftServer server, DificultadState state) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerWorld world = player.getServerWorld();

            // 16: los bloques de magma hacen 10000000 de daño
            if (state.isEnabled(16) && player.isOnGround() && !player.isSneaking() && !player.isSpectator()) {
                BlockPos pie = BlockPos.ofFloored(player.getX(), player.getY() - 0.2, player.getZ());
                if (world.getBlockState(pie).isOf(Blocks.MAGMA_BLOCK)) {
                    player.damage(world.getDamageSources().hotFloor(), 10000000f);
                }
            }

            // 8: las puertas ya no dan aire bajo el agua
            if (state.isEnabled(8) && !player.isCreative() && !player.isSpectator()) {
                BlockPos ojos = BlockPos.ofFloored(player.getEyePos());
                if (world.getBlockState(ojos).getBlock() instanceof DoorBlock) {
                    boolean hayAgua = false;
                    for (Direction direccion : Direction.values()) {
                        if (world.getFluidState(ojos.offset(direccion)).isIn(FluidTags.WATER)) {
                            hayAgua = true;
                            break;
                        }
                    }
                    if (hayAgua) {
                        int aire = player.getAir() - 5;
                        if (aire <= -20) {
                            aire = 0;
                            player.damage(world.getDamageSources().drown(), 2.0f);
                        }
                        player.setAir(aire);
                    }
                }
            }

            // 20: la comida da menos saturación (se reduce a la mitad lo que sube)
            if (state.isEnabled(20)) {
                HungerManager hambre = player.getHungerManager();
                float saturacion = hambre.getSaturationLevel();
                Float ultima = ULTIMA_SATURACION.get(player.getUuid());
                if (ultima != null && saturacion > ultima) {
                    saturacion = ultima + (saturacion - ultima) * 0.5f;
                    hambre.setSaturationLevel(saturacion);
                }
                ULTIMA_SATURACION.put(player.getUuid(), saturacion);
            } else {
                ULTIMA_SATURACION.remove(player.getUuid());
            }
        }

        // 21: las pociones de regeneración explotan al caer
        if (state.isEnabled(21)) {
            for (ServerWorld world : server.getWorlds()) {
                List<? extends PotionEntity> pociones =
                        world.getEntitiesByType(TypeFilter.instanceOf(PotionEntity.class), e -> true);
                for (PotionEntity pocion : pociones) {
                    if (esRegeneracion(pocion.getStack())) {
                        POCIONES.put(pocion, pocion.getPos());
                    }
                }
            }
            Iterator<Map.Entry<PotionEntity, Vec3d>> it = POCIONES.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<PotionEntity, Vec3d> entrada = it.next();
                PotionEntity pocion = entrada.getKey();
                if (pocion.isRemoved()) {
                    if (pocion.getRemovalReason() == Entity.RemovalReason.DISCARDED
                            && pocion.getWorld() instanceof ServerWorld serverWorld) {
                        Vec3d p = entrada.getValue();
                        serverWorld.createExplosion(null, p.x, p.y, p.z, 3.0f, World.ExplosionSourceType.TNT);
                    }
                    it.remove();
                }
            }
        } else if (!POCIONES.isEmpty()) {
            POCIONES.clear();
        }
    }

    private static boolean esRegeneracion(ItemStack stack) {
        PotionContentsComponent contenido = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (contenido == null) {
            return false;
        }
        for (StatusEffectInstance efecto : contenido.getEffects()) {
            if (efecto.getEffectType().equals(StatusEffects.REGENERATION)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------------------------
    // Cada segundo
    // ------------------------------------------------------------------------------------------

    private static void cadaSegundo(MinecraftServer server, DificultadState state) {
        aplicarAlCambiar(server, state);

        for (ServerWorld world : server.getWorlds()) {
            List<? extends HostileEntity> hostiles =
                    world.getEntitiesByType(TypeFilter.instanceOf(HostileEntity.class), e -> true);
            List<? extends ZombieEntity> zombies =
                    world.getEntitiesByType(TypeFilter.instanceOf(ZombieEntity.class), e -> true);
            List<? extends AbstractSkeletonEntity> esqueletos =
                    world.getEntitiesByType(TypeFilter.instanceOf(AbstractSkeletonEntity.class), e -> true);

            // 3, 25, 29 y 30: cosas de todos los mobs hostiles
            for (HostileEntity mob : hostiles) {
                // 3: el doble de vida
                EntityAttributeInstance vida = mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
                if (vida != null) {
                    if (state.isEnabled(3)) {
                        if (!vida.hasModifier(MOD_VIDA_MOBS)) {
                            vida.addTemporaryModifier(new EntityAttributeModifier(MOD_VIDA_MOBS, 1.0,
                                    EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                            mob.setHealth(mob.getMaxHealth());
                        }
                    } else if (vida.hasModifier(MOD_VIDA_MOBS)) {
                        vida.removeModifier(MOD_VIDA_MOBS);
                    }
                }
                // 25: 50% más de daño
                modificador(mob, EntityAttributes.GENERIC_ATTACK_DAMAGE, MOD_DANO_MOBS, 0.5,
                        EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, state.isEnabled(25));
                // 29: te detectan desde muy lejos
                modificador(mob, EntityAttributes.GENERIC_FOLLOW_RANGE, MOD_RANGO_MOBS, 32.0,
                        EntityAttributeModifier.Operation.ADD_VALUE, state.isEnabled(29));
                // 30: resistencia al daño
                if (state.isEnabled(30)) {
                    mob.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 60, 1, true, false));
                }
            }

            // 2: zombies y esqueletos inmunes al sol
            if (state.isEnabled(2)) {
                for (ZombieEntity zombi : zombies) {
                    inmuneAlSol(world, zombi);
                }
                for (AbstractSkeletonEntity esqueleto : esqueletos) {
                    inmuneAlSol(world, esqueleto);
                }
            }

            // 14: zombies con fuerza 5 y velocidad x2 / 17: bebés con diamante
            for (ZombieEntity zombi : zombies) {
                if (state.isEnabled(14)) {
                    zombi.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 60, 4, true, false));
                }
                modificador(zombi, EntityAttributes.GENERIC_MOVEMENT_SPEED, MOD_ZOMBI_RAPIDO, 1.0,
                        EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, state.isEnabled(14));

                if (state.isEnabled(17) && zombi.isBaby() && !zombi.getCommandTags().contains(ETIQUETA_BEBE)) {
                    equiparBebe(world, zombi);
                }
            }

            // 7: los ahogados nadan más rápido
            if (state.isEnabled(7)) {
                for (DrownedEntity ahogado : world.getEntitiesByType(EntityType.DROWNED, e -> true)) {
                    ahogado.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 60, 1, true, false));
                }
            }

            // 13: piglins brutos con fuerza 5
            if (state.isEnabled(13)) {
                for (MobEntity bruto : world.getEntitiesByType(EntityType.PIGLIN_BRUTE, e -> true)) {
                    bruto.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 60, 4, true, false));
                }
            }

            // 9: los creepers solo aparecen cargados
            if (state.isEnabled(9)) {
                for (CreeperEntity creeper : world.getEntitiesByType(EntityType.CREEPER, e -> true)) {
                    net.minecraft.nbt.NbtCompound nbt = new net.minecraft.nbt.NbtCompound();
                    creeper.writeNbt(nbt);
                    if (!nbt.getBoolean("powered")) {
                        nbt.putBoolean("powered", true);
                        creeper.readNbt(nbt);
                    }
                }
            }

            // 6: las arañas se reemplazan por arañas de cueva
            if (state.isEnabled(6)) {
                for (SpiderEntity arana : world.getEntitiesByType(EntityType.SPIDER, e -> true)) {
                    CaveSpiderEntity cueva = EntityType.CAVE_SPIDER.create(world);
                    if (cueva == null) {
                        continue;
                    }
                    cueva.refreshPositionAndAngles(arana.getX(), arana.getY(), arana.getZ(), arana.getYaw(), arana.getPitch());
                    world.spawnEntity(cueva);
                    arana.discard();
                }
            }

            if (world.getRegistryKey() == World.OVERWORLD) {
                // 27: tormenta eléctrica eterna
                if (state.isEnabled(27) && !world.isThundering()) {
                    world.setWeather(0, 6000, true, true);
                }
            }
        }

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            // 26: el hambre baja 3 veces más rápido
            if (state.isEnabled(26) && !player.isCreative() && !player.isSpectator()) {
                player.getHungerManager().addExhaustion(0.6f);
            }
            // 28: debilidad permanente
            if (state.isEnabled(28)) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 60, 1, true, false));
            }
            // 31: oscuridad constante
            if (state.isEnabled(31)) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 60, 0, true, false));
            }
            // 23: solo 5 corazones de vida máxima
            modificador(player, EntityAttributes.GENERIC_MAX_HEALTH, MOD_VIDA_JUGADOR, -10.0,
                    EntityAttributeModifier.Operation.ADD_VALUE, state.isEnabled(23));
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }
    }

    private static void inmuneAlSol(ServerWorld world, MobEntity mob) {
        mob.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 100, 0, true, false));
        if (mob.isOnFire() && world.isDay() && world.isSkyVisible(mob.getBlockPos())) {
            mob.extinguish();
        }
    }

    /** 17: armadura de diamante con protección 4 y espada de diamante con filo 5. */
    private static void equiparBebe(ServerWorld world, ZombieEntity zombi) {
        Registry<Enchantment> registro = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        RegistryEntry<Enchantment> proteccion = registro.getEntry(Enchantments.PROTECTION).orElseThrow();
        RegistryEntry<Enchantment> filo = registro.getEntry(Enchantments.SHARPNESS).orElseThrow();

        zombi.equipStack(EquipmentSlot.HEAD, encantado(Items.DIAMOND_HELMET, proteccion, 4));
        zombi.equipStack(EquipmentSlot.CHEST, encantado(Items.DIAMOND_CHESTPLATE, proteccion, 4));
        zombi.equipStack(EquipmentSlot.LEGS, encantado(Items.DIAMOND_LEGGINGS, proteccion, 4));
        zombi.equipStack(EquipmentSlot.FEET, encantado(Items.DIAMOND_BOOTS, proteccion, 4));
        zombi.equipStack(EquipmentSlot.MAINHAND, encantado(Items.DIAMOND_SWORD, filo, 5));
        zombi.addCommandTag(ETIQUETA_BEBE);
    }

    private static ItemStack encantado(Item item, RegistryEntry<Enchantment> encantamiento, int nivel) {
        ItemStack stack = new ItemStack(item);
        stack.addEnchantment(encantamiento, nivel);
        return stack;
    }

    /** Pone o quita un modificador de atributo temporal según esté activado el cambio. */
    private static void modificador(LivingEntity entity, RegistryEntry<EntityAttribute> atributo, Identifier id,
                                    double valor, EntityAttributeModifier.Operation operacion, boolean activo) {
        EntityAttributeInstance instancia = entity.getAttributeInstance(atributo);
        if (instancia == null) {
            return;
        }
        if (activo) {
            if (!instancia.hasModifier(id)) {
                instancia.addTemporaryModifier(new EntityAttributeModifier(id, valor, operacion));
            }
        } else if (instancia.hasModifier(id)) {
            instancia.removeModifier(id);
        }
    }

    // ------------------------------------------------------------------------------------------
    // Apariciones naturales (cada 5 segundos)
    // ------------------------------------------------------------------------------------------

    private static void aparicionesNaturales(MinecraftServer server, DificultadState state) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.isSpectator()) {
                continue;
            }
            ServerWorld world = player.getServerWorld();

            if (world.getRegistryKey() == World.OVERWORLD) {
                // 5: wither skeletons en todo el overworld (en zonas oscuras)
                if (state.isEnabled(5)) {
                    for (int i = 0; i < 2; i++) {
                        intentarAparecer(world, player, EntityType.WITHER_SKELETON, 4, true);
                    }
                }
                // 24: el doble de monstruos de noche
                if (state.isEnabled(24)) {
                    for (int i = 0; i < 3; i++) {
                        EntityType<? extends MobEntity> tipo =
                                MONSTRUOS_NOCHE.get(ThreadLocalRandom.current().nextInt(MONSTRUOS_NOCHE.size()));
                        intentarAparecer(world, player, tipo, 12, true);
                    }
                }
            } else if (world.getRegistryKey() == World.NETHER) {
                // 10: blazes por todo el nether
                if (state.isEnabled(10)) {
                    for (int i = 0; i < 2; i++) {
                        intentarAparecer(world, player, EntityType.BLAZE, 6, false);
                    }
                }
                // 13: piglins brutos por todo el nether
                if (state.isEnabled(13)) {
                    for (int i = 0; i < 2; i++) {
                        intentarAparecer(world, player, EntityType.PIGLIN_BRUTE, 4, false);
                    }
                }
            }
        }
    }

    /** Intenta hacer aparecer un mob en el suelo a 20-45 bloques del jugador, respetando un límite cercano. */
    private static void intentarAparecer(ServerWorld world, ServerPlayerEntity jugador,
                                         EntityType<? extends MobEntity> tipo, int limite, boolean soloOscuro) {
        ThreadLocalRandom azar = ThreadLocalRandom.current();
        int x = jugador.getBlockX() + (azar.nextBoolean() ? 1 : -1) * (20 + azar.nextInt(25));
        int z = jugador.getBlockZ() + (azar.nextBoolean() ? 1 : -1) * (20 + azar.nextInt(25));

        if (!world.isChunkLoaded(new BlockPos(x, jugador.getBlockY(), z))) {
            return;
        }
        if (world.getEntitiesByType(tipo, jugador.getBoundingBox().expand(64.0), e -> true).size() >= limite) {
            return;
        }

        BlockPos lugar = null;
        for (int y = jugador.getBlockY() + 12; y >= jugador.getBlockY() - 16; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockPos suelo = pos.down();
            if (world.isAir(pos) && world.isAir(pos.up()) && world.getBlockState(suelo).isSolidBlock(world, suelo)) {
                lugar = pos;
                break;
            }
        }
        if (lugar == null || !world.getWorldBorder().contains(lugar)) {
            return;
        }
        if (soloOscuro && world.getLightLevel(lugar) > 7) {
            return;
        }
        if (world.getClosestPlayer(lugar.getX(), lugar.getY(), lugar.getZ(), 16.0, false) != null) {
            return;
        }
        tipo.spawn(world, lugar, SpawnReason.NATURAL);
    }
}
