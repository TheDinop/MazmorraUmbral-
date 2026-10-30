# Mazmorra del Umbral — Estado del proyecto (Fase 2)

Fecha: 2026-10-30. Objetivo: Minecraft 1.20.1 · Forge 47.2.19 (dato del usuario, **sin verificar contra el modpack**) · Java 17.

## Resumen honesto

- **Compilado aquí: NO.** Este entorno no llega al Maven de Forge (403) y tiene Java 21. El usuario compiló la fase 1 en su PC; esta fase no ha sido compilada por nadie todavía.
- **Probado en Minecraft: NO.** Nada de la fase 2 se ha ejecutado.
- **Comprobación realizada:** `javac` sin classpath de Forge sobre todo el código: no hay errores de sintaxis. Los 100 errores que quedan son símbolos de Minecraft/Forge que faltan (esperados). Eso no demuestra que el código funcione: los tipos genéricos y las firmas de API de Forge 47 no están verificados.
- **Recursos:** todos los JSON validan; texturas y lang generados por script.

## Lo que había de una pasada anterior (no revisado a fondo)

Al retomar encontré archivos que no escribí en esta conversación: criaturas (`entity/*`), `util/*`, `item/*` de crates y tomos, `ModEntities`, `ModTabs`, `UmbralConfig` ampliada, `UmbralSavedData` ampliada. Los he tratado como base y los he integrado. Hay que revisarlos: no sé con certeza que compilen ni que cumplan la especificación.

## Fase 2 — escrito en esta pasada

| Área | Archivos | Estado |
|---|---|---|
| Disposición del Umbral (franjas por piso, Z) | `world/UmbralLayout` | Escrito, no probado |
| Construcción de pisos 1–8 por código (arena con divisoria, pilares, cofres, sello) | `world/FloorBuilder` | Escrito, no probado |
| Generador de oleadas persistente (estado, expedición, marca en cada mob) | `blockentity/WaveSpawnerBlockEntity` | Escrito, no probado |
| Variantes de XP / élite (vida, daño, ×multiplicador de XP solo a mobs marcados) | `encounter/Variante`, `encounter/FloorSpec` | Escrito, no probado |
| Ascenso entre pisos (clic en el bloque tras limpiar) | `block/FloorExitBlock`, `UmbralTeleporter.subirDePiso` | Escrito, no probado |
| Bóveda: una vez por jugador y expedición, consume Llave, estado antes que premio | `block/VaultBlock` | Escrito, no probado |
| Recompensa del jefe por jugador (XP real, Llave, Corazón Dorado con probabilidad provisional) | `WaveSpawnerBlockEntity.recompensarJefe` | Escrito, no probado |
| Umbral del Vacío: colapso del portal de origen, el marco permanece | `UmbralTeleporter.colapsarPortal` | Escrito, no probado |
| Reinicio de expedición solo con la dimensión vacía y sin progreso activo | `event/UmbralEvents` | Escrito, no probado |
| Crates: 5 tiradas, categoría por matriz, objeto por tabla | `item/CrateRarity`, `item/CrateItem` | Escrito, no probado |
| Crate de XP, Tomo de Experiencia (500), Tomo de Almacenamiento (10.000, transacción en el mismo tick) | `item/XpCrateItem`, `XpTomeItem`, `XpStorageTomeItem` | Escrito, no probado |
| Loot tables: 7 pisos de oleadas, cofres por piso, 5 crates | `data/mazmorra_umbral/loot_tables/*` | Escrito; tablas con pesos propuestos |
| Receta de la Zanahoria Dorada del Umbral (sin fabricar el Corazón por otra vía) | `recipes/zanahoria_dorada_umbral.json` | Escrito |
| Renderers (modelo vanilla de zombi/fantasma con textura propia) | `client/*` | Escrito, no probado |
| Texturas, modelos, lang ES/EN de todo lo nuevo | `assets/*` | Generado por script; placeholders |

## Decisiones y límites

1. **Un solo mundo, una dimensión compartida.** Los pisos están en franjas Z de una misma dimensión. El progreso se guarda por jugador dentro de la expedición del mundo. Si la especificación quiere instancias por grupo, es decisión de arquitectura pendiente.
2. **Reinicio de expedición:** solo cuando hay una bóveda reclamada, la dimensión está vacía y ningún jugador conectado tiene una expedición a medias. Un jugador desconectado a mitad de expedición **no** bloquea el reinicio: es una limitación conocida y puede hacer que pierda progreso.
3. **Oleadas:** los mobs se marcan con su generador y la expedición (NBT). Sin esa marca no cuentan ni dan premio. La oleada se marca como superada antes de entregar el botín (un fallo pierde premio, nunca lo duplica). Los mobs de una oleada sin sitio para spawnear no se dan por lanzados y se reintenta.
4. **Dificultad de los pisos 4–7 (criaturas personalizadas):** las composiciones de `FloorSpec` son propuestas mías. El jefe tiene una sola fase de datos de vida (`JEFE_VIDA_BASE` × escalado por jugadores); las tres fases de ataque están en `CaballeroNetheriteVacio`, escritas antes y **no revisadas en esta pasada**.
5. **Recompensa de la bóveda** (`VaultBlock`): contenido propuesto por mí. La especificación solo dice "crates, llaves y recompensas finales según progreso".
6. **Corazón Dorado:** la especificación no define cantidad ni probabilidad. `corazonDoradoProbabilidad` está a 1.0 como valor provisional de prueba: cámbialo en la config.
7. **Fantasma / Caballero / etc.:** su comportamiento de habilidades está en los archivos heredados; no los he probado ni revisado en profundidad.
8. **Dependencias de otros mods:** no hay integración con Apotheosis, Ars Nouveau, etc. Las tablas de loot solo usan objetos vanilla y de este mod. La especificación exige integraciones opcionales tras auditar el modpack: **pendiente**.
9. **Auditoría del modpack:** sigue sin hacerse. No tengo la carpeta `mods` ni el log. Los ids vanilla usados son reales en 1.20.1, pero no se han verificado con el modpack.
10. **Ids de objetos inexistentes:** no hay. Todos los `minecraft:*` son de 1.20.1 y los del mod están registrados en `ModItems`. Revisar antes de compilar.

## Pendiente

- Compilar y arreglar errores reales de API (esperables con Forge 47.2.19).
- Pruebas en juego: piso 1 completo, ascenso, bóveda, reinicio, muerte, cierre y reapertura, multijugador.
- Auditoría del modpack e integraciones opcionales (Apotheosis, L_Ender's Cataclysm, Simply Swords, Ars Nouveau, etc.).
- Pilares de equipo fabricable y encantamientos (sección 12–13 de la especificación): no implementados.
- Misiones (sección 15) y llaves adicionales (sección 16): no implementadas.
- Presagio del Umbral (sección 14): no implementado.
- Loot de oleadas y cofres: los pesos son propuestas; falta el balance con la progresión real.
- Corazón Dorado: cantidad y probabilidad definitivas.
- Filo del Vacío Umbral: habilidad pendiente de aprobación según la especificación.
- Contradicciones del PDF: las probabilidades de los objetos exclusivos de crates no cubren toda la tabla; hay que definir el resto.
