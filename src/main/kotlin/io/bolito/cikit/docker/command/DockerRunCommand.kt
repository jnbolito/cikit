package io.bolito.cikit.docker.command

import io.bolito.cikit.shell.ShellArgument
import io.bolito.cikit.shell.ShellHelper
import io.bolito.cikit.util.io.generateTempFilePath
import io.bolito.cikit.util.io.toPath
import io.bolito.cikit.internal.util.kotlin.returnThis
import java.io.File
import java.nio.file.Path
import java.time.Instant


class DockerRunCommand(
        private val shellHelper: ShellHelper,
        private val image: String,
        private val command: String?,
        private val commandArguments: List<String>,
        private val mountArguments: List<DockerMountArgument>,
        private val portMappingArguments: List<DockerPortMappingArgument>,
        private val environmentVariables: Set<DockerEnvVarArgument>,
        private val removeContainer: Boolean = false,
        private val name: String? = null,
        private val workDir: String? = null,
        private val cidFilePrefix: String = "cikit-docker"
) : DockerCommand<DockerRunCommandResult> {
    init {
        require(!image.isBlank()) { "Image name cannot be blank!" }
    }

    override fun execute(): DockerRunCommandResult = execute(emptyMap())

    fun execute(envVars: Map<String, Any>): DockerRunCommandResult {
        // note that unlike createTempFile this DOESN'T actually create the file. This is important as docker won't
        // let you do anything if the file already exists.
        val cidFile = File(generateTempFilePath(cidFilePrefix).toString())

        try {
            val args = ArrayList<String>(listOf("docker", "run", "--cidfile", cidFile.absolutePath))

            // 'dynamic' env vars
            args.addAll(
                    envVars.entries.asSequence()
                            .map { DockerEnvVarArgument(it.key, it.value, shellHelper.shellQuote).asShellArgument }
            )

            args.addAll(dockerRunArgs)

            val shellResult = shellHelper.sh(args)
            if (!shellResult.isSuccess) {
                throw DockerCommandException(
                        "Docker run command failed! Command:\n\t${args.joinToString(" ")}"
                )
            }

            return DockerRunCommandResult(cidFile.readText())
        } finally {
            cidFile.delete()
        }
    }

    private val dockerRunArgs: List<String> = let {
        val args = ArrayList<String>()

        // --rm
        if (removeContainer) {
            args.add("--rm")
        }

        if (!name.isNullOrBlank()) {
            args.add("--name")
            args.add(name)
        }

        // mount arguments
        args.addAll(mountArguments.asSequence().map { it.asShellArgument })

        // port arguments
        args.addAll(portMappingArguments.asSequence().map { it.asShellArgument })

        // 'static' environment variables
        args.addAll(environmentVariables.asSequence().map { it.asShellArgument })


        // with working directory
        if (workDir !== null) {
            args.add("--workdir")
            args.add(workDir)
        }

        // with image
        args.add(image)

        // with command
        if (command !== null) {
            args.add(command)
        }

        //with command args
        args.addAll(commandArguments)
        return@let args
    }


    class Builder(private val shellHelper: ShellHelper, private val image: String) {
        private var removeContainerAfterRun: Boolean = false
        private var command: String? = null
        private val commandArguments: MutableList<String> = ArrayList()
        private val mountArguments: MutableList<DockerMountArgument> = ArrayList()
        private val portMappingArguments: MutableList<DockerPortMappingArgument> = ArrayList()
        private val envVarArguments: MutableSet<DockerEnvVarArgument> = HashSet()
        private var workDir: String? = null

        fun addMountArgument(mountArgument: DockerMountArgument) = returnThis { mountArguments.add(mountArgument) }

        fun addBindMount(source: File, destination: String, readOnly: Boolean = true) =
                addMountArgument(
                        DockerMountArgument(
                                DockerMountType.BIND,
                                source.toPath(),
                                destination,
                                readOnly
                        )
                )

        fun addBindMount(source: Path, destination: String, readOnly: Boolean = true) =
                addMountArgument(
                        DockerMountArgument(
                                DockerMountType.BIND,
                                source,
                                destination,
                                readOnly
                        )
                )

        fun addBindMount(source: String, destination: String, readOnly: Boolean = true) =
                addMountArgument(
                        DockerMountArgument(
                                DockerMountType.BIND,
                                source.toPath(),
                                destination,
                                readOnly
                        )
                )

        fun addNamedVolume(volumeName: String, destination: String, readOnly: Boolean = true) =
                addMountArgument(DockerMountArgument(DockerMountType.VOLUME, volumeName, destination, readOnly))

        fun addEnvVar(envVarArgument: DockerEnvVarArgument) =
                returnThis { envVarArguments.add(envVarArgument) }

        fun addEnvVar(varName: String, varValue: Any) =
                addEnvVar(DockerEnvVarArgument(varName, varValue, shellHelper.shellQuote))

        fun addPortMapping(portMappingArgument: DockerPortMappingArgument) =
                returnThis { portMappingArguments.add(portMappingArgument) }

        fun addPortMapping(
                hostPort: Int,
                containerPort: Int,
                protocol: DockerPortMappingArgument.Protocol = DOCKER_DEFAULT_PROTOCOL
        ) = addPortMapping(DockerPortMappingArgument(hostPort, containerPort, protocol))

        fun withCommand(command: String) = returnThis { this.command = command }

        fun addCommandArgument(vararg commandArgument: String) = returnThis { commandArguments.addAll(commandArgument) }

        fun removeContainerAfterRun() = returnThis { removeContainerAfterRun = true }

        fun withWorkDir(workDir: String) = returnThis { this.workDir = workDir }

        fun withEnvVars(envVars: Map<String, Any>)
                = returnThis { envVars.asSequence().forEach { addEnvVar(it.key, it.value) } }

        fun toCommand(): DockerRunCommand = DockerRunCommand(
                shellHelper,
                image,
                command,
                commandArguments.toList(),
                mountArguments.toList(),
                portMappingArguments.toList(),
                envVarArguments.toSet(),
                removeContainerAfterRun,
                workDir = workDir
        )

        fun execute(removeContainer: Boolean = false): DockerRunCommandResult {
            if (removeContainer) {
                removeContainerAfterRun()
            }

            return toCommand().execute()
        }
    }
}

data class DockerRunCommandResult(
        val containerId: String,
        override val timestamp: Instant = Instant.now()
) : DockerCommandResult {
    val shortContainerId = containerId.substring(0, 13)

    override fun toString(): String {
        return "DockerRunCommandResult(containerId='$containerId', timestamp=$timestamp, shortContainerId='$shortContainerId')"
    }
}

enum class DockerMountType(val mountTypeName: String) {
    BIND("bind"),
    VOLUME("volume"),
    TMPFS("tmpfs")
}

data class DockerVolumeOption(val name: String, val value: String) {
    val asOption: String = "volume-opt=$name=$value"
}

val DOCKER_DEFAULT_PROTOCOL = DockerPortMappingArgument.Protocol.TCP

data class DockerPortMappingArgument(
        val hostPort: Int,
        val containerPort: Int,
        val protocol: Protocol = DOCKER_DEFAULT_PROTOCOL
) : ShellArgument {
    override val asShellArgument: String = "--mount $hostPort:$containerPort/${protocol.protocolName}"

    enum class Protocol(val protocolName: String) {
        TCP("tcp"),
        UDP("udp")
    }
}


data class DockerEnvVarArgument(
        val name: String,
        val value: Any,
        val shellQuote: String = "'"
) : ShellArgument {
    override val asShellArgument: String = "-e $name=$shellQuote$value$shellQuote"
}


data class DockerMountArgument(
        val type: DockerMountType,
        val source: String?,
        val destination: String,
        val readOnly: Boolean = true,
        val volumeOptions: List<DockerVolumeOption> = emptyList()
) : ShellArgument {
    constructor(
            type: DockerMountType,
            source: Path?,
            destination: String,
            readOnly: Boolean = true,
            volumeOptions: List<DockerVolumeOption> = emptyList()
    ) : this(type, source?.toAbsolutePath().toString(), destination, readOnly, volumeOptions)

    init {
        // only volumes may omit the source option
        require(source !== null || type != DockerMountType.VOLUME) {
            "Mount source is only allowed to be empty when mount is of type VOLUME! (option: $this)"
        }
    }


    override val asShellArgument: String = let {
        val stringBuffer = StringBuffer("--mount ")
        stringBuffer.append("'type=${type.mountTypeName}")

        if (source !== null) {
            stringBuffer.append(",src=$source")
        }
        stringBuffer.append(",dst=$destination")
        if (readOnly) {
            stringBuffer.append(",readonly")
        }
        if (volumeOptions.isNotEmpty()) {
            stringBuffer.append(volumeOptions.joinToString(separator = ",", prefix = ",") { it.asOption })
        }

        stringBuffer.append("'")
        return@let stringBuffer.toString()
    }
}