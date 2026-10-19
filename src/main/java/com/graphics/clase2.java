package com.graphics; // Agrupa las cuatro lecciones dentro del mismo paquete.

import static org.lwjgl.glfw.GLFW.*; // Permite consultar teclas y cambiar el título de la ventana.

/**
 * CLASE 2: CREACIÓN Y MOVIMIENTO DEL AUTO.
 * Hereda la ciudad de clase1 y agrega únicamente lo necesario para conducir.
 * Orden de lectura: variables, teclado, movimiento, colisiones, cámara y dibujo.
 */
public class clase2 extends clase1 {

    // ==================== 1. VARIABLES DEL AUTO ====================
    protected float autoX = -47.5f; // Carril derecho de la calle oeste, mirando al norte.
    protected float autoZ = 50; // Posición inicial sobre el eje que recorre el fondo de la ciudad.
    protected float angulo = 0; // Orientación en radianes; cero apunta hacia -Z.
    protected float velocidad = 0; // Unidades por segundo; un valor negativo significa reversa.
    protected boolean camaraAerea = false; // false: seguir el auto; true: observar toda la ciudad.
    protected static final float RADIO_AUTO = 1.65f; // Radio que contiene al vehículo para las colisiones.
    // La chata tiene su propio eje: gira alrededor del enganche, no rígidamente con el auto.
    protected float chataX = -47.5f;
    protected float chataZ = autoZ + ENGANCHE_AUTO + ENGANCHE_A_CENTRO_CHATA;
    protected float anguloChata = 0;
    private static final float ENGANCHE_AUTO = 1.37f;
    private static final float LARGO_LANZA = 0.65f;
    private static final float ANCHO_CHATA = 2.12f; // Incluye las ruedas.
    private static final float LARGO_CHATA = 2.00f;
    private static final float ENGANCHE_A_CENTRO_CHATA = LARGO_LANZA + LARGO_CHATA * 0.5f;
    // Un remolque pequeño puede hacer tijera al retroceder, pero no atravesar el auto.
    private static final float MAX_ANGULO_ARTICULACION = 0.70f; // 40 grados, justo antes de tocar el auto.
    // En reversa la chata es inestable; el asistente contragira para llevarla al ángulo pedido.
    private static final float ANGULO_REVERSA = 0.30f; // Ángulo de la chata con A/D en reversa (17 grados).
    private static final float CORRECCION_REVERSA = 1.2f; // Rapidez de la corrección, por metro recorrido.
    private static final float VOLANTE_MAXIMO_REVERSA = 2f; // El asistente puede girar el volante a fondo.
    private static final float PASO_FISICA = 1f / 120f;
    private static final float MARGEN_CAMARA = 0.4f; // Reserva espacio para el plano cercano y las cornisas.

    // ==================== 2. TECLADO Y REINICIO ====================

    /** Atiende acciones que deben ocurrir una sola vez por pulsación. */
    @Override // Sustituye el método de clase1 y conserva sus acciones mediante super.
    protected void tecla(int key) {
        super.tecla(key); // Ejecuta el control ESC definido en clase1.

        if (key == GLFW_KEY_C) { // Comprueba si se presionó la tecla de cámara.
            camaraAerea = !camaraAerea; // Invierte el modo de cámara actual.
        }

        if (key == GLFW_KEY_R) { // Comprueba si el usuario quiere comenzar de nuevo.
            reiniciar(); // Restaura las variables de conducción.
        }
    }

    /** Coloca nuevamente el auto en su punto de partida. */
    protected void reiniciar() {
        autoX = -47.5f; // Recupera el carril derecho inicial.
        autoZ = 50; // Recupera la coordenada Z de inicio.
        angulo = 0; // Orienta el frente hacia -Z.
        velocidad = 0; // Detiene cualquier movimiento previo.
        chataX = -47.5f;
        chataZ = autoZ + ENGANCHE_AUTO + ENGANCHE_A_CENTRO_CHATA;
        anguloChata = 0;
    }

    // ==================== 3. MOVIMIENTO POR CUADRO ====================

    /** Actualiza la conducción; deltaTime contiene los segundos transcurridos entre cuadros. */
    @Override // Cambia la cámara orbital de clase1 por el control del vehículo.
    protected void actualizar(float deltaTime) {
        float acelerador = 0; // Sin teclas pulsadas no se aplica aceleración del motor.

        if (pulsada(GLFW_KEY_W) || pulsada(GLFW_KEY_UP)) { // Acepta W o flecha arriba para avanzar.
            acelerador += 1; // Solicita aceleración hacia delante.
        }

        if (pulsada(GLFW_KEY_S) || pulsada(GLFW_KEY_DOWN)) { // Acepta S o flecha abajo para retroceder.
            acelerador -= 1; // Primero reduce la velocidad positiva y después entra en reversa.
        }

        float direccion = 0; // Sin dirección presionada el volante permanece recto.

        if (pulsada(GLFW_KEY_A) || pulsada(GLFW_KEY_LEFT)) { // Comprueba el giro hacia la izquierda.
            direccion += 1; // Selecciona el sentido positivo de rotación.
        }

        if (pulsada(GLFW_KEY_D) || pulsada(GLFW_KEY_RIGHT)) { // Comprueba el giro hacia la derecha.
            direccion -= 1; // Selecciona el sentido negativo de rotación.
        }

        actualizarConjunto(deltaTime, acelerador, direccion, pulsada(GLFW_KEY_SPACE));
        actualizarTitulo(); // Muestra los controles y la velocidad actual.
    }

    /** Simulación determinista y sin OpenGL: también permite probar la física sin una ventana. */
    void actualizarConjunto(float deltaTime, float acelerador, float direccion, boolean freno) {
        float restante = Math.max(0, Math.min(deltaTime, 0.25f));
        while (restante > 0.00001f) {
            float paso = Math.min(PASO_FISICA, restante);
            simularPaso(paso, acelerador, direccion, freno);
            restante -= paso;
        }
    }

    private void simularPaso(float deltaTime, float acelerador, float direccion, boolean freno) {
        // El remolque añade masa: acelera, frena y retrocede más despacio que el auto solo.
        velocidad += acelerador * 6.5f * deltaTime;
        float resistencia = freno ? 8f : 1.0f;
        velocidad *= (float) Math.exp(-resistencia * deltaTime);
        velocidad = Math.max(-4.5f, Math.min(12f, velocidad));

        float volante = velocidad < 0 ? volanteReversa(direccion) : direccion;
        float siguienteAngulo = angulo + volante * velocidad * 0.11f * deltaTime;
        float frenteX = -(float) Math.sin(siguienteAngulo); // Obtiene la componente X del frente del vehículo.
        float frenteZ = -(float) Math.cos(siguienteAngulo); // Obtiene la componente Z; con ángulo cero vale -1.
        float siguienteX = autoX + frenteX * velocidad * deltaTime; // Propone la nueva posición X.
        float siguienteZ = autoZ + frenteZ * velocidad * deltaTime; // Propone la nueva posición Z.

        float[] siguienteChata = posicionChata(siguienteX, siguienteZ, siguienteAngulo);
        if (puedeCircular(siguienteX, siguienteZ)
                && puedeCircularChata(siguienteChata[0], siguienteChata[1], siguienteChata[2])
                && chataNoInvadeAuto(siguienteX, siguienteZ, siguienteAngulo,
                    siguienteChata[0], siguienteChata[1], siguienteChata[2])
                && articulacionPermitida(siguienteAngulo, siguienteChata[2])) {
            autoX = siguienteX; // Acepta el desplazamiento horizontal.
            autoZ = siguienteZ; // Acepta el desplazamiento en profundidad.
            angulo = siguienteAngulo;
            chataX = siguienteChata[0];
            chataZ = siguienteChata[1];
            anguloChata = siguienteChata[2];
        } else { // La posición propuesta invadiría una manzana o saldría del mapa.
            velocidad = 0; // Detiene el auto conservando su última posición válida.
        }
    }

    /**
     * Retroceder con remolque es inestable: sin corregir, cualquier ángulo crece hasta la tijera.
     * Como haría un conductor, el asistente contragira para que la chata vaya recta sin teclas
     * o hacia el lado de A/D. Despeja el volante de la ecuación de la articulación por metro:
     * dφ/ds = sen(φ)/L - 0.11·volante·(1 + E/L·cos(φ)), con E = enganche y L = enganche-eje.
     */
    private float volanteReversa(float direccion) {
        float articulacion = normalizarAngulo(angulo - anguloChata); // Positiva: la chata va a la izquierda.
        float objetivo = direccion * ANGULO_REVERSA;
        float apoyo = 1 + ENGANCHE_AUTO / ENGANCHE_A_CENTRO_CHATA * (float) Math.cos(articulacion);
        float volante = ((float) Math.sin(articulacion) / ENGANCHE_A_CENTRO_CHATA
            + CORRECCION_REVERSA * (articulacion - objetivo)) / (0.11f * apoyo);
        return Math.max(-VOLANTE_MAXIMO_REVERSA, Math.min(VOLANTE_MAXIMO_REVERSA, volante));
    }

    /** Compone el texto de la ventana sin mezclarlo con las fórmulas de conducción. */
    private void actualizarTitulo() {
        int kilometrosPorHora = Math.round(Math.abs(velocidad) * 3.6f); // Convierte m/s a km/h y redondea.
        String titulo = getClass().getSimpleName(); // Obtiene el nombre de la etapa que se está ejecutando.
        titulo += " | WASD/flechas: conducir | Espacio: freno"; // Añade los controles del vehículo.
        titulo += " | C: camara | R: reiniciar | "; // Añade los controles de cámara y reinicio.
        titulo += kilometrosPorHora + " km/h"; // Añade la magnitud de la velocidad.
        titulo += estadoExtra(); // Permite a clase3 y clase4 añadir luces y entregas.
        glfwSetWindowTitle(ventana, titulo); // Publica el texto en la barra superior de la ventana.
    }

    /** Deja un espacio para los indicadores de las próximas lecciones. */
    protected String estadoExtra() {
        return ""; // En clase2 todavía no hay información adicional.
    }

    // ==================== 4. COLISIONES CON LA CIUDAD ====================

    /** Comprueba si el círculo del auto cabe en una posición sin tocar manzanas ni bordes. */
    protected boolean puedeCircular(float x, float z) {
        return puedeCircularConRadio(x, z, RADIO_AUTO);
    }

    /** Variante reutilizable para que la chata compruebe el mismo mapa con su propio ancho. */
    private boolean puedeCircularConRadio(float x, float z, float radio) {
        float limitePermitido = LIMITE - radio; // Reserva espacio para que el objeto completo quede dentro.

        if (Math.abs(x) > limitePermitido || Math.abs(z) > limitePermitido) { // Detecta salida por cualquier borde.
            return false; // Rechaza la posición exterior.
        }

        for (int fila = 0; fila < MAPA.length; fila++) { // Recorre las filas del mapa.
            for (int columna = 0; columna < MAPA[fila].length; columna++) { // Recorre las celdas de cada fila.
                if (MAPA[fila][columna] == 0) { // Una celda con cero representa calle.
                    continue; // Omite la calle porque no es un obstáculo.
                }

                float centroX = centro(columna); // Convierte la columna al centro X de la manzana.
                float centroZ = centro(fila); // Convierte la fila al centro Z de la manzana.
                float cercaX = Math.max(centroX - 5, Math.min(x, centroX + 5)); // Busca el X más cercano dentro de la acera.
                float cercaZ = Math.max(centroZ - 5, Math.min(z, centroZ + 5)); // Busca el Z más cercano dentro de la acera.
                float distanciaX = x - cercaX; // Calcula la separación horizontal del auto al rectángulo.
                float distanciaZ = z - cercaZ; // Calcula la separación en profundidad al rectángulo.
                float distanciaCuadrada = distanciaX * distanciaX + distanciaZ * distanciaZ; // Aplica Pitágoras sin raíz.

                if (distanciaCuadrada < radio * radio) { // Comprueba si el círculo invade la manzana.
                    return false; // Rechaza el movimiento que produciría una colisión.
                }
            }
        }

        return true; // Acepta la posición porque no se encontró ningún obstáculo.
    }

    /**
     * Articulación rígida de la lanza: el eje de la chata queda a distancia fija del enganche.
     * Los pasos cortos de la simulación evitan saltos cuando baja la tasa de cuadros.
     */
    private float[] posicionChata(float nuevoAutoX, float nuevoAutoZ, float nuevoAngulo) {
        float engancheX = nuevoAutoX + (float) Math.sin(nuevoAngulo) * ENGANCHE_AUTO;
        float engancheZ = nuevoAutoZ + (float) Math.cos(nuevoAngulo) * ENGANCHE_AUTO;
        // El eje solo avanza en la dirección de la lanza: la chata apunta a donde quedó el enganche.
        float nuevoAnguloChata = (float) Math.atan2(chataX - engancheX, chataZ - engancheZ);
        float x = engancheX + (float) Math.sin(nuevoAnguloChata) * ENGANCHE_A_CENTRO_CHATA;
        float z = engancheZ + (float) Math.cos(nuevoAnguloChata) * ENGANCHE_A_CENTRO_CHATA;
        return new float[] {x, z, nuevoAnguloChata};
    }

    /** El rectángulo orientado de la chata (incluidas ruedas) no puede tocar bloques ni bordes. */
    protected boolean puedeCircularChata(float x, float z, float orientacion) {
        float mitadX = ANCHO_CHATA * 0.5f, mitadZ = LARGO_CHATA * 0.5f;
        for (int sx : new int[] {-1, 1}) for (int sz : new int[] {-1, 1}) {
            float esquinaX = x + (float) Math.cos(orientacion) * sx * mitadX
                + (float) Math.sin(orientacion) * sz * mitadZ;
            float esquinaZ = z - (float) Math.sin(orientacion) * sx * mitadX
                + (float) Math.cos(orientacion) * sz * mitadZ;
            if (Math.abs(esquinaX) > LIMITE || Math.abs(esquinaZ) > LIMITE) return false;
        }
        for (int fila = 0; fila < MAPA.length; fila++) for (int columna = 0; columna < MAPA[fila].length; columna++) {
            if (MAPA[fila][columna] != 0 && rectangulosSeCruzan(x, z, mitadX, mitadZ, orientacion,
                    centro(columna), centro(fila), 5, 5, 0)) return false;
        }
        return true;
    }

    /** Colisión precisa entre las dos carrocerías; sustituye la antigua distancia entre centros. */
    static boolean chataNoInvadeAuto(float xAuto, float zAuto, float anguloAuto,
                                     float xChata, float zChata, float anguloChata) {
        return !rectangulosSeCruzan(xAuto, zAuto, 1.03f, 1.34f, anguloAuto,
            xChata, zChata, ANCHO_CHATA * 0.5f, LARGO_CHATA * 0.5f, anguloChata);
    }

    /** Separating Axis Theorem para dos rectángulos orientados en el plano X/Z. */
    private static boolean rectangulosSeCruzan(float ax, float az, float ahx, float ahz, float aa,
                                               float bx, float bz, float bhx, float bhz, float ba) {
        float[][] ejes = {{(float) Math.cos(aa), -(float) Math.sin(aa)}, {(float) Math.sin(aa), (float) Math.cos(aa)},
            {(float) Math.cos(ba), -(float) Math.sin(ba)}, {(float) Math.sin(ba), (float) Math.cos(ba)}};
        for (float[] eje : ejes) {
            float distancia = Math.abs((bx - ax) * eje[0] + (bz - az) * eje[1]);
            float alcanceA = proyeccionRectangulo(ahx, ahz, aa, eje);
            float alcanceB = proyeccionRectangulo(bhx, bhz, ba, eje);
            if (distancia >= alcanceA + alcanceB) return false;
        }
        return true;
    }

    private static float proyeccionRectangulo(float mitadX, float mitadZ, float angulo, float[] eje) {
        float lateral = Math.abs((float) Math.cos(angulo) * eje[0] - (float) Math.sin(angulo) * eje[1]);
        float longitudinal = Math.abs((float) Math.sin(angulo) * eje[0] + (float) Math.cos(angulo) * eje[1]);
        return mitadX * lateral + mitadZ * longitudinal;
    }

    static boolean articulacionValida(float anguloAuto, float anguloRemolque) {
        return Math.abs(normalizarAngulo(anguloRemolque - anguloAuto)) <= MAX_ANGULO_ARTICULACION;
    }

    /** Permite salir de una tijera existente, pero nunca aumentarla. */
    private boolean articulacionPermitida(float anguloAutoPropuesto, float anguloChataPropuesto) {
        float propuesta = Math.abs(normalizarAngulo(anguloChataPropuesto - anguloAutoPropuesto));
        if (propuesta <= MAX_ANGULO_ARTICULACION) return true;
        float actual = Math.abs(normalizarAngulo(anguloChata - angulo));
        return propuesta + 0.0001f < actual;
    }

    private static float normalizarAngulo(float valor) {
        while (valor > Math.PI) valor -= (float) (2 * Math.PI);
        while (valor < -Math.PI) valor += (float) (2 * Math.PI);
        return valor;
    }

    // ==================== 5. CÁMARA ====================

    /** Elige entre una vista general y una cámara situada detrás del auto. */
    @Override // Personaliza la cámara creada en clase1.
    protected void configurarCamara() {
        if (camaraAerea) { // Comprueba si está activa la vista general.
            super.configurarCamara(); // Reutiliza la cámara oblicua de la primera lección.
            return; // Evita reemplazarla con la cámara de seguimiento.
        }

        float[] ojo = posicionCamaraSeguimiento(); // Acerca la cámara si un edificio interrumpe la vista del auto.
        vector("uOjo", ojo[0], ojo[1], ojo[2]);
        vector("uObjetivo", autoX, 0.8f, autoZ); // Orienta la cámara hacia la carrocería.
        decimal("uAspecto", (float) ancho / alto); // Mantiene las proporciones al redimensionar la ventana.
    }

    /** Calcula la cámara sin OpenGL para poder comprobar colisiones en las pruebas. */
    protected float[] posicionCamaraSeguimiento() {
        float deseadaX = autoX + (float) Math.sin(angulo) * 12;
        float deseadaZ = autoZ + (float) Math.cos(angulo) * 12;
        float fraccion = fraccionCamaraLibre(autoX, 0.8f, autoZ, deseadaX, 9, deseadaZ);
        return new float[] {
            autoX + (deseadaX - autoX) * fraccion,
            0.8f + 8.2f * fraccion,
            autoZ + (deseadaZ - autoZ) * fraccion
        };
    }

    /** Encuentra el primer obstáculo en TODO el segmento objetivo-ojo, no solo en su extremo. */
    protected float fraccionCamaraLibre(float x, float y, float z, float ojoX, float ojoY, float ojoZ) {
        float dx = ojoX - x;
        float dy = ojoY - y;
        float dz = ojoZ - z;
        float primeraEntrada = 1;
        float semiancho = 4.1f + MARGEN_CAMARA; // Cubre fachadas, balcones y el radio de la cámara.
        for (int fila = 0; fila < MAPA.length; fila++) {
            for (int columna = 0; columna < MAPA[fila].length; columna++) {
                if (MAPA[fila][columna] != 1) continue;
                float centroX = centro(columna);
                float centroZ = centro(fila);
                float entrada = entradaSegmentoCaja(x, y, z, dx, dy, dz,
                    centroX - semiancho, -MARGEN_CAMARA, centroZ - semiancho,
                    centroX + semiancho, alturaEdificio(fila, columna) + 2.2f + MARGEN_CAMARA,
                    centroZ + semiancho);
                primeraEntrada = Math.min(primeraEntrada, entrada);
            }
        }
        if (primeraEntrada >= 1) return 1;
        float longitud = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        return Math.max(0, primeraEntrada - 0.15f / Math.max(longitud, 0.001f));
    }

    /** Intersección por intervalos; funciona también con rayos paralelos a una fachada. */
    private static float entradaSegmentoCaja(float x, float y, float z, float dx, float dy, float dz,
                                             float minX, float minY, float minZ,
                                             float maxX, float maxY, float maxZ) {
        float[] origen = {x, y, z};
        float[] direccion = {dx, dy, dz};
        float[] minimo = {minX, minY, minZ};
        float[] maximo = {maxX, maxY, maxZ};
        float entrada = 0;
        float salida = 1;
        for (int eje = 0; eje < 3; eje++) {
            if (Math.abs(direccion[eje]) < 0.00001f) {
                if (origen[eje] < minimo[eje] || origen[eje] > maximo[eje]) return 1;
                continue;
            }
            float primero = (minimo[eje] - origen[eje]) / direccion[eje];
            float segundo = (maximo[eje] - origen[eje]) / direccion[eje];
            entrada = Math.max(entrada, Math.min(primero, segundo));
            salida = Math.min(salida, Math.max(primero, segundo));
            if (entrada > salida) return 1;
        }
        return entrada;
    }

    // ==================== 6. DIBUJO DEL AUTO ====================

    /** Dibuja primero la ciudad existente y luego el vehículo. */
    @Override // Amplía el dibujo de clase1 sin copiar su código.
    protected void escena() {
        super.escena(); // Dibuja asfalto, calles, edificios y parques.
        dibujarAuto(); // Añade el modelo del vehículo sobre la ciudad.
    }

    /** Usa el mismo modelo que el tráfico, conservando color y estado propios del jugador. */
    protected void dibujarAuto() {
        dibujarVehiculo(autoX, autoZ, angulo, 0.88f, 0.20f, 0.09f, lucesVehiculoEncendidas());
        dibujarChata();
    }

    /** Chata de madera de un eje: piso, laterales, marco metálico, lanza y ruedas. */
    private void dibujarChata() {
        // La lanza une visualmente ambos puntos de la articulación.
        float engancheX = autoX + (float) Math.sin(angulo) * ENGANCHE_AUTO;
        float engancheZ = autoZ + (float) Math.cos(angulo) * ENGANCHE_AUTO;
        float frenteX = chataX - (float) Math.sin(anguloChata) * (LARGO_CHATA * 0.5f);
        float frenteZ = chataZ - (float) Math.cos(anguloChata) * (LARGO_CHATA * 0.5f);
        float medioX = (engancheX + frenteX) * 0.5f, medioZ = (engancheZ + frenteZ) * 0.5f;
        float largoLanza = (float) Math.hypot(frenteX - engancheX, frenteZ - engancheZ);
        float anguloLanza = (float) Math.atan2(-(frenteX - engancheX), -(frenteZ - engancheZ));
        cajaGiradaMaterial(Materiales.METAL, medioX, 0.48f, medioZ, 0.12f, 0.12f, largoLanza,
            0.12f, 0.15f, 0.17f, anguloLanza);

        piezaChata(Materiales.METAL, 0, 0.36f, 0, 1.62f, 0.14f, 2.05f, 0.16f, 0.19f, 0.21f);
        piezaChata(Materiales.MADERA, 0, 0.48f, 0, 1.48f, 0.12f, 1.88f, 0.52f, 0.29f, 0.12f);
        for (int lado : new int[] {-1, 1}) {
            // Los laterales apoyan sobre el piso (sin ranura) y los postes quedan sobre la rueda.
            piezaChata(Materiales.MADERA, lado * 0.74f, 0.875f, 0, 0.10f, 0.66f, 1.96f, 0.52f, 0.30f, 0.13f);
            for (float poste : new float[] {-0.92f, 0, 0.92f}) {
                piezaChata(Materiales.METAL, lado * 0.81f, 0.935f, poste, 0.10f, 0.75f, 0.10f, 0.10f, 0.13f, 0.15f);
            }
        }
        for (float extremo : new float[] {-0.94f, 0.94f}) {
            // Encajan entre los laterales: sin caras superpuestas que parpadeen en las esquinas.
            piezaChata(Materiales.MADERA, 0, 0.875f, extremo, 1.38f, 0.66f, 0.10f, 0.52f, 0.30f, 0.13f);
        }
        for (int lado : new int[] {-1, 1}) dibujarRuedaChata(lado);
    }

    private void dibujarRuedaChata(int lado) {
        // El eje está en el origen de la chata: coincide con el punto que sigue la física.
        piezaChata(Materiales.LISO, lado * 0.93f, 0.275f, 0, 0.22f, 0.55f, 0.62f, 0.035f, 0.043f, 0.052f);
        piezaChata(Materiales.METAL, lado * 1.04f, 0.275f, 0, 0.04f, 0.27f, 0.27f, 0.61f, 0.65f, 0.69f);
    }

    private void piezaChata(int material, float x, float y, float z, float sx, float sy, float sz,
                            float r, float g, float b) {
        float coseno = (float) Math.cos(anguloChata), seno = (float) Math.sin(anguloChata);
        float mundoX = chataX + coseno * x + seno * z;
        float mundoZ = chataZ - seno * x + coseno * z;
        cajaGiradaMaterial(material, mundoX, y, mundoZ, sx, sy, sz, r, g, b, anguloChata);
    }

    /** La tercera etapa conecta esta apariencia con el interruptor F de los faros. */
    protected boolean lucesVehiculoEncendidas() {
        return false;
    }

    /** Modelo compacto compartido: carrocería pintada, cristales, neumáticos, llantas y luces. */
    protected void dibujarVehiculo(float x, float z, float orientacion, float r, float g, float b,
                                  boolean lucesEncendidas) {
        entero("uEmision", 0);
        piezaVehiculo(x, z, orientacion, Materiales.LISO,
            0, 0.42f, 0, 1.55f, 0.18f, 2.45f, 0.07f, 0.085f, 0.10f);
        piezaVehiculo(x, z, orientacion, Materiales.PINTURA,
            0, 0.68f, 0, 1.65f, 0.48f, 2.6f, r, g, b);
        piezaVehiculo(x, z, orientacion, Materiales.PINTURA,
            0, 0.94f, -0.9f, 1.57f, 0.08f, 0.72f, r, g, b);
        piezaVehiculo(x, z, orientacion, Materiales.PINTURA,
            0, 1.10f, 0.13f, 1.32f, 0.48f, 1.30f, r * 0.83f, g * 0.83f, b * 0.83f);
        piezaVehiculo(x, z, orientacion, Materiales.CRISTAL,
            0, 1.11f, -0.54f, 1.17f, 0.33f, 0.035f, 0.30f, 0.52f, 0.62f);
        piezaVehiculo(x, z, orientacion, Materiales.CRISTAL,
            0, 1.11f, 0.80f, 1.17f, 0.33f, 0.035f, 0.23f, 0.41f, 0.50f);
        piezaVehiculo(x, z, orientacion, Materiales.PINTURA,
            0, 1.36f, 0.13f, 1.36f, 0.07f, 1.32f, r, g, b);

        for (int lado : new int[] {-1, 1}) {
            piezaVehiculo(x, z, orientacion, Materiales.CRISTAL,
                lado * 0.68f, 1.11f, 0.13f, 0.035f, 0.33f, 1.12f, 0.27f, 0.48f, 0.58f);
            piezaVehiculo(x, z, orientacion, Materiales.LISO,
                lado * 0.705f, 1.11f, 0.11f, 0.035f, 0.37f, 0.07f, 0.10f, 0.12f, 0.14f);
            piezaVehiculo(x, z, orientacion, Materiales.PINTURA,
                lado * 0.86f, 1.01f, -0.43f, 0.16f, 0.12f, 0.21f, r, g, b);
            piezaVehiculo(x, z, orientacion, Materiales.METAL,
                lado * 0.835f, 0.83f, 0.34f, 0.025f, 0.045f, 0.16f, 0.65f, 0.69f, 0.72f);
            for (float eje : new float[] {-0.82f, 0.82f}) {
                dibujarRueda(x, z, orientacion, lado, eje);
            }
            piezaVehiculo(x, z, orientacion, Materiales.METAL,
                0, 0.48f, lado * 1.32f, 1.50f, 0.12f, 0.07f, 0.29f, 0.33f, 0.37f);
        }
        piezaVehiculo(x, z, orientacion, Materiales.LISO,
            0, 0.67f, -1.32f, 0.55f, 0.17f, 0.045f, 0.07f, 0.09f, 0.11f);
        entero("uEmision", lucesEncendidas ? 1 : 0);
        for (float lado : new float[] {-0.54f, 0.54f}) {
            piezaVehiculo(x, z, orientacion, Materiales.CRISTAL,
                lado, 0.73f, -1.33f, 0.35f, 0.17f, 0.06f,
                lucesEncendidas ? 1 : 0.65f, lucesEncendidas ? 0.94f : 0.72f, lucesEncendidas ? 0.73f : 0.75f);
            piezaVehiculo(x, z, orientacion, Materiales.CRISTAL,
                lado, 0.71f, 1.33f, 0.32f, 0.15f, 0.06f, 0.82f, 0.055f, 0.035f);
        }
        entero("uEmision", 0); // Evita que los materiales de los siguientes objetos emitan luz.
    }

    /** Perfil escalonado del neumático, apoyado en Y=0, con llanta visible en la cara exterior. */
    private void dibujarRueda(float x, float z, float orientacion, int lado, float eje) {
        float[] alturas = {-0.26f, -0.16f, 0, 0.16f, 0.26f};
        float[] profundidades = {0.32f, 0.50f, 0.62f, 0.50f, 0.32f};
        for (int franja = 0; franja < alturas.length; franja++) {
            piezaVehiculo(x, z, orientacion, Materiales.LISO,
                lado * 0.87f, 0.31f + alturas[franja], eje,
                0.23f, franja == 2 ? 0.22f : 0.10f, profundidades[franja], 0.035f, 0.043f, 0.052f);
        }
        piezaVehiculo(x, z, orientacion, Materiales.METAL,
            lado * 0.997f, 0.31f, eje, 0.03f, 0.29f, 0.29f, 0.61f, 0.65f, 0.69f);
        piezaVehiculo(x, z, orientacion, Materiales.LISO,
            lado * 1.017f, 0.31f, eje, 0.015f, 0.11f, 0.11f, 0.14f, 0.17f, 0.20f);
    }

    /** Coloca una pieza local sin modificar el estado del jugador al dibujar el tráfico. */
    private void piezaVehiculo(float autoCentroX, float autoCentroZ, float orientacion, int material,
                              float x, float y, float z, float sx, float sy, float sz,
                              float r, float g, float b) {
        float coseno = (float) Math.cos(orientacion);
        float seno = (float) Math.sin(orientacion);
        float mundoX = autoCentroX + coseno * x + seno * z;
        float mundoZ = autoCentroZ - seno * x + coseno * z;
        cajaGiradaMaterial(material, mundoX, y, mundoZ, sx, sy, sz, r, g, b, orientacion);
    }

    /** Transforma una pieza del espacio local del auto al espacio de la ciudad. */
    protected void pieza(float x, float y, float z, float sx, float sy, float sz, float r, float g, float b) {
        float coseno = (float) Math.cos(angulo); // Calcula el coseno de la orientación del auto.
        float seno = (float) Math.sin(angulo); // Calcula el seno de la misma orientación.
        float mundoX = autoX + coseno * x + seno * z; // Gira la posición local y suma la posición X del auto.
        float mundoZ = autoZ - seno * x + coseno * z; // Gira la posición local y suma la posición Z del auto.
        cajaGirada(mundoX, y, mundoZ, sx, sy, sz, r, g, b, angulo); // Dibuja la pieza con la orientación del vehículo.
    }

    /** Punto de entrada para ejecutar únicamente la segunda etapa. */
    public static void main(String[] args) {
        clase2 aplicacion = new clase2(); // Crea la aplicación con ciudad y vehículo.
        aplicacion.run(); // Inicia la ventana y el ciclo de dibujo heredados.
    }
}
