# Ruleta de Dificultad 2 (Fabric 1.21.1)

Segundo mod de ruleta de dificultad. Se instala aparte del primero (usa otro id: `ruletadificultad2`).

- Ítem: **Ruleta de dificultad 2**. Clic derecho abre un menú con fondo transparente (rueda de colores, números y un recuadro con el cambio, ACTIVAR y DESACTIVAR).
- Al activar o desactivar no sale ningún mensaje; el cambio se guarda en el mundo.
- Solo pueden usarla los admins (operadores nivel 2 o más).
- Requiere Fabric Loader 0.16+ y Fabric API, en el servidor y en los clientes.
- Para obtenerla: `/give @s ruletadificultad2:ruleta_dificultad_2` (o en el creativo, pestaña de herramientas).

## Cambios de dificultad

Del 1 al 21 (los pedidos):

1. las vacas no darán más leche
2. los zombies y esqueletos serán inmunes al sol
3. los mobs hostiles tendrán el doble de vida
4. las puertas, botones, trampillas y escotillas tendrán un 30% de probabilidad de hacer insta kill al activarlas
5. los wither skeletons aparecerán naturalmente en todo el overworld
6. las arañas serán reemplazadas por arañas de cueva
7. los ahogados zombie nadarán más rápido
8. las puertas ya no harán burbujas de aire bajo el agua
9. los creepers aparecerán solo cargados
10. los blazes aparecerán por todo el nether de manera natural
11. los esqueletos harán insta kill de un flechazo
12. los aldeanos ya no tradearán por armaduras
13. los piglins brutos tendrán fuerza 5 y aparecerán naturalmente en todo el nether
14. los zombies tendrán fuerza 5 y velocidad x2
15. la lava se esparcirá como agua
16. los bloques de magma harán 10000000 de daño
17. los zombies bebés aparecerán con full diamante protección 4 y espada de diamante con filo 5
18. los mobs no droparán ítems al morir
19. la comida ya no regenerará vida, solo las manzanas doradas y de Notch
20. la comida regenerará menos saturación
21. las pociones de regeneración explotarán al arrojarse

Extra (22 al 31), para hacer el juego mucho más difícil:

22. los jugadores recibirán el doble de daño
23. los jugadores tendrán solo 5 corazones de vida máxima
24. aparecerán el doble de monstruos de noche cerca de los jugadores
25. los mobs hostiles harán 50% más de daño
26. el hambre bajará 3 veces más rápido
27. habrá tormenta eléctrica eterna
28. los jugadores tendrán debilidad permanente
29. los monstruos te detectarán desde muy lejos
30. los mobs hostiles tendrán resistencia al daño
31. los jugadores sufrirán oscuridad constante

## Notas de cómo funcionan algunos

- 5, 10, 13 y 24: el mod hace aparecer los mobs cada 5 segundos cerca de los jugadores (en zonas oscuras para el overworld), con un límite por zona.
- 19 usa la regla `naturalRegeneration`; al desactivarlo se vuelve a activar.
- 20 reduce a la mitad la saturación que gana el jugador al comer.
- 18 borra los ítems que sueltan los mobs al morir (no los de jugadores).
- 3, 9, 14 y 17 se aplican a los mobs que ya existen y a los que van apareciendo.

Para cambiar textos o íconos: `Dificultades.java`. Para cambiar lo que hace cada uno: `DificultadLogic.java`.

## Descargar el .jar

Al subir el código a la rama `main`, GitHub Actions compila el mod y publica el `.jar` en **Releases** (etiqueta `latest`).
