# Compilar desde un celular Android (sin instalar Java/Gradle en el teléfono)

La forma más sencilla es usar GitHub Actions, que compila el proyecto en una máquina en la nube. Necesitas una cuenta gratuita de GitHub y conexión a Internet.

1. En el navegador del teléfono, inicia sesión en GitHub y crea un repositorio nuevo. Puedes llamarlo `MazmorraUmbral` y dejarlo privado.
2. Descomprime `MazmorraUmbral_Fuente.zip` en el teléfono.
3. Sube el CONTENIDO de la carpeta `MazmorraUmbral_Fuente` al repositorio, conservando las carpetas (`src`, `.github`, etc.). En la web de GitHub, usa **Add file → Upload files**. Asegúrate de que también se suba `.github/workflows/build.yml`; si el selector de archivos oculta carpetas que empiezan por punto, crea esa ruta desde la web o sube los archivos usando GitHub Codespaces.
4. Abre la pestaña **Actions** del repositorio.
5. Elige **Compilar Mazmorra del Umbral** y pulsa **Run workflow**. Si no aparece, abre el archivo `.github/workflows/build.yml` y confirma que se haya subido a la rama `main`.
6. Espera a que termine el trabajo. Si aparece una marca verde, abre la ejecución y baja hasta **Artifacts**.
7. Descarga `mazmorra-umbral-fase1`, descomprime el ZIP del artefacto y copia el archivo `.jar` resultante a la carpeta `mods` de tu instancia Forge 1.20.1.

## Importante
- Si la compilación falla, abre la ejecución de Actions, copia el primer error rojo de Gradle/Java y compártelo para poder corregir el código.
- Que compile no garantiza que el mod funcione correctamente en Prominence II Classic; debe probarse primero en una instancia separada y con copia de seguridad del mundo.
- El proyecto usa Forge 1.20.1 y Java 17. No pongas el ZIP del proyecto fuente directamente en `mods`.
