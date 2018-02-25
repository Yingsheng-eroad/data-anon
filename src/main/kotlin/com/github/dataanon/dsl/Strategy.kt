package com.github.dataanon.dsl

import com.github.dataanon.jdbc.TableReader
import com.github.dataanon.jdbc.TableWriter
import com.github.dataanon.model.DbConfig
import com.github.dataanon.model.Table
import com.github.dataanon.utils.ProgressBarGenerator
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.util.concurrent.CountDownLatch



abstract class Strategy {
    protected val tables = mutableListOf<Table>()

    fun execute(progressBarEnabled: Boolean = true) {
        val latch = CountDownLatch(tables.size)

        Flux.fromIterable(tables)
                .parallel(tables.size)
                .runOn(Schedulers.parallel())
                .subscribe { table -> executeOnTable(table, progressBarEnabled, latch) }

        latch.await()
    }

    private fun executeOnTable(table: Table, progressBarEnabled: Boolean, latch: CountDownLatch) {
        try {
            val reader      = TableReader(sourceDbConfig(), table)
            val progressBar = ProgressBarGenerator(progressBarEnabled, table.name, { reader.totalNoOfRecords() })
            val writer      = TableWriter(destDbConfig(), table, progressBar)

            Flux.fromIterable(Iterable { reader }).map { table.execute(it) }.subscribe(writer)
        } finally {
            latch.countDown()
        }
    }

    abstract protected fun sourceDbConfig(): DbConfig
    abstract protected fun destDbConfig():   DbConfig
}