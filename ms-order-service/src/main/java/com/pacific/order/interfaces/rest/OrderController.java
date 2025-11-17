package com.pacific.order.interfaces.rest;


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
import com.pacific.order.infrastructure.client.dto.ValidateTokenRequest;
import com.pacific.order.infrastructure.client.dto.ValidateTokenResponse;
import com.pacific.order.interfaces.rest.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
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
 * REST Controller for Order operations
 * Demonstrates CQRS pattern and Feign client authentication
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Orders", description = "Order management API")
public class OrderController {

    private final CommandBus commandBus;        // From backend-core
    private final QueryBus queryBus;            // From backend-core
    private final AuthServiceClient authClient;

    @PostMapping
    @Operation(summary = "Create new order")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody CreateOrderRequest request) {

        log.info("Received create order request");

        // 1. Validate authentication via Feign
        ValidateTokenResponse authResponse = authClient.validateToken(
            new ValidateTokenRequest(token)
        );

        if (!authResponse.isValid()) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid token", "UNAUTHORIZED"));
        }

        // 2. Create and execute command
        CreateOrderCommand command = CreateOrderCommand.builder()
            .userId(authResponse.getUserId())
            .items(request.getItems())
            .initiator(authResponse.getUsername())
            .correlationId(UUID.randomUUID().toString())
            .build();

        CommandResult<OrderResponse> result = commandBus.execute(command);

        // 3. Handle result
        if (!result.isSuccess()) {
            return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(result.getErrorMessage(), result.getErrorCode()));
        }

        log.info("Order created successfully: {}", result.getData().getOrderId());

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(result.getData(), "Order created successfully"));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @PathVariable String orderId) {

        log.debug("Received get order request: {}", orderId);

        GetOrderByIdQuery query = GetOrderByIdQuery.builder()
            .orderId(orderId)
            .correlationId(UUID.randomUUID().toString())
            .build();

        QueryResult<OrderResponse> result = queryBus.execute(query);

        if (result.getData().isEmpty()) {
            return ResponseEntity
                .notFound()
                .build();
        }

        return ResponseEntity.ok(
            ApiResponse.success(result.getData().get(), "Order found")
        );
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all orders for a user")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getUserOrders(
            @PathVariable String userId) {

        log.debug("Received get user orders request: {}", userId);

        GetOrdersByUserQuery query = GetOrdersByUserQuery.builder()
            .userId(userId)
            .correlationId(UUID.randomUUID().toString())
            .build();

        QueryResult<List<OrderResponse>> result = queryBus.execute(query);

        return ResponseEntity.ok(
            ApiResponse.success(result.getData().orElse(List.of()), "Orders retrieved")
        );
    }

    @DeleteMapping("/{orderId}")
    @Operation(summary = "Cancel order")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @RequestHeader("Authorization") String token,
            @PathVariable String orderId,
            @RequestParam(required = false) String reason) {

        log.info("Received cancel order request: {}", orderId);

        // 1. Validate authentication
        ValidateTokenResponse authResponse = authClient.validateToken(
            new ValidateTokenRequest(token)
        );

        if (!authResponse.isValid()) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid token", "UNAUTHORIZED"));
        }

        // 2. Create and execute command
        CancelOrderCommand command = CancelOrderCommand.builder()
            .orderId(orderId)
            .userId(authResponse.getUserId())
            .reason(reason)
            .initiator(authResponse.getUsername())
            .correlationId(UUID.randomUUID().toString())
            .build();

        CommandResult<OrderResponse> result = commandBus.execute(command);

        // 3. Handle result
        if (!result.isSuccess()) {
            return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(result.getErrorMessage(), result.getErrorCode()));
        }

        log.info("Order cancelled successfully: {}", orderId);

        return ResponseEntity.ok(
            ApiResponse.success(result.getData(), "Order cancelled successfully")
        );
    }
}

