# Mazmorra del Umbral (Fase 1)

Mod de Forge para Minecraft 1.20.1 (Prominence II: Classic). Lee `ESTADO.md` antes de nada: describe qué está escrito, qué NO está compilado ni probado y qué falta.

## Requisitos para compilar

- JDK 17 (el entorno de desarrollo original solo tenía Java 21; Forge 1.20.1 requiere 17).
- Gradle 8.1.x–8.8 (recomendado el que traiga el MDK de Forge 1.20.1) o el wrapper generado desde el MDK.
- Acceso a `maven.minecraftforge.net` y `maven.parchmentmc.org` no hace falta (se usan mapeos oficiales).

## Compilar

```
gradle build          # o ./gradlew build si copias el wrapper del MDK de Forge 1.20.1
```
El `.jar` queda en `build/libs/mazmorra_umbral-0.1.0.jar`.

Para probar en un cliente de desarrollo: `gradle runClient`.

## Instalar en la copia de desarrollo del modpack

1. Haz una **copia** de la instancia (no toques la original ni tu mundo principal).
2. Copia el `.jar` a la carpeta `mods` de la copia.
3. Arranca con un mundo de prueba nuevo o una copia del mundo.

## Probar la fase 1

1. `/give @s mazmorra_umbral:piedra_del_umbral 14` y `/give @s mazmorra_umbral:fragmento_del_umbral 1`.
2. Construye un marco vertical de Piedra del Umbral con interior libre de 2x3 (o mayor).
3. Clic derecho en el marco con el Fragmento: el interior se llena de portal y se consume el fragmento (en supervivencia).
4. Cruza el portal: llegas a una plataforma en la dimensión `mazmorra_umbral:umbral`. Cruza el portal de regreso para volver al punto de origen.
5. Comprueba el bloqueo de receta: tras un encendido exitoso **en supervivencia**, la receta del Fragmento ya no debe producir resultado.

Reporta cualquier error de arranque con el `latestlog.txt` completo.
