import java.nio.file.Files
import java.util.UUID

plugins {
    idea
    `maven-publish`
    jacoco
    id("net.minecraftforge.gradle") version "6.0.54"
    id("org.parchmentmc.librarian.forgegradle") version "1.2.0"
    id("org.spongepowered.mixin") version "0.7.38"
}

group = "com.bettercontent"
version = property("mod_version") as String
base {
    archivesName.set(property("artifact_name") as String)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

// Mirror the pack's exact optional-upstream repairs only in the nonshipping source fixture.
val nativeFixture by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
    runtimeClasspath += sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath
}
val authoredRespiteOverrides = listOf(
    "recipes/filling/water_cup.json", "recipes/pouring/water_cup.json",
    "recipes/emptying/water_cup.json", "loot_tables/blocks/kettle.json",
    "advancements/main/stunt_tea_bush.json"
).associateWith { file("../../better-content-modpack/kubejs/data/farmersrespite/$it") }
tasks.named<ProcessResources>(nativeFixture.processResourcesTaskName) {
    authoredRespiteOverrides.forEach { (relative, authored) ->
        from(authored) { into("data/farmersrespite/${relative.substringBeforeLast('/')}") }
    }
}

val nativeRunDirectory = file("run-gametest/attempts/${UUID.randomUUID()}")

minecraft {
    mappings("official", property("minecraft_version") as String)
    copyIdeResources = true
    jarJar.enable()

    runs {
        configureEach {
            workingDirectory(project.file("run"))
            property("forge.logging.console.level", "debug")
            property("mixin.env.remapRefMap", "true")
            property("mixin.env.refMapRemappingFile", "${projectDir}/build/createSrgToMcp/output.srg")
            mods {
                create(property("mod_id") as String) {
                    source(sourceSets.main.get())
                }
            }
        }
        create("client")
        create("server") { arg("--nogui") }
        create("gameTestServer") {
            workingDirectory(nativeRunDirectory)
            property("forge.enableGameTest", "true")
            property("forge.gameTestServer", "true")
            property("forge.enabledGameTestNamespaces", property("mod_id") as String)
            mods { named(property("mod_id") as String) { source(nativeFixture) } }
            arg("--nogui")
        }
    }
}

repositories {
    maven("https://maven.minecraftforge.net")
    maven("https://maven.createmod.net")
    maven("https://maven.ithundxr.dev/mirror")
    maven("https://maven.tterrag.com/")
    maven("https://harleyoconnor.com/maven")
    maven("https://repo.spongepowered.org/repository/maven-public/")
    maven("https://maven.llamalad7.mixinextras.org/releases/")
    maven("https://maven.valkyrienskies.org") { content { includeGroup("org.valkyrienskies.core") } }
    maven("https://www.cursemaven.com") { content { includeGroup("curse.maven") } }
    mavenCentral()
}

dependencies {
    minecraft("net.minecraftforge:forge:${property("minecraft_version")}-${property("forge_version")}")
    implementation(fg.deobf("com.simibubi.create:create-${property("minecraft_version")}:6.0.8-291:slim"))
    implementation(fg.deobf("net.createmod.ponder:Ponder-Forge-${property("minecraft_version")}:1.0.92"))
    compileOnly(fg.deobf("dev.engine-room.flywheel:flywheel-forge-api-${property("minecraft_version")}:1.0.5"))
    runtimeOnly(fg.deobf("dev.engine-room.flywheel:flywheel-forge-${property("minecraft_version")}:1.0.5"))
    implementation(fg.deobf("com.tterrag.registrate:Registrate:MC1.20-1.3.3"))
    compileOnly(annotationProcessor("io.github.llamalad7:mixinextras-common:0.5.0")!!)
    implementation(jarJar("io.github.llamalad7:mixinextras-forge:[0.5.0,0.6.0)")!!)
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
    compileOnly(fg.deobf("curse.maven:ars-nouveau-401955:6688854"))
    compileOnly(fg.deobf("curse.maven:hyle-609850:7736352"))
    compileOnly(fg.deobf("curse.maven:thirst-was-taken-679270:6660408"))
    compileOnly(fg.deobf("curse.maven:cold-sweat-506194:7893262"))
    compileOnly(fg.deobf("curse.maven:pollution-of-the-realms-269973:8554528"))
    compileOnly(fg.deobf("curse.maven:little-logistics-570050:4799459"))
    compileOnly(fg.deobf("curse.maven:weather-storms-tornadoes-237746:5244118"))
    compileOnly(fg.deobf("curse.maven:creativecore-257814:7649757"))
    compileOnly(fg.deobf("curse.maven:ambientsounds-254284:7550220"))
    compileOnly(fg.deobf("curse.maven:oculus-581495:6020952"))
    compileOnly(fg.deobf("curse.maven:sophisticated-core-618298:7916595"))
    compileOnly(fg.deobf("curse.maven:sophisticated-storage-619320:7973265"))
    compileOnly(fg.deobf("curse.maven:sophisticated-backpacks-422301:7916619"))
    compileOnly(fg.deobf("curse.maven:curios-api-309927:6418456"))
    compileOnly(fg.deobf("curse.maven:mantle-74924:7563777"))
    compileOnly(fg.deobf("curse.maven:tinkers-construct-74072:7449219"))
    runtimeOnly(fg.deobf("curse.maven:tinkers-construct-74072:7449219"))
    runtimeOnly(fg.deobf("curse.maven:mantle-74924:7563777"))
    compileOnly(fg.deobf("curse.maven:polymorph-388800:6450982"))
    compileOnly(fg.deobf("curse.maven:architectury-api-419699:5137938"))
    compileOnly(fg.deobf("curse.maven:epic-fight-mod-405076:8049910"))
    compileOnly(fg.deobf("curse.maven:valkyrien-skies-258371:7906689"))
    compileOnly(fg.deobf("curse.maven:realistic-block-physics-375616:6393411"))
    compileOnly(fg.deobf("curse.maven:realistic-physics-1030082:6026115"))
    compileOnly(fg.deobf("curse.maven:rehooked-1096531:6341096"))
    testRuntimeOnly(fg.deobf("curse.maven:rehooked-1096531:6341096"))
    compileOnly(fg.deobf("curse.maven:patchouli-306770:7731017"))
    compileOnly("org.valkyrienskies.core:api:1.1.0+cf208d8b56")
    runtimeOnly(fg.deobf("curse.maven:thirst-was-taken-679270:6660408"))
    runtimeOnly(fg.deobf("curse.maven:curios-api-309927:6418456"))
    runtimeOnly(fg.deobf("curse.maven:architectury-api-419699:5137938"))
    runtimeOnly(fg.deobf("curse.maven:farmers-delight-398521:8007609"))
    runtimeOnly(fg.deobf("curse.maven:farmers-respite-551453:5543547"))
    runtimeOnly(fg.deobf("curse.maven:brewin-and-chewin-637808:6237884"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("com.google.code.gson:gson:2.10.1")
}

tasks.named<Jar>("jar") {
    dependsOn(tasks.named("compileJava"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(layout.buildDirectory.file("tmp/compileJava/compileJava-refmap.json")) {
        rename { "better_drinking_water.refmap.json" }
    }
    finalizedBy("reobfJar")
}

tasks.named<JavaCompile>("compileJava") {
    outputs.file(layout.buildDirectory.file("tmp/compileJava/compileJava-refmap.json"))
}

val stageRuntimeJar by tasks.registering(Copy::class) {
    group = "build"
    description = "Stages the reobfuscated runtime jar into build/libs using the canonical release filename."
    dependsOn(tasks.named("reobfJar"))
    mustRunAfter(tasks.named("jarJar"))
    mustRunAfter(tasks.named("reobfJarJar"))
    from(layout.buildDirectory.file("reobfJar/output.jar"))
    into(layout.buildDirectory.dir("libs"))
    rename { "${base.archivesName.get()}-$version.jar" }
}

tasks.named("assemble") {
    dependsOn(stageRuntimeJar)
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.register("headlessGameTest") {
    group = "verification"
    description = "Runs Forge game tests in a headless dedicated server."
    dependsOn(tasks.named("runGameTestServer"))
}

tasks.register("verifyFast") {
    group = "verification"
    description = "Runs deterministic unit/resource checks without Forge game tests."
    dependsOn(tasks.named("check"), "verifyNativeSetup")
}
val verifyNativeSetup by tasks.registering(Exec::class) {
    commandLine("python3", "-B", "-m", "unittest", "discover", "-s", "scripts", "-p", "test_native_*.py")
}

val verifyNativeDiagnostics by tasks.registering {
    group = "verification"
    dependsOn(tasks.named("headlessGameTest"))
    doLast {
        exec {
            commandLine("python3", "-B", "scripts/audit_native_diagnostics.py", "44",
                nativeRunDirectory.resolve("logs/latest.log").absolutePath,
                nativeRunDirectory.resolve("logs/debug.log").absolutePath)
        }
    }
}
tasks.register("verifyFull") {
    group = "verification"
    description = "Runs unit checks, native GameTests and strict native diagnostics."
    dependsOn(tasks.named("verifyFast"))
    dependsOn(verifyNativeDiagnostics)
}

val prepareNativeAttempt = tasks.register("prepareNativeAttempt") {
    doLast {
        check(!Files.isSymbolicLink(nativeRunDirectory.parentFile.toPath()) &&
            !Files.isSymbolicLink(nativeRunDirectory.parentFile.parentFile.toPath())) { "Native attempt ancestry must not be a symlink" }
        check(!nativeRunDirectory.exists()) { "Refusing an existing native attempt: $nativeRunDirectory" }
        check(nativeRunDirectory.mkdirs()) { "Cannot create native attempt: $nativeRunDirectory" }
        logger.lifecycle("Native GameTest fresh attempt admitted: $nativeRunDirectory")
    }
}

val syncGameTestStructures = tasks.register<Sync>("syncGameTestStructures") {
    dependsOn(prepareNativeAttempt)
    from(layout.projectDirectory.dir("src/main/resources/gameteststructures"))
    into(nativeRunDirectory.resolve("gameteststructures"))
}

tasks.matching { it.name.startsWith("prepareRunGameTestServer") }.configureEach {
    dependsOn(syncGameTestStructures, tasks.named(nativeFixture.classesTaskName))
    doLast {
        exec { commandLine("python3", "-B", "scripts/ensure_native_properties.py", nativeRunDirectory.absolutePath) }
    }
}
tasks.named("runGameTestServer") {
    doFirst {
        val actual = file(minecraft.runs.getByName("gameTestServer").workingDirectory)
        check(actual.canonicalFile == nativeRunDirectory.canonicalFile) { "Native working-directory drift: $actual" }
        check(!nativeRunDirectory.resolve("world").exists()) { "Refusing existing native world: $nativeRunDirectory" }
    }
}

tasks.processResources {
    val props = mapOf(
        "minecraft_version" to project.property("minecraft_version"),
        "forge_version" to project.property("forge_version"),
        "mod_id" to project.property("mod_id"),
        "mod_name" to project.property("mod_name"),
        "mod_version" to project.property("mod_version")
    )
    inputs.properties(props)
    filesMatching(listOf("META-INF/mods.toml", "pack.mcmeta")) {
        expand(props)
    }
}

mixin {
    add(sourceSets.main.get(), "better_drinking_water.refmap.json")
    config("better_drinking_water.mixins.json")
}

apply(from = "gradle/native-compat-providers.gradle")
