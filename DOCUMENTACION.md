# Documentación: Ciudad Interactiva 3D con OpenGL

Asignatura: Programación Gráfica. Autor: Isael Ortiz.  
Tecnologías: Java 17, LWJGL 3.3.3, GLFW, OpenGL 3.3 Core y shaders GLSL.

## Implementación y alcance

Este documento describe las funciones del proyecto y sus límites. No asigna una nota ni garantiza una bonificación: la evaluación corresponde al docente y al enunciado.

| Área | Implementación |
|---|---|
| Ciudad | Matriz 11 × 11, celdas de 10 unidades, 18 edificios, 7 parques y calles interconectadas |
| Conducción | Aceleración, frenado, reversa, giro, colisiones del jugador con manzanas y bordes |
| Cámara | Seguimiento con comprobación de edificios y vista aérea oblicua |
| Iluminación | Inicio de día, alternancia día/noche, sol y faros del jugador; clase4 incluye 23 luces de entorno (16 de calle + 7 de parque) |
| Materiales | Array de texturas OpenGL con 11 capas, incluidas superficies lisas, mipmaps y respuesta de brillo diferenciada |
| Sombras | Sombra direccional del sol; sin sombras individuales para luces locales |
| Decoración | Ventanas, balcones, antenas, puertas, árboles, bancos, senderos, pasos peatonales y semáforos |
| Partida | Cinco entregas consecutivas, destino señalado, cronómetro y reinicio |
| Minimapa | Vista superior, jugador, destino y tráfico, activable con M |
| Interfaz | HUD con velocidad, entregas, tiempo, distrito, luces, cámara, distancia en línea recta y ayuda activable con H |
| Tráfico | Tres vehículos con modelo completo, carril derecho, giros curvos y frenado ante semáforos |

## Diseño de la ciudad

Las 121 celdas ocupan 110 × 110 unidades. El mundo se extiende desde -55 hasta +55 en X y Z. Las filas y columnas pares forman las calles; las posiciones impares contienen edificios o parques.

```text
C C C C C C C C C C C
C E C E C P C E C E C
C C C C C C C C C C C
C E C P C E C P C E C
C C C C C C C C C C C
C P C E C E C E C P C
C C C C C C C C C C C
C E C E C P C E C E C
C C C C C C C C C C C
C E C P C E C E C E C
C C C C C C C C C C C

C = calle; E = edificio; P = parque.
Filas: eje Z, de norte (-Z) a sur (+Z).
Columnas: eje X, de oeste (-X) a este (+X).
```

El jugador comienza en (-47.5, 50), en el carril derecho y orientado al norte (-Z). El radio del auto reserva un margen al comprobar el borde y las manzanas. Una función compartida determina la altura de cada edificio y de su decoración: ventanas, balcones, cornisas y antenas ya no calculan alturas incompatibles.

Los centros de los carriles están separados 2,5 unidades del eje de cada calle. Se conserva la cantidad original de 25 pasos peatonales: uno al norte de cada manzana, junto a la esquina, orientado transversalmente y abarcando ambos carriles. La línea de detención no se dibuja; el tráfico conserva su punto de frenado. El tráfico sigue arcos al girar para enlazar con el carril derecho de salida.

El ciclo de semáforos dura 24 segundos. Cada eje tiene 8 segundos de verde y 2 de amarillo, seguidos por 2 segundos con todos los sentidos en rojo antes de dar paso al eje perpendicular. Las luces visibles y la simulación comparten ese reloj. El tráfico frena en rojo o amarillo antes de la intersección y vuelve a avanzar con verde; si ya rebasó el límite de detención en verde, termina de despejar el cruce. Los semáforos no frenan el auto del jugador automáticamente.

## Materiales y presentación

Las texturas se generan de forma reproducible en código y se cargan en un array de OpenGL de 11 capas: liso, asfalto, acera, césped, ladrillo, estuco, madera, metal, cristal, pintura y hojas. Algunas capas representan superficies lisas; no son once imágenes con patrones distintos. Se repiten a escala del mundo para evitar estirarlas al cambiar el tamaño de una caja. Los mipmaps reducen el ruido a distancia. No se requieren imágenes descargadas ni un nuevo paquete de dependencias.

Asfalto, pavimento, fachadas, césped y madera tienen patrones diferenciados. Cristal y metal reciben brillo diferente al de materiales mates. La iluminación combina ambiente, luz direccional y luces locales; las superficies emisivas, como ciertas ventanas nocturnas, se distinguen de las superficies que reciben luz. El cielo cambia entre día y noche.

La sombra del sol utiliza una pasada de profundidad y se aplica a la vista principal. Las farolas y los faros no disponen de sombras individuales: pueden iluminar al otro lado de una pared. El modelo es didáctico y no equivale a un sistema de iluminación físicamente exacto.

Los senderos se elevan sobre el césped para que no queden enterrados y conectan los extremos del parque. Los bancos se sitúan al lado del camino y miran hacia él, sin ocupar su recorrido. La disposición de ventanas evita el zócalo, y las puertas se separan de la superficie de la fachada para reducir superposiciones. La vegetación y las ventanas varían sin necesitar objetos externos.

## Partida e interfaz

| Entrega | Coordenadas X, Z | Zona |
|---|---|---|
| 1 | 50, -50 | Esquina noreste |
| 2 | -50, -10 | Sector oeste |
| 3 | 30, 30 | Sector sureste |
| 4 | -10, -50 | Avenida norte |
| 5 | -50, 50 | Regreso a la esquina suroeste, junto al inicio |

Para entregar hay que acercarse a menos de 3,5 unidades del destino y reducir la rapidez por debajo de 1,2 unidades/segundo. El HUD comunica progreso, distancia y tiempo. Al completar las cinco paradas se muestra el resultado; R reinicia el auto, el progreso y el tráfico.

Los distritos se calculan a partir de la posición: Norte, Sur, Residencial Oeste, Comercial Este y Centro Urbano. El minimapa usa proyección ortográfica, conserva el norte arriba y distingue jugador, tráfico y destino. El título de ventana complementa la interfaz; ya no es el único lugar donde consultar el estado.

## Controles y ejecución

| Control | Función |
|---|---|
| W / arriba | Acelerar |
| S / abajo | Frenar y retroceder |
| A / D o izquierda / derecha | Girar |
| Espacio | Frenar |
| N | Alternar día/noche |
| F | Encender/apagar faros |
| C | Alternar cámara |
| M | Mostrar/ocultar minimapa |
| H | Mostrar/ocultar ayuda |
| R | Reiniciar |
| ESC | Salir |

Ejecutar directamente el juego completo:

```powershell
.\run_clase4.bat
```

Menú de selección de las cuatro etapas:

```powershell
.\run.bat
```

Compilación y ejecución con el lanzador de Maven local:

```powershell
.\mvnw.cmd compile exec:exec "-DmainClass=com.graphics.clase4"
```

Los lanzadores Windows vuelven a compilar antes de ejecutar, usan la carpeta del proyecto aunque se invoquen desde otro directorio y conservan el código de salida. `mvnw.cmd` usa la distribución local de Maven o una instalación disponible en PATH; no descarga Maven por su cuenta. Se requiere JDK 17 o posterior y OpenGL 3.3.

## Arquitectura didáctica

- `clase1.java`: mapa, ciclo GLFW/OpenGL, geometría, transformaciones, materiales, cámara orbital y proyección.
- `clase2.java`: física de conducción, colisiones con manzanas/bordes, seguimiento de cámara y modelo reutilizable del vehículo.
- `clase3.java`: iluminación ambiental/direccional/local, día/noche, faros y sombra solar.
- `clase4.java`: decoración, ventanas, semáforos, entregas, distritos, tráfico, minimapa y HUD.
- `Materiales.java`: generación y gestión de las once capas de material de OpenGL.
- `Hud.java`: dibujo del panel de estado y la ayuda dentro de la ventana.
- `Semaforos.java`: dimensiones viales compartidas y ciclo común de luces para dibujo y circulación.
- `Trafico.java`: posición en el carril, giros curvos, aceleración y frenado de la IA, sin depender de OpenGL.

La herencia permite ejecutar cada etapa con su propio `main` sin duplicar la ciudad. El movimiento usa tiempo transcurrido, con un límite al paso temporal para evitar saltos excesivos. El tráfico se simula con pasos de 1/120 de segundo, avanza por el carril derecho y decide su continuación en intersecciones; no intenta resolver una simulación vial completa.

## Comprobaciones y límites

`.\mvnw.cmd test` ejecuta las pruebas de lógica. La propiedad JVM `demo.frames` permite un arranque gráfico acotado para comprobar shaders y dibujo; necesita una sesión con pantalla. Las pruebas de arranque no comprueban por sí solas el aspecto final ni un recorrido completo.

Solo la IA frena automáticamente ante los semáforos; el jugador conserva el control del freno. No hay colisiones entre vehículos, cesión de paso entre autos que giran ni evitación del jugador por parte del tráfico. Tampoco hay peatones, audio ni sombras de farolas/faros. La revisión manual debe incluir día/noche, cámaras junto a fachadas, lectura del HUD al redimensionar, minimapa, carriles, giros, un ciclo completo de semáforo, pasos peatonales, senderos libres y las cinco entregas.
