plugins {
    `java-gradle-plugin`
    `maven-publish`
}

group = "com.github.Hanhanawa.cnwearoverlay"
version = providers.gradleProperty("version").getOrElse("1.0.1")

java {
    // 涓?xposed 鏋勫缓缁熶竴浣跨敤 Java 17銆?
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

dependencies {
    compileOnly(gradleApi())
    // 涓?xposed 鏋勫缓浣跨敤鍚屼竴 AGP 鍩虹嚎锛涜繍琛屾椂鐢变娇鐢ㄦ柟 AGP 鎻愪緵銆?
    compileOnly("com.android.tools.build:gradle-api:9.4.1")
    // ASM 鐢?AGP 鐨?instrumentation 杩愯鏃舵彁渚?
    compileOnly("org.ow2.asm:asm:9.10.1")
    // 浠呯敤浜?transformClassesWith 鐨?Function1 lambda 杩斿洖 Unit
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:2.4.20")
}

gradlePlugin {
    plugins {
        create("cnWearOverlay") {
            id = "cc.star0.wear.lib.cnwearoverlay"
            implementationClass = "cc.star0.wear.lib.cnwearoverlay.gradle.CnWearOverlayPlugin"
            displayName = "CN Wear Overlay Gradle Plugin"
            description = "Make androidx wear-compose rotary haptics work on Xiaomi/OPPO watches (build-time bytecode instrumentation)."
        }
    }
}

// ---------------------------------------------------------------------------
// 鍐呭祵杩愯鏃?jar锛氭敞鍏ュ埌浣跨敤鏂?app 涓殑杈呭姪绫讳笌 com.google.wear.input 妗╃被銆?
// 杩愯鏃舵簮鐮侀拡瀵规瀬绠€ android 妗╃被缂栬瘧锛堜笉闅?jar 鍙戝竷锛夛紝浜х墿鎵撹繘鎻掍欢璧勬簮銆?
// ---------------------------------------------------------------------------

val runtimeClassesDir = layout.buildDirectory.dir("cnwear/runtime-classes")
val embeddedDir = layout.buildDirectory.dir("cnwear/embedded")

val compileCnWearOverlayRuntime = tasks.register<JavaCompile>("compileCnWearOverlayRuntime") {
    description = "Compile CN Wear Overlay Runtime"
    source(fileTree("../runtime/src/main/java"), fileTree("../runtime/src/stubs"))
    classpath = files()
    destinationDirectory.set(runtimeClassesDir)
    options.release.set(17)
    options.encoding = "UTF-8"
}

fun embeddedRuntimeJar(taskName: String, fileName: String, vararg includes: String) =
    tasks.register<Jar>(taskName) {
        description = "Embedded CN Wear Overlay Runtime JAR"
        dependsOn(compileCnWearOverlayRuntime)
        archiveFileName.set(fileName)
        destinationDirectory.set(embeddedDir)
        from(runtimeClassesDir)
        include(*includes)
    }

val runtimeHelperJar = embeddedRuntimeJar("cnWearRuntimeHelperJar", "cnwearoverlay-runtime.jar", "cc/**")
runtimeHelperJar.configure {
    from("../runtime/src/main/resources")
    include("META-INF/proguard/**")
}
val runtimeStubJar = embeddedRuntimeJar("cnWearRuntimeStubJar", "cnwearoverlay-stub.jar", "com/**")

// 涓や釜 jar 杈撳嚭鍒板悓涓€鐩綍锛宻rcDir 鍙渶鎸備竴娆?
sourceSets.main {
    resources.srcDir(embeddedDir)
}

tasks.named("processResources") {
    dependsOn(runtimeHelperJar, runtimeStubJar)
}
