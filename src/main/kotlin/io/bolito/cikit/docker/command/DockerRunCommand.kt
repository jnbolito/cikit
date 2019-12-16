package io.bolito.cikit.docker.command

import io.bolito.cikit.docker.DockerMountArgument
import io.bolito.cikit.docker.DockerMountType
import io.bolito.cikit.docker.DockerPortMappingArgument
import io.bolito.cikit.shell.ShellArgument
import io.bolito.cikit.shell.ShellHelper
import io.bolito.cikit.util.io.generateTempFilePath
import io.bolito.cikit.util.io.toPath
import io.bolito.cikit.util.kotlin.returnThis
import java.io.File
import java.nio.file.Path
import java.time.Instant

data class DockerRunCommandResult(
    val containerId: String,
    override val timestamp: Instant = Instant.now()
) : DockerCommandResult {
    val shortContainerId = containerId.substring(0, 13)

    override fun toString(): String {
        return "DockerRunCommandResult(containerId='$containerId', timestamp=$timestamp, shortContainerId='$shortContainerId')"
    }
}

data class DockerEnvVarArgument(
    val name: String,
    val value: String,
    val shellQuote: String = "'"
) : ShellArgument {
    override val asShellArgument: String = "-e $name=$shellQuote$value$shellQuote"
}

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
    private val cidFilePrefix: String = "cikit-docker"
) : DockerCommand<DockerRunCommandResult> {
    init {
        require(!image.isBlank()) { "Image name cannot be blank!" }
    }

    override fun execute(): DockerRunCommandResult = execute(emptyMap())

    fun execute(envVars: Map<String, String>): DockerRunCommandResult {
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


    class Builder(private val shellHelper: ShellHelper) {
        private var image: String? = null
        private var removeContainer: Boolean = false
        private var command: String? = null
        private val commandArguments: MutableList<String> = ArrayList()
        private val mountArguments: MutableList<DockerMountArgument> = ArrayList()
        private val portMappingArguments: MutableList<DockerPortMappingArgument> = ArrayList()
        private val envVarArguments: MutableSet<DockerEnvVarArgument> = HashSet()

        fun addMountArgument(mountArgument: DockerMountArgument) = returnThis { mountArguments.add(mountArgument) }

        fun addBindMount(source: Path, destination: Path, readOnly: Boolean = true) =
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
                    destination.toPath(),
                    readOnly
                )
            )

        fun addEnvironmentVariable(envVarArgument: DockerEnvVarArgument) =
            returnThis { envVarArguments.add(envVarArgument) }

        fun addEnvironmentVariable(varName: String, varValue: String) =
            addEnvironmentVariable(DockerEnvVarArgument(varName, varValue, shellHelper.shellQuote))

        fun withCommand(command: String) = returnThis { this.command = command }

        fun addCommandArgument(vararg commandArgument: String) = returnThis { commandArguments.addAll(commandArgument) }

        fun removeContainer() = returnThis { removeContainer = true }

        fun withImage(image: String) = returnThis { this.image = image }

        fun toCommand(): DockerRunCommand = DockerRunCommand(
            shellHelper,
            requireNotNull(image) { "Container name cannot be null!" },
            command,
            commandArguments.toList(),
            mountArguments.toList(),
            portMappingArguments.toList(),
            envVarArguments.toSet(),
            removeContainer
        )

        fun execute() = toCommand().execute()
    }
}