# Mazmorra del Umbral — proyecto fuente (Fase 1)

Este proyecto reconstruye la estructura de desarrollo a partir de `mazmorra_umbral_fase1.jar`.
El JAR original no incluye clases compiladas `.class`; por eso Forge no puede cargarlo correctamente.

## Requisitos
- JDK 17 (no solo JRE)
- Gradle compatible con ForgeGradle 6 (recomendado ejecutar con Gradle 8.1+)
- Conexión a Internet la primera vez para descargar ForgeGradle y las dependencias
- Minecraft Forge 1.20.1, rama 47.4.x

## Compilar
Desde esta carpeta, ejecuta:

```bash
gradle build
```

El resultado esperado estará en `build/libs/mazmorra_umbral_fase1-1.0-fase1.jar`.
Copia ese JAR a la carpeta `mods` de una instancia de Minecraft Forge 1.20.1.

## Importante
- Haz copia de seguridad del mundo antes de probar el mod.
- Prueba primero en una instancia separada, sin tu mundo principal.
- Este paquete de fuentes aún necesita compilarse y probarse en un entorno Forge real. No se afirma que el JAR final esté ya validado.
