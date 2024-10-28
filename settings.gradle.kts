rootProject.name = "ec-config-template"

pluginManagement {
    repositories {
        mavenCentral()
        maven("https://maven.pkg.github.com/socotra/config-sdk-template") {
            credentials {
                username = "lydiaahrens"
                password = "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"
            }
        }
    }
}
