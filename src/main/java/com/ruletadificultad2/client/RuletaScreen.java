package com.ruletadificultad2.client;

import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.ruletadificultad2.Dificultades;
import com.ruletadificultad2.RuletaDificultad2Mod;
import com.ruletadificultad2.TogglePayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Menú de la ruleta: la rueda arriba, los números en 4 filas y, al elegir un número,
 * un recuadro con el cambio de dificultad y los botones ACTIVAR y DESACTIVAR.
 * El fondo es transparente (no se oscurece el juego).
 */
public class RuletaScreen extends Screen {

    private static final Identifier RUEDA = Identifier.of(RuletaDificultad2Mod.MOD_ID, "textures/gui/ruleta.png");

    // Medidas en "coordenadas virtuales"; todo se escala si la ventana es pequeña
    private static final int ALTO_VIRTUAL = 300;
    private static final int RUEDA_TAM = 80;
    private static final int CASILLA = 20;
    private static final int SEPARACION = 4;
    private static final int COLUMNAS = 8;
    private static final int CASILLAS_Y = 92;
    private static final int PANEL_W = 270;
    private static final int PANEL_H = 98;
    private static final int PANEL_Y = 198;

    private int mask;
    private int seleccion = 0;
    private float escala = 1f;

    public RuletaScreen(int mask) {
        super(Text.literal("Ruleta"));
        this.mask = mask;
    }

    public void setMask(int mask) {
        this.mask = mask;
    }

    private boolean activo(int id) {
        return (mask & (1 << (id - 1))) != 0;
    }

    @Override
    protected void init() {
        escala = Math.min(1f, (this.height - 6) / (float) ALTO_VIRTUAL);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    /** Fondo transparente: no se dibuja nada. */
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    private float centroX() {
        return (this.width / escala) / 2f;
    }

    private int casillaX(int indice) {
        int total = COLUMNAS * CASILLA + (COLUMNAS - 1) * SEPARACION;
        return (int) (centroX() - total / 2f) + (indice % COLUMNAS) * (CASILLA + SEPARACION);
    }

    private int casillaY(int indice) {
        return CASILLAS_Y + (indice / COLUMNAS) * (CASILLA + SEPARACION);
    }

    private int panelX() {
        return (int) (centroX() - PANEL_W / 2f);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float vx = mouseX / escala;
        float vy = mouseY / escala;

        context.getMatrices().push();
        context.getMatrices().scale(escala, escala, 1f);

        // Rueda
        RenderSystem.enableBlend();
        context.drawTexture(RUEDA, (int) (centroX() - RUEDA_TAM / 2f), 4, RUEDA_TAM, RUEDA_TAM,
                0f, 0f, 128, 128, 128, 128);
        RenderSystem.disableBlend();

        // Números (los que tienen cambio asignado)
        for (int i = 0; i < Dificultades.TOTAL_NUMEROS; i++) {
            int id = i + 1;
            int x = casillaX(i);
            int y = casillaY(i);
            boolean asignado = Dificultades.obtener(id) != null;

            context.fill(x, y, x + CASILLA, y + CASILLA, 0xFF000000);

            int color = !asignado ? 0xFF777777 : (activo(id) ? 0xFF55FF55 : 0xFFFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(String.valueOf(id)),
                    x + CASILLA / 2, y + 6, color);

            boolean encima = vx >= x && vx < x + CASILLA && vy >= y && vy < y + CASILLA;
            if (id == seleccion) {
                context.drawBorder(x - 1, y - 1, CASILLA + 2, CASILLA + 2, 0xFFFFFF00);
            } else if (encima) {
                context.drawBorder(x - 1, y - 1, CASILLA + 2, CASILLA + 2, 0xFFFFFFFF);
            }
        }

        if (seleccion > 0) {
            dibujarPanel(context);
        }

        context.getMatrices().pop();
    }

    private void dibujarPanel(DrawContext context) {
        int x = panelX();
        int y = PANEL_Y;
        Dificultades.Cambio cambio = Dificultades.obtener(seleccion);

        // Recuadro blanco con borde negro
        context.fill(x, y, x + PANEL_W, y + PANEL_H, 0xFF000000);
        context.fill(x + 2, y + 2, x + PANEL_W - 2, y + PANEL_H - 2, 0xFFFFFFFF);

        // Número arriba a la derecha
        context.getMatrices().push();
        context.getMatrices().translate(x + PANEL_W - 24, y + 6, 0);
        context.getMatrices().scale(2f, 2f, 1f);
        context.drawText(this.textRenderer, String.valueOf(seleccion), 0, 0, 0xFF000000, false);
        context.getMatrices().pop();

        if (cambio == null) {
            context.drawText(this.textRenderer, "este número no tiene", x + 12, y + 22, 0xFF000000, false);
            context.drawText(this.textRenderer, "un cambio asignado", x + 12, y + 34, 0xFF000000, false);
            return;
        }

        // Ícono (el doble de grande) con X roja si es algo que deja de funcionar
        int iconoX = x + 8;
        int iconoY = y + 8;
        context.getMatrices().push();
        context.getMatrices().translate(iconoX, iconoY, 0);
        context.getMatrices().scale(2f, 2f, 1f);
        context.drawItem(new ItemStack(cambio.icono()), 0, 0);
        context.getMatrices().pop();

        if (cambio.tachar()) {
            for (int i = 0; i < 30; i++) {
                context.fill(iconoX + 1 + i, iconoY + 1 + i, iconoX + 4 + i, iconoY + 4 + i, 0xFFFF0000);
                context.fill(iconoX + 28 - i, iconoY + 1 + i, iconoX + 31 - i, iconoY + 4 + i, 0xFFFF0000);
            }
        }

        // Texto: "cambio de dificultad" y qué hace
        context.drawText(this.textRenderer, "cambio de dificultad", x + 50, y + 8, 0xFF000000, false);
        List<OrderedText> lineas = this.textRenderer.wrapLines(Text.literal(cambio.descripcion()), PANEL_W - 50 - 30);
        for (int i = 0; i < lineas.size() && i < 5; i++) {
            context.drawText(this.textRenderer, lineas.get(i), x + 50, y + 19 + i * 10, 0xFF000000, false);
        }

        // Botones ACTIVAR (verde) y DESACTIVAR (rojo)
        int[] a = botonActivar();
        int[] d = botonDesactivar();
        context.fill(a[0], a[1], a[0] + a[2], a[1] + a[3], 0xFF00FF00);
        context.fill(d[0], d[1], d[0] + d[2], d[1] + d[3], 0xFFFF0000);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("ACTIVAR"),
                a[0] + a[2] / 2, a[1] + 3, 0xFF000000);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("DESACTIVAR"),
                d[0] + d[2] / 2, d[1] + 3, 0xFF000000);

        // Un borde negro marca en qué estado está ahora
        if (activo(seleccion)) {
            context.drawBorder(a[0], a[1], a[2], a[3], 0xFF000000);
        } else {
            context.drawBorder(d[0], d[1], d[2], d[3], 0xFF000000);
        }
    }

    /** x, y, ancho, alto */
    private int[] botonActivar() {
        return new int[]{panelX() + 8, PANEL_Y + PANEL_H - 18, 70, 12};
    }

    private int[] botonDesactivar() {
        return new int[]{panelX() + PANEL_W - 8 - 90, PANEL_Y + PANEL_H - 18, 90, 12};
    }

    private static boolean dentro(double px, double py, int[] r) {
        return px >= r[0] && px < r[0] + r[2] && py >= r[1] && py < r[1] + r[3];
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        double vx = mouseX / escala;
        double vy = mouseY / escala;

        // Botones del recuadro
        if (seleccion > 0 && Dificultades.obtener(seleccion) != null) {
            if (dentro(vx, vy, botonActivar())) {
                enviar(true);
                return true;
            }
            if (dentro(vx, vy, botonDesactivar())) {
                enviar(false);
                return true;
            }
        }

        // Casillas con números
        for (int i = 0; i < Dificultades.TOTAL_NUMEROS; i++) {
            int x = casillaX(i);
            int y = casillaY(i);
            if (vx >= x && vx < x + CASILLA && vy >= y && vy < y + CASILLA) {
                seleccion = i + 1;
                sonido();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void enviar(boolean activar) {
        ClientPlayNetworking.send(new TogglePayload(seleccion, activar));
        // Se refleja de inmediato en el menú; el servidor confirma con el estado real
        if (activar) {
            mask |= (1 << (seleccion - 1));
        } else {
            mask &= ~(1 << (seleccion - 1));
        }
        sonido();
    }

    private void sonido() {
        if (this.client != null) {
            this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.client != null && this.client.options.inventoryKey.matchesKey(keyCode, scanCode)) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
