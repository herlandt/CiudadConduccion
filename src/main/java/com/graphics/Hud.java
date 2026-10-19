package com.graphics;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL33.*;

/** Texto antialias de Java2D compuesto sobre OpenGL 3.3, sin bibliotecas adicionales. */
final class Hud implements AutoCloseable {
    private static final Color TINTA = new Color(238, 244, 241);
    private static final Color SECUNDARIO = new Color(161, 183, 183);
    private static final Color CIAN = new Color(106, 219, 205);
    private static final Color ORO = new Color(241, 194, 107);
    private int programa, vao, textura;
    private BufferedImage imagen;
    private ByteBuffer pixeles;
    private Estado anterior;
    private long ultimaActualizacion;

    private record Estado(int ancho, int alto, int velocidad, int entregas, int total,
                          int segundos, int distancia, String distrito, boolean noche,
                          boolean faros, boolean aerea, boolean ayuda, int mapa, int margenMapa) {}

    void dibujar(int ancho, int alto, int velocidad, int entregas, int total, int segundos,
                 int distancia, String distrito, boolean noche, boolean faros,
                 boolean aerea, boolean ayuda, int mapa, int margenMapa) {
        if (ancho <= 0 || alto <= 0) return;
        Estado actual = new Estado(ancho, alto, velocidad, entregas, total, segundos,
            distancia, distrito, noche, faros, aerea, ayuda, mapa, margenMapa);

        // El HUD es un pase independiente: no deja modificados ni el VAO ni el programa 3D.
        int programaPrevio = glGetInteger(GL_CURRENT_PROGRAM);
        int vaoPrevio = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        int unidadPrevia = glGetInteger(GL_ACTIVE_TEXTURE);
        glActiveTexture(GL_TEXTURE0);
        int texturaPrevia = glGetInteger(GL_TEXTURE_BINDING_2D);
        boolean profundidad = glIsEnabled(GL_DEPTH_TEST);
        boolean recorte = glIsEnabled(GL_SCISSOR_TEST);
        boolean caras = glIsEnabled(GL_CULL_FACE);
        boolean mezcla = glIsEnabled(GL_BLEND);
        boolean escribirProfundidad = glGetBoolean(GL_DEPTH_WRITEMASK);
        int fuenteRGB = glGetInteger(GL_BLEND_SRC_RGB), destinoRGB = glGetInteger(GL_BLEND_DST_RGB);
        int fuenteAlfa = glGetInteger(GL_BLEND_SRC_ALPHA), destinoAlfa = glGetInteger(GL_BLEND_DST_ALPHA);
        try {
            if (programa == 0) iniciar();
            glBindTexture(GL_TEXTURE_2D, textura);
            long ahora = System.nanoTime();
            boolean cambioTamano = anterior == null || anterior.ancho != ancho || anterior.alto != alto;
            boolean cambioControl = anterior == null || anterior.ayuda != ayuda || anterior.mapa != mapa
                || anterior.noche != noche || anterior.faros != faros || anterior.aerea != aerea
                || anterior.entregas != entregas;
            if (cambioTamano || cambioControl || (!actual.equals(anterior)
                    && ahora - ultimaActualizacion >= 200_000_000L)) {
                actualizarTextura(actual);
                anterior = actual;
                ultimaActualizacion = ahora;
            }
            glDisable(GL_DEPTH_TEST);
            glDepthMask(false);
            glDisable(GL_CULL_FACE);
            glDisable(GL_SCISSOR_TEST);
            glEnable(GL_BLEND);
            glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
            glUseProgram(programa);
            glBindVertexArray(vao);
            glDrawArrays(GL_TRIANGLES, 0, 6);
        } finally {
            glBindVertexArray(vaoPrevio);
            glUseProgram(programaPrevio);
            glBindTexture(GL_TEXTURE_2D, texturaPrevia);
            glActiveTexture(unidadPrevia);
            restaurar(GL_DEPTH_TEST, profundidad);
            glDepthMask(escribirProfundidad);
            restaurar(GL_SCISSOR_TEST, recorte);
            restaurar(GL_CULL_FACE, caras);
            restaurar(GL_BLEND, mezcla);
            glBlendFuncSeparate(fuenteRGB, destinoRGB, fuenteAlfa, destinoAlfa);
        }
    }

    private static void restaurar(int estado, boolean activo) {
        if (activo) glEnable(estado); else glDisable(estado);
    }

    private void iniciar() {
        int vertices = compilar(GL_VERTEX_SHADER, """
            #version 330 core
            out vec2 uv;
            const vec2 esquinas[6] = vec2[6](
                vec2(0,0), vec2(1,0), vec2(1,1),
                vec2(1,1), vec2(0,1), vec2(0,0));
            void main() {
                uv = esquinas[gl_VertexID];
                gl_Position = vec4(uv.x * 2.0 - 1.0, 1.0 - uv.y * 2.0, 0.0, 1.0);
            }
            """);
        int fragmentos = 0;
        try {
            fragmentos = compilar(GL_FRAGMENT_SHADER, """
                #version 330 core
                in vec2 uv;
                uniform sampler2D uHud;
                out vec4 color;
                void main() { color = texture(uHud, uv); }
                """);
            programa = glCreateProgram();
            glAttachShader(programa, vertices);
            glAttachShader(programa, fragmentos);
            glLinkProgram(programa);
            if (glGetProgrami(programa, GL_LINK_STATUS) == GL_FALSE) {
                throw new IllegalStateException("HUD: " + glGetProgramInfoLog(programa));
            }
            glUseProgram(programa);
            glUniform1i(glGetUniformLocation(programa, "uHud"), 0);
            vao = glGenVertexArrays(); // gl_VertexID genera el rectángulo: no necesita VBO.
            textura = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textura);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        } finally {
            glDeleteShader(vertices);
            if (fragmentos != 0) glDeleteShader(fragmentos);
        }
    }

    private static int compilar(int tipo, String codigo) {
        int shader = glCreateShader(tipo);
        glShaderSource(shader, codigo);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String mensaje = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            throw new IllegalStateException("HUD: " + mensaje);
        }
        return shader;
    }

    private void actualizarTextura(Estado estado) {
        // Limita la textura en pantallas 4K sin cambiar las posiciones relativas de los paneles.
        float resolucion = Math.min(1, Math.min(1920f / estado.ancho, 1200f / estado.alto));
        int ancho = Math.max(1, Math.round(estado.ancho * resolucion));
        int alto = Math.max(1, Math.round(estado.alto * resolucion));
        boolean nueva = imagen == null || imagen.getWidth() != ancho || imagen.getHeight() != alto;
        if (nueva) {
            imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
            pixeles = BufferUtils.createByteBuffer(ancho * alto * 4);
        }
        Graphics2D g = imagen.createGraphics();
        try {
            g.setComposite(AlphaComposite.Clear);
            g.fillRect(0, 0, ancho, alto);
            g.setComposite(AlphaComposite.SrcOver);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            float escala = Math.min(1.12f, Math.max(0.55f, Math.min(estado.ancho / 960f, estado.alto / 600f)));
            g.scale(escala * resolucion, escala * resolucion);
            dibujarPaneles(g, estado, estado.ancho / escala, estado.alto / escala, escala);
        } finally {
            g.dispose();
        }
        pixeles.clear();
        for (int pixel : ((DataBufferInt) imagen.getRaster().getDataBuffer()).getData()) {
            pixeles.put((byte) (pixel >> 16)).put((byte) (pixel >> 8))
                .put((byte) pixel).put((byte) (pixel >>> 24));
        }
        pixeles.flip();
        if (nueva) {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, ancho, alto, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixeles);
        } else {
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, ancho, alto, GL_RGBA, GL_UNSIGNED_BYTE, pixeles);
        }
    }

    private static void dibujarPaneles(Graphics2D g, Estado e, float ancho, float alto, float escala) {
        int margen = 18;
        int espacioMapa = e.mapa > 0 ? (int) ((e.mapa + e.margenMapa) / escala) + margen : 0;
        int anchoMision = Math.min(360, (int) ancho - espacioMapa - margen * 2);
        boolean compacto = e.alto < 420 || e.ancho < 600;
        int altoMision = compacto ? 136 : 170;
        panel(g, margen, margen, anchoMision, altoMision);
        texto(g, "CIUDAD / CONDUCCIÓN", margen + 18, margen + 27, 11, true, CIAN, anchoMision - 36);
        texto(g, e.distrito, margen + 18, margen + 57, 22, true, TINTA, anchoMision - 36);
        int barra = (anchoMision - 36 - (e.total - 1) * 7) / e.total;
        for (int i = 0; i < e.total; i++) {
            g.setColor(i < e.entregas ? CIAN : new Color(65, 84, 86));
            g.fillRoundRect(margen + 18 + i * (barra + 7), margen + 74, barra, 5, 5, 5);
        }
        boolean terminado = e.entregas == e.total;
        texto(g, terminado ? "¡RECORRIDO COMPLETADO!" : "ENTREGAS  " + e.entregas + " / " + e.total,
            margen + 18, margen + 104, 13, true, terminado ? CIAN : TINTA, anchoMision - 36);
        texto(g, terminado ? "Tiempo: " + reloj(e.segundos) + "  ·  R para volver a jugar"
                : "Próxima parada · " + e.distancia + " m en línea recta",
            margen + 18, margen + 127, 13, false, ORO, anchoMision - 36);
        if (!compacto) {
            texto(g, terminado ? "Las cinco entregas llegaron a destino."
                    : e.distancia <= 4 ? "Frena dentro de la marca para entregar."
                    : "Busca la marca dorada y detente allí.",
                margen + 18, margen + 151, 12, false, SECUNDARIO, anchoMision - 36);
        }

        int anchoPanel = Math.min(850, (int) ancho - margen * 2);
        boolean ayuda = e.ayuda && !compacto;
        int altoPanel = ayuda ? 134 : 84;
        int y = (int) alto - margen - altoPanel;
        panel(g, margen, y, anchoPanel, altoPanel);
        texto(g, Integer.toString(e.velocidad), margen + 18, y + 48, 40, true, TINTA, 64);
        texto(g, "km/h", margen + 85, y + 45, 13, false, SECUNDARIO, 40);
        texto(g, "VELOCIDAD", margen + 20, y + 68, 10, true, CIAN, 100);
        g.setColor(new Color(63, 87, 91));
        g.drawLine(margen + 132, y + 18, margen + 132, y + 66);
        texto(g, (e.noche ? "NOCHE" : "DÍA") + "   ·   FAROS " + (e.faros ? "ON" : "OFF"),
            margen + 150, y + 31, 13, true, e.faros ? CIAN : TINTA, anchoPanel - 170);
        texto(g, "Cámara " + (e.aerea ? "aérea" : "seguimiento") + "   ·   " + reloj(e.segundos),
            margen + 150, y + 56, 12, false, SECUNDARIO, anchoPanel - 170);
        if (anchoPanel > 610) {
            texto(g, "H  " + (ayuda ? "Ocultar ayuda" : "Mostrar ayuda"),
                margen + anchoPanel - 145, y + 31, 12, false, SECUNDARIO, 130);
        }
        if (ayuda) {
            g.setColor(new Color(51, 73, 77));
            g.drawLine(margen + 18, y + 80, margen + anchoPanel - 18, y + 80);
            texto(g, "WASD / Flechas  Conducir     Espacio  Frenar     H  Ayuda",
                margen + 18, y + 102, 12, false, TINTA, anchoPanel - 36);
            texto(g, "C  Cámara    N  Día/noche    F  Faros    M  Mapa    R  Reiniciar    Esc  Salir",
                margen + 18, y + 122, 12, false, SECUNDARIO, anchoPanel - 36);
        } else if (anchoPanel <= 610) {
            texto(g, "H: ayuda", margen + anchoPanel - 76, y + 71, 10, false, SECUNDARIO, 65);
        }

        if (e.mapa > 0) {
            int xMapa = (int) ((e.ancho - e.mapa - e.margenMapa) / escala);
            int yMapa = (int) ((e.margenMapa + e.mapa) / escala) + 7;
            int anchoMapa = (int) (e.mapa / escala);
            if (yMapa + 27 < y - 8) {
                panel(g, xMapa, yMapa, anchoMapa, 27);
                texto(g, "N ↑   ·   M: mapa   ·   ● destino",
                    xMapa + 10, yMapa + 18, 11, false, ORO, anchoMapa - 20);
            }
        }
    }

    private static void panel(Graphics2D g, int x, int y, int ancho, int alto) {
        g.setColor(new Color(8, 20, 25, 222));
        g.fillRoundRect(x, y, ancho, alto, 16, 16);
        g.setColor(new Color(150, 190, 188, 55));
        g.setStroke(new BasicStroke(1));
        g.drawRoundRect(x, y, ancho, alto, 16, 16);
    }

    private static void texto(Graphics2D g, String texto, int x, int y, int tamano,
                              boolean negrita, Color color, int anchoMaximo) {
        g.setFont(new Font(Font.SANS_SERIF, negrita ? Font.BOLD : Font.PLAIN, tamano));
        while (g.getFontMetrics().stringWidth(texto) > anchoMaximo && tamano > 10) {
            g.setFont(g.getFont().deriveFont((float) --tamano));
        }
        if (g.getFontMetrics().stringWidth(texto) > anchoMaximo) {
            while (texto.length() > 1 && g.getFontMetrics().stringWidth(texto + "…") > anchoMaximo) {
                texto = texto.substring(0, texto.length() - 1);
            }
            texto += "…";
        }
        g.setColor(color);
        g.drawString(texto, x, y);
    }

    private static String reloj(int segundos) {
        return (segundos / 60) + ":" + (segundos % 60 < 10 ? "0" : "") + (segundos % 60);
    }

    @Override
    public void close() {
        if (textura != 0) glDeleteTextures(textura);
        if (vao != 0) glDeleteVertexArrays(vao);
        if (programa != 0) glDeleteProgram(programa);
        textura = vao = programa = 0;
        imagen = null;
        pixeles = null;
        anterior = null;
    }
}
