package io.bolito.cikit.docker

import io.bolito.cikit.shell.ShellArgument
import io.bolito.cikit.shell.ShellHelper
import io.bolito.cikit.util.io.toPath
import java.nio.file.Path
import java.nio.file.Paths

enum class DockerMountType(val mountTypeName: String) {
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
        val volumeOptions: List<DockerVolumeOption> = emptyList()
) : ShellArgument {
    constructor(
            type: DockerMountType,
            source: String?,
            destination: String,
            readOnly: Boolean = true,
            volumeOptions: List<DockerVolumeOption> = emptyList()
    ) : this(type, source.toPath(), Paths.get(destination), readOnly, volumeOptions)

    init {
        // only volumes may omit the source option
        require(source !== null || type != DockerMountType.VOLUME) {
            "Mount source is only allowed to be empty when mount is of type VOLUME! (option: $this)"
        }
    }

    override val asShellArgument: String by lazy {
        val stringBuffer = StringBuffer("--mount ")
        stringBuffer.append(" 'type=${type.mountTypeName}")

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
        private val image: String,
        private val commandArguments: List<String>,
        private val mountArguments: List<DockerMountArgument>,
        private val removeContainer: Boolean = false
) {
    val shArgs: MutableList<String> = ArrayList(listOf("docker", "run"))
    fun run() {
        val args = ArrayList(listOf("docker", "run"))

        if (removeContainer) {
            args.add("--rm")
        }

        args.addAll(mountArguments.map { it.asShellArgument })
        args.add(image)
        args.addAll(commandArguments)

        shellHelper.sh(args)
    }

    class Builder(private val shellHelper: ShellHelper) {
        private var image: String? = null
        private var removeContainer: Boolean = false

        private val containerArguments: MutableList<String> = ArrayList()
        private val mountArguments: MutableList<DockerMountArgument> = ArrayList()

        fun addMountArgument(mountArgument: DockerMountArgument): Builder {
            mountArguments.add(mountArgument)

            return this
        }

        fun addBindMount(source: Path, destination: Path, readOnly: Boolean = true) =
                addMountArgument(DockerMountArgument(DockerMountType.BIND, source, destination, readOnly))

        fun addCommandArgument(containerArgument: String): Builder {
            containerArguments.add(containerArgument)

            return this
        }

        fun removeContainer() {
            removeContainer = true
        }

        fun withImage(image: String): Builder {
            this.image = image

            return this
        }

        fun build(): DockerRunner = DockerRunner(
                shellHelper,
                requireNotNull(image) { "Container name cannot be null!" },
                containerArguments,
                mountArguments,
                removeContainer
        )
    }
}