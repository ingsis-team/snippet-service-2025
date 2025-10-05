# Configuración de Hooks Git

## Instalación Automática

Para configurar automáticamente los hooks de git en tu copia local del repositorio, ejecuta:

```bash
./setup-hooks.sh
```

## ¿Qué hace este script?

- Verifica que estés en un repositorio git válido
- Instala el hook de pre-commit que ejecuta `ktlintFormat` automáticamente
- Configura el formateo de código antes de cada commit

## Para nuevos desarrolladores

Cuando clones el repositorio por primera vez:

```bash
git clone <url-del-repo>
cd snippet-service-2025
./setup-hooks.sh
```

## Bypass del hook (emergencias)

Si necesitas hacer un commit sin ejecutar el formateo:

```bash
git commit --no-verify -m "mensaje del commit"
```

¡El formateo automático asegura consistencia en el código del equipo!
