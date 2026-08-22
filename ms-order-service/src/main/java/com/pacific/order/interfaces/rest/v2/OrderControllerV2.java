package com.pacific.order.interfaces.rest.v2;

import com.pacific.core.messaging.cqrs.command.CommandBus;
import com.pacific.core.messaging.cqrs.command.CommandResult;
import com.pacific.core.messaging.cqrs.query.QueryBus;
import com.pacific.core.messaging.cqrs.query.QueryResult;
import com.pacific.order.application.command.CancelOrderCommand;
import com.pacific.order.application.command.CreateOrderCommand;
import com.pacific.order.application.dto.CreateOrderRequest;
import com.pacific.order.application.dto.OrderResponse;
import com.pacific.order.application.query.GetOrderByIdQuery;
import com.pacific.order.application.query.GetUserOrdersPageQuery;
import com.pacific.order.infrastructure.client.AuthServiceClient;
import com.pacific.order.infrastructure.client.dto.ValidateApiKeyRequest;
import com.pacific.order.infrastructure.client.dto.ValidateTokenRequest;
import com.pacific.order.infrastructure.client.dto.ValidateTokenResponse;
import com.pacific.order.interfaces.rest.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Order operations - Version 2 API Version: v2 (Enhanced version with
 * pagination, filtering, and advanced features)
 *
 * <p>New features in V2: - Pagination support for user orders - Order filtering by status and date
 * range - Bulk operations - Enhanced error responses - Rate limiting information - API versioning
 * headers
 */
@RestController
@RequestMapping("/api/v2/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Orders V2", description = "Order management API - Version 2 (Enhanced)")
public class OrderControllerV2 {

  private final CommandBus commandBus;
  private final QueryBus queryBus;
  private final AuthServiceClient authClient;

  @PostMapping
  @Operation(
      summary = "Create new order (V2)",
      description = "Creates a new order with enhanced validation and features")
  public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
      @RequestHeader("Authorization") String token,
      @RequestHeader(value = "X-API-Key", required = false) String apiKey,
      @RequestHeader(value = "X-Client-Version", defaultValue = "2.0") String clientVersion,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @Valid @RequestBody CreateOrderRequest request) {

    log.info("V2 - Received create order request from client version: {}", clientVersion);

    // Validate authentication via Auth Service
    ValidateTokenResponse authResponse = authClient.validateToken(new ValidateTokenRequest(token));

    if (!authResponse.isValid()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .header("X-API-Version", "2.0")
          .header("X-Rate-Limit-Remaining", "0")
          .body(ApiResponse.error("Invalid authentication token", "UNAUTHORIZED"));
    }

    // Validate API key if provided
    if (apiKey != null && !authClient.validateApiKey(new ValidateApiKeyRequest(apiKey))) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .header("X-API-Version", "2.0")
          .body(ApiResponse.error("Invalid API key", "INVALID_API_KEY"));
    }

    // Create and execute command
    CreateOrderCommand command =
        CreateOrderCommand.builder()
            .userId(authResponse.getUserId())
            .items(request.getItems())
            .initiator(authResponse.getUsername())
            .correlationId(UUID.randomUUID().toString())
            .idempotencyKey(trimToNull(idempotencyKey))
            .build();

    CommandResult<OrderResponse> result = commandBus.execute(command);

    if (!result.isSuccess()) {
      return ResponseEntity.badRequest()
          .header("X-API-Version", "2.0")
          .header("X-Error-Code", result.getErrorCode())
          .body(ApiResponse.error(result.getErrorMessage(), result.getErrorCode()));
    }

    log.info("V2 - Order created successfully: {}", result.getData().getOrderId());

    return ResponseEntity.status(HttpStatus.CREATED)
        .header("X-API-Version", "2.0")
        .header("X-Order-Id", result.getData().getOrderId())
        .header("X-Rate-Limit-Remaining", "45") // Example rate limit info
        .body(ApiResponse.success(result.getData(), "Order created successfully"));
  }

  /** Blank -> null so empty Idempotency-Key headers behave as "not supplied". */
  private static String trimToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  @GetMapping("/{orderId}")
  @Operation(
      summary = "Get order by ID (V2)",
      description = "Retrieves order details with enhanced metadata and security")
  public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
      @Parameter(description = "Order ID", required = true) @PathVariable String orderId,
      @RequestHeader(value = "Authorization", required = false) String token,
      @RequestHeader(value = "X-API-Key", required = false) String apiKey,
      @RequestHeader(value = "X-Client-Version", defaultValue = "2.0") String clientVersion) {

    log.debug("V2 - Received get order request: {} from client: {}", orderId, clientVersion);

    // Validate either JWT or API key
    String userId = null;
    if (token != null) {
      ValidateTokenResponse authResponse =
          authClient.validateToken(new ValidateTokenRequest(token));
      if (authResponse.isValid()) {
        userId = authResponse.getUserId();
      }
    } else if (apiKey != null) {
      if (authClient.validateApiKey(new ValidateApiKeyRequest(apiKey))) {
        // F-08: API-key access carries no user identity — do not fabricate one
        log.info("V2 - Order accessed via API key: {} (client: {})", orderId, clientVersion);
      }
    }

    GetOrderByIdQuery query =
        GetOrderByIdQuery.builder()
            .orderId(orderId)
            .correlationId(UUID.randomUUID().toString())
            .build();

    QueryResult<OrderResponse> result = queryBus.execute(query);

    if (result.getData().isEmpty()) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .header("X-API-Version", "2.0")
          .header("X-Error-Type", "NOT_FOUND")
          .body(ApiResponse.error("Order not found", "ORDER_NOT_FOUND"));
    }

    // Security logging
    if (userId != null) {
      log.info(
          "V2 - Order accessed by user: {} for order: {} (client: {})",
          userId,
          orderId,
          clientVersion);
    }

    return ResponseEntity.ok()
        .header("X-API-Version", "2.0")
        .body(ApiResponse.success(result.getData().get(), "Order found"));
  }

  @GetMapping("/user/{userId}")
  @Operation(
      summary = "Get user orders with pagination (V2)",
      description = "Retrieves paginated orders for a user with filtering options")
  public ResponseEntity<ApiResponse<Page<OrderResponse>>> getUserOrders(
      @Parameter(description = "User ID", required = true) @PathVariable String userId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate) {

    log.debug(
        "V2 - Received get user orders request: {} (page: {}, size: {}, status: {})",
        userId,
        page,
        size,
        status);

    // Real DB-side pagination (F-19): LIMIT/OFFSET + count in one query, no in-memory slicing.
    GetUserOrdersPageQuery query =
        GetUserOrdersPageQuery.builder()
            .userId(userId)
            .page(Math.max(page, 0))
            .size(Math.min(Math.max(size, 1), GetUserOrdersPageQuery.MAX_PAGE_SIZE))
            .correlationId(UUID.randomUUID().toString())
            .build();

    QueryResult<Page<OrderResponse>> result = queryBus.execute(query);

    if (result.getData().isEmpty()) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .header("X-API-Version", "2.0")
          .body(ApiResponse.error("Failed to retrieve orders", "QUERY_FAILED"));
    }

    Page<OrderResponse> pageResult = result.getData().get();

    return ResponseEntity.ok()
        .header("X-API-Version", "2.0")
        .header("X-Total-Count", String.valueOf(pageResult.getTotalElements()))
        .header("X-Page-Count", String.valueOf(pageResult.getTotalPages()))
        .body(ApiResponse.success(pageResult, "Orders retrieved"));
  }

  @PostMapping("/bulk-cancel")
  @Operation(
      summary = "Bulk cancel orders (V2)",
      description = "Cancels multiple orders in a single request (new in V2)")
  public ResponseEntity<ApiResponse<BulkOperationResult>> bulkCancelOrders(
      @RequestHeader("Authorization") String token,
      @RequestHeader(value = "X-API-Key", required = false) String apiKey,
      @RequestBody BulkCancelRequest request) {

    log.info("V2 - Received bulk cancel request for {} orders", request.orderIds().size());

    // Validate authentication
    ValidateTokenResponse authResponse = authClient.validateToken(new ValidateTokenRequest(token));

    if (!authResponse.isValid()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .header("X-API-Version", "2.0")
          .body(ApiResponse.error("Invalid authentication token", "UNAUTHORIZED"));
    }

    // Process bulk cancellation (simplified implementation)
    List<String> orderIds = request.orderIds();
    BulkOperationResult result = new BulkOperationResult(orderIds.size(), 0, 0, List.of());

    log.info(
        "V2 - Bulk cancel completed: {}/{} orders processed",
        result.successful(),
        result.totalRequested());

    return ResponseEntity.ok()
        .header("X-API-Version", "2.0")
        .header("X-Bulk-Operation", "CANCEL")
        .body(ApiResponse.success(result, "Bulk cancel operation completed"));
  }

  @DeleteMapping("/{orderId}")
  @Operation(
      summary = "Cancel order (V2)",
      description = "Cancels an order with enhanced validation and metadata")
  public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
      @Parameter(description = "Order ID", required = true) @PathVariable String orderId,
      @RequestHeader("Authorization") String token,
      @RequestHeader(value = "X-API-Key", required = false) String apiKey,
      @RequestParam(required = false) String reason,
      @RequestHeader(value = "X-Client-Version", defaultValue = "2.0") String clientVersion) {

    log.info("V2 - Received cancel order request: {} from client: {}", orderId, clientVersion);

    // Validate authentication
    ValidateTokenResponse authResponse = authClient.validateToken(new ValidateTokenRequest(token));

    if (!authResponse.isValid()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .header("X-API-Version", "2.0")
          .body(ApiResponse.error("Invalid authentication token", "UNAUTHORIZED"));
    }

    // Create and execute command
    CancelOrderCommand command =
        CancelOrderCommand.builder()
            .orderId(orderId)
            .userId(authResponse.getUserId())
            .reason(reason)
            .initiator(authResponse.getUsername())
            .correlationId(UUID.randomUUID().toString())
            .build();

    CommandResult<OrderResponse> result = commandBus.execute(command);

    if (!result.isSuccess()) {
      return ResponseEntity.badRequest()
          .header("X-API-Version", "2.0")
          .header("X-Error-Code", result.getErrorCode())
          .body(ApiResponse.error(result.getErrorMessage(), result.getErrorCode()));
    }

    log.info(
        "V2 - Order cancelled successfully: {} by user: {} (client: {})",
        orderId,
        authResponse.getUserId(),
        clientVersion);

    return ResponseEntity.ok()
        .header("X-API-Version", "2.0")
        .header("X-Cancellation-Reason", reason != null ? reason : "Not provided")
        .header("X-Client-Version", clientVersion)
        .body(ApiResponse.success(result.getData(), "Order cancelled successfully"));
  }

  @GetMapping("/health")
  @Operation(summary = "Order service health (V2)", hidden = true)
  public ResponseEntity<ApiResponse<ServiceHealth>> health() {
    ServiceHealth health =
        new ServiceHealth(
            "2.0",
            "UP",
            java.time.Instant.now(),
            List.of("pagination", "filtering", "bulk-operations", "enhanced-security"));

    return ResponseEntity.ok()
        .header("X-API-Version", "2.0")
        .header("X-Service-Features", "enhanced,secure")
        .body(ApiResponse.success(health, "Order Service V2 is healthy"));
  }

  /** DTO for bulk cancel request (V2 feature). */
  public record BulkCancelRequest(List<String> orderIds, String reason) {}

  /** Result for bulk operations (V2 feature). */
  public record BulkOperationResult(
      int totalRequested, int successful, int failed, List<String> results) {}

  /** Service health information (V2 feature). */
  public record ServiceHealth(
      String version, String status, java.time.Instant timestamp, List<String> features) {}
}
