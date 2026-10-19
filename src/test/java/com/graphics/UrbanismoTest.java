package com.graphics;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;

/** Comprueba la geometría realmente enviada al renderizador sin crear un contexto OpenGL. */
public class UrbanismoTest extends TestCase {
    private record Caja(float x, float y, float z, float sx, float sy, float sz,
                        float r, float g, float b, int material, boolean emisiva) {}

    private static class Registro extends clase4 {
        final List<Caja> cajas = new ArrayList<>();
        private int materialActual;
        private boolean emision;
        @Override protected void material(int tipo) { materialActual = tipo; }
        @Override protected void entero(String nombre, int valor) {
            if (nombre.equals("uEmision")) emision = valor == 1;
        }
        @Override protected void cajaGirada(float x, float y, float z, float sx, float sy, float sz,
                                           float r, float g, float b, float angulo) {
            cajas.add(new Caja(x, y, z, sx, sy, sz, r, g, b, materialActual, emision));
        }
    }

    public void testCebrasConectanAmbasAcerasEnLosCuatroSentidos() throws Exception {
        Method paso = clase4.class.getDeclaredMethod("dibujarPasoPeatonal", float.class, float.class,
            int.class, int.class);
        paso.setAccessible(true);
        for (int[] direccion : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            Registro dibujo = new Registro();
            int dx = direccion[0], dz = direccion[1];
            paso.invoke(dibujo, 10f, 10f, dx, dz);
            float minimo = Float.POSITIVE_INFINITY, maximo = Float.NEGATIVE_INFINITY;
            int franjas = 0;
            for (Caja caja : dibujo.cajas) {
                float longitudinal = (caja.x - 10) * dx + (caja.z - 10) * dz;
                if (Math.abs(longitudinal + Semaforos.DISTANCIA_PASO) > 0.01f) continue;
                float transversal = (caja.x - 10) * -dz + (caja.z - 10) * dx;
                float mitad = (Math.abs(dz) * caja.sx + Math.abs(dx) * caja.sz) / 2;
                minimo = Math.min(minimo, transversal - mitad);
                maximo = Math.max(maximo, transversal + mitad);
                assertTrue("La cebra no invade el área central de giro", longitudinal + 1 < -5);
                franjas++;
            }
            assertTrue(franjas > 1);
            assertEquals("Debe llegar a la acera izquierda", -5f, minimo, 0.001f);
            assertEquals("Debe llegar a la acera derecha", 5f, maximo, 0.001f);
        }
    }

    public void testSinLineaVisibleYAutoDetenidoAntesDeCebra() throws Exception {
        Registro dibujo = new Registro();
        Method paso = clase4.class.getDeclaredMethod("dibujarPasoPeatonal", float.class, float.class,
            int.class, int.class);
        paso.setAccessible(true);
        paso.invoke(dibujo, 0f, 0f, 1, 0);
        assertEquals("Solo se dibujan las diez franjas, sin línea de detención", 10, dibujo.cajas.size());
        for (Caja franja : dibujo.cajas) {
            assertEquals(-Semaforos.DISTANCIA_PASO, franja.x, 0.001f);
            assertEquals(2f, franja.sx, 0.001f);
            assertEquals(0.70f, franja.sz, 0.001f);
        }
        assertTrue("El parachoques conserva su límite de frenado invisible",
            -Semaforos.DISTANCIA_DETENCION + 1.36f < -Semaforos.DISTANCIA_LINEA);
        assertTrue("El límite lógico queda detrás de la cebra",
            -Semaforos.DISTANCIA_LINEA < -Semaforos.DISTANCIA_PASO - 1);
    }

    public void testConservaSoloLos25PasosDeCebra() throws Exception {
        Registro dibujo = new Registro();
        Method decorar = clase4.class.getDeclaredMethod("decorarCiudad");
        decorar.setAccessible(true);
        decorar.invoke(dibujo);
        List<Caja> franjas = dibujo.cajas.stream().filter(caja ->
            caja.material == Materiales.PINTURA && Math.abs(caja.y - 0.045f) < 0.001f).toList();
        assertEquals("25 pasos de diez franjas; sin pasos ni líneas adicionales", 250, franjas.size());
        int pasos = 0;
        for (int fila = 0; fila < clase1.MAPA.length; fila++) {
            for (int columna = 0; columna < clase1.MAPA[fila].length; columna++) {
                if (clase1.MAPA[fila][columna] == 0) continue;
                float centroX = clase1.centro(columna) + clase1.CELDA - Semaforos.DISTANCIA_PASO;
                float centroZ = clase1.centro(fila) - clase1.CELDA;
                List<Caja> paso = franjas.stream().filter(caja -> Math.abs(caja.x - centroX) < 0.001f
                    && Math.abs(caja.z - centroZ) < 5).toList();
                assertEquals("Un único paso al norte de cada manzana", 10, paso.size());
                float extremoNorte = Float.POSITIVE_INFINITY, extremoSur = Float.NEGATIVE_INFINITY;
                for (Caja franja : paso) {
                    assertEquals("Franjas orientadas para cruzar la calle", 2f, franja.sx, 0.001f);
                    assertEquals(0.70f, franja.sz, 0.001f);
                    extremoNorte = Math.min(extremoNorte, franja.z - franja.sz / 2);
                    extremoSur = Math.max(extremoSur, franja.z + franja.sz / 2);
                }
                assertEquals(centroZ - 5, extremoNorte, 0.001f);
                assertEquals(centroZ + 5, extremoSur, 0.001f);
                pasos++;
            }
        }
        assertEquals(25, pasos);
    }

    public void testBancoFueraDeSenderoContinuo() throws Exception {
        Registro dibujo = new Registro();
        Method parque = clase4.class.getDeclaredMethod("dibujarParque", float.class, float.class,
            int.class, int.class);
        parque.setAccessible(true);
        parque.invoke(dibujo, 0f, 0f, 1, 5);
        boolean sendero = false, plataforma = false;
        int piezasBanco = 0;
        for (Caja caja : dibujo.cajas) {
            if (caja.material == Materiales.ACERA && caja.x == 0) {
                assertEquals(10f, caja.sz, 0.001f);
                assertTrue(caja.y - caja.sy / 2 >= 0.369f);
                sendero = true;
            }
            if (caja.material == Materiales.ACERA && caja.x > 0) {
                assertEquals("El área del banco conecta con el camino", 0.8f,
                    caja.x - caja.sx / 2, 0.001f);
                plataforma = true;
            }
            if (caja.material == Materiales.MADERA && caja.sy <= 0.5f) {
                assertTrue("Asiento y respaldo quedan fuera del paso", caja.x - caja.sx / 2 > 0.8f);
                assertTrue("El banco queda paralelo al sendero", caja.sz > caja.sx);
                piezasBanco++;
            }
        }
        assertTrue(sendero && plataforma);
        assertEquals(4, piezasBanco);
    }

    public void testLuzVisibleCoincideConControlDelTrafico() throws Exception {
        Method dibujar = clase4.class.getDeclaredMethod("dibujarSemaforo", float.class, float.class,
            int.class, int.class);
        dibujar.setAccessible(true);
        Field reloj = clase4.class.getDeclaredField("tiempoAmbiente");
        reloj.setAccessible(true);
        for (int[] direccion : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            for (double instante : new double[]{1, 9, 11, 13, 21, 23}) {
                Registro dibujo = new Registro();
                reloj.setDouble(dibujo, instante);
                dibujar.invoke(dibujo, 0f, 0f, direccion[0], direccion[1]);
                Semaforos.Estado estado = Semaforos.estado(direccion[0], direccion[1], instante);
                List<Caja> encendidas = dibujo.cajas.stream().filter(Caja::emisiva).toList();
                assertEquals(1, encendidas.size());
                Caja luz = encendidas.get(0);
                assertEquals(estado == Semaforos.Estado.VERDE ? 0.02f : 1f, luz.r, 0.001f);
                assertEquals(estado == Semaforos.Estado.ROJO ? 0.02f : 1f, luz.g, 0.001f);
                assertTrue("El foco mira hacia el conductor que llega",
                    luz.x * direccion[0] + luz.z * direccion[1] < 0);
            }
        }
    }
}
