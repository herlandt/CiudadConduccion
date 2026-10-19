# Ciudad y conducción con OpenGL: cuatro clases

Proyecto de enseñanza en Java 17, Maven, LWJGL 3.3.3 y OpenGL 3.3 Core. La ciudad, los vehículos y las texturas se generan con código: no hacen falta imágenes ni modelos externos. Las cuatro clases acumulan los avances hasta un juego de conducción y cinco entregas.

## Ejecutar en Windows

Desde PowerShell, en la carpeta del proyecto:

```powershell
.\run_clase4.bat
```

El lanzador compila los cambios y abre el juego completo. También puedes abrir el menú y elegir `4`:

```powershell
.\run.bat
```

Para ejecutar una etapa concreta, usa `run_clase1.bat`, `run_clase2.bat`, `run_clase3.bat` o `run_clase4.bat`. Para compilar sin abrir una ventana:

```powershell
.\compile.bat
```

Necesitas un JDK 17 o superior y un controlador compatible con OpenGL 3.3. Comprueba Java con `java -version`. El lanzador `mvnw.cmd` usa `apache-maven-3.9.6` si está en esta carpeta; si no, busca Maven instalado en `PATH`. Es un lanzador local, no un Maven Wrapper que descargue Maven automáticamente. La primera compilación puede necesitar Internet para obtener dependencias.

La alternativa equivalente, conservando las comillas de la propiedad en PowerShell, es:

```powershell
.\mvnw.cmd compile exec:exec "-DmainClass=com.graphics.clase4"
```

Los archivos por lotes resuelven sus rutas desde su propia ubicación y devuelven el código de salida de Maven. Así funcionan también si se invocan desde otra carpeta y muestran los errores de compilación antes de intentar abrir el juego.

## Otras plataformas

Con Maven instalado:

```sh
mvn compile exec:exec -DmainClass=com.graphics.clase1
mvn compile exec:exec -DmainClass=com.graphics.clase2
mvn compile exec:exec -DmainClass=com.graphics.clase3
mvn compile exec:exec -DmainClass=com.graphics.clase4
```

Ejecuta un comando por vez. `mvn compile exec:exec` abre clase4 por defecto. En macOS, el perfil Maven agrega `-XstartOnFirstThread`; al ejecutar desde un IDE también debe incluirse en las opciones de la JVM. Usa `exec:exec` para conservar el lanzamiento en un proceso independiente.

El POM incluye bibliotecas nativas para macOS Intel/Apple Silicon, Windows x64 y Linux x64/ARM64. El juego requiere una sesión gráfica; no está preparado para un servidor sin pantalla.

## Alcance actual

El mapa tiene 11 × 11 celdas de 10 unidades: 110 × 110 unidades en total, 18 edificios y 7 parques. En clase4 hay 23 luces de entorno: 16 farolas de calle y 7 luminarias de parque, además de los faros del jugador. Incluye tres vehículos de tráfico, semáforos que regulan su circulación y un recorrido de cinco entregas. El juego comienza de día; N permite ver la iluminación nocturna.

Las mejoras visuales incluyen materiales diferenciados mediante texturas procedurales repetibles, mipmaps para verlas a distancia, brillo según el material, sombra direccional del sol y fondo distinto para día y noche. OpenGL almacena 11 capas de material, incluidas superficies lisas: asfalto, acera, césped, ladrillo, estuco, madera, metal, cristal, pintura, hojas y liso. Fachadas, ventanas, balcones y antenas comparten el cálculo de altura del edificio. Los senderos quedan sobre el césped y los vehículos de tráfico usan el mismo modelo con ruedas y faros que el jugador.

La cámara de seguimiento comprueba los edificios y acorta su distancia cuando hay un obstáculo. El panel dentro del juego muestra velocidad, progreso, tiempo, distrito, luces, cámara y distancia en línea recta al próximo destino. El minimapa permite orientarse durante las entregas.

Se conservan los 25 pasos peatonales originales, uno al norte de cada manzana junto a la esquina, orientados para cruzar todo el ancho de la calle. La línea de detención es invisible. El tráfico circula por el carril derecho y describe curvas al girar: frena antes de la intersección en rojo o amarillo y arranca con verde. Las luces visibles y los autos consultan el mismo ciclo. El jugador comienza en su carril derecho, en (-47.5, 50), mirando al norte; su frenado sigue siendo manual. En los parques, los bancos quedan al lado del sendero, dejando libre el paso.

## Cómo se acumulan los avances

```text
clase1: ciudad, mapa, geometría, materiales y renderizador
  └── clase2 extends clase1: auto, conducción, colisiones y cámara
        └── clase3 extends clase2: iluminación y sombra solar
              └── clase4 extends clase3: decoración, entregas, tráfico, minimapa y HUD
```

Cada clase tiene su propio `main`. Las etapas posteriores necesitan las anteriores: `extends` reutiliza el estado y los métodos, y `super.escena()` dibuja lo heredado. Las clases auxiliares `Materiales` y `Hud` organizan las texturas y la interfaz; `Semaforos` comparte el ciclo de luces y `Trafico` mantiene la circulación por carriles sin depender de OpenGL. Se conservan los nombres en minúscula de las cuatro etapas del ejercicio; en otros proyectos Java es habitual escribir `Clase1`.

La geometría sigue basada en cajas transformadas con matrices, VAO/VBO y shaders GLSL. En `caja()` y `pieza()`, los argumentos describen posición X/Y/Z, tamaño X/Y/Z y color rojo/verde/azul; `cajaGirada()` agrega el ángulo. Los materiales y las pasadas de sombra amplían este renderizador sin incorporar un motor externo.

## Guion de enseñanza

### Clase 1: crear la ciudad

Resultado: ciudad con calles, marcas viales, edificios, aceras y parques. Las flechas izquierda/derecha orbitan la cámara.

1. Leer `MAPA`: 0 es calle, 1 edificio y 2 parque; filas corresponden a Z, columnas a X, y Y es altura.
2. Revisar `centro()`: convierte índices en coordenadas del mundo. Los límites están en -55 y +55.
3. Seguir la creación de geometría, VAO/VBO, transformaciones y proyección.
4. Comparar las texturas de asfalto, aceras y fachadas; observar repetición, escala y mipmaps.
5. Cambiar una manzana o su altura y comprobar que el cálculo compartido conserva la alineación de los detalles.

### Clase 2: crear y mover el auto

Resultado: vehículo con carrocería, cabina, ruedas y faros, conducción con aceleración/frenado y cámara de seguimiento.

1. Relacionar el frente local -Z con el seno/coseno usados para trasladar y girar.
2. Estudiar el movimiento en función de `deltaTime`, la resistencia y el giro al retroceder.
3. Revisar `puedeCircular()`: círculo contra rectángulo para manzanas y margen en los bordes.
4. Comparar la vista aérea con el seguimiento y probar la cámara cerca de una fachada.
5. Seguir la composición reutilizable del vehículo: posición y color cambian entre jugador y tráfico.

El choque del jugador detiene el auto; no hay rebotes. El paso temporal se limita para reducir saltos tras una pausa larga.

### Clase 3: iluminación y materiales

Resultado: cambio día/noche con N, farolas, faros con F, respuesta especular de materiales y sombra solar.

1. Distinguir color de textura, luz ambiental, componente difusa y brillo.
2. Relacionar las posiciones de luces con las farolas visibles.
3. Estudiar atenuación por distancia y el cono de los faros.
4. Comparar emisión de una bombilla con luz recibida por otras superficies.
5. Seguir la pasada de profundidad del sol y su consulta al sombrear la escena.

La sombra solar da profundidad durante el día. Las luces puntuales y los faros no tienen mapas de sombras propios, por lo que no resuelven la oclusión de cada luz local.

### Clase 4: juego, tráfico y presentación

Resultado: ciudad decorada, minimapa, tráfico y un recorrido de cinco entregas con información superpuesta.

1. Revisar la altura compartida de fachadas y detalles, y la variación de ventanas entre día y noche.
2. Leer `DESTINOS`: una entrega se completa al acercarse a menos de 3,5 unidades y bajar de 1,2 unidades/segundo.
3. Seguir en `Trafico` la navegación por el carril derecho, los giros curvos y el frenado; comparar el ciclo de `Semaforos` con las luces dibujadas.
4. Comparar la proyección principal y la ortográfica del minimapa; estudiar `glScissor` y restauración del viewport.
5. Revisar el HUD y alternar la ayuda de controles con H.
6. Completar las cinco entregas y reiniciar con R.

El minimapa mantiene el norte (-Z) arriba. El vehículo del jugador aparece en cian/blanco y el destino activo se marca en dorado; también hay una baliza en el mundo.

## Controles

| Tecla | Acción | Desde |
|---|---|---|
| Izquierda / derecha | Orbitar la ciudad | Solo clase1 |
| W / S o arriba / abajo | Acelerar / frenar y retroceder | Clase2 |
| A / D o izquierda / derecha | Girar mientras el auto se mueve | Clase2 |
| Espacio | Freno | Clase2 |
| C | Seguimiento / vista aérea oblicua | Clase2 |
| R | Reiniciar auto y, en clase4, entregas y tráfico | Clase2 |
| N | Día / noche | Clase3 |
| F | Encender / apagar faros | Clase3 |
| M | Mostrar / ocultar minimapa | Clase4 |
| H | Mostrar / ocultar ayuda de controles | Clase4 |
| ESC | Salir | Todas |

La velocidad en km/h supone que una unidad del mundo equivale a un metro. Su valor máximo efectivo depende de la resistencia, además del límite de velocidad.

## Limitaciones

El frenado automático ante semáforos solo se aplica al tráfico: el jugador decide cuándo frenar. Un auto que ya pasó la línea con verde termina de despejar el cruce aunque cambie la luz. No hay colisiones entre vehículos, cesión de paso entre autos que giran ni evitación del jugador. Las luces locales no proyectan sombras. No se incluyen peatones, audio, físicas realistas ni modelos importados. Estas limitaciones forman parte del alcance didáctico y no implican una calificación garantizada del enunciado.

## Verificación

```powershell
.\mvnw.cmd test
```

Las pruebas de lógica se ejecutan sin abrir una ventana. Para una comprobación gráfica breve existe la propiedad JVM `demo.frames`: un valor positivo cierra la ventana tras ese número de cuadros. Debe llegar a la JVM del juego, no únicamente al proceso de Maven.

Recorrido manual recomendado: abrir las cuatro etapas; conducir, frenar, retroceder y chocar con una acera; probar la cámara junto a edificios; alternar N/F/C/M/H; redimensionar la ventana; revisar sombras y texturas de cerca y lejos; observar un ciclo completo de semáforo, los carriles y los giros del tráfico; comprobar que los pasos peatonales cruzan la calle y los bancos dejan libre el sendero; completar las cinco entregas y reiniciar. Una prueba breve de arranque no sustituye este recorrido.

La versión objetivo es JDK 17. Versiones posteriores de Java pueden mostrar advertencias de acceso nativo de LWJGL 3.3.3; un error de compilación o de OpenGL debe revisarse por separado.

Consulta [DOCUMENTACION.md](DOCUMENTACION.md) para el diseño y [MODIFICACIONES.md](MODIFICACIONES.md) para el registro de mejoras.
