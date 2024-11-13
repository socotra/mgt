plugins {
    java
    id("socotra-ec-config-developer") version "v0.6.5"
}

`socotra-config-developer` {
    apiUrl.set("https://api-ec-sandbox.socotra.com")
    tenantLocator.set("f901fb1d-27eb-4e7d-a2a4-62eba696765c")
    personalAccessToken.set("SOCP_01JCGX79G7PNFBKEA6HSYRGBNT")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
}
