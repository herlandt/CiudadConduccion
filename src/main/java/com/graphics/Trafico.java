package com.graphics;

import java.util.Random;

/** Simulacion de carriles y cruces independiente de OpenGL. */
public final class Trafico {
    public static final float CARRIL = Semaforos.CARRIL;
    private static final double PASO = 1.0 / 120;
    private static final double ACELERACION = 3.5;
    private static final double FRENADO = 8;
    private static final double VELOCIDAD_GIRO = 6;
    private static final double EPSILON = 1e-7;

    private Trafico() { }

    public static class Auto {
        public float x, z, dirX, dirZ, angulo;
        public final float velocidad, r, g, b;
        public float velocidadActual;
        private final Random ruta;
        private double posicionX, posicionZ, rapidez;
        private double acumulado, tiempoPropio;
        private double objetivoX, objetivoZ;
        private int entradaX, entradaZ, salidaX, salidaZ;
        private boolean comprometido, enCruce;
        private double recorridoCruce, longitudCruce, radio;
        private double inicioCruceX, inicioCruceZ;

        /** xCentroNodo/zCentroNodo son el centro de un cruce, antes de aplicar el carril derecho. */
        public Auto(float xCentroNodo, float zCentroNodo, float dirX, float dirZ,
                    float velocidad, float r, float g, float b) {
            if (Math.abs(dirX) + Math.abs(dirZ) != 1 || dirX != Math.rint(dirX)
                    || dirZ != Math.rint(dirZ) || velocidad <= 0 || !Float.isFinite(velocidad)) {
                throw new IllegalArgumentException("Se requiere una direccion cardinal y velocidad positiva");
            }
            entradaX = (int) dirX;
            entradaZ = (int) dirZ;
            this.velocidad = velocidad;
            this.r = r;
            this.g = g;
            this.b = b;
            rapidez = velocidad;
            posicionX = xCentroNodo - entradaZ * CARRIL;
            posicionZ = zCentroNodo + entradaX * CARRIL;
            objetivoX = xCentroNodo + entradaX * 20;
            objetivoZ = zCentroNodo + entradaZ * 20;
            if (Math.abs(objetivoX) > 50 || Math.abs(objetivoZ) > 50) {
                throw new IllegalArgumentException("La direccion inicial debe apuntar hacia otro cruce del mapa");
            }
            ruta = new Random(42L + Float.floatToIntBits(xCentroNodo) * 31L
                + Float.floatToIntBits(zCentroNodo));
            elegirSalida();
            publicar(entradaX, entradaZ);
        }

        /** Compatibilidad para simulaciones aisladas: el reloj comienza en cero. */
        public void actualizar(float deltaTime) {
            actualizar(deltaTime, tiempoPropio);
            if (Float.isFinite(deltaTime) && deltaTime > 0) tiempoPropio += deltaTime;
        }

        /** tiempoInicio es el reloj comun de la escena ANTES de sumar deltaTime. */
        public void actualizar(float deltaTime, double tiempoInicio) {
            if (!Float.isFinite(deltaTime) || deltaTime <= 0 || !Double.isFinite(tiempoInicio)) return;
            double instante = tiempoInicio - acumulado;
            acumulado += deltaTime;
            while (acumulado + EPSILON >= PASO) {
                simular(PASO, instante);
                acumulado -= PASO;
                if (acumulado < 0 && acumulado > -EPSILON) acumulado = 0;
                instante += PASO;
            }
        }

        private double distanciaAlNodo() {
            return (objetivoX - posicionX) * entradaX + (objetivoZ - posicionZ) * entradaZ;
        }

        private void simular(double dt, double tiempo) {
            boolean avanzar = enCruce || comprometido
                || Semaforos.estado(entradaX, entradaZ, tiempo) == Semaforos.Estado.VERDE;
            double hastaLinea = distanciaAlNodo() - Semaforos.DISTANCIA_DETENCION;
            double deseada = velocidad;
            if (radio > 0) {
                double hastaCurva = enCruce ? 0 : Math.max(0, distanciaAlNodo() - Semaforos.MEDIA_INTERSECCION);
                deseada = Math.min(deseada, Math.sqrt(VELOCIDAD_GIRO * VELOCIDAD_GIRO + 2 * FRENADO * hastaCurva));
            }
            if (!avanzar) deseada = Math.min(deseada, Math.sqrt(2 * FRENADO * Math.max(0, hastaLinea)));
            double anterior = rapidez;
            if (rapidez < deseada) rapidez = Math.min(deseada, rapidez + ACELERACION * dt);
            else rapidez = Math.max(deseada, rapidez - FRENADO * dt);
            double distancia = (anterior + rapidez) * 0.5 * dt;
            if (!avanzar && distancia >= Math.max(0, hastaLinea)) {
                distancia = Math.max(0, hastaLinea);
                rapidez = 0;
            }
            // Al rebasar la linea con verde, el coche termina de despejar aunque cambie la luz.
            if (!enCruce && avanzar && distancia >= hastaLinea) comprometido = true;
            recorrer(distancia);
            velocidadActual = (float) rapidez;
        }

        private void recorrer(double distancia) {
            while (distancia > EPSILON) {
                if (!enCruce) {
                    double hastaEntrada = Math.max(0, distanciaAlNodo() - Semaforos.MEDIA_INTERSECCION);
                    double avance = Math.min(distancia, hastaEntrada);
                    posicionX += entradaX * avance;
                    posicionZ += entradaZ * avance;
                    distancia -= avance;
                    publicar(entradaX, entradaZ);
                    if (hastaEntrada - avance <= EPSILON) iniciarCruce();
                } else {
                    double avance = Math.min(distancia, longitudCruce - recorridoCruce);
                    recorridoCruce += avance;
                    distancia -= avance;
                    if (radio == 0) {
                        posicionX = inicioCruceX + entradaX * recorridoCruce;
                        posicionZ = inicioCruceZ + entradaZ * recorridoCruce;
                        publicar(entradaX, entradaZ);
                    } else {
                        double t = recorridoCruce / radio;
                        double seno = Math.sin(t), coseno = Math.cos(t);
                        posicionX = inicioCruceX + radio * (entradaX * seno + salidaX * (1 - coseno));
                        posicionZ = inicioCruceZ + radio * (entradaZ * seno + salidaZ * (1 - coseno));
                        publicar(entradaX * coseno + salidaX * seno, entradaZ * coseno + salidaZ * seno);
                    }
                    if (longitudCruce - recorridoCruce <= EPSILON) finalizarCruce();
                }
            }
        }

        private void iniciarCruce() {
            inicioCruceX = posicionX;
            inicioCruceZ = posicionZ;
            recorridoCruce = 0;
            enCruce = true;
        }

        /** La salida se decide antes de llegar para reducir la velocidad antes de una curva. */
        private void elegirSalida() {
            int[] candidatos = new int[4];
            int cantidad = 0;
            for (int i = 0; i < 4; i++) {
                int dx = i == 0 ? 1 : i == 1 ? -1 : 0;
                int dz = i == 2 ? 1 : i == 3 ? -1 : 0;
                if (dx == -entradaX && dz == -entradaZ) continue;
                if (Math.abs(objetivoX + dx * 20) <= 50 && Math.abs(objetivoZ + dz * 20) <= 50) {
                    candidatos[cantidad++] = i;
                }
            }
            int elegida = candidatos[ruta.nextInt(cantidad)];
            salidaX = elegida == 0 ? 1 : elegida == 1 ? -1 : 0;
            salidaZ = elegida == 2 ? 1 : elegida == 3 ? -1 : 0;
            boolean recto = entradaX == salidaX && entradaZ == salidaZ;
            boolean derecha = salidaX == -entradaZ && salidaZ == entradaX;
            radio = recto ? 0 : Semaforos.MEDIA_INTERSECCION + (derecha ? -CARRIL : CARRIL);
            longitudCruce = recto ? 2 * Semaforos.MEDIA_INTERSECCION : radio * Math.PI / 2;
        }

        private void finalizarCruce() {
            entradaX = salidaX;
            entradaZ = salidaZ;
            // Mismo punto geometrico que el final del arco: corrige solo el redondeo acumulado.
            posicionX = objetivoX + entradaX * Semaforos.MEDIA_INTERSECCION - entradaZ * CARRIL;
            posicionZ = objetivoZ + entradaZ * Semaforos.MEDIA_INTERSECCION + entradaX * CARRIL;
            objetivoX += entradaX * 20;
            objetivoZ += entradaZ * 20;
            enCruce = false;
            comprometido = false;
            elegirSalida();
            publicar(entradaX, entradaZ);
        }

        private void publicar(double direccionX, double direccionZ) {
            x = (float) posicionX;
            z = (float) posicionZ;
            dirX = (float) direccionX;
            dirZ = (float) direccionZ;
            angulo = (float) Math.atan2(-direccionX, -direccionZ);
            velocidadActual = (float) rapidez;
        }
    }
}
