package com.graphics;

import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.*;

/** Texturas originales generadas al iniciar: repetibles, sin descargas ni archivos externos. */
public final class Materiales {
    public static final int LISO = 0, ASFALTO = 1, ACERA = 2, CESPED = 3,
            LADRILLO = 4, ESTUCO = 5, MADERA = 6, METAL = 7,
            CRISTAL = 8, PINTURA = 9, HOJAS = 10;
    private static final int TAMANO = 256, CAPAS = 11;
    private int textura;

    public void iniciar() {
        textura = glGenTextures();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D_ARRAY, textura);
        ByteBuffer datos = BufferUtils.createByteBuffer(TAMANO * TAMANO * CAPAS * 4);
        for (int capa = 0; capa < CAPAS; capa++) {
            for (int y = 0; y < TAMANO; y++) {
                for (int x = 0; x < TAMANO; x++) {
                    float grano = ruido(x, y, capa);
                    float valor = 1;
                    switch (capa) {
                        case ASFALTO -> valor = 0.76f + grano * 0.30f;
                        case ACERA -> {
                            boolean junta = x % 128 < 3 || y % 128 < 3;
                            valor = junta ? 0.52f : 0.91f + grano * 0.12f;
                        }
                        case CESPED -> valor = 0.65f + grano * 0.40f
                                + 0.09f * (float) Math.sin(2 * Math.PI * (18.0 * x + 5.0 * y) / TAMANO);
                        case LADRILLO -> {
                            int fila = y / 32;
                            int columna = (x + (fila % 2) * 32) % 64;
                            boolean junta = y % 32 < 3 || columna < 3;
                            valor = junta ? 0.52f : 0.80f + ruido(columna / 64 + x / 64, fila, 17) * 0.18f + grano * 0.08f;
                        }
                        case ESTUCO -> valor = 0.89f + grano * 0.17f;
                        case MADERA -> valor = 0.78f + 0.14f * (float) Math.sin(
                                2 * Math.PI * (x * 18.0 / TAMANO + 0.22 * Math.sin(y * 2 * Math.PI / TAMANO)))
                                + grano * 0.09f;
                        case METAL -> valor = 0.92f + ruido(x, 0, 3) * 0.08f;
                        case HOJAS -> valor = 0.77f + grano * 0.27f;
                        default -> { }
                    }
                    int tono = Math.max(0, Math.min(255, Math.round(valor * 245)));
                    datos.put((byte) tono).put((byte) tono).put((byte) tono).put((byte) 255);
                }
            }
        }
        datos.flip();
        glTexImage3D(GL_TEXTURE_2D_ARRAY, 0, GL_RGBA8, TAMANO, TAMANO, CAPAS,
                0, GL_RGBA, GL_UNSIGNED_BYTE, datos);
        glGenerateMipmap(GL_TEXTURE_2D_ARRAY);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_REPEAT);
        if (GL.getCapabilities().GL_EXT_texture_filter_anisotropic) {
            glTexParameterf(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAX_ANISOTROPY_EXT,
                    Math.min(8, glGetFloat(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT)));
        }
    }

    private static float ruido(int x, int y, int semilla) {
        int n = x * 374761393 + y * 668265263 + semilla * 1442695041;
        n = (n ^ (n >>> 13)) * 1274126177;
        return ((n ^ (n >>> 16)) & 0xffff) / 65535f;
    }

    public void activar() {
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D_ARRAY, textura);
    }

    public void cerrar() {
        if (textura != 0) glDeleteTextures(textura);
        textura = 0;
    }

    /** UV por cara en unidades del objeto: el patrón no se estira con el tamaño de la caja. */
    public static String codigoGLSL() {
        return """
            uniform sampler2DArray uTexturas;
            uniform int uMaterial;
            in vec3 vLocal;
            in vec3 vNormalLocal;
            vec3 colorMaterial(vec3 base) {
                if (uMaterial == 0 || uMaterial == 8 || uMaterial == 9) return base;
                vec3 n = abs(vNormalLocal);
                vec2 uv = n.y > 0.5 ? vLocal.xz : (n.x > 0.5 ? vLocal.zy : vLocal.xy);
                float escala = 0.5;
                if (uMaterial == 1 || uMaterial == 3 || uMaterial == 10) escala = 1.2;
                if (uMaterial == 4) escala = 0.5;
                if (uMaterial == 6) escala = 0.8;
                return base * texture(uTexturas, vec3(uv * escala, float(uMaterial))).rgb;
            }
            """;
    }
}
