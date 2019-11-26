package io.bolito.cikit.docker

import java.lang.RuntimeException

class DockerException: RuntimeException {
    constructor() : super()
    constructor(p0: String?) : super(p0)
    constructor(p0: String?, p1: Throwable?) : super(p0, p1)
    constructor(p0: Throwable?) : super(p0)
}