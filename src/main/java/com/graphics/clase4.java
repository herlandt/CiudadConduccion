package com.graphics;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;

/** Ciudad completa: entregas, tráfico por nodos, decoración, minimapa e interfaz. */
public class clase4 extends clase3 {
    private boolean mostrarMapa = true;
    private boolean mostrarAyuda = true;
    private int entregas;
    private float tiempo;
    private double tiempoAmbiente; // Un único reloj gobierna luces visibles y decisiones del tráfico.
    private final Hud hud = new Hud();

    static final float[][] DESTINOS = {
        {50, -50}, {-50, -10}, {30, 30}, {-10, -50}, {-50, 50}
    };

    /** Conserva el nombre usado en los ejercicios; la navegación reside en Trafico. */
    static class AutoIA extends Trafico.Auto {
        AutoIA(float x, float z, float dirX, float dirZ, float velocidad,
               float r, float g, float b) {
            super(x, z, dirX, dirZ, velocidad, r, g, b);
        }
    }

    private final AutoIA[] traficoIA = new AutoIA[3];

    public clase4() {
        inicializarTrafico();
    }

    private void inicializarTrafico() {
        traficoIA[0] = new AutoIA(10, -50, 1, 0, 10, 0.16f, 0.40f, 0.70f);
        traficoIA[1] = new AutoIA(-50, -10, 0, 1, 11, 0.22f, 0.54f, 0.37f);
        traficoIA[2] = new AutoIA(30, 50, -1, 0, 9.5f, 0.90f, 0.65f, 0.20f);
    }

    @Override
    protected void tecla(int key) {
        super.tecla(key);
        if (key == GLFW_KEY_M) mostrarMapa = !mostrarMapa;
        if (key == GLFW_KEY_H) mostrarAyuda = !mostrarAyuda;
    }

    @Override
    protected void reiniciar() {
        super.reiniciar();
        entregas = 0;
        tiempo = 0;
        tiempoAmbiente = 0;
        inicializarTrafico(); // Los constructores también restablecen las semillas de las rutas.
    }

    @Override
    protected void actualizar(float deltaTime) {
        super.actualizar(deltaTime);
        for (AutoIA auto : traficoIA) auto.actualizar(deltaTime, tiempoAmbiente);
        tiempoAmbiente += deltaTime;
        if (entregas >= DESTINOS.length) return;
        tiempo += deltaTime;
        if (distanciaDestino() < 3.5f && Math.abs(velocidad) < 1.2f) entregas++;
    }

    private float distanciaDestino() {
        if (entregas == DESTINOS.length) return 0;
        return (float) Math.hypot(autoX - DESTINOS[entregas][0], autoZ - DESTINOS[entregas][1]);
    }

    private String obtenerDistrito() {
        if (autoZ < -20) return "Distrito Norte";
        if (autoZ > 20) return "Distrito Sur";
        if (autoX < -20) return "Residencial Oeste";
        if (autoX > 20) return "Comercial Este";
        return "Centro Urbano";
    }

    @Override
    protected String estadoExtra() {
        return super.estadoExtra() + " | " + obtenerDistrito() + " | Entregas "
            + entregas + "/" + DESTINOS.length + " | M: mapa | H: ayuda";
    }

    @Override
    protected void escena() {
        super.escena();
        if (!vistaMapa) decorarCiudad();
        if (entregas < DESTINOS.length && !paseSombras) dibujarDestino();
        for (AutoIA auto : traficoIA) {
            dibujarVehiculo(auto.x, auto.z, auto.angulo, auto.r, auto.g, auto.b, noche);
        }
    }

    /** La baliza es un indicador, por eso no proyecta una sombra física sobre la ciudad. */
    private void dibujarDestino() {
        float x = DESTINOS[entregas][0], z = DESTINOS[entregas][1];
        entero("uEmision", 1);
        // Marco abierto: permite ver el asfalto y reconocer dónde detener el vehículo.
        caja(x - 2.6f, 0.08f, z, 0.18f, 0.05f, 5.4f, 1, 0.73f, 0.24f);
        caja(x + 2.6f, 0.08f, z, 0.18f, 0.05f, 5.4f, 1, 0.73f, 0.24f);
        caja(x, 0.08f, z - 2.6f, 5.4f, 0.05f, 0.18f, 1, 0.73f, 0.24f);
        caja(x, 0.08f, z + 2.6f, 5.4f, 0.05f, 0.18f, 1, 0.73f, 0.24f);
        if (!vistaMapa) {
            float altura = 3.8f + (float) Math.sin(tiempoAmbiente * 2.5f) * 0.35f;
            cajaGirada(x, altura, z, 0.8f, 0.8f, 0.8f, 1, 0.80f, 0.24f, (float) tiempoAmbiente);
        }
        entero("uEmision", 0);
    }

    private void decorarCiudad() {
        for (int fila = 0; fila < MAPA.length; fila++) {
            for (int columna = 0; columna < MAPA[fila].length; columna++) {
                float x = centro(columna), z = centro(fila);
                int tipo = MAPA[fila][columna];
                if (tipo == 2) dibujarParque(x, z, fila, columna);
                if (tipo == 1) {
                    float altura = alturaEdificio(fila, columna);
                    dibujarVentanas(x, z, altura, fila, columna);
                    dibujarDetallesEdificio(x, z, altura, fila, columna);
                }
                // Conserva un único paso al norte de cada manzana (25 en total).
                // Cerca de la esquina, queda por delante del punto de frenado existente.
                if (tipo != 0 && !paseSombras) dibujarPasoPeatonal(x + CELDA, z - CELDA, 1, 0);
            }
        }
        dibujarCruces();
    }

    private void dibujarParque(float x, float z, int fila, int columna) {
        int indice = 0;
        for (float dx : new float[]{-2.6f, 2.6f}) {
            for (float dz : new float[]{-2.6f, 2.6f}) {
                int variante = (fila * 7 + columna * 3 + indice++) % 3;
                float altura = 2.3f + variante * 0.38f;
                float giro = (fila + columna + indice) * 0.32f;
                cajaMaterial(Materiales.MADERA, x + dx, 1.4f, z + dz, 0.32f, 2.1f, 0.32f,
                    0.38f, 0.27f, 0.18f);
                cajaGiradaMaterial(Materiales.HOJAS, x + dx, altura + 0.2f, z + dz,
                    2.0f + variante * 0.14f, 1.7f, 2.0f, 0.29f, 0.47f + variante * 0.045f, 0.21f, giro);
                cajaGiradaMaterial(Materiales.HOJAS, x + dx + 0.15f, altura + 1.15f, z + dz,
                    1.35f, 1.1f, 1.35f, 0.36f, 0.55f, 0.24f, giro + 0.3f);
            }
        }
        // Sendero continuo y un ensanche lateral de descanso conectado con él.
        cajaMaterial(Materiales.ACERA, x, 0.40f, z, 1.6f, 0.06f, 10,
            0.68f, 0.66f, 0.58f);
        cajaMaterial(Materiales.ACERA, x + 1.8f, 0.40f, z, 2.0f, 0.06f, 3.4f,
            0.68f, 0.66f, 0.58f);
        // Banco paralelo al camino; respaldo hacia el césped, asiento mirando al sendero.
        for (int lama = 0; lama < 3; lama++) {
            cajaMaterial(Materiales.MADERA, x + 2.1f - 0.24f + lama * 0.24f, 0.96f, z,
                0.19f, 0.12f, 2.7f, 0.56f, 0.36f, 0.19f);
        }
        for (float dz : new float[]{-0.95f, 0.95f}) {
            cajaMaterial(Materiales.METAL, x + 2.1f, 0.68f, z + dz,
                0.62f, 0.5f, 0.12f, 0.19f, 0.23f, 0.22f);
            cajaMaterial(Materiales.METAL, x + 2.43f, 1.12f, z + dz,
                0.08f, 0.88f, 0.10f, 0.19f, 0.23f, 0.22f);
        }
        cajaMaterial(Materiales.MADERA, x + 2.45f, 1.35f, z,
            0.13f, 0.50f, 2.7f, 0.56f, 0.36f, 0.19f);

        for (int lado : new int[]{-1, 1}) {
            cajaMaterial(Materiales.ESTUCO, x + lado * 3.6f, 0.65f, z + lado * 3.6f,
                0.65f, 0.55f, 0.65f, 0.60f, 0.35f, 0.24f);
            cajaMaterial(Materiales.HOJAS, x + lado * 3.6f, 1.0f, z + lado * 3.6f,
                0.55f, 0.24f, 0.55f, 0.30f, 0.50f, 0.20f);
            caja(x + lado * 3.6f, 1.16f, z + lado * 3.6f, 0.34f, 0.18f, 0.34f,
                0.88f, 0.38f + (fila % 3) * 0.12f, 0.39f + (columna % 3) * 0.10f);
        }

        cajaMaterial(Materiales.METAL, x + 3.5f, 1.73f, z - 3.5f, 0.12f, 2.7f, 0.12f,
            0.16f, 0.20f, 0.22f);
        entero("uEmision", noche ? 1 : 0);
        caja(x + 3.5f, 3.1f, z - 3.5f, 0.4f, 0.3f, 0.4f, 1, 0.88f, 0.64f);
        entero("uEmision", 0);
        cajaMaterial(Materiales.METAL, x + 3.5f, 3.30f, z - 3.5f, 0.55f, 0.08f, 0.55f,
            0.18f, 0.22f, 0.23f);

    }

    private void dibujarVentanas(float x, float z, float altura, int fila, int columna) {
        int piso = 0;
        for (float y = 2.5f; y + 0.5f < altura + 0.3f; y += 2.0f, piso++) {
            for (int ventana = -1; ventana <= 1; ventana++) {
                for (int cara = 0; cara < 4; cara++) {
                    boolean lateral = cara >= 2;
                    float signo = cara % 2 == 0 ? -1 : 1;
                    float cx = x + (lateral ? signo * 3.54f : ventana * 2.0f);
                    float cz = z + (lateral ? ventana * 2.0f : signo * 3.54f);
                    // Marcos no emisivos y cristal ligeramente adelantado evitan z-fighting.
                    cajaMaterial(Materiales.METAL, cx, y, cz, lateral ? 0.07f : 1.05f, 1.13f,
                        lateral ? 1.05f : 0.07f, 0.18f, 0.23f, 0.24f);
                    float cristalX = cx + (lateral ? signo * 0.045f : 0);
                    float cristalZ = cz + (lateral ? 0 : signo * 0.045f);
                    int patron = Math.floorMod(fila * 13 + columna * 7 + piso * 11 + ventana * 3 + cara * 5, 9);
                    boolean encendida = noche && patron < 5;
                    entero("uEmision", encendida ? 1 : 0);
                    float r = encendida ? 0.95f : noche ? 0.12f : 0.39f;
                    float g = encendida ? 0.70f + patron * 0.035f : noche ? 0.20f : 0.60f;
                    float b = encendida ? 0.34f + patron * 0.045f : noche ? 0.27f : 0.69f;
                    cajaMaterial(Materiales.CRISTAL, cristalX, y, cristalZ,
                        lateral ? 0.025f : 0.86f, 0.94f, lateral ? 0.86f : 0.025f, r, g, b);
                    entero("uEmision", 0);
                    cajaMaterial(Materiales.ACERA, cx, y - 0.58f, cz,
                        lateral ? 0.20f : 1.13f, 0.09f, lateral ? 1.13f : 0.20f, 0.65f, 0.65f, 0.60f);
                }
            }
        }
    }

    private void dibujarDetallesEdificio(float x, float z, float altura, int fila, int columna) {
        cajaMaterial(Materiales.PINTURA, x, 2.0f, z + 3.85f, 1.7f, 0.09f, 0.62f,
            0.57f, 0.25f, 0.21f);
        cajaMaterial(Materiales.PINTURA, x + 1.5f, 2.0f, z - 3.85f, 1.4f, 0.09f, 0.62f,
            0.22f, 0.40f, 0.42f);
        for (float y = 3.8f; y + 0.8f < altura; y += 4) {
            cajaMaterial(Materiales.ACERA, x, y, z + 3.85f, 2.5f, 0.12f, 0.72f, 0.55f, 0.55f, 0.51f);
            cajaMaterial(Materiales.METAL, x, y + 0.65f, z + 4.17f, 2.5f, 0.07f, 0.06f,
                0.23f, 0.28f, 0.28f);
            for (float dx : new float[]{-1.15f, 0, 1.15f}) {
                cajaMaterial(Materiales.METAL, x + dx, y + 0.33f, z + 4.17f, 0.06f, 0.65f, 0.06f,
                    0.23f, 0.28f, 0.28f);
            }
        }
        float antenaX = x - ((fila + columna) % 2 == 0 ? 1 : -1);
        // La cubierta termina en altura+0.6; el mástil comienza exactamente allí.
        cajaMaterial(Materiales.METAL, antenaX, altura + 1.45f, z, 0.06f, 1.7f, 0.06f,
            0.45f, 0.48f, 0.48f);
        cajaMaterial(Materiales.METAL, antenaX, altura + 2.1f, z, 1.1f, 0.06f, 0.06f,
            0.45f, 0.48f, 0.48f);
        cajaMaterial(Materiales.METAL, x + 0.8f, 1.3f, z + 3.60f, 0.35f, 0.25f, 0.03f,
            0.75f, 0.78f, 0.76f);
    }

    /** Un semáforo por acceso existente; las cebras se distribuyen por manzana. */
    private void dibujarCruces() {
        int[][] direcciones = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        float bordeNodos = LIMITE - CELDA / 2;
        for (int fila = 0; fila < MAPA.length; fila += 2) {
            for (int columna = 0; columna < MAPA[fila].length; columna += 2) {
                float x = centro(columna), z = centro(fila);
                for (int[] direccion : direcciones) {
                    int dx = direccion[0], dz = direccion[1];
                    if (Math.abs(x - dx * 20) > bordeNodos || Math.abs(z - dz * 20) > bordeNodos) continue;
                    // Poste en la acera derecha del acceso, orientado hacia los coches que llegan.
                    float posteX = x - dx * Semaforos.DISTANCIA_PASO - dz * 5.6f;
                    float posteZ = z - dz * Semaforos.DISTANCIA_PASO + dx * 5.6f;
                    dibujarSemaforo(posteX, posteZ, dx, dz);
                }
            }
        }
    }

    /** Las franjas se repiten transversalmente y abarcan los diez metros entre aceras. */
    private void dibujarPasoPeatonal(float cruceX, float cruceZ, int dx, int dz) {
        int derechaX = -dz, derechaZ = dx;
        for (int marca = 0; marca < 10; marca++) {
            float transversal = -4.65f + marca * (9.3f / 9);
            float x = cruceX - dx * Semaforos.DISTANCIA_PASO + derechaX * transversal;
            float z = cruceZ - dz * Semaforos.DISTANCIA_PASO + derechaZ * transversal;
            cajaMaterial(Materiales.PINTURA, x, 0.045f, z,
                Math.abs(dx) * 2f + Math.abs(derechaX) * 0.70f, 0.04f,
                Math.abs(dz) * 2f + Math.abs(derechaZ) * 0.70f, 0.90f, 0.89f, 0.81f);
        }
        // El punto de detención sigue siendo lógico: no se pinta una línea en la calzada.
    }

    private void dibujarSemaforo(float x, float z, int dx, int dz) {
        float giro = (float) Math.atan2(dx, dz); // Frente local -Z apunta hacia el acceso (-dirección).
        cajaMaterial(Materiales.METAL, x, 1.7f, z, 0.16f, 2.8f, 0.16f, 0.23f, 0.26f, 0.26f);
        cajaGiradaMaterial(Materiales.METAL, x, 3.1f, z, 0.55f, 1.2f, 0.45f,
            0.10f, 0.13f, 0.14f, giro);
        cajaGirada(x - dx * 0.12f, 3.72f, z - dz * 0.12f,
            0.62f, 0.07f, 0.68f, 0.08f, 0.11f, 0.12f, giro);
        Semaforos.Estado estado = Semaforos.estado(dx, dz, tiempoAmbiente);
        for (int luz = 0; luz < 3; luz++) {
            boolean encendida = luz == 0 ? estado == Semaforos.Estado.ROJO
                : luz == 1 ? estado == Semaforos.Estado.AMARILLO : estado == Semaforos.Estado.VERDE;
            float brillo = encendida ? 1 : 0.12f;
            entero("uEmision", encendida ? 1 : 0);
            cajaGirada(x - dx * 0.25f, 3.45f - luz * 0.35f, z - dz * 0.25f,
                0.28f, 0.25f, 0.07f, luz < 2 ? brillo : 0.02f,
                luz > 0 ? brillo : 0.02f, 0.02f, giro);
        }
        entero("uEmision", 0);
    }

    @Override
    protected void dibujarFrame() {
        super.dibujarFrame();
        if (ancho <= 0 || alto <= 0) return;
        int dimensionMenor = Math.min(ancho, alto);
        int lado = Math.min(280, dimensionMenor / 3);
        int margen = Math.min(18, dimensionMenor / 20);
        if (mostrarMapa && lado > 0) dibujarMinimapa(lado, margen);
        // La interfaz también se dibuja cuando M oculta el mapa.
        hud.dibujar(ancho, alto, Math.round(Math.abs(velocidad) * 3.6f), entregas, DESTINOS.length,
            (int) tiempo, Math.round(distanciaDestino()), obtenerDistrito(), noche, faros,
            camaraAerea, mostrarAyuda, mostrarMapa ? lado : 0, margen);
        if (glGetError() != GL_NO_ERROR) throw new IllegalStateException("Error OpenGL en minimapa/HUD");
    }

    private void dibujarMinimapa(int lado, int margen) {
        int x = ancho - lado - margen, y = alto - lado - margen;
        glEnable(GL_SCISSOR_TEST);
        glScissor(x - 2, y - 2, lado + 4, lado + 4);
        glClearColor(0.35f, 0.52f, 0.56f, 1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glScissor(x, y, lado, lado);
        glClearColor(0.07f, 0.12f, 0.15f, 1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glViewport(x, y, lado, lado);
        entero("uMapa", 1);
        vistaMapa = true;
        try {
            escena();
            cajaGirada(autoX, 25, autoZ, 2.4f, 0.1f, 3.4f, 0.20f, 0.97f, 0.95f, angulo);
            caja(autoX - (float) Math.sin(angulo) * 2.2f, 26,
                autoZ - (float) Math.cos(angulo) * 2.2f, 1, 0.1f, 1, 1, 1, 1);
            for (AutoIA auto : traficoIA) {
                caja(auto.x, 24, auto.z, 2.2f, 0.1f, 2.2f, auto.r, auto.g, auto.b);
            }
        } finally {
            vistaMapa = false;
            entero("uMapa", 0);
            glDisable(GL_SCISSOR_TEST);
            glViewport(0, 0, ancho, alto);
        }
    }

    @Override
    protected void liberarRecursosExtra() {
        hud.close();
        super.liberarRecursosExtra();
    }

    public static void main(String[] args) {
        new clase4().run();
    }
}
