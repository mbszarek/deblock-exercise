package org.deblock.exercise

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.ComponentScan.Filter
import org.springframework.context.annotation.FilterType

@SpringBootApplication
@ComponentScan(
        excludeFilters = [
            Filter(
                    type = FilterType.REGEX,
                    pattern = ["org.deblock.exercise.infrastructure.adapters.flights.crazyair.*", "org.deblock.exercise.infrastructure.adapters.flights.toughjet.*"]
            )
        ]
)
class ExerciseApplication

fun main(args: Array<String>) {
    runApplication<ExerciseApplication>(*args)
}