plugins {
    java
    id("socotra-ec-config-developer") version "v0.6.5"
}

`socotra-config-developer` {
    apiUrl.set("https://api-kernel-dev.socotra.com")
    tenantLocator.set("a534f068-ec84-459c-a449-e407adb32881")
    personalAccessToken.set("SOCP_01JBAA566F06Q4E17YWKSDECP0")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
}
