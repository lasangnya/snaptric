// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}

// Some drives (exFAT/FAT, e.g. many USB disks) make macOS write a hidden "._" file next to every
// file, which stops Gradle from cleaning its build folders. Setting `snaptric.buildRoot` in
// local.properties moves all build output to a folder on another disk, e.g.
//   snaptric.buildRoot=/Users/you/.gradle-builds/snaptric
val buildRoot = rootProject.file("local.properties")
    .takeIf { it.exists() }
    ?.let { file -> java.util.Properties().apply { file.inputStream().use(::load) } }
    ?.getProperty("snaptric.buildRoot")

if (buildRoot != null) {
    allprojects {
        layout.buildDirectory.set(file("$buildRoot/${if (this == rootProject) "root" else name}"))
    }
}
