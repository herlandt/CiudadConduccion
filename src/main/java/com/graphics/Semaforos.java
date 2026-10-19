package com.graphics;

/** Un unico ciclo para las luces dibujadas y las decisiones del trafico. */
public final class Semaforos {
    public enum Estado { ROJO, AMARILLO, VERDE }

    public static final float CARRIL = 2.5f;
    public static final float MEDIA_INTERSECCION = 5f;
    public static final float DISTANCIA_PASO = 6.3f;
    public static final float DISTANCIA_LINEA = 8f;
    public static final float DISTANCIA_DETENCION = 9.6f;
    public static final double CICLO = 24;

    private Semaforos() { }

    /** Los sentidos opuestos comparten verde; cada cambio incluye dos segundos de todo-rojo. */
    public static Estado estado(float dirX, float dirZ, double tiempo) {
        double fase = ((tiempo % CICLO) + CICLO) % CICLO;
        boolean horizontal = Math.abs(dirX) > Math.abs(dirZ);
        if (!horizontal) fase = (fase + 12) % CICLO;
        if (fase < 8) return Estado.VERDE;
        if (fase < 10) return Estado.AMARILLO;
        return Estado.ROJO;
    }
}
