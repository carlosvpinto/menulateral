# ===================================================================
# --- INICIO: Reglas Definitivas para Librerías de Serialización ---
# ===================================================================

# 1. Reglas específicas para KLAXON
# Mantiene las anotaciones que Klaxon necesita para funcionar.
-keep class com.beust.klaxon.** { *; }
-keepattributes Signature
-keepattributes *Annotation*


# 2. Regla "A Prueba de Balas" para TODOS los modelos de datos
# Mantiene intactas TODAS las clases que estén en CUALQUIER subpaquete
# dentro de "com.carlosv.dolaraldia.model".
# Esto protege a apiAlcambioEuro, datosPMovil y cualquier otro que crees,
# sin importar si usan Gson o Klaxon.
-keep public class com.carlosv.dolaraldia.model.** {
    public <init>();
    <fields>;
    <methods>;
}


# 3. Reglas generales recomendadas para GSON (por si se usa en otra parte)
-keep class com.google.gson.** { *; }

# ===================================================================
# --- FIN: Reglas para Librerías de Serialización ---
# ===================================================================