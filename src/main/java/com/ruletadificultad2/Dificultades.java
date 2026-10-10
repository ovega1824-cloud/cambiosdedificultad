package com.ruletadificultad2;

import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

/**
 * Lista de cambios de dificultad. El número del menú corresponde al id de cada cambio.
 * Del 1 al 21 son los que pidió el usuario; del 22 al 31 son los extra para hacer el juego mucho más difícil.
 */
public final class Dificultades {

    /**
     * @param id          número que se elige en el menú
     * @param descripcion texto que se muestra en el menú
     * @param icono       ítem que se dibuja como ícono
     * @param tachar      si es true, se dibuja una X roja sobre el ícono (algo que deja de funcionar)
     */
    public record Cambio(int id, String descripcion, Item icono, boolean tachar) {
    }

    public static final int TOTAL_NUMEROS = 32;

    public static final List<Cambio> CAMBIOS = List.of(
            new Cambio(1, "las vacas no darán más leche", Items.MILK_BUCKET, true),
            new Cambio(2, "los zombies y esqueletos serán inmunes al sol", Items.SKELETON_SKULL, false),
            new Cambio(3, "los mobs hostiles tendrán el doble de vida", Items.ZOMBIE_HEAD, false),
            new Cambio(4, "las puertas, botones, trampillas y escotillas tendrán un 30% de probabilidad de hacer insta kill al activarlas", Items.OAK_DOOR, false),
            new Cambio(5, "los wither skeletons aparecerán naturalmente en todo el overworld", Items.WITHER_SKELETON_SKULL, false),
            new Cambio(6, "las arañas serán reemplazadas por arañas de cueva", Items.SPIDER_EYE, false),
            new Cambio(7, "los ahogados zombie nadarán más rápido", Items.NAUTILUS_SHELL, false),
            new Cambio(8, "las puertas ya no harán burbujas de aire bajo el agua", Items.IRON_DOOR, true),
            new Cambio(9, "los creepers aparecerán solo cargados", Items.CREEPER_HEAD, false),
            new Cambio(10, "los blazes aparecerán por todo el nether de manera natural", Items.BLAZE_ROD, false),
            new Cambio(11, "los esqueletos harán insta kill de un flechazo", Items.ARROW, false),
            new Cambio(12, "los aldeanos ya no tradearán por armaduras", Items.IRON_CHESTPLATE, true),
            new Cambio(13, "los piglins brutos tendrán fuerza 5 y aparecerán naturalmente en todo el nether", Items.GOLDEN_AXE, false),
            new Cambio(14, "los zombies tendrán fuerza 5 y velocidad x2", Items.ROTTEN_FLESH, false),
            new Cambio(15, "la lava se esparcirá como agua", Items.LAVA_BUCKET, false),
            new Cambio(16, "los bloques de magma harán 10000000 de daño", Items.MAGMA_BLOCK, false),
            new Cambio(17, "los zombies bebés aparecerán con full diamante protección 4 y espada de diamante con filo 5", Items.DIAMOND_SWORD, false),
            new Cambio(18, "los mobs no droparán ítems al morir", Items.BONE, true),
            new Cambio(19, "la comida ya no regenerará vida, solo las manzanas doradas y de Notch", Items.COOKED_BEEF, true),
            new Cambio(20, "la comida regenerará menos saturación", Items.BREAD, false),
            new Cambio(21, "las pociones de regeneración explotarán al arrojarse", Items.SPLASH_POTION, false),
            // --- Extra: para hacer el juego mucho más difícil ---
            new Cambio(22, "los jugadores recibirán el doble de daño", Items.NETHERITE_SWORD, false),
            new Cambio(23, "los jugadores tendrán solo 5 corazones de vida máxima", Items.POISONOUS_POTATO, false),
            new Cambio(24, "aparecerán el doble de monstruos de noche cerca de los jugadores", Items.ZOMBIE_SPAWN_EGG, false),
            new Cambio(25, "los mobs hostiles harán 50% más de daño", Items.STONE_AXE, false),
            new Cambio(26, "el hambre bajará 3 veces más rápido", Items.COOKED_PORKCHOP, false),
            new Cambio(27, "habrá tormenta eléctrica eterna", Items.LIGHTNING_ROD, false),
            new Cambio(28, "los jugadores tendrán debilidad permanente", Items.WOODEN_SWORD, false),
            new Cambio(29, "los monstruos te detectarán desde muy lejos", Items.ENDER_EYE, false),
            new Cambio(30, "los mobs hostiles tendrán resistencia al daño", Items.SHIELD, false),
            new Cambio(31, "los jugadores sufrirán oscuridad constante", Items.COAL, false)
    );

    private Dificultades() {
    }

    /** Devuelve el cambio asignado a ese número o null si el número no tiene cambio. */
    public static Cambio obtener(int id) {
        if (id < 1 || id > CAMBIOS.size()) {
            return null;
        }
        return CAMBIOS.get(id - 1);
    }
}
