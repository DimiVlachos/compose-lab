package dev.dimvlachos.lab.core.demo

import co.touchlab.kermit.Logger

internal object RecordingLog {
    private val logger = Logger.withTag("LabRecorder")

    fun started(id: String) {
        logger.i { "LAB_DEMO_START $id" }
    }

    fun done(id: String) {
        logger.i { "LAB_DEMO_DONE $id" }
    }

    fun unknownDemo(id: String) {
        logger.w { "LAB_WARN unknown demo '$id', showing the catalog" }
    }
}
