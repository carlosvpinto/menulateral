import java.util.Properties
plugins {

    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    kotlin("kapt")
}

// --- BLOQUE CORRECTO PARA .kts ---
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}


android {
    namespace = "com.carlosv.menulateral"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.carlosv.menulateral"
        minSdk = 26
        targetSdk = 36
        versionCode = 133
        versionName = "WidgetyPermisos1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "FULL" // Para el código nativo C/C++
            }

        }

        // Esto crea campos en BuildConfig para todas tus variantes de build (debug, release, etc.)
        all {
            buildConfigField("String", "MERCANTIL_MERCHANT_ID", "\"${localProperties.getProperty("MERCANTIL_MERCHANTID")}\"")
            buildConfigField("String", "MERCANTIL_CLIENT_ID", "\"${localProperties.getProperty("MERCANTIL_CLIENTID")}\"")
            buildConfigField("String", "MERCANTIL_SECRET_KEY", "\"${localProperties.getProperty("MERCANTIL_SECRETKEY")}\"")
            buildConfigField("String", "MERCANTIL_INTEGRATOR_ID", "\"${localProperties.getProperty("MERCANTIL_INTEGRATORID")}\"")
            buildConfigField("String", "MERCANTIL_TERMINAL_ID", "\"${localProperties.getProperty("MERCANTIL_TERMINALID")}\"")
            buildConfigField("String", "MERCANTIL_PHONE_NUMBER", "\"${localProperties.getProperty("MERCANTIL_PHONE_NUMBER")}\"")
            buildConfigField("String", "FCM_SERVER_KEY", "\"${localProperties.getProperty("FCM_SERVER_KEY")}\"")
        }



    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

}

dependencies {

    //Para Usar el ckBar
    implementation("com.google.android.material:material:1.12.0")
    //implementation("androidx.compose.material3:material3:1.3.2")




    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-analytics")

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")

    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.5")
    // implementation("com.google.firebase:firebase-analytics:21.5.0")
    // implementation("com.google.firebase:firebase-crashlytics:18.6.0")

    // Add the dependency for the Firebase SDK for Google Analytics
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-config-ktx") // Agrega esta línea para Firebase Remote Config

    // Glide para Imágenes
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.makeramen:roundedimageview:2.3.0")

    // Klaxon
    implementation("com.beust:klaxon:5.5")

    // Image Slider
    implementation("com.github.denzcoskun:ImageSlideshow:0.1.2")

    // RETROFIT2
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.1") // La versión puede variar
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:0.8.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.9.1")

    // Para usar @SerializedName
    implementation("com.google.code.gson:gson:2.8.9")

    // Convertidor a JSON
    implementation("com.beust:klaxon:5.5")

    // SwipeRefreshLayout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // AdMob
    implementation("com.google.android.gms:play-services-ads:23.6.0")
    //implementation("com.google.android.gms:play-services-ads:24.4.0")
    implementation("com.google.android.play:review-ktx:2.0.2")


    // Jsoup
    implementation("org.jsoup:jsoup:1.16.1")
    implementation("com.google.android.ump:user-messaging-platform:2.2.0")
    implementation("androidx.compose.material3:material3-android:1.3.1")
    implementation("androidx.activity:activity:1.8.0")

//    // Facebook SDK
//    implementation("com.facebook.android:facebook-android-sdk:16.0.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    val lifecycle_version = "2.7.0"
    val arch_version = "2.2.0"

    // Room
    val room_version = "2.6.1" // Reemplaza con la última versión

    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    kapt("androidx.room:room-compiler:$room_version") // Usa 'kapt'

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel:$lifecycle_version")
    // LiveData
    implementation("androidx.lifecycle:lifecycle-livedata:$lifecycle_version")
    // Lifecycles only (without ViewModel or LiveData)
    implementation("androidx.lifecycle:lifecycle-runtime:$lifecycle_version")

    // Saved state module for ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:$lifecycle_version")

    // Annotation processor
    annotationProcessor("androidx.lifecycle:lifecycle-compiler:$lifecycle_version")
    // Alternativamente - si usas Java8, usa lo siguiente en lugar de lifecycle-compiler
    implementation("androidx.lifecycle:lifecycle-common-java8:$lifecycle_version")

    // Opcional - helpers para implementar LifecycleOwner en un Service
    implementation("androidx.lifecycle:lifecycle-service:$lifecycle_version")

    // Opcional - ProcessLifecycleOwner proporciona un ciclo de vida para todo el proceso de la aplicación
    implementation("androidx.lifecycle:lifecycle-process:$lifecycle_version")

    // Opcional - Soporte de ReactiveStreams para LiveData
    implementation("androidx.lifecycle:lifecycle-reactivestreams:$lifecycle_version")

    // Opcional - Test helpers para LiveData
    testImplementation("androidx.arch.core:core-testing:$arch_version")

    // Opcional - Test helpers para Lifecycle runtime
    testImplementation("androidx.lifecycle:lifecycle-runtime-testing:$lifecycle_version")



    //Imagen
    implementation ("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.github.bumptech.glide:glide:4.13.2")
    kapt("com.github.bumptech.glide:compiler:4.13.2")


    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.24")

    implementation ("com.google.android.gms:play-services-base:18.5.0")







    
}

// Aplicar el plugin de Google Services fuera del bloque dependencies
apply(plugin = "com.google.gms.google-services")
