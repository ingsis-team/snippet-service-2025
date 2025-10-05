#!/bin/bash

# setup-hooks.sh - Script para configurar automáticamente los hooks de git

echo "Configurando hooks de git para el proyecto..."

# Verificar que estamos en un repositorio git
if [ ! -d ".git" ]; then
    echo "Error: No se encontró un repositorio git en el directorio actual"
    exit 1
fi

# Verificar que gradlew existe
if [ ! -f "./gradlew" ]; then
    echo "Error: No se encontró gradlew en el directorio actual"
    exit 1
fi

# Dar permisos de ejecución a gradlew si no los tiene
chmod +x ./gradlew

echo "Instalando hook de pre-commit personalizado para ktlint..."

# Crear el hook personalizado profesional (sin emojis)
cat > .git/hooks/pre-commit << 'EOF'
#!/bin/bash
#
# Hook de pre-commit personalizado para ktlint con auto-format
# Este hook verifica el formato del código y lo corrige automáticamente si es necesario
#

echo "Ejecutando verificación de formato de código con ktlint..."

# Ejecutar ktlintCheck
./gradlew ktlintCheck --no-daemon

# Capturar el código de salida
KTLINT_CHECK_EXIT_CODE=$?

if [ $KTLINT_CHECK_EXIT_CODE -eq 0 ]; then
    echo "Formato de código correcto. Continuando con el commit..."
    exit 0
else
    echo "Se encontraron problemas de formato en el código."
    echo "Ejecutando auto-formateo con ktlintFormat..."

    # Ejecutar ktlintFormat para corregir automáticamente
    ./gradlew ktlintFormat --no-daemon

    # Capturar el código de salida del formateo
    KTLINT_FORMAT_EXIT_CODE=$?

    if [ $KTLINT_FORMAT_EXIT_CODE -eq 0 ]; then
        echo "Código formateado correctamente."
        echo ""
        echo "IMPORTANTE: Los archivos han sido modificados para corregir el formato."
        echo "Por favor, revisa los cambios y añade los archivos corregidos al commit:"
        echo "git add ."
        echo "git commit"
        echo ""
        echo "Commit abortado para permitir revisión de cambios."
        exit 1
    else
        echo "Error al formatear el código. Por favor, revisa manualmente los problemas."
        exit 1
    fi
fi
######## KTLINT-GRADLE HOOK START ########

CHANGED_FILES="$(git --no-pager diff --name-status --no-color --cached | awk '$1 != "D" && $NF ~ /\.kts?$/ { print $NF }')"

if [ -z "$CHANGED_FILES" ]; then
    echo "No hay archivos Kotlin en el área de preparación."
    exit 0
fi;

echo "Ejecutando ktlint en estos archivos:"
echo "$CHANGED_FILES"

diff=.git/unstaged-ktlint-git-hook.diff
git diff --color=never > $diff
if [ -s $diff ]; then
  git apply -R $diff
fi

./gradlew --quiet ktlintFormat -PinternalKtlintGitFilter="$CHANGED_FILES"
gradle_command_exit_code=$?

echo "Ejecutada la verificación de ktlint."

echo "$CHANGED_FILES" | while read -r file; do
    if [ -f $file ]; then
        git add $file
    fi
done


if [ -s $diff ]; then
  git apply --ignore-whitespace $diff
fi
rm $diff
unset diff

echo "Hook de ktlint completado."
exit $gradle_command_exit_code
######## KTLINT-GRADLE HOOK END ########
EOF

# Dar permisos de ejecución al hook
chmod +x .git/hooks/pre-commit

if [ $? -eq 0 ]; then
    echo "Hook de pre-commit personalizado instalado exitosamente"
    echo "Configuración completa!"
    echo ""
    echo "Funcionalidades del hook:"
    echo "  • Verifica el formato del código antes de cada commit"
    echo "  • Si encuentra problemas, ejecuta automáticamente ktlintFormat"
    echo "  • Aplica formateo inteligente solo a archivos modificados"
    echo "  • Te permite revisar los cambios antes de hacer el commit final"
    echo ""
    echo "Uso:"
    echo "  • Commit normal: el hook se ejecuta automáticamente"
    echo "  • Saltar hook: git commit --no-verify"
    echo ""
    echo "El proyecto está listo para desarrollo con formato automático!"
else
    echo "Error al instalar el hook de pre-commit"
    exit 1
fi
