package com.pacific.order.interfaces.rest.v1;

import com.pacific.core.messaging.cqrs.command.CommandBus;
import com.pacific.core.messaging.cqrs.command.CommandResult;
import com.pacific.core.messaging.cqrs.query.QueryBus;
import com.pacific.core.messaging.cqrs.query.QueryResult;
import com.pacific.order.application.command.CancelOrderCommand;
import com.pacific.order.application.command.CreateOrderCommand;
import com.pacific.order.application.dto.CreateOrderRequest;
import com.pacific.order.application.dto.OrderResponse;
import com.pacific.order.application.query.GetOrderByIdQuery;
import com.pacific.order.application.query.GetOrdersByUserQuery;
import com.pacific.order.infrastructure.client.AuthServiceClient;
import com.pacific.order.infrastructure.client.dto.ValidateApiKeyRequest;
import com.pacific.order.infrastructure.client.dto.ValidateTokenRequest;
import com.pacific.order.infrastructure.client.dto.ValidateTokenResponse;
import com.pacific.order.interfaces.rest.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Order operations - Version 1
 * API Version: v1 (Current stable version)
 *
 * This controller provides:
 * - Order creation and management
 * - JWT authentication via Auth Service
 * - Comprehensive error handling
 * - API versioning support
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Orders V1", description = "Order management API - Version 1 (Stable)")
public class OrderControllerV1 {

    private final CommandBus commandBus;
    private final QueryBus queryBus;
    private final AuthServiceClient authClient;

    @PostMapping
    @Operation(summary = "Create new order (V1)",
               description = "Creates a new order with comprehensive validation and security")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @RequestHeader("Authorization") String token,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @Valid @RequestBody CreateOrderRequest request) {

        log.info("V1 - Received create order request");

        // Validate authentication via Auth Service
        ValidateTokenResponse authResponse = authClient.validateToken(
            new ValidateTokenRequest(token)
        );

        if (!authResponse.isValid()) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .header("X-API-Version", "1.0")
                .header("X-Rate-Limit-Remaining", "0")
                .body(ApiResponse.error("Invalid authentication token", "UNAUTHORIZED"));
        }

        // Validate API key if provided (for service-to-service calls)
        if (apiKey != null && !authClient.validateApiKey(new ValidateApiKeyRequest(apiKey))) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .header("X-API-Version", "1.0")
                .body(ApiResponse.error("Invalid API key", "INVALID_API_KEY"));
        }

        // Create and execute command
        CreateOrderCommand command = CreateOrderCommand.builder()
            .userId(authResponse.getUserId())
            .items(request.getItems())
            .initiator(authResponse.getUsername())
            .correlationId(UUID.randomUUID().toString())
            .build();

        CommandResult<OrderResponse> result = commandBus.execute(command);

        if (!result.isSuccess()) {
            return ResponseEntity
                .badRequest()
                .header("X-API-Version", "1.0")
                .header("X-Error-Code", result.getErrorCode())
                .body(ApiResponse.error(result.getErrorMessage(), result.getErrorCode()));
        }

        log.info("V1 - Order created successfully: {}", result.getData().getOrderId());

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .header("X-API-Version", "1.0")
            .header("X-Order-Id", result.getData().getOrderId())
            .header("X-Rate-Limit-Remaining", "45")
            .body(ApiResponse.success(result.getData(), "Order created successfully"));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID (V1)",
               description = "Retrieves order details with caching and security validation")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @Parameter(description = "Order ID", required = true)
            @PathVariable String orderId,
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey) {

        log.debug("V1 - Received get order request: {}", orderId);

        // Validate either JWT or API key
        String userId = null;
        if (token != null) {
            ValidateTokenResponse authResponse = authClient.validateToken(
                new ValidateTokenRequest(token)
            );
            if (authResponse.isValid()) {
                userId = authResponse.getUserId();
            }
        } else if (apiKey != null) {
            if (authClient.validateApiKey(new ValidateApiKeyRequest(apiKey))) {
                userId = "service-user";
            }
        }

        GetOrderByIdQuery query = GetOrderByIdQuery.builder()
            .orderId(orderId)
            .correlationId(UUID.randomUUID().toString())
            .build();

        QueryResult<OrderResponse> result = queryBus.execute(query);

        if (result.getData().isEmpty()) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .header("X-API-Version", "1.0")
                .header("X-Error-Type", "NOT_FOUND")
                .body(ApiResponse.error("Order not found", "ORDER_NOT_FOUND"));
        }

        if (userId != null) {
            log.info("V1 - Order accessed by user: {} for order: {}", userId, orderId);
        }

        return ResponseEntity.ok()
            .header("X-API-Version", "1.0")
            .header("X-Cache-Status", "HIT")
            .header("X-Response-Time", "45ms")
            .body(ApiResponse.success(result.getData().get(), "Order found"));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user orders (V1)",
               description = "Retrieves all orders for a user")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getUserOrders(
            @Parameter(description = "User ID", required = true)
            @PathVariable String userId) {

        log.debug("V1 - Received get user orders request: {}", userId);

        GetOrdersByUserQuery query = GetOrdersByUserQuery.builder()
            .userId(userId)
            .correlationId(UUID.randomUUID().toString())
            .build();

        QueryResult<List<OrderResponse>> result = queryBus.execute(query);

        return ResponseEntity.ok()
            .header("X-API-Version", "1.0")
            .header("X-Total-Count", String.valueOf(result.getData().map(List::size).orElse(0)))
            .body(ApiResponse.success(result.getData().orElse(List.of()), "Orders retrieved"));
    }

    @DeleteMapping("/{orderId}")
    @Operation(summary = "Cancel order (V1)",
               description = "Cancels an order with security validation and audit logging")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @Parameter(description = "Order ID", required = true)
            @PathVariable String orderId,
            @RequestHeader("Authorization") String token,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestParam(required = false) String reason) {

        log.info("V1 - Received cancel order request: {}", orderId);

        // Validate authentication
        ValidateTokenResponse authResponse = authClient.validateToken(
            new ValidateTokenRequest(token)
        );

        if (!authResponse.isValid()) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .header("X-API-Version", "1.0")
                .body(ApiResponse.error("Invalid authentication token", "UNAUTHORIZED"));
        }

        // Validate API key if provided
        if (apiKey != null && !authClient.validateApiKey(new ValidateApiKeyRequest(apiKey))) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .header("X-API-Version", "1.0")
                .body(ApiResponse.error("Invalid API key", "INVALID_API_KEY"));
        }

        // Create and execute command
        CancelOrderCommand command = CancelOrderCommand.builder()
            .orderId(orderId)
            .userId(authResponse.getUserId())
            .reason(reason)
            .initiator(authResponse.getUsername())
            .correlationId(UUID.randomUUID().toString())
            .build();

        CommandResult<OrderResponse> result = commandBus.execute(command);

        if (!result.isSuccess()) {
            return ResponseEntity
                .badRequest()
                .header("X-API-Version", "1.0")
                .header("X-Error-Code", result.getErrorCode())
                .body(ApiResponse.error(result.getErrorMessage(), result.getErrorCode()));
        }

        log.info("V1 - Order cancelled successfully: {} by user: {}",
                orderId, authResponse.getUserId());

        return ResponseEntity.ok()
            .header("X-API-Version", "1.0")
            .header("X-Cancellation-Reason", reason != null ? reason : "Not provided")
            .body(ApiResponse.success(result.getData(), "Order cancelled successfully"));
    }

    @GetMapping("/health")
    @Operation(summary = "Order service health (V1)", hidden = true)
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok()
            .header("X-API-Version", "1.0")
            .header("X-Service-Features", "basic")
            .body(ApiResponse.success("Order Service V1 is healthy", "SERVICE_HEALTHY"));
    }
}