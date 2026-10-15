#!/usr/bin/env bash
# Demostración del Módulo 4: llevar un fix de seguridad de main a feature/new-login sin traer el resto de main.
set -euo pipefail

DIR=$(mktemp -d)
trap 'rm -rf "$DIR"' EXIT
cd "$DIR" || exit 1

git init -q -b main
git config user.name "Demo"
git config user.email "demo@example.com"
git config core.autocrlf false

paso() {
  local titulo="$1"
  printf '\n\033[1;34m▶ %s\033[0m\n' "$titulo"
  return 0
}

paso "Estado inicial de main"
printf 'def login(user, password):\n    return check(user, password)\n' > auth.py
git add auth.py && git commit -qm "feat: login básico"

paso "Se crea feature/new-login y avanza"
git switch -qc feature/new-login
printf 'def login_sso(token):\n    return validate(token)\n' > sso.py
git add sso.py && git commit -qm "feat: login con SSO"

paso "Mientras tanto, en main: un cambio cualquiera, el FIX DE SEGURIDAD y otro cambio"
git switch -q main
echo "# Reportes" > reportes.py && git add reportes.py && git commit -qm "feat: módulo de reportes"
sed -i.bak 's/return check(user, password)/if not rate_limit(user):\n        raise TooManyAttempts()\n    return check(user, password)/' auth.py && rm -f auth.py.bak
git add auth.py && git commit -qm "fix(security): limitar intentos de login (fuerza bruta)"
FIX=$(git rev-parse HEAD)
echo "# Dashboard" > dashboard.py && git add dashboard.py && git commit -qm "feat: dashboard"
git log --oneline main

paso "Cherry-pick SOLO del fix sobre feature/new-login"
git switch -q feature/new-login
git cherry-pick -x "$FIX"
git log --oneline -n 3

paso "Verificaciones"
grep -q "rate_limit" auth.py && echo "✔ La feature tiene el fix de seguridad"
[[ ! -f reportes.py && ! -f dashboard.py ]] && echo "✔ No se trajeron reportes.py ni dashboard.py"
git log -1 --format=%B | grep -q "cherry picked from commit $FIX" && echo "✔ Trazabilidad: el mensaje referencia el commit original"

paso "Commits de main que aún NO están en la feature (el fix ya no aparece: mismo patch-id)"
git log --cherry-pick --right-only --oneline feature/new-login...main
