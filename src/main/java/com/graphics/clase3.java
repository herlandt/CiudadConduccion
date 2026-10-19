package com.graphics;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;

/** Etapa 3: materiales, luz difusa y especular, farolas, faros y sombras del sol. */
public class clase3 extends clase2 {
    protected boolean noche = false;
    protected boolean faros = true;
    protected static final float[][] LUCES = {
        {-36, 4.5f, -36}, {-16, 4.5f, -36}, {4, 4.5f, -36}, {24, 4.5f, -36},
        {36, 4.5f, -36}, {-36, 4.5f, -16}, {4, 4.5f, -16}, {36, 4.5f, -16},
        {-16, 4.5f, 4}, {24, 4.5f, 4}, {-36, 4.5f, 24}, {4, 4.5f, 24},
        {36, 4.5f, 24}, {-36, 4.5f, 36}, {-16, 4.5f, 36}, {24, 4.5f, 36}
    };

    @Override
    protected void tecla(int key) {
        super.tecla(key);
        if (key == GLFW_KEY_N) noche = !noche;
        if (key == GLFW_KEY_F) faros = !faros;
    }

    @Override
    protected boolean lucesVehiculoEncendidas() { return faros; }

    @Override
    protected boolean usarSombras() { return !noche; }

    @Override
    protected void colorCielo() {
        if (noche) glClearColor(0.035f, 0.065f, 0.12f, 1);
        else glClearColor(0.48f, 0.67f, 0.83f, 1);
    }

    @Override
    protected String estadoExtra() {
        return " | " + (noche ? "Noche" : "Dia") + " | Faros " + (faros ? "ON" : "OFF");
    }

    @Override
    protected void prepararLuces() {
        entero("uNoche", noche ? 1 : 0);
        entero("uFaros", faros ? 1 : 0);
        entero("uEmision", 0);
        int cantidad = 0;
        for (float[] luz : LUCES) {
            vector("uLuces[" + cantidad++ + "]", luz[0], luz[1], luz[2]);
        }
        // Los faroles decorativos existen en la etapa 4; su luz ahora afecta el parque.
        if (this instanceof clase4) {
            for (int fila = 0; fila < MAPA.length; fila++) {
                for (int columna = 0; columna < MAPA[fila].length; columna++) {
                    if (MAPA[fila][columna] == 2) {
                        vector("uLuces[" + cantidad++ + "]", centro(columna) + 3.5f, 3.1f, centro(fila) - 3.5f);
                    }
                }
            }
        }
        entero("uNumeroLuces", cantidad);
        vector("uAuto", autoX, 0.7f, autoZ);
        vector("uFrente", -(float) Math.sin(angulo), 0, -(float) Math.cos(angulo));
    }

    @Override
    protected void escena() {
        super.escena();
        for (float[] luz : LUCES) {
            float x = luz[0], z = luz[2];
            cajaMaterial(Materiales.METAL, x, 2.2f, z, 0.15f, 4.4f, 0.15f, 0.24f, 0.28f, 0.30f);
            cajaMaterial(Materiales.METAL, x, 0.45f, z, 0.40f, 0.3f, 0.40f, 0.28f, 0.30f, 0.32f);
            cajaMaterial(Materiales.METAL, x, 4.75f, z, 0.85f, 0.14f, 0.85f, 0.18f, 0.22f, 0.25f);
            entero("uEmision", noche ? 1 : 0);
            caja(x, luz[1], z, 0.60f, 0.30f, 0.60f, 1, 0.83f, 0.50f);
            entero("uEmision", 0);
        }
    }

    @Override
    protected String fragmentShader() {
        return """
            #version 330 core
            in vec3 vMundo;
            in vec3 vNormal;
            in vec4 vSol;
            uniform vec3 uColor;
            uniform vec3 uOjo;
            uniform vec3 uLuces[32];
            uniform int uNumeroLuces;
            uniform vec3 uAuto;
            uniform vec3 uFrente;
            uniform int uNoche;
            uniform int uFaros;
            uniform int uEmision;
            uniform int uMapa;
            uniform int uSombraPase;
            uniform int uUsarSombras;
            uniform sampler2D uSombra;
            out vec4 color;
            """ + Materiales.codigoGLSL() + """

            // PCF 3x3: compara profundidad con un margen para evitar acne.
            float sombraSolar(vec3 normal, vec3 sol) {
                if (uUsarSombras == 0) return 1.0;
                vec3 p = vSol.xyz * 0.5 + 0.5;
                if (p.z < 0.0 || p.z > 1.0 || any(lessThan(p.xy, vec2(0.0)))
                    || any(greaterThan(p.xy, vec2(1.0)))) return 1.0;
                float margen = max(0.00045 * (1.0 - dot(normal, sol)), 0.00016);
                vec2 texel = 1.0 / vec2(textureSize(uSombra, 0));
                float visible = 0.0;
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        float profundidad = texture(uSombra, p.xy + vec2(x, y) * texel).r;
                        visible += p.z - margen <= profundidad ? 1.0 : 0.0;
                    }
                }
                return visible / 9.0;
            }

            float brillo(vec3 n, vec3 luz, vec3 vista, float potencia) {
                if (dot(n, luz) <= 0.0) return 0.0;
                return pow(max(dot(n, normalize(luz + vista)), 0.0), potencia);
            }

            void main() {
                if (uSombraPase == 1) { color = vec4(1.0); return; }
                if (uMapa == 1) { color = vec4(uColor, 1.0); return; }
                if (uEmision == 1) { color = vec4(uColor, 1.0); return; }

                vec3 base = colorMaterial(uColor);
                vec3 normal = normalize(vNormal);
                vec3 vista = normalize(uOjo - vMundo);
                vec3 sol = normalize(vec3(0.4, 1.0, 0.3));
                float especular = 0.025;
                float potencia = 24.0;
                if (uMaterial == 7) { especular = 0.50; potencia = 64.0; }
                if (uMaterial == 8) { especular = 0.85; potencia = 96.0; }
                if (uMaterial == 9) { especular = 0.35; potencia = 56.0; }

                float cielo = 0.75 + 0.25 * max(normal.y, 0.0);
                vec3 luz = uNoche == 1 ? vec3(0.12, 0.17, 0.27) : vec3(0.48, 0.52, 0.58) * cielo;
                float visibilidad = sombraSolar(normal, sol);
                float fuerzaSol = uNoche == 1 ? 0.12 : 1.35;
                luz += vec3(1.0, 0.95, 0.84) * max(dot(normal, sol), 0.0) * fuerzaSol * visibilidad;
                vec3 reflejo = vec3(especular * brillo(normal, sol, vista, potencia) * fuerzaSol * visibilidad);

                if (uNoche == 1) {
                    for (int i = 0; i < uNumeroLuces; i++) {
                        vec3 delta = uLuces[i] - vMundo;
                        float d = length(delta);
                        vec3 direccion = delta / max(d, 0.001);
                        float alcance = 1.0 - smoothstep(14.0, 24.0, d);
                        float intensidad = 4.3 * alcance / (1.0 + 0.14 * d + 0.075 * d * d);
                        vec3 calida = vec3(1.0, 0.74, 0.40);
                        luz += calida * max(dot(normal, direccion), 0.0) * intensidad;
                        reflejo += calida * brillo(normal, direccion, vista, potencia) * intensidad * especular;
                    }
                }

                if (uFaros == 1) {
                    for (int i = 0; i < 2; i++) {
                        vec3 lateral = vec3(-uFrente.z, 0.0, uFrente.x);
                        vec3 origen = uAuto + uFrente * 1.42 + lateral * (i == 0 ? -0.55 : 0.55);
                        vec3 delta = vMundo - origen;
                        float distancia = length(delta);
                        vec3 direccion = delta / max(distancia, 0.001);
                        vec3 eje = normalize(uFrente + vec3(0.0, -0.12, 0.0));
                        float cono = smoothstep(0.88, 0.97, dot(direccion, eje));
                        float alcance = 1.0 - smoothstep(20.0, 32.0, distancia);
                        float intensidad = cono * alcance * (uNoche == 1 ? 7.0 : 2.0)
                            / (1.0 + 0.06 * distancia * distancia);
                        luz += vec3(1.0, 0.93, 0.73) * max(dot(normal, -direccion), 0.0) * intensidad;
                        reflejo += vec3(especular * brillo(normal, -direccion, vista, potencia) * intensidad);
                    }
                }

                // Se ilumina en espacio lineal y se comprimen los brillos intensos.
                vec3 lineal = pow(base, vec3(2.2)) * luz + reflejo * 0.18;
                vec3 finalColor = pow(lineal / (vec3(1.0) + lineal * 0.28), vec3(1.0 / 2.2));
                vec3 colorCielo = uNoche == 1 ? vec3(0.035, 0.065, 0.12) : vec3(0.48, 0.67, 0.83);
                float niebla = smoothstep(65.0, 230.0, length(uOjo - vMundo)) * (uNoche == 1 ? 0.20 : 0.32);
                color = vec4(mix(finalColor, colorCielo, niebla), 1.0);
            }
            """;
    }

    public static void main(String[] args) {
        new clase3().run();
    }
}
