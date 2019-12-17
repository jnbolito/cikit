package io.bolito.cikit.docker.command

import io.bolito.cikit.shell.ShellHelper
import java.nio.file.Path
import java.time.Instant

data class DockerCopyCommandResult(override val timestamp: Instant): DockerCommandResult
sealed class DockerCopyCommand(val shellHelper: ShellHelper) : DockerCommand<DockerCopyCommandResult>

//class DockerCopyToVolumeCommand(
//        shellHelper: ShellHelper,
//        private val source: Path,
//
//        ): DockerCopyCommand(shellHelper) {
//    override fun execute(): DockerCopyCommandResult {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//}