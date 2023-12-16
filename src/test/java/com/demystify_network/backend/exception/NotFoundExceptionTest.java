package com.demystify_network.backend.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.demystify_network.backend.exception.NotFoundException;

class NotFoundExceptionTest {

  @Test
  @DisplayName("Ensure exception has ResponseStatus annotation and returns 404")
  void ensureExceptionHasResponseStatusAnnotationAndReturns404() {
    ResponseStatus responseStatus =
        NotFoundException.class.getDeclaredAnnotation(ResponseStatus.class);
    assertNotNull(responseStatus);

    assertEquals(responseStatus.value(), HttpStatus.NOT_FOUND);
  }
}
