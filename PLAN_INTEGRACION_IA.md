# Plan de Integración de IA: Coach Adaptativo 1% (Arquitectura Client-Side)

## 1. Visión General
Transformar la aplicación en un coach personal que utiliza el SDK de **Vertex AI for Firebase** directamente desde Android para generar misiones diarias. El sistema se basa en la mejora exponencial del 1% diario y en un proceso de **negociación inicial** para asegurar que el plan se adapta al usuario desde el primer día.

## 2. Experiencia de Usuario Detallada (UX)

### A. El Onboarding del Goal (Negociación del Plan)
1.  **Formulario de Intención**: Título, categoría y **Dificultad Percibida** (1-10).
2.  **Propuesta de la IA**: El móvil llama a Gemini y muestra una previsualización de las primeras 7 misiones.
3.  **Bucle de Ajuste (Feedback Inmediato)**:
    - Botones: 🔵 **Demasiado Fácil**, 🟡 **OK, me gusta** y 🔴 **Demasiado Difícil**.
    - La IA ajusta la intensidad base según la elección.
4.  **Confirmación**: Al pulsar **OK**, se crea el `Goal` y se inyectan las 7 `Task` en Firestore.

### B. El Ritual de Nueva Semana (Día de Cosecha)
1.  **Pantalla de Resumen**: Éxito de la semana anterior y porcentaje de mejora real percibida.
2.  **Misión Épica (Día 7)**: El último día de la semana es un reto de alta intensidad que otorga un **bonus de XP** (5x) y sirve de "puerta" para subir el nivel de la semana siguiente.
3.  **Calibración y Generación**: Feedback del usuario y creación de los nuevos 7 días considerando el **Contexto de Vida Real** (misiones más largas en fines de semana).

---

## 3. Arquitectura Técnica y Datos

### Modelo de Datos Único: `Task`
Para mantener la simplicidad, las misiones generadas por IA usarán el modelo `Task.kt` existente con los siguientes valores:
- `isAiGenerated = true`
- `goalId = [ID_DEL_GOAL]`
- `xp`: Calculado por la IA (Días 1-6: Normal, Día 7: Épico).
- `dayIndex`: 1 a 7.

### Formato de Respuesta de la IA (JSON)
```json
{
  "tasks": [
    {
      "title": "Misión Día 1",
      "description": "...",
      "xp": 50,
      "difficulty": 2,
      "dayIndex": 1
    },
    ...
    {
      "title": "EL RETO ÉPICO",
      "description": "Misión final de semana para validar tu progreso",
      "xp": 300,
      "difficulty": 5,
      "dayIndex": 7
    }
  ]
}
```

---

## 4. Algoritmo de Inteligencia y Contexto

### Contexto de Vida Real
El prompt de la IA incluirá:
- *"Es fin de semana: propón tareas que requieran más tiempo pero menos equipo técnico."*
- *"Es día laboral: misiones rápidas (<15 min) centradas en la constancia."*

### Algoritmo "1.01"
- **Nueva_Intensidad = Intensidad_Anterior * (1.01)^7**.
- Si el usuario falla la Misión Épica, la IA mantiene la intensidad para la semana siguiente (meseta de aprendizaje).

---

## 5. Cambios Necesarios en Modelos de Datos

### `Goal.kt`
- `currentIntensity: Float`, `nextGenerationDate: Long`.
- `aiRoadmapStatus: Status` (NEGOTIATING, READY).

### `Task.kt`
- `isAiGenerated: Boolean`, `difficultyScore: Float`.

### `WeeklySummary.kt` (Colección Raíz)
- Documentos cortos con el resumen del desempeño para servir de "memoria" a la IA sin saturar el contexto de tokens.

## 6. Control de Costes y Cuotas de Uso

### A. Sistema de Créditos (Anti-Spam)
Para evitar costes excesivos, el sistema implementará:
- **Cuota Semanal**: Cada usuario tendrá un máximo de 5 generaciones de IA por semana (almacenado en `UserProfile.availableCredits`).
- **Límite de Negociación**: El bucle "Fácil/Difícil/OK" tendrá un máximo de 3 reiteraciones por sesión.
- **Validación previa**: El ViewModel bloqueará el botón de "Generar" si el usuario ha agotado sus créditos.

### B. Optimización de Modelo
- **Uso de Gemini 1.5 Flash**: Modelo optimizado para baja latencia y coste reducido (hasta 10x más barato que 1.5 Pro).
- **Prompt Engineering Eficiente**: Instrucciones para forzar respuestas JSON puras sin texto decorativo redundante.

### C. Seguridad Financiera
- **Firebase App Check**: Requisito obligatorio para asegurar que solo la App legítima consume la cuota de la IA.
- **Límites de Google Cloud**: Configuración de alertas de presupuesto y "Hard Quotas" en la consola de Google para detener el servicio si se excede un presupuesto mensual (ej: 5€).

---
*Plan finalizado para la Fase 2 del Proyecto 1percent - 28 de Abril de 2026*
