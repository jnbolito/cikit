package io.bolito.cikit.util.preconditions

fun requireEnvVars(vararg envVars: String, envVarMap: Map<String, String> = System.getenv()) {
    val missingEnvVars = envVars.filter { !envVarMap.containsKey(it) }
    if (missingEnvVars.isNotEmpty()) {
        val missingEnvVarsString = missingEnvVars.joinToString("\n - ", "\n - ")
        error("Required environment variables are missing:$missingEnvVarsString")
    }
}
