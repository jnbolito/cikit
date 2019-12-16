package io.bolito.cikit.docker.command

import io.bolito.cikit.shell.ShellArgument
import io.bolito.cikit.shell.ShellHelper
import io.bolito.cikit.util.io.generateTempFilePath
import io.bolito.cikit.util.kotlin.returnThis
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant

data class DockerBuildCommandResult(
    val imageId: String,
    val tags: Set<DockerTagArgument>,
    override val timestamp: Instant = Instant.now()
) : DockerCommandResult

data class DockerBuildArgument(val argumentName: String, val argumentValue: String, val shellQuote: String = "'") :
    ShellArgument {
    override val asShellArgument: String = "--build-arg $argumentName=$shellQuote$argumentValue$shellQuote"
}

data class DockerTagArgument(val name: String, val version: String = "latest") : ShellArgument {
    override val asShellArgument: String = "--tag $name:$version"
}

class DockerBuildCommand(
    private val shellHelper: ShellHelper,
    private val buildPath: Path,
    private val tags: Set<DockerTagArgument>,
    private val buildArguments: Set<DockerBuildArgument>
) : DockerCommand<DockerBuildCommandResult> {
    private val dockerBuildArgs: List<String> = let {
        val args = ArrayList<String>()

        // build args
        args.addAll(buildArguments.map { it.asShellArgument })

        // tags
        args.addAll(tags.asSequence().map { it.asShellArgument })

        // build path
        args.add(buildPath.toAbsolutePath().toString())
        return@let args
    }

    override fun execute(): DockerBuildCommandResult {
        val imageIdFile = File(generateTempFilePath("cikit-docker").toString())
        val args = ArrayList<String>(listOf("docker", "build", "--iidfile", imageIdFile.absolutePath))

        args.addAll(dockerBuildArgs)
        val shellResult = shellHelper.sh(args)

        if (!shellResult.isSuccess) {
            throw DockerCommandException(
                "Docker build command failed! Command:\n\t${args.joinToString(" ")}"
            )
        }

        return DockerBuildCommandResult(imageIdFile.readText(), tags)
    }

    class Builder(private val shellHelper: ShellHelper) {
        private var buildPath: Path? = null
        private val tags: MutableSet<DockerTagArgument> = HashSet()
        private val buildArguments: MutableSet<DockerBuildArgument> = HashSet()

        fun atBuildPath(buildPath: Path) = returnThis { this.buildPath = buildPath }

        fun atBuildPath(buildPath: String) = returnThis { this.buildPath = Paths.get(buildPath) }

        fun addTag(tag: DockerTagArgument) = returnThis { tags.add(tag) }

        fun addTag(tagName: String, tagVersion: String = "latest") = addTag(DockerTagArgument(tagName, tagVersion))

        fun addBuildArgument(buildArgument: DockerBuildArgument) = returnThis { buildArguments.add(buildArgument) }

        fun addBuildArgument(buildArgumentName: String, buildArgumentValue: String) =
            addBuildArgument(DockerBuildArgument(buildArgumentName, buildArgumentValue, shellHelper.shellQuote))

        fun toCommand() = DockerBuildCommand(
            shellHelper,
            requireNotNull(buildPath) { "Build path cannot be null!" },
            tags.toSet(),
            buildArguments.toSet()
        )

        fun execute() = toCommand().execute()
    }
}