package com.graphics;

import java.util.ArrayDeque;
import java.util.Queue;
import junit.framework.TestCase;

/** Comprueba conducción, rutas y cámara sin crear una ventana ni un contexto OpenGL. */
public class ConduccionTest extends TestCase {

    /** Todas las entregas deben conectarse al inicio mediante calles que admiten el ancho del auto. */
    public void testCallesYDestinosConectados() {
        clase2 juego = new clase2();
        boolean[][] visitado = new boolean[clase1.MAPA.length][clase1.MAPA[0].length];
        Queue<int[]> pendientes = new ArrayDeque<>();
        int filaInicial = indice(juego.autoZ);
        int columnaInicial = indice(juego.autoX);
        visitado[filaInicial][columnaInicial] = true;
        pendientes.add(new int[] {filaInicial, columnaInicial});
        int[][] pasos = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        while (!pendientes.isEmpty()) {
            int[] actual = pendientes.remove();
            float origenX = clase1.centro(actual[1]);
            float origenZ = clase1.centro(actual[0]);
            for (int[] paso : pasos) {
                int fila = actual[0] + paso[0];
                int columna = actual[1] + paso[1];
                if (fila < 0 || fila >= visitado.length || columna < 0 || columna >= visitado[fila].length
                    || visitado[fila][columna] || clase1.MAPA[fila][columna] != 0) continue;
                for (float avance = 0; avance <= clase1.CELDA; avance += 0.25f) {
                    assertTrue("Un tramo de calle invade un obstaculo",
                        juego.puedeCircular(origenX + paso[1] * avance, origenZ + paso[0] * avance));
                }
                visitado[fila][columna] = true;
                pendientes.add(new int[] {fila, columna});
            }
        }
        for (float[] destino : clase4.DESTINOS) {
            assertTrue("El destino no es transitable", juego.puedeCircular(destino[0], destino[1]));
            assertTrue("El destino no conecta con el punto inicial", visitado[indice(destino[1])][indice(destino[0])]);
        }
        assertFalse(juego.puedeCircular(0, 0));
    }

    private static int indice(float coordenada) {
        return Math.round((coordenada - clase1.centro(0)) / clase1.CELDA);
    }

    /** Los bordes se comprueban en calles libres: una manzana no puede ocultar un error de límite. */
    public void testObstaculosYLimitesReales() {
        clase2 juego = new clase2();
        assertFalse(juego.puedeCircular(-20, -20));
        assertFalse(juego.puedeCircular(20, -20));
        assertFalse(juego.puedeCircular(-25.5f, -20));
        assertTrue(juego.puedeCircular(-27, -20));
        float borde = clase1.LIMITE - clase2.RADIO_AUTO;
        for (int signo : new int[] {-1, 1}) {
            assertTrue(juego.puedeCircular(signo * borde, 50));
            assertTrue(juego.puedeCircular(50, signo * borde));
            assertFalse(juego.puedeCircular(signo * (borde + 0.01f), 50));
            assertFalse(juego.puedeCircular(50, signo * (borde + 0.01f)));
        }
    }

    public void testReinicio() {
        clase4 juego = new clase4();
        juego.autoX = 12;
        juego.autoZ = 4;
        juego.velocidad = 10;
        juego.angulo = 2;
        juego.reiniciar();
        assertEquals(-47.5f, juego.autoX, 0f);
        assertEquals(50f, juego.autoZ, 0f);
        assertEquals(0f, juego.velocidad, 0f);
        assertEquals(0f, juego.angulo, 0f);
        assertTrue(juego.puedeCircular(juego.autoX, juego.autoZ));
    }

    /** Reproduce el caso donde antes el ojo quedaba dentro del edificio en (-20,-40). */
    public void testCamaraSeDetieneAntesDeLaFachada() {
        clase2 juego = new clase2();
        juego.autoX = -30;
        juego.autoZ = -40;
        juego.angulo = (float) (Math.PI / 2);
        float[] ojo = juego.posicionCamaraSeguimiento();
        assertTrue("La camara debe acercarse al auto", ojo[0] < -24.5f);
        assertTrue("El ojo debe quedar detras del auto", ojo[0] > juego.autoX);
        assertTrue(ojo[1] > 0.8f && ojo[1] < 9);
        assertEquals(-40f, ojo[2], 0.0001f);
    }

    /** Un ojo fuera del edificio tampoco puede mirar atravesando todo el edificio. */
    public void testCamaraCompruebaElSegmentoCompleto() {
        clase2 juego = new clase2();
        float libre = juego.fraccionCamaraLibre(-30, 0.8f, -40, -10, 9, -40);
        assertTrue(libre > 0 && libre < 0.3f);
        assertEquals(1f, juego.fraccionCamaraLibre(-30, 0.8f, -40,
            -30 + 20 * libre, 0.8f + 8.2f * libre, -40), 0f);
    }

    /** Calles paralelas a la fachada y trayectorias por encima del techo permanecen libres. */
    public void testCamaraLibreYAlturasReales() {
        clase2 juego = new clase2();
        assertEquals(1f, juego.fraccionCamaraLibre(-30, 0.8f, -40, -30, 9, -28), 0f);
        float sobreTecho = clase1.alturaEdificio(1, 3) + 3;
        assertEquals(1f, juego.fraccionCamaraLibre(-30, sobreTecho, -40, -10, sobreTecho, -40), 0f);
        float[] ojo = juego.posicionCamaraSeguimiento();
        assertEquals(-47.5f, ojo[0], 0f);
        assertEquals(9f, ojo[1], 0f);
        assertEquals(62f, ojo[2], 0f);
    }

    /** Una misma ruta y semilla dan el mismo recorrido con tiempos de cuadro diferentes. */
    public void testTraficoNoDependeDeLosFPS() {
        Trafico.Auto rapido = new Trafico.Auto(10, -50, 1, 0, 10, 0.1f, 0.4f, 0.9f);
        Trafico.Auto lento = new Trafico.Auto(10, -50, 1, 0, 10, 0.1f, 0.4f, 0.9f);
        clase2 juego = new clase2();
        // 120.35 s incluye varias fases de semaforo y giros, usando el mismo reloj simulado.
        for (int cuadro = 0; cuadro < 7221; cuadro++) {
            rapido.actualizar(1f / 60);
            assertTrue("El trafico debe permanecer en calles", juego.puedeCircular(rapido.x, rapido.z));
        }
        for (int cuadro = 0; cuadro < 2407; cuadro++) {
            lento.actualizar(1f / 20);
            assertTrue("El trafico debe permanecer en calles", juego.puedeCircular(lento.x, lento.z));
        }
        assertEquals(rapido.x, lento.x, 0.025f);
        assertEquals(rapido.z, lento.z, 0.025f);
        assertEquals(rapido.dirX, lento.dirX, 0.01f);
        assertEquals(rapido.dirZ, lento.dirZ, 0.01f);
    }

    /** La chata parte enganchada, cubre sus ruedas y no puede cruzar una manzana. */
    public void testChataTieneColisionPropia() {
        clase2 juego = new clase2();
        assertTrue(juego.puedeCircularChata(juego.chataX, juego.chataZ, juego.anguloChata));
        assertFalse(juego.puedeCircularChata(-20, -20, 0));
        assertTrue(clase2.chataNoInvadeAuto(juego.autoX, juego.autoZ, juego.angulo,
            juego.chataX, juego.chataZ, juego.anguloChata));
        assertFalse(clase2.chataNoInvadeAuto(0, 0, 0, 0, 2.1f, 0));
        assertTrue(clase2.articulacionValida(0, 0.69f));
        assertFalse(clase2.articulacionValida(0, 0.71f));
        juego.chataX = 1;
        juego.chataZ = 1;
        juego.anguloChata = 1;
        juego.reiniciar();
        assertEquals(-47.5f, juego.chataX, 0f);
        assertEquals(53.02f, juego.chataZ, 0.001f);
        assertEquals(0f, juego.anguloChata, 0f);
    }

    /** La chata sigue una curva y la misma entrada conserva resultado con 20 o 60 FPS. */
    public void testFisicaChataGiroYFrecuencia() {
        clase2 rapido = new clase2();
        clase2 lento = new clase2();
        rapido.autoX = lento.autoX = -30;
        rapido.autoZ = lento.autoZ = 0;
        rapido.angulo = lento.angulo = 0;
        rapido.chataX = lento.chataX = -30;
        rapido.chataZ = lento.chataZ = 3.02f;
        rapido.anguloChata = lento.anguloChata = 0;
        for (int i = 0; i < 120; i++) rapido.actualizarConjunto(1f / 60, 1, 0.35f, false);
        for (int i = 0; i < 40; i++) lento.actualizarConjunto(1f / 20, 1, 0.35f, false);
        assertTrue(Math.abs(rapido.anguloChata) > 0.02f);
        assertTrue(rapido.puedeCircularChata(rapido.chataX, rapido.chataZ, rapido.anguloChata));
        assertEquals(rapido.autoX, lento.autoX, 0.03f);
        assertEquals(rapido.autoZ, lento.autoZ, 0.03f);
        assertEquals(rapido.chataX, lento.chataX, 0.03f);
        assertEquals(rapido.chataZ, lento.chataZ, 0.03f);
    }

    /** Al retroceder desde el inicio, la chata toca el borde sur y el auto se frena antes que él. */
    public void testChataChocaYFrenaAlAuto() {
        clase2 juego = new clase2();
        for (int i = 0; i < 180; i++) juego.actualizarConjunto(1f / 60, -1, 0, false);
        assertTrue("Solo, el auto llegaria hasta z=53.35", juego.autoZ < 51.1f);
        assertEquals(0f, juego.velocidad, 0f);
        for (int i = 0; i < 60; i++) juego.actualizarConjunto(1f / 60, 1, 0, false);
        assertTrue("Hacia el otro lado puede salir", juego.autoZ < 50);
    }

    /** Coloca la chata enganchada con un ángulo de articulación dado, en una calle vertical libre. */
    private static clase2 conChataArticulada(float articulacion) {
        clase2 juego = new clase2();
        juego.autoX = -30;
        juego.autoZ = 0;
        juego.angulo = 0;
        juego.anguloChata = -articulacion;
        juego.chataX = -30 + (float) Math.sin(-articulacion) * 1.65f;
        juego.chataZ = 1.37f + (float) Math.cos(-articulacion) * 1.65f;
        return juego;
    }

    /** En reversa sin teclas, el asistente contragira y la chata se endereza en vez de hacer tijera. */
    public void testReversaRectaEnderezaLaChata() {
        clase2 juego = conChataArticulada(0.17f);
        for (int i = 0; i < 240; i++) {
            juego.actualizarConjunto(1f / 60, -1, 0, false);
            assertTrue(clase2.articulacionValida(juego.angulo, juego.anguloChata));
        }
        assertTrue("Sigue retrocediendo", juego.velocidad < -1);
        assertTrue(juego.autoZ > 8);
        assertEquals(0f, juego.angulo - juego.anguloChata, 0.03f);
        assertEquals(-30f, juego.autoX, 2.5f);
    }

    /** En reversa, A elige hacia dónde va la chata: se dobla hasta el ángulo pedido y no se detiene. */
    public void testReversaConDireccionDoblaLaChata() {
        clase2 juego = conChataArticulada(0);
        for (int i = 0; i < 150; i++) juego.actualizarConjunto(1f / 60, -1, 1, false);
        assertTrue("Sigue retrocediendo", juego.velocidad < -1);
        assertEquals(0.30f, juego.angulo - juego.anguloChata, 0.06f);
    }
}
