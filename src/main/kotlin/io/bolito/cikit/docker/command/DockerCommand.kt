package io.bolito.cikit.docker.command

import java.time.Instant

interface DockerCommandResult {
    val isSuccess: Boolean
    val timestamp: Instant
}

interface DockerCommand<out R : DockerCommandResult> {
    fun execute(): R
}