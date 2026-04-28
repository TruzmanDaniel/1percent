# Plan de Integración de IA: Coach Adaptativo 1% (Arquitectura Client-Side)

## 1. Visión General
Transformar la aplicación en un coach personal que utiliza el SDK de **Vertex AI for Firebase** directamente desde Android para generar misiones diarias. El sistema se basa en la mejora exponencial del 1% diario y en un proceso de **negociación inicial** para asegurar que el plan se adapta al usuario desde el primer día.

## 2. Experiencia de Usuario Detallada (UX)

### A. El Onboarding del Goal (Negociación del Plan)
Cuando el usuario pulsa "Crear Objetivo", el flujo es:
1.  **Formulario de Intención**: Título, categoría y **Dificultad Percibida** (1-10) por el usuario.
2.  **Propuesta de la IA**: El móvil llama a Gemini y muestra una previsualización de las primeras 7 misiones.
3.  **Bucle de Ajuste (Feedback Inmediato)**:
    - Debajo de la propuesta aparecen tres botones: 🔵 **Demasiado Fácil**, 🟡 **OK, me gusta** y 🔴 **Demasiado Difícil**.
    - **Reiteración**: Si elige Fácil o Difícil, la app envía una nueva instrucción a la IA (ej: *"Aumenta la carga un 10%"*) y muestra una nueva propuesta al instante.
4.  **Confirmación**: Solo cuando el usuario pulsa **OK**, el `Goal` se crea en Firestore y las 7 misiones se inyectan en la colección `/tasks`.

### B. El Ritual de Nueva Semana (Transición)
Se dispara cuando ha pasado una semana y el usuario accede a la app:
1.  **Pantalla de Cosecha**: Resumen visual del éxito de la semana pasada (ej: "Mejora del 7.2%").
2.  **Calibración de Energía**: El usuario elige entre 🟢 Sobrado, 🟡 Perfecto o 🔴 Agotado.
3.  **Generación y Preview**: La app propone los nuevos 7 días. El usuario puede volver a usar el bucle de "Fácil/Difícil" antes de aceptar el nuevo bloque semanal.

---

## 3. Arquitectura Técnica (Requisito: Android Studio Direct)

### Componentes en Android
- **Vertex AI SDK**: Librería `firebase-vertexai` para comunicación directa con Gemini 1.5 Flash.
- **AIViewModel**: Gestiona el estado de la "negociación" (la lista temporal de misiones antes de ser guardadas).
- **Prompt Dinámico**: El prompt incluye el historial de ajustes del usuario en esa misma sesión para que la IA no repita errores.

---

## 4. Lógica del Algoritmo "1.01"
- **Punto de Partida**: Definido por la Dificultad Percibida del usuario + el bucle de ajuste inicial.
- **Crecimiento**: `Nueva_Intensidad = Intensidad_Anterior * (1.01)^7`.
- **Ajuste Semanal**: El feedback de "Sobrado/Agotado" modifica el multiplicador para la semana siguiente.

---

## 5. Cambios Necesarios en Modelos de Datos

### `Goal.kt`
- `initialDifficulty: Int` (El valor 1-10 del usuario).
- `currentIntensity: Float`, `nextGenerationDate: Long`.
- `aiRoadmapStatus: Status` (NEGOTIATING, READY).

### `Task.kt`
- `isAiGenerated: Boolean`, `difficultyScore: Float`.

### `WeeklySummary.kt` (Memoria de la IA)
- Almacena el resultado de cada semana para que Gemini sepa si el usuario suele pecar de optimista o de conservador en sus ajustes.

---
*Plan adaptado para ejecución directa desde el cliente Android con bucle de feedback - 28 de Abril de 2026*
