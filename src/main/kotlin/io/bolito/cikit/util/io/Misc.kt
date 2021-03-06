package io.bolito.cikit.util.io

import org.gradle.api.Project
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.math.absoluteValue
import kotlin.random.Random

fun String?.toPathNullable(): Path? = if (this === null) null else Paths.get(this)
fun String.toPath(): Path = Paths.get(this)

private val TEMP_DIR_PATH = System.getProperty("java.io.tmpdir")

fun generateTempFilePath(prefix: String = "tmp", suffix: String = ""): Path =
    Paths.get(TEMP_DIR_PATH, prefix + Random.nextLong().absoluteValue + suffix).toAbsolutePath()

fun Project.copyFile(src: String, dst: String, overwrite: Boolean = false) = file(src).copyTo(file(dst), overwrite)
fun Project.moveFile(src: String, dst: String, overwrite: Boolean = false) {
    val srcPath = file(src).toPath()
    val dstPath = file(dst).toPath()
    val copyOptions = if (overwrite) arrayOf(StandardCopyOption.REPLACE_EXISTING) else emptyArray()

    Files.move(srcPath, dstPath, *copyOptions)
}

fun copyFile(src: String, dst: File, overwrite: Boolean = false) = File(src).copyTo(dst, overwrite)

/**
 * Like [Project.file] but resolves a path relative to the __root project__ directory instead.
 */
fun Project.rootFile(path: Any) = rootProject.file(path)

/**
 * This converts a path to its absolute [File] counterpart. Exists purely for preserving Gradle file referencing
 * semantics. Should be used only when file exists outside of the project directory.
 */
fun absFile(path: String) = File(path).absoluteFile