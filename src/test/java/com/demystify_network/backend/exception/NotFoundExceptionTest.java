package com.demystify_network.backend.exception;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

class NotFoundExceptionTest {

  @Test
  @DisplayName("Ensure exception has ResponseStatus annotation and returns 404")
  void ensureExceptionHasResponseStatusAnnotationAndReturns404() {
    ResponseStatus responseStatus =
        NotFoundException.class.getDeclaredAnnotation(ResponseStatus.class);
    assertThat(responseStatus).isNotNull();

    assertThat(HttpStatus.NOT_FOUND).isEqualTo(responseStatus.value());
  }
}
