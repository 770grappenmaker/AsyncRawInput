plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.weave)
}

group = "com.grappenmaker"
version = "0.1"

repositories {
    mavenCentral()
    maven("https://repo.weavemc.dev/releases")
    maven("https://repo.spongepowered.org/maven/")
}

kotlin { jvmToolchain(8) }

minecraft {
    configure {
        name = "AsyncRawInput"
        modId = "async-raw-input"
        hooks = listOf("com.grappenmaker.rawinput.MouseHelperHook")
        mcpMappings()
    }

    version("1.8.9")
}

dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly(libs.bundles.weave)
    implementation(libs.jinput)
    implementation(variantOf(libs.jinput) { classifier("natives-all") })
}

tasks {
    jar {
        from(configurations.runtimeClasspath.map { conf ->
            conf.map { if (it.isDirectory) it else zipTree(it) }
        })

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}