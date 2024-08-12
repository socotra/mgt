rootProject.name = "ec-config-template"

pluginManagement {
    buildscript {
        repositories {
            flatDir {
                dirs("plugin")
            }
        }
        dependencies {
            classpath(":kernel-plugin-tasks")
        }
    }
}
