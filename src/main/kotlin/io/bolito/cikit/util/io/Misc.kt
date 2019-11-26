package io.bolito.cikit.util.io

import java.nio.file.Path
import java.nio.file.Paths

fun String?.toPath(): Path? = if (this === null) null else Paths.get(this)
