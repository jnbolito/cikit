package io.bolito.cikit.docker.command

import java.time.Instant

interface DockerCommandResult {
    val timestamp: Instant
}

interface DockerCommand<out R : DockerCommandResult> {
    fun execute(): R
}

class DockerCommandException : Exception {
    constructor() : super()
    constructor(p0: String?) : super(p0)
    constructor(p0: String?, p1: Throwable?) : super(p0, p1)
    constructor(p0: Throwable?) : super(p0)
    constructor(p0: String?, p1: Throwable?, p2: Boolean, p3: Boolean) : super(p0, p1, p2, p3)
}
