
repositories {
    mavenCentral()
    mavenLocal()
}

plugins {
    java
    `java-library`
    kotlin("jvm") version ("1.8.10")
}


description = "The base content chunkstories is built on."
group = "xyz.chunkstories"
version = "2.0.4"

val apiRevisionBuiltAgainst: String by extra { "2.0.4" }

dependencies {
    implementation("xyz.chunkstories:api:$apiRevisionBuiltAgainst")
}

val jar: Jar by tasks
jar.apply {
    destinationDirectory.set(file("$rootDir/res"))
    archiveVersion.set(null as String?)
}

val buildContentPack: Zip = tasks.create<Zip>("buildContentPack") {
    dependsOn(jar)

    from("res/") {
        exclude("**/*.pdn")
        exclude("**/*.ps")
        exclude("**/*.xcf")
        exclude("**/*.aup")
        exclude("**/*.blend")
    }

    //We want the final archive to bear our mod name
    archiveBaseName.set("core_content.zip")
}

artifacts {
    archives(jar)
    archives(buildContentPack)
}