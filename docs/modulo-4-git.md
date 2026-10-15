# Módulo 4 – Incorporar un fix de seguridad de `main` en `feature/new-login`

## Respuesta corta

```bash
git cherry-pick -x <sha-del-fix>
```

**`git cherry-pick`** aplica **únicamente** el cambio introducido por ese commit sobre la rama actual, sin traer el
resto de la historia de `main`. Un `git merge main` o un `git rebase main` traerían *todas* las actualizaciones de
`main`, que es justo lo que no se quiere.

`-x` agrega al mensaje la línea `(cherry picked from commit <sha>)`: queda trazabilidad de dónde salió el cambio,
algo importante en un fix de seguridad (auditoría, notas de versión, verificar luego que la feature lo tiene).

## Paso a paso

```bash
# 1. Traer el estado actual del remoto
git fetch origin

# 2. Identificar el commit exacto del fix
git log origin/main --oneline -n 20
git log origin/main --oneline --grep="security"      # o por archivo: git log origin/main -- src/auth/
git show <sha>                                        # revisar qué cambia antes de aplicarlo

# 3. Ir a la rama de trabajo, con el árbol limpio
git switch feature/new-login
git status                                            # si hay cambios sin commit: git stash

# 4. Aplicar solo ese commit
git cherry-pick -x <sha>

# 5. Si hay conflicto: resolver, marcar y continuar (o abortar)
git status
git add <archivos-resueltos>
git cherry-pick --continue                            # git cherry-pick --abort para deshacer

# 6. Verificar y publicar
./mvnw verify                                         # o la suite de tests del proyecto
git log --oneline -n 3
git push origin feature/new-login
```

## Casos especiales

| Situación | Comando |
|---|---|
| El fix entró a `main` como **merge commit** de un PR | `git cherry-pick -x -m 1 <sha-del-merge>` (`-m 1` toma como base el primer padre, `main`) |
| El fix son **varios commits consecutivos** | `git cherry-pick -x A^..B` |
| Quiero revisar antes de commitear | `git cherry-pick --no-commit <sha>` y luego `git commit` |
| El PR se integró con *squash* | Es un solo commit normal: `git cherry-pick -x <sha>` |
| Ya no recuerdo si lo apliqué | `git log --cherry-pick --right-only --oneline feature/new-login...origin/main` lista lo que aún falta |

## ¿Qué pasa cuando la feature se integre a `main`?

El commit cherry-picked tiene **otro SHA** pero el **mismo patch**. Al hacer merge o rebase de la feature sobre
`main`, Git detecta que el cambio ya está (mismo *patch-id*) y no genera conflicto ni duplica el código. Se puede
comprobar con:

```bash
git log --cherry-pick --oneline main...feature/new-login
```

## Alternativas descartadas

| Alternativa | Por qué no |
|---|---|
| `git merge main` en la feature | Trae **todo** lo nuevo de `main`, no solo el fix |
| `git rebase main` | Igual que el merge: reescribe la base con todos los cambios de `main` |
| Copiar el cambio a mano | Propenso a errores, pierde trazabilidad y no se reconoce como el mismo patch |
| `git checkout main -- archivo` | Trae el archivo entero en su versión de `main`, que puede incluir otros cambios |

## Buenas prácticas de proceso

- Los fixes de seguridad van en **commits atómicos** (un commit = un cambio) y, idealmente, en una rama
  `hotfix/*` desde `main`: así son fáciles de cherry-pickear a releases y features.
- Con varias features activas, se avisa al equipo y cada dueño hace el cherry-pick en su rama.
- Si la feature está compartida, se prefiere cherry-pick (agrega un commit) frente a reescribir historia.

## Demostración reproducible

[`scripts/demo-cherry-pick.sh`](../scripts/demo-cherry-pick.sh) crea un repositorio temporal con `main` y
`feature/new-login`, simula el fix de seguridad junto con otros cambios en `main`, aplica el cherry-pick y
verifica que la feature recibió el fix y nada más.

```bash
bash scripts/demo-cherry-pick.sh
```
