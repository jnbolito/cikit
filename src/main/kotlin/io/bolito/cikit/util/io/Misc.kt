package io.bolito.cikit.util.io

import java.nio.file.Path
import java.nio.file.Paths
import kotlin.math.absoluteValue
import kotlin.random.Random

fun String?.toPathNullable(): Path? = if (this === null) null else Paths.get(this)
fun String.toPath(): Path = Paths.get(this)

private val TEMP_DIR_PATH = System.getProperty("java.io.tmpdir")

fun generateTempFilePath(prefix: String = "tmp", suffix: String = ""): Path =
    Paths.get(TEMP_DIR_PATH, prefix + Random.nextLong().absoluteValue + suffix).toAbsolutePath()