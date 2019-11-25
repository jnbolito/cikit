package io.bolito.cikit.shell

import org.gradle.api.Project
import java.io.OutputStream

private val DEFAULT_SHELL_ARGS = listOf("sh", "-c")

private object NullOutputStream : OutputStream() {
    override fun write(byte: Int) {}
}

private fun internalSh(printToStdout: Boolean, project: Project, shellCommandArgs: List<String>, vararg argument: String) {
    val args = ArrayList(shellCommandArgs)
    args.add(argument.joinToString(" "))
    project.exec {
        it.commandLine(args)
        if (!printToStdout) {
            it.standardOutput = NullOutputStream
        }
    }
}

class ShellHelper(private val shellCommandArgs: List<String> = DEFAULT_SHELL_ARGS, private val project: Project) {
    fun sh(printToStdout: Boolean = true, vararg argument: String) = internalSh(printToStdout, project, shellCommandArgs, *argument)
}

fun Project.sh(printToStdout: Boolean, vararg argument: String) = internalSh(printToStdout, this, DEFAULT_SHELL_ARGS, *argument)

fun Project.sh(vararg argument: String) = internalSh(true, this, DEFAULT_SHELL_ARGS, *argument)
