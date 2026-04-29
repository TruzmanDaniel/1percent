package es.uc3m.android.a1percent.data.model.enums

import kotlinx.serialization.Serializable

@Serializable
enum class EnergyFeedback(val displayName: String) {
    SOBRADO("Sobrado"),
    PERFECTO("Perfecto"),
    AGOTADO("Agotado")
}
