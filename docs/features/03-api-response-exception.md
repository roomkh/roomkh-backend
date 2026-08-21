# docs/features/03-api-response-exception.md

# Feature: API Response and Exception Handling

**Status:** Completed

## Purpose

Guarantee that every API response, success or failure, follows one consistent JSON structure across the entire application.

## Main Files/Components Involved

- `ApiResponse<T>`
- `ValidationErrorResponse`
- `ResourceNotFoundException`
- `BadRequestException`
- `DuplicateResourceException`
- `GlobalExceptionHandler`

## Database Changes

None.

## API Changes

None directly — this establishes the response contract used by every future endpoint.

## Business Rules

- Successful responses always include `success`, `message`, and `data`.
- Validation errors return field-level error messages under `data`.
- No internal exception details or stack traces are ever returned to the client.

## Security Considerations

- Preventing leakage of internal error details (such as database exception messages) reduces information available to a potential attacker.

## Testing Checklist

- [x] Success response matches the standard format
- [x] Validation error response returns field-level messages
- [x] Unhandled exceptions return a generic 500 message without internal details

## Related Commit Message
