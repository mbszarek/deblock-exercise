package org.deblock.exercise.infrastructure.adapters.flights.web

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler


@ControllerAdvice
class ExceptionHandling {

    @ExceptionHandler(value = [IllegalArgumentException::class])
    fun illegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ErrorMessage> {
        val errorMessage = ErrorMessage(
                message = ex.message
        )
        return ResponseEntity(errorMessage, HttpStatus.BAD_REQUEST)
    }
}

data class ErrorMessage(
        val message: String?
)