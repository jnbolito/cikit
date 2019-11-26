package io.bolito.cikit.docker

import io.bolito.cikit.shell.ShellArgument
import io.bolito.cikit.shell.ShellHelper
import io.bolito.cikit.util.io.toPath
import java.nio.file.Path
import java.nio.file.Paths

enum class DockerMountType(val stringValue: String) {
    BIND("bind"),
    VOLUME("volume"),
    TMPFS("tmpfs")
}

data class DockerVolumeOption(val name: String, val value: String) {
    val asOption: String = "volume-opt=$name=$value"
}

data class DockerMountArgument(
    val type: DockerMountType,
    val source: Path?,
    val destination: Path,
    val readOnly: Boolean = true,
    val volumeOptions: List<DockerVolumeOption>
) : ShellArgument {
    constructor(
        type: DockerMountType,
        source: String?,
        destination: String,
        readOnly: Boolean = true,
        volumeOptions: List<DockerVolumeOption>
    ) : this(type, source.toPath(), Paths.get(destination), readOnly, volumeOptions)

    init {
        // only volumes may omit the source option
        require(source !== null || type != DockerMountType.VOLUME) {
            "Mount source is only allowed to be empty when mount is of type VOLUME! (option: $this)"
        }
    }

    override val asShellArgument: String by lazy {
        val stringBuffer = StringBuffer("--mount ")
        stringBuffer.append(" 'type=${type.stringValue}")

        if (source !== null) {
            stringBuffer.append(",src=${source.toAbsolutePath()}")
        }
        stringBuffer.append("dst=$destination")
        if (readOnly) {
            stringBuffer.append(",readonly")
        }
        if (volumeOptions.isNotEmpty()) {
            stringBuffer.append(volumeOptions.joinToString(separator = ",", prefix = ",") { it.asOption })
        }
        return@lazy stringBuffer.toString()
    }
}

class DockerRunner(
    private val shellHelper: ShellHelper,
    private val containerName: String,
    private val containerArgs: List<String>,
    private val mountArguments: List<DockerMountArgument>,
    private val removeContainer: Boolean = false
) {
    val shArgs: MutableList<String> = ArrayList(listOf("docker", "run"))
    fun run() {

    }
}