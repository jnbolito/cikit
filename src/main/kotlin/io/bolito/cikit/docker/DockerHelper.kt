package io.bolito.cikit.docker

import io.bolito.cikit.docker.command.DockerBuildCommand
import io.bolito.cikit.docker.command.DockerRunCommand
import io.bolito.cikit.shell.OutputMode
import io.bolito.cikit.shell.ShellHelper
import org.gradle.api.Project
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

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

    fun run(image: String) = DockerRunCommand.Builder(shellHelper, image)
    fun build(buildPath: Path) = DockerBuildCommand.Builder(shellHelper, buildPath)
    fun build(buildPath: File) = DockerBuildCommand.Builder(shellHelper, buildPath.toPath())
    fun build(buildPath: String) = DockerBuildCommand.Builder(shellHelper, Paths.get(buildPath))
//    fun copy(source: Path) = DockerCopyCommand.Builder(shellHelper)
}
