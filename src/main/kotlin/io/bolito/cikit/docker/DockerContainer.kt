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

private val DEFAULT_PROTOCOL = DockerPortArgument.Protocol.TCP

data class DockerPortArgument(
        val hostPort: Int,
        val containerPort: Int,
        val protocol: Protocol = DEFAULT_PROTOCOL
) : ShellArgument {
    override val asShellArgument: String by lazy {
        return@lazy ""
    }

    enum class Protocol {
        TCP,
        UDP
    }
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


    override val asShellArgument: String = initShellArgument()
    private fun initShellArgument(): String {
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
        return stringBuffer.toString()
    }
}


class DockerContainer(
        private val shellHelper: ShellHelper,
        private val image: String,
        private val command: String?,
        private val commandArguments: List<String>,
        private val mountArguments: List<DockerMountArgument>,
        private val removeContainer: Boolean = false
) {
    init {
        require(!image.isBlank()) { "Image name cannot be blank!" }
    }

    val shArgs: MutableList<String> = ArrayList(listOf("docker", "run"))
    fun run(name: String? = null) {
        val args = ArrayList(listOf("docker", "run"))

        if (removeContainer) {
            args.add("--rm")
        }

        if (name != null) {
            args.add("--name")
            args.add(name)
        }

        args.addAll(mountArguments.map { it.asShellArgument })
        args.add(image)
        if (command !== null) {
            args.add(command)
        }
        args.addAll(commandArguments)

        shellHelper.sh(args)
    }


    class Builder(private val shellHelper: ShellHelper) {
        private var image: String? = null
        private var removeContainer: Boolean = false
        private var command: String? = null
        private val commandArguments: MutableList<String> = ArrayList()
        private val mountArguments: MutableList<DockerMountArgument> = ArrayList()

        fun addMountArgument(mountArgument: DockerMountArgument): Builder {
            mountArguments.add(mountArgument)
            return this
        }

        fun addBindMount(source: Path, destination: Path, readOnly: Boolean = true) =
                addMountArgument(DockerMountArgument(DockerMountType.BIND, source, destination, readOnly))

        fun withCommand(command: String): Builder {
            this.command = command
            return this
        }

        fun addCommandArgument(vararg commandArgument: String): Builder {
            commandArguments.addAll(commandArgument)
            return this
        }

        fun removeContainer(): Builder {
            removeContainer = true
            return this
        }

        fun withImage(image: String): Builder {
            this.image = image
            return this
        }

        fun toContainer(): DockerContainer = DockerContainer(
                shellHelper,
                requireNotNull(image) { "Container name cannot be null!" },
                command,
                commandArguments,
                mountArguments,
                removeContainer
        )

        fun run(name: String? = null) {
            toContainer().run(name)
        }
    }
}

fun main() {
    println(DockerMountArgument(DockerMountType.BIND, "/", "/").asShellArgument)
}