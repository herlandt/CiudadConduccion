package com.graphics;

import junit.framework.TestCase;

/** Regresiones de circulacion sin contexto grafico. */
public class TraficoTest extends TestCase {
    public void testFasesCompartidasYSinVerdesPerpendiculares() {
        for (double tiempo = 0; tiempo < 48; tiempo += 0.125) {
            Semaforos.Estado horizontal = Semaforos.estado(1, 0, tiempo);
            Semaforos.Estado vertical = Semaforos.estado(0, 1, tiempo);
            assertEquals(horizontal, Semaforos.estado(-1, 0, tiempo));
            assertEquals(vertical, Semaforos.estado(0, -1, tiempo));
            assertFalse(horizontal != Semaforos.Estado.ROJO && vertical != Semaforos.Estado.ROJO);
        }
        assertEquals(Semaforos.Estado.VERDE, Semaforos.estado(1, 0, 0));
        assertEquals(Semaforos.Estado.AMARILLO, Semaforos.estado(1, 0, 8));
        assertEquals(Semaforos.Estado.ROJO, Semaforos.estado(1, 0, 10));
        assertEquals(Semaforos.Estado.ROJO, Semaforos.estado(0, 1, 10));
        assertEquals(Semaforos.Estado.VERDE, Semaforos.estado(0, 1, 12));
    }

    public void testPosicionInicialEnCarrilDerecho() {
        int[][] direcciones = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] direccion : direcciones) {
            Trafico.Auto auto = auto(-10, -10, direccion[0], direccion[1]);
            assertEquals(-10f - direccion[1] * Semaforos.CARRIL, auto.x, 0f);
            assertEquals(-10f + direccion[0] * Semaforos.CARRIL, auto.z, 0f);
        }
    }

    public void testRojoDetieneAntesDeCebraYVerdeReanudaSuavemente() {
        Trafico.Auto auto = auto(-10, -10, 0, 1);
        for (int i = 0; i < 600; i++) {
            auto.actualizar(1f / 60, i / 60.0);
            assertTrue("El frontal no debe alcanzar el paso peatonal", auto.z <= 10 - Semaforos.DISTANCIA_DETENCION + 0.001f);
        }
        assertEquals(10 - Semaforos.DISTANCIA_DETENCION, auto.z, 0.001f);
        assertEquals(0f, auto.velocidadActual, 0f);
        assertEquals(-12.5f, auto.x, 0f);
        auto.actualizar(1f / 60, 12);
        assertTrue(auto.velocidadActual > 0 && auto.velocidadActual < 0.1f);
        assertTrue(auto.z > 10 - Semaforos.DISTANCIA_DETENCION);
    }

    public void testAmarilloTambienImpideNuevaEntrada() {
        Trafico.Auto auto = auto(-10, -10, 1, 0);
        for (int i = 0; i < 180; i++) auto.actualizar(1f / 60, 8 + i / 60.0);
        assertEquals(10 - Semaforos.DISTANCIA_DETENCION, auto.x, 0.001f);
        assertEquals(0f, auto.velocidadActual, 0f);
    }

    public void testCocheComprometidoDespejaAunqueCambieLaLuz() {
        Trafico.Auto auto = auto(-10, -10, 1, 0);
        for (int i = 0; i < 68; i++) auto.actualizar(1f / 60, 6.9 + i / 60.0);
        assertTrue(auto.x > 10 - Semaforos.DISTANCIA_DETENCION);
        for (int i = 68; i < 240; i++) auto.actualizar(1f / 60, 6.9 + i / 60.0);
        assertTrue("Un coche que ya entro no se queda dentro del cruce", Math.abs(auto.x - 10) >= 5 || Math.abs(auto.z + 10) >= 5);
    }

    public void testCurvasContinuasYCarrilCorrectoEnCalles() {
        Trafico.Auto auto = auto(10, -50, 1, 0);
        clase2 mapa = new clase2();
        boolean curva = false;
        for (int i = 0; i < 300 * 60; i++) {
            float anteriorX = auto.x, anteriorZ = auto.z;
            auto.actualizar(1f / 60, i / 60.0);
            assertTrue("Un giro no debe teletransportar el vehiculo", Math.hypot(auto.x - anteriorX, auto.z - anteriorZ) <= auto.velocidad / 60 + 0.002);
            assertTrue("La ruta invade una manzana", mapa.puedeCircular(auto.x, auto.z));
            assertTrue(Math.abs(auto.x) <= 52.501 && Math.abs(auto.z) <= 52.501);
            assertEquals(1, Math.hypot(auto.dirX, auto.dirZ), 0.0001);
            if (Math.abs(auto.dirX) > 0.01 && Math.abs(auto.dirZ) > 0.01) {
                curva = true;
                assertTrue("Los giros deben hacerse a velocidad reducida", auto.velocidadActual <= 6.1);
            }
            if (distanciaAEje(auto.x) > 5.01) {
                assertEquals("Calle horizontal: carril derecho", auto.dirX * Semaforos.CARRIL, desfaseAEje(auto.z), 0.001f);
            }
            if (distanciaAEje(auto.z) > 5.01) {
                assertEquals("Calle vertical: carril derecho", -auto.dirZ * Semaforos.CARRIL, desfaseAEje(auto.x), 0.001f);
            }
        }
        assertTrue("La ruta debe ejercitar giros curvos", curva);
    }

    private static Trafico.Auto auto(float x, float z, float dx, float dz) {
        return new Trafico.Auto(x, z, dx, dz, 10, 0.2f, 0.5f, 0.8f);
    }

    private static float desfaseAEje(float coordenada) {
        return coordenada - (-50 + Math.round((coordenada + 50) / 20) * 20);
    }

    private static float distanciaAEje(float coordenada) {
        return Math.abs(desfaseAEje(coordenada));
    }
}
