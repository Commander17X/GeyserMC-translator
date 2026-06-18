plugins {
    id("geyser.base-conventions")
    id("me.champeau.jmh") version "0.7.2"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    jmhImplementation(projects.core)
    jmhAnnotationProcessor(libs.jmh.generator.annprocess)
    jmhImplementation(libs.bundles.jmh)
}

jmh {
    jmhVersion.set(libs.versions.jmh.get())
    warmupIterations.set(2)
    iterations.set(5)
    fork.set(1)
    benchmarkMode.set(listOf("avgt"))
    timeUnit.set("ns")
    resultFormat.set("TEXT")
}
