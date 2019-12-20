package io.bolito.cikit.docker.command

import io.bolito.cikit.docker.DockerHelper
import io.bolito.cikit.shell.ShellHelper
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.util.*

data class DockerCopyCommandResult(override val timestamp: Instant) : DockerCommandResult
sealed class DockerCopyCommand : DockerCommand<DockerCopyCommandResult>

enum class VolumeCopyStrategy {
    SINGLE_CONTAINER,
    CREATE_AND_COPY
}

class DockerCopyToVolumeCommand(
        private val dockerHelper: DockerHelper,
        private val source: Path,
        private val dest: String,
        private val volumeName: String,
        private val volumeCopyStrategy: VolumeCopyStrategy = VolumeCopyStrategy.CREATE_AND_COPY
) : DockerCopyCommand() {
    override fun execute() = when (volumeCopyStrategy) {
        VolumeCopyStrategy.SINGLE_CONTAINER -> singleContainerCopy()
        VolumeCopyStrategy.CREATE_AND_COPY -> createAndCopy()
    }

    private fun singleContainerCopy(): DockerCopyCommandResult {
        val fileName = source.fileName?.toString() ?: "."
        val recursive = if (Files.isDirectory(source)) "-r " else ""

        val runResult = dockerHelper.run("busybox")
                .addBindMount(source, "/bind-mount")
                .addNamedVolume(volumeName, "/volume", false)
                .withCommand("cp $recursive/bind-mount/$fileName /volume/$dest")
                .execute(true)

        return DockerCopyCommandResult(runResult.timestamp)
    }

    private fun createAndCopy(): DockerCopyCommandResult {
        val containerUiid = UUID.randomUUID().toString()
        val shellHelper = dockerHelper.shellHelper

//        shellHelper.sh("docker", "container", "create", )
        TODO()
    }
}
