package io.bolito.cikit.docker.command

import io.bolito.cikit.docker.DockerMountArgument
import io.bolito.cikit.docker.DockerMountType
import io.bolito.cikit.docker.DockerPortMappingArgument
import io.bolito.cikit.shell.ShellHelper
import io.bolito.cikit.util.io.toPath
import io.bolito.cikit.util.kotlin.returnThis
import java.io.File
import java.nio.file.Path
import java.time.Instant

data class DockerRunCommandResult(
        override val isSuccess: Boolean,
        override val timestamp: Instant = Instant.now(),
        val containerId: String
) : DockerCommandResult


class DockerRunCommand(
        private val shellHelper: ShellHelper,
        private val image: String,
        private val command: String?,
        private val commandArguments: List<String>,
        private val mountArguments: List<DockerMountArgument>,
        private val portMappingArguments: List<DockerPortMappingArgument>,
        private val removeContainer: Boolean = false,
        private val name: String? = null
) : DockerCommand<DockerRunCommandResult> {
    init {
        require(!image.isBlank()) { "Image name cannot be blank!" }
    }

    override fun execute(): DockerRunCommandResult {
        shellHelper.sh(dockerRunArgs)
        TODO()
    }

    private val dockerRunArgs: List<String> = let {
        val args = ArrayList(listOf("docker", "run"))

        // --rm
        if (removeContainer) {
            args.add("--rm")
        }

        if (!name.isNullOrBlank()) {
            args.add("--name")
            args.add(name)
        }

        // mount arguments
        args.addAll(mountArguments.map { it.asShellArgument })

        // port arguments
        args.addAll(portMappingArguments.map { it.asShellArgument })

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

        fun withCommand(command: String) = returnThis { this.command = command }

        fun addCommandArgument(vararg commandArgument: String) = returnThis { commandArguments.addAll(commandArgument) }

        fun removeContainer() = returnThis { removeContainer = true }

        fun withImage(image: String) = returnThis { this.image = image }

        fun toCommand(): DockerRunCommand = DockerRunCommand(
                shellHelper,
                requireNotNull(image) { "Container name cannot be null!" },
                command,
                commandArguments,
                mountArguments,
                portMappingArguments,
                removeContainer
        )

        fun execute() = toCommand().execute()
    }
}