package io.bolito.cikit.docker

import io.bolito.cikit.docker.command.DockerBuildCommand
import io.bolito.cikit.docker.command.DockerRunCommand
import io.bolito.cikit.shell.OutputMode
import io.bolito.cikit.shell.ShellHelper
import org.gradle.api.Project

class DockerHelper(
    private val project: Project,
    private val shellHelper: ShellHelper = ShellHelper(project = project)
) {
    val clientVersion: String by lazy {
        shellHelper.sh(OutputMode.STRING, "docker", "version", "--format", "'{{.Client.Version}}'")
            .standardOutput
            .trim()
    }

    val serverVersion: String by lazy {
        shellHelper.sh(OutputMode.STRING, "docker", "version", "--format", "'{{.Server.Version}}'")
            .standardOutput
            .trim()
    }

    val container: DockerContainer.Builder = DockerContainer.Builder(shellHelper)
    fun run() = DockerRunCommand.Builder(shellHelper)
    fun build() = DockerBuildCommand.Builder(shellHelper)
}
