package io.bolito.cikit.shell

import io.bolito.cikit.internal.util.io.NullOutputStream
import io.bolito.cikit.internal.util.io.TeeOutputStream
import org.gradle.api.Project
import org.gradle.process.ExecSpec
import java.io.ByteArrayOutputStream
import java.io.OutputStream

private val DEFAULT_SHELL_ARGS = listOf("sh", "-c")
private val DEFAULT_OUTPUT_MODE = OutputMode.STDOUT

interface ShellArgument {
    val asShellArgument: String
}

data class ShellResult(
    val exitCode: Int,
    val standardOutput: String
) {
    val isSuccess: Boolean = exitCode == 0
}

interface OutputCapturer {
    fun getCapturedOutput(): String
}

object EmptyOutputCapturer : OutputCapturer {
    override fun getCapturedOutput(): String = ""
}

class ByteArrayOutputCapturer(private val outputStream: ByteArrayOutputStream) : OutputCapturer {
    override fun getCapturedOutput(): String = outputStream.use {
        return it.toString()
    }
}

data class ShellOutputData(
    val outputStream: OutputStream,
    val outputCapturer: OutputCapturer = EmptyOutputCapturer
)

enum class OutputMode(val shellOutputDataFactory: () -> ShellOutputData) {
    NONE({ ShellOutputData(NullOutputStream, EmptyOutputCapturer) }),
    STDOUT({ ShellOutputData(System.out, EmptyOutputCapturer) }),
    STRING({
        val byteOutputStream = ByteArrayOutputStream()
        ShellOutputData(byteOutputStream, ByteArrayOutputCapturer(byteOutputStream))
    }),
    STRING_AND_STDOUT({
        val byteOutputStream = ByteArrayOutputStream()
        ShellOutputData(
            TeeOutputStream(
                System.out,
                byteOutputStream
            ), ByteArrayOutputCapturer(byteOutputStream)
        )
    })
}

private val DO_NOTHING_ACTION: (ExecSpec) -> Unit = {}

private fun internalSh(
    outputMode: OutputMode,
    project: Project,
    shellCommandArgs: List<String>,
    customAction: (ExecSpec) -> Unit = DO_NOTHING_ACTION,
    vararg argument: String
): ShellResult {
    val args = ArrayList(shellCommandArgs)
    args.add(argument.joinToString(" "))

    val (outputStream, outputCapturer) = outputMode.shellOutputDataFactory()
    val exitValue = project.exec {
        it.workingDir(project.rootDir)
        customAction(it)
        it.commandLine(args)
        it.standardOutput = outputStream
    }.exitValue

    return ShellResult(exitValue, outputCapturer.getCapturedOutput())
}

class ShellHelper(
    val shellQuote: String = "'",
    private val shellCommandArgs: List<String> = DEFAULT_SHELL_ARGS,
    private val project: Project
) {
    fun sh(outputMode: OutputMode, vararg argument: String, customAction: (ExecSpec) -> Unit = DO_NOTHING_ACTION) =
        internalSh(outputMode, project, shellCommandArgs, customAction, *argument)

    fun sh(outputMode: OutputMode, arguments: List<String>, customAction: (ExecSpec) -> Unit = DO_NOTHING_ACTION) =
        internalSh(outputMode, project, shellCommandArgs, customAction, *arguments.toTypedArray())

    fun sh(arguments: List<String>, customAction: (ExecSpec) -> Unit = DO_NOTHING_ACTION) =
        internalSh(DEFAULT_OUTPUT_MODE, project, shellCommandArgs, customAction, *arguments.toTypedArray())

    fun sh(vararg argument: String, customAction: (ExecSpec) -> Unit = DO_NOTHING_ACTION) =
        internalSh(DEFAULT_OUTPUT_MODE, project, shellCommandArgs, customAction, *argument)
}

fun Project.sh(outputMode: OutputMode, vararg argument: String, customAction: (ExecSpec) -> Unit = DO_NOTHING_ACTION) =
    internalSh(outputMode, this, DEFAULT_SHELL_ARGS, customAction, *argument)

fun Project.sh(vararg argument: String, customAction: (ExecSpec) -> Unit = DO_NOTHING_ACTION) =
    internalSh(DEFAULT_OUTPUT_MODE, this, DEFAULT_SHELL_ARGS, customAction, *argument)

fun Project.sh(outputMode: OutputMode, arguments: List<String>, customAction: (ExecSpec) -> Unit = DO_NOTHING_ACTION) =
    internalSh(outputMode, this, DEFAULT_SHELL_ARGS, customAction, *arguments.toTypedArray())

fun Project.sh(arguments: List<String>, customAction: (ExecSpec) -> Unit = DO_NOTHING_ACTION) =
    internalSh(DEFAULT_OUTPUT_MODE, this, DEFAULT_SHELL_ARGS, customAction, *arguments.toTypedArray())
