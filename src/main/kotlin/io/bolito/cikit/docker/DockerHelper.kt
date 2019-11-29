package io.bolito.cikit.docker

import io.bolito.cikit.shell.OutputMode
import io.bolito.cikit.shell.ShellHelper
import org.gradle.api.Project

class DockerHelper(
    private val project: Project,
    internal val shellHelper: ShellHelper = ShellHelper(project = project)
) {
    val clientVersion: String by lazy {
        shellHelper.sh(OutputMode.STRING, "docker", "version", "--format", "'{{.Client.Version}}'").trim()
    }

    val serverVersion: String by lazy {
        shellHelper.sh(OutputMode.STRING, "docker", "version", "--format", "'{{.Server.Version}}'").trim()
    }

    val container: DockerContainer.Builder = DockerContainer.Builder(shellHelper)
}
