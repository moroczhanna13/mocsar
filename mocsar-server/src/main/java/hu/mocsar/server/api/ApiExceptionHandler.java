package hu.mocsar.server.api;

import hu.mocsar.core.IllegalMoveException;
import hu.mocsar.server.api.dto.ErrorResponse;
import hu.mocsar.server.room.RoomException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(RoomException.class)
    public ResponseEntity<ErrorResponse> room(RoomException e) {
        return ResponseEntity.status(e.status()).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler({IllegalMoveException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponse> conflict(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<ErrorResponse> badRequest(Exception e) {
        String message = e instanceof MethodArgumentNotValidException ? "Hibás kérés: becenév kötelező, legfeljebb 32 karakter"
                : e.getMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(message));
    }
}
