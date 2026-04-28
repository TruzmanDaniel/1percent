package es.uc3m.android.a1percent.data.model.enums

import kotlinx.serialization.Serializable

// PREDEFINED CATEGORIES (Custom Ones go to the Database)
@Serializable
enum class Category(val displayName: String) {
    // Future behavior: let the app infer one of the predefined categories.
    AUTOMATIC("Automatic"),

    HEALTH("Health"),
    FITNESS("Fitness"),
    STUDY("Study"),
    WORK("Work"),
    PERSONAL("Personal"),
    FINANCE("Finance"),
    SOCIAL("Social")
}
