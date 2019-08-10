// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        jcenter()
        google()
    }
    dependencies {
        classpath(Dependencies.Build.gradle)
        classpath(Dependencies.Kotlin.plugin)
    }
}

allprojects {
    repositories {
        jcenter()
        google()
    }
}

tasks {
    wrapper {
        gradleVersion = Config.gradleVersion
        distributionType = Wrapper.DistributionType.ALL
    }
}
