plugins {
    id("java")
    alias(libs.plugins.shadow)
}

group = "cn.flowerinsnow.miteoperator"
version = "1.0.2"

repositories {
    System.getenv("GRADLE_CENTRAL_MIRROR")?.let {
        maven(it)
    }
    mavenCentral()
}

dependencies {
    implementation(libs.asm.tree)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}

tasks.shadowJar {
    from("LICENSE")
    from("NOTICE")

    manifest {
        attributes(mapOf(
            "Premain-Class" to "cn.flowerinsnow.miteoperator.MITEOperatorAgent"
        ))
    }
    archiveClassifier = ""

    relocate("org.objectweb.asm", "cn.flowerinsnow.miteoperator.shaded.org.objectweb.asm")
}