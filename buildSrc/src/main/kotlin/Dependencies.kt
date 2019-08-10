object Dependencies {

    object Kotlin {
        private object Versions {
            const val kotlin = "1.3.41"
        }

        const val stdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin}"
        const val plugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    }

    object Build {
        private object Versions {
            const val gradle = "3.4.2"
        }

        const val gradle = "com.android.tools.build:gradle:${Versions.gradle}"
    }

    object AndroidX {
        private object Versions {
            const val ktx = "1.2.0-alpha03"
            const val appcompat = "1.1.0-rc01"
            const val material = "1.1.0-alpha09"
            const val recyclerView = "1.1.0-beta01"
            const val constraintLayout = "2.0.0-beta2"
        }

        const val coreKtx = "androidx.core:core-ktx:${Versions.ktx}"
        const val appcompat = "androidx.appcompat:appcompat:${Versions.appcompat}"
        const val material = "com.google.android.material:material:${Versions.material}"
        const val recyclerVIew = "androidx.recyclerview:recyclerview:${Versions.recyclerView}"
        const val constraintLayout = "androidx.constraintlayout:constraintlayout:${Versions.constraintLayout}"
    }
}