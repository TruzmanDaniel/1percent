package es.uc3m.android.a1percent.data.model.enums

// PREDEFINED CATEGORIES (Custom Ones go to the Database)
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
