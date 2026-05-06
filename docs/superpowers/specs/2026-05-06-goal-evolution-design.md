# Evolucionando el Sistema de Objetivos en 1Percent

**Fecha**: 2026-05-06
**Estado**: Aprobado
**Alcance**: Modelo de datos, Ritual Semanal, UI diferenciada (Finito/Infinito), Pausa/Vacaciones, Milestones, Prompts de IA

---

## Resumen

El sistema actual trata todos los objetivos de forma idéntica. Este rediseño introduce una bifurcación basada en la existencia del campo `deadline` en el modelo `Goal`, creando dos caminos diferenciados:

- **Objetivos Finitos** (con deadline): Proyectos con fecha de examen. Progreso lineal 0-100%, cuenta atrás, intensidad creciente hacia el final.
- **Objetivos Infinitos** (sin deadline): Hábitos de por vida. Racha semanal, nivel de intensidad sostenible, milestones de constancia.

Adicionalmente, el Ritual Semanal pasa de ser un AlertDialog a una pantalla completa inmersiva, y se implementa un sistema de Pausa individual + Modo Vacaciones global.

---

## 1. Cambios en el Modelo de Datos

### 1.1 Goal.kt — Campos nuevos

```kotlin
val weeklyStreak: Int = 0,           // Semanas consecutivas completadas (por goal)
val extensionCount: Int = 0,         // Veces que se ha extendido el deadline (solo finitos)
val streakStartDate: Long? = null,   // Cuándo empezó la racha actual
val pausedBy: PausedBy? = null,      // Quién pausó: USER o VACATION (null = no pausado)
```

Propiedad computada (no serializada):

```kotlin
val goalType: GoalType
    get() = if (deadline != null) GoalType.FINITE else GoalType.INFINITE
```

### 1.2 Nuevo enum GoalType.kt

```kotlin
enum class GoalType { FINITE, INFINITE }
```

### 1.3 Nuevo enum PausedBy.kt

```kotlin
enum class PausedBy { USER, VACATION }
```

### 1.4 AiRoadmapStatus.kt — Nuevo valor

```kotlin
enum class AiRoadmapStatus {
    NONE, NEGOTIATING, READY,
    PAUSED  // Goal pausado (individual o por vacaciones)
}
```

### 1.5 UserProfile.kt — Campos nuevos

```kotlin
val isVacationMode: Boolean = false,
val vacationStartDate: Long? = null
```

### 1.6 GoalExtensions.kt (nuevo archivo)

Extension functions que centralizan toda la lógica de presentación:

```kotlin
val Goal.isFinite: Boolean get() = goalType == GoalType.FINITE
val Goal.isInfinite: Boolean get() = goalType == GoalType.INFINITE

fun Goal.weekLabel(currentWeek: Int): String
// Finito: "Semana 18 de 52"  |  Infinito: "Semana 18"

fun Goal.progressDisplay(): Int?
// Finito: 0-100  |  Infinito: null

fun Goal.intensityDisplay(): String?
// Infinito: "3.5"  |  Finito: null

fun Goal.streakDisplay(): String?
// Infinito: "12 sem"  |  Finito: null

fun Goal.weeksRemaining(): Int?
// Finito: semanas hasta deadline  |  Infinito: null

fun Goal.nextMilestone(): Int?
// Infinito: próximo hito (4, 12, 26, 52)  |  Finito: null

fun Goal.justReachedMilestone(): Int?
// Comprueba si weeklyStreak coincide con un hito
// Usa lista [52, 26, 12, 4] y devuelve el primer match con streak % milestone == 0
// Prioriza el mayor para evitar doble-disparo
```

### 1.7 Milestone persistence (Firestore)

Nueva subcolección `goals/{goalId}/milestones/{milestoneId}`:

```kotlin
data class MilestoneRecord(
    val id: String,
    val milestone: Int,        // 4, 12, 26, 52
    val weeklyStreak: Int,     // Racha en el momento del desbloqueo
    val xpAwarded: Int,
    val unlockedAt: Long
)
```

Persiste para siempre, independiente de la racha actual. Alimenta la sección de logros en ProgressScreen.

---

## 2. Ritual Semanal Inmersivo

### 2.1 Arquitectura

Nueva ruta en el NavGraph: `ritual/{goalId}`. Cuando `HomeViewModel` detecta que `now >= goal.nextGenerationDate`, navega a esta ruta en lugar de mostrar un AlertDialog.

Nuevo `RitualViewModel` con máquina de estados:

```kotlin
enum class RitualStep {
    SUMMARY,          // Resumen: X/Y misiones, XP ganado
    EPIC_RESULT,      // ¿Pasaste la Misión Épica?
    DEADLINE_CHECK,   // Solo finitos: si esta semana == deadline
    FEEDBACK,         // Sobrado / Perfecto / Agotado
    INTENSITY_CHANGE, // Animación del cambio de nivel
    MILESTONE,        // Solo infinitos: si weeklyStreak alcanza hito
    GENERATING,       // Llamada real a AI + loading
    COMPLETE          // "¡Tu semana está lista!" → Home
}
```

### 2.2 Pasos visibles (calculados al inicio)

```
Siempre:      SUMMARY → EPIC_RESULT → FEEDBACK → INTENSITY_CHANGE → GENERATING → COMPLETE

Condicional:  DEADLINE_CHECK  (si goal.isFinite && deadline cae dentro de la semana actual)
              MILESTONE       (si goal.isInfinite && goal.justReachedMilestone() != null)

Posición:     DEADLINE_CHECK va después de EPIC_RESULT
              MILESTONE va después de INTENSITY_CHANGE
```

### 2.3 Detalle de cada paso

**SUMMARY**: Fondo con color de categoría. Título del goal. Etiqueta temporal ("Semana X de Y" o "Semana X"). Tarjetas animadas de misiones completadas apareciendo una a una. Total: "5/6 misiones — 180 XP".

**EPIC_RESULT**: Si completada: animación de celebración (confetti). "Mision Epica superada!". Si no completada: tono neutro, "La Epica se resistio esta semana", sin penalización emocional.

**DEADLINE_CHECK** (condicional): Se activa cuando el deadline del goal cae dentro de la semana actual del ritual (entre `now` y `now + 7 días`), es decir, esta es la última semana antes de la fecha límite. Muestra progreso alcanzado (ej. "78%"), stats del camino, extensionCount si aplica. Dos opciones:
- "Extender deadline" → abre date picker, incrementa `extensionCount`
- "Completar objetivo" → marca goal como `COMPLETED`, otorga `goalCompletionBonus`, navega a celebración/Hall of Fame. **Sale del ritual sin pasar por FEEDBACK ni GENERATING.**

**FEEDBACK**: Tres botones grandes: Sobrado / Perfecto / Agotado, cada uno con icono y descripción corta. Selección guardada en `RitualUiState.selectedFeedback`.

**INTENSITY_CHANGE**: Animación del nivel cambiando: viejo → nuevo con flecha y color (verde sube, rojo baja). Ej: "Nivel 3.2 → 3.4".

**MILESTONE** (condicional): Celebración grande: nombre del hito, XP bonus con animación, próximo milestone a alcanzar.

**GENERATING**: "Preparando tu proxima semana..." con loading real mientras `AICoachService.generateWeeklyTasks()` ejecuta la llamada a OpenAI.

**COMPLETE**: "Tu semana esta lista!" con resumen (nuevo nivel, racha actual) y botón "Ver misiones" → Home.

### 2.4 Skip

Botón discreto en esquina superior derecha, visible en SUMMARY, EPIC_RESULT e INTENSITY_CHANGE. Salta directo a FEEDBACK (único paso obligatorio del ritual). GENERATING y COMPLETE no se pueden saltar.

### 2.5 BackHandler

El botón "Atrás" del dispositivo está interceptado durante todo el ritual. Si se pulsa, el ritual se cancela sin guardar nada. La próxima vez que entre al Home, `nextGenerationDate` sigue expirado y el ritual se dispara de nuevo.

### 2.6 Catch-up (2+ semanas sin actividad)

Flujo reducido: `SUMMARY(adaptado) → FEEDBACK(adaptado) → INTENSITY_CHANGE → GENERATING → COMPLETE`.
- Sin EPIC_RESULT ni DEADLINE_CHECK
- SUMMARY muestra "Has vuelto! Llevas X semanas sin entrar"
- FEEDBACK usa opciones adaptadas: "Con energia / Normal / Cansado"

### 2.7 Estado del RitualViewModel

```kotlin
data class RitualUiState(
    val goal: Goal,
    val visibleSteps: List<RitualStep>,
    val currentStepIndex: Int = 0,
    val tasksCompleted: Int,
    val totalTasks: Int,
    val epicMissionPassed: Boolean,
    val xpEarned: Int,
    val selectedFeedback: EnergyFeedback? = null,
    val newIntensity: Float? = null,
    val oldIntensity: Float? = null,
    val milestoneReached: Int? = null,
    val newDeadline: Long? = null,
    val isGenerating: Boolean = false,
    val isCatchUp: Boolean = false
)
```

---

## 3. UI Diferenciada — TargetsScreen y GoalDetailScreen

### 3.1 Goal Cards en TargetsScreen

**Objetivo Finito**:
- Badge de categoría (color accent rojo/naranja)
- Etiqueta "Semana X de Y" en esquina superior derecha
- Barra de progreso 0-100% con porcentaje
- Footer: cuenta atrás ("X semanas restantes") + XP
- Indicador de Epica ("Epica en X dias" o "Epica hoy") como pill en footer

**Objetivo Infinito**:
- Badge de categoría (color accent verde/teal)
- Etiqueta "Semana X" en esquina superior derecha
- Nivel de Intensidad grande (X/10) + Racha semanal con fuego
- Footer: próximo hito ("Proximo hito: 26 semanas") + XP
- Indicador de Epica (mismo formato que finito)

**Objetivo Pausado** (ambos tipos):
- Opacidad reducida en la card
- Badge "Pausado" superpuesto
- Misiones siguen visibles y completables

### 3.2 GoalDetailScreen Header

**Finito**: Dashboard de 3 columnas — Progreso (%) | Semanas restantes | Intensidad

**Infinito**: Dashboard de 3 columnas — Nivel | Racha (semanas) | Misiones totales

Implementación: `Row` con `weight(1f)` por columna y `VerticalDivider`.

### 3.3 Colores semánticos

En lugar de hardcodear colores, usar roles de Material 3:
- **Finitos**: Perfil basado en `tertiary` / tonos cálidos (urgencia)
- **Infinitos**: Perfil basado en `primary` / tonos fríos (constancia)
- Soporte para Dark/Light mode y Dynamic Color (Android 12+)

---

## 4. Sistema de Pausa / Vacaciones

### 4.1 Pausa Individual (por Goal)

**UI**: Opción en menú de GoalDetailScreen: "Pausar objetivo" / "Reanudar objetivo".

**Al pausar**:
1. `goal.aiRoadmapStatus` → `PAUSED`
2. `goal.pausedBy` → `PausedBy.USER`
3. `goal.nextGenerationDate` se congela (no se borra)
4. `goal.weeklyStreak` se congela (no se resetea)
5. Misiones de la semana en curso se congelan en su sitio, completables con badge "Pausado"
6. Goal card en TargetsScreen: opacidad reducida + badge

**Al reanudar**:
1. `goal.aiRoadmapStatus` → `READY`
2. `goal.pausedBy` → `null`
3. `goal.nextGenerationDate` → `now + 7 dias`
4. `goal.weeklyStreak` continúa (sin penalización)
5. Si es goal finito, el deadline **no se mueve** (pérdida consciente de semanas)

### 4.2 Modo Vacaciones (Global)

**UI**: Toggle en ProfileScreen/Settings. Icono de palmera/avión. "Modo Vacaciones".

**Al activar**:
1. `userProfile.isVacationMode` → `true`
2. `userProfile.vacationStartDate` → `now`
3. Todos los goals con `aiRoadmapStatus == READY` → `PAUSED` con `pausedBy = VACATION`
4. Goals que ya estaban `PAUSED` con `pausedBy = USER` **no se tocan**
5. `userProfile.streakDays` se congela
6. HomeScreen: banner permanente "Modo Vacaciones activo desde hace X dias" + botón "Volver al 1%"
7. Notificaciones/alarmas silenciadas

**Al desactivar** ("Volver al 1%"):
1. `userProfile.isVacationMode` → `false`
2. `userProfile.vacationStartDate` → `null`
3. Solo goals con `pausedBy == VACATION` → `READY` con `nextGenerationDate` recalculado y `pausedBy = null`
4. Goals con `pausedBy == USER` permanecen pausados (respeto a la intención original)
5. Notificaciones reactivadas

### 4.3 Misiones en curso al pausar

Opción A (confirmada): Las misiones de la semana se congelan en su sitio. Si iba por el miércoles de la Semana 4, al volver de vacaciones sigue en el miércoles de la Semana 4. El usuario puede completarlas a su ritmo. Al completar la épica (o cuando expire nextGenerationDate tras reanudar), el ritual se dispara normalmente.

---

## 5. Sistema de Milestones y XP

### 5.1 Milestones de Constancia (solo Objetivos Infinitos)

| Semanas | Nombre | XP Bonus | Formula |
|---------|--------|----------|---------|
| 4 | "Primer Mes" | `difficulty x 40` | base x1 |
| 12 | "Trimestre de Hierro" | `difficulty x 80` | base x2 |
| 26 | "Medio Ano Imparable" | `difficulty x 150` | base x3 |
| 52 | "Un Ano Legendario" | `difficulty x 300` | base x5 |

Después de 52 semanas, el ciclo se repite (semana 56 = nuevo "Primer Mes"). El XP bonus del segundo ciclo y sucesivos se mantiene igual.

### 5.2 Calculo de weeklyStreak

Se actualiza durante el ritual semanal:

```
Si tasksCompleted > 0: weeklyStreak += 1
Si tasksCompleted == 0: weeklyStreak = 0 (reset)
Si goal PAUSED: weeklyStreak no cambia (congelado)
```

Criterio: basta con completar al menos 1 mision para mantener la racha. Constancia > perfección.

### 5.3 Deteccion de milestones

`justReachedMilestone()` usa lista descendente `[52, 26, 12, 4]`, devuelve el primer match con `streak % milestone == 0`. Prioriza el mayor para evitar doble-disparo (semana 52 no dispara también el de 4 ni el de 12).

### 5.4 Cambios en XpManager

```kotlin
suspend fun awardMilestoneBonus(userId: String, goal: Goal, milestone: Int): Result<Unit> {
    val multiplier = when(milestone) {
        4 -> 1; 12 -> 2; 26 -> 3; 52 -> 5
        else -> 1
    }
    val bonus = goal.difficulty * 40 * multiplier
    // Guardar MilestoneRecord en Firestore
    // Aplicar XP gain al perfil
}
```

### 5.5 Dónde aparecen los milestones

1. **Ritual Semanal** (paso MILESTONE): Celebración principal al alcanzar un hito
2. **Goal Card infinita**: "Proximo hito: 26 semanas"
3. **ProgressScreen**: Nueva sección "Hitos conseguidos" — badges desbloqueados por goal

---

## 6. Diferenciacion de Prompts de IA

### 6.1 Contexto segun GoalType

**Goals FINITOS**:

```
CONTEXTO DEL PROYECTO:
- Tipo: Proyecto finito con fecha limite
- Deadline: [fecha]
- Semanas restantes: [X de Y]
- Progreso actual: [Z]%
- Extensiones usadas: [N]

DIRECTRIZ: Este es un proyecto con fecha de examen. Diseña las misiones
para un progreso lineal que se intensifique gradualmente hacia el deadline.
Si quedan pocas semanas, prioriza las tareas mas criticas para el objetivo
final. La mision epica debe simular un "ensayo general" del reto final.
```

**Goals INFINITOS**:

```
CONTEXTO DEL HABITO:
- Tipo: Habito de por vida (sin fecha limite)
- Semanas activas: [X]
- Racha actual: [Y] semanas consecutivas
- Proximo hito de constancia: [Z] semanas

DIRECTRIZ: Este es un habito para toda la vida. Prioriza la variedad y
la sostenibilidad a largo plazo. Evita la monotonia rotando tipos de
actividad. La mision epica debe ser un pico de motivacion y diversion,
no un examen. Si la racha es larga (>12 semanas), introduce retos
creativos para mantener el interes fresco.
```

### 6.2 Estructura del prompt en AICoachService

```kotlin
private fun buildPromptContext(goal: Goal, summary: WeeklySummary?): String {
    val baseContext = // titulo, categoria, intensidad, feedback previo, regla de 7 dias
    val typeContext = when (goal.goalType) {
        GoalType.FINITE -> buildFiniteContext(goal)
        GoalType.INFINITE -> buildInfiniteContext(goal)
    }
    return baseContext + typeContext
}
```

Las reglas rigidas (exactamente 7 misiones, dayIndex 1-7, formato de respuesta) permanecen en `baseContext` por encima del `typeContext` creativo.

### 6.3 Curvas de intensidad diferenciadas

**Finitos**: Crecimiento base 7.2%. Cuando `weeksRemaining <= 4`, el crecimiento sube a `7.2% × (1 + (4 - weeksRemaining) / 4)`, es decir: 4 semanas → 7.2%, 3 → 9%, 2 → 10.8%, 1 → 12.6%. Hard cap: `difficulty × 2.0`. Siempre respeta el feedback del usuario (AGOTADO sigue reduciendo un 10%).

**Infinitos**: Crecimiento base 7.2% con hard cap reducido a `difficulty × 1.5` (en vez de ×2.0). La intensidad sube más despacio y se estabiliza antes, priorizando sostenibilidad. Si el usuario lleva >26 semanas con intensidad variando menos de ±0.3, el prompt sugiere variedad en vez de subir dificultad.

La logica de calculo se centraliza en `AICoachService.calculateNewIntensity()` recibiendo el Goal completo (incluyendo goalType y weeksRemaining).

---

## Decisiones de Diseno Clave

| Decision | Razon |
|----------|-------|
| `goalType` computado desde `deadline` | Evita redundancia; un solo campo determina el tipo |
| `AiRoadmapStatus.PAUSED` separado de `GoalStatus.PAUSED` | Permite goal ACTIVE con IA pausada |
| `PausedBy` enum | Resuelve conflicto entre pausa manual y vacaciones |
| Racha por goal (no global) | Milestones de constancia tienen sentido por objetivo individual |
| Skip en el ritual salta a FEEDBACK | FEEDBACK es obligatorio (alimenta el motor de IA), animaciones son opcionales |
| BackHandler cancela sin guardar | El ritual se re-dispara al volver; no hay estado inconsistente |
| Misiones se congelan al pausar | El usuario puede completarlas a su ritmo; sin borrado en Firestore |
| Deadline no se mueve al pausar finitos | Refleja la realidad; la pausa tiene un coste consciente |
| Extension sin limite pero con contador | Respeta la autonomia del usuario sin juzgar |
| tasksCompleted > 0 mantiene racha | Constancia > perfeccion; filosofia del 1% |
| Milestones persistidos en subcoleccion | Sobreviven a resets de racha; alimentan logros en ProgressScreen |

---

## Enfoque Arquitectonico

**Enfoque B: GoalType computado + RitualScreen dedicado**

- Propiedad computada `goalType` en Goal (sin serializar)
- Extension functions en `GoalExtensions.kt` para toda la logica de presentacion
- `RitualViewModel` con maquina de estados (`RitualStep`) para el flujo multi-paso
- Roles de color de Material 3 para la diferenciacion visual
- Vacation mode como toggle en UserProfile con resolucion de conflictos via PausedBy
