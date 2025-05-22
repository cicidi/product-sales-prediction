package com.example.productapi.mcp.tools;

import com.example.productapi.mcp.model.ToolDefinition;
import com.example.productapi.mcp.model.ToolResponse;
import com.example.productapi.mcp.service.Tool;
import com.example.productapi.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import com.example.productapi.model.Order;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class OrderListTool implements Tool {
    
    private final OrderService orderService;
    private final ToolDefinition definition;
    
    @Autowired
    public OrderListTool(OrderService orderService) {
        this.orderService = orderService;
        
        // 构建工具定义
        this.definition = ToolDefinition.builder()
                .name("get_sellers_orders")
                .displayName("订单列表查询")
                .description("查询特定卖家的订单记录，支持时间范围和分页功能")
                .parameters(Arrays.asList(
                    ToolDefinition.ParameterDefinition.builder()
                        .name("seller_id")
                        .type("string")
                        .description("卖家ID，必填参数，卖家不能查看别的卖家的订单")
                        .required(true)
                        .example("SELLER789")
                        .build(),
                    ToolDefinition.ParameterDefinition.builder()
                        .name("start_time")
                        .type("string")
                        .description("开始时间 (格式: yyyy/MM/dd)，可选参数，默认为2025/02/01")
                        .required(false)
                        .example("2025/02/01")
                        .build(),
                    ToolDefinition.ParameterDefinition.builder()
                        .name("end_time")
                        .type("string")
                        .description("结束时间 (格式: yyyy/MM/dd)，可选参数，默认为2025/05/01")
                        .required(false)
                        .example("2025/05/01")
                        .build(),
                    ToolDefinition.ParameterDefinition.builder()
                        .name("page")
                        .type("integer")
                        .description("页码，从0开始，可选参数，默认为0（第一页）")
                        .required(false)
                        .defaultValue(0)
                        .example(0)
                        .build(),
                    ToolDefinition.ParameterDefinition.builder()
                        .name("size")
                        .type("integer")
                        .description("每页记录数，可选参数，默认为20，最大为100")
                        .required(false)
                        .defaultValue(20)
                        .example(20)
                        .build()
                ))
                .outputSchema(Map.of(
                    "orders", "订单列表",
                    "current_page", "当前页码",
                    "total_items", "总记录数",
                    "total_pages", "总页数",
                    "start_time", "查询开始时间",
                    "end_time", "查询结束时间"
                ))
                .build();
    }
    
    @Override
    public ToolDefinition getDefinition() {
        return definition;
    }
    
    @Override
    public ToolResponse execute(Map<String, Object> parameters) {
        // 验证必填参数
        if (!parameters.containsKey("seller_id")) {
            return ToolResponse.error(getName(), "seller_id是必填参数");
        }
        
        // 提取参数
        String sellerId = parameters.get("seller_id").toString();
        
        // 处理时间参数
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        LocalDateTime startTime = LocalDateTime.parse("2025/02/01 00:00:00", DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
        LocalDateTime endTime = LocalDateTime.parse("2025/05/01 23:59:59", DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
        
        if (parameters.containsKey("start_time") && parameters.get("start_time") != null) {
            try {
                startTime = LocalDateTime.parse(parameters.get("start_time").toString() + " 00:00:00", 
                    DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
            } catch (Exception e) {
                return ToolResponse.error(getName(), "start_time格式无效，请使用yyyy/MM/dd格式");
            }
        }
        
        if (parameters.containsKey("end_time") && parameters.get("end_time") != null) {
            try {
                endTime = LocalDateTime.parse(parameters.get("end_time").toString() + " 23:59:59", 
                    DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
            } catch (Exception e) {
                return ToolResponse.error(getName(), "end_time格式无效，请使用yyyy/MM/dd格式");
            }
        }
        
        // 处理分页参数
        int page = 0;
        if (parameters.containsKey("page")) {
            if (parameters.get("page") instanceof Integer) {
                page = (Integer) parameters.get("page");
            } else {
                try {
                    page = Integer.parseInt(parameters.get("page").toString());
                } catch (NumberFormatException e) {
                    return ToolResponse.error(getName(), "page必须是有效的整数");
                }
            }
        }
        
        int size = 20;
        if (parameters.containsKey("size")) {
            if (parameters.get("size") instanceof Integer) {
                size = (Integer) parameters.get("size");
            } else {
                try {
                    size = Integer.parseInt(parameters.get("size").toString());
                } catch (NumberFormatException e) {
                    return ToolResponse.error(getName(), "size必须是有效的整数");
                }
            }
            
            // 限制每页最大记录数
            if (size > 100) {
                size = 100;
            }
        }
        
        try {
            // 调用服务方法
            Page<Order> orderPage = orderService.getOrdersWithFilters(
                sellerId,
                null, // productId
                null, // category
                startTime,
                endTime,
                page,
                size
            );
            
            // 构建响应
            Map<String, Object> result = new HashMap<>();
            result.put("orders", convertOrders(orderPage.getContent()));
            result.put("current_page", orderPage.getNumber());
            result.put("total_items", orderPage.getTotalElements());
            result.put("total_pages", orderPage.getTotalPages());
            result.put("start_time", startTime.format(dateFormatter));
            result.put("end_time", endTime.format(dateFormatter));
            
            return ToolResponse.success(getName(), result);
        } catch (Exception e) {
            return ToolResponse.error(getName(), "执行订单查询时发生错误: " + e.getMessage());
        }
    }

    private List<Map<String, Object>> convertOrders(List<Order> orders) {
        return orders.stream().map(order -> {
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("order_id", order.getOrderId());
            orderMap.put("product_id", order.getProductId());
            orderMap.put("buyer_id", order.getBuyerId());
            orderMap.put("seller_id", order.getSellerId());
            orderMap.put("unit_price", order.getUnitPrice());
            orderMap.put("quantity", order.getQuantity());
            orderMap.put("total_price", order.getTotalPrice());
            orderMap.put("timestamp", order.getTimestamp().toString());
            return orderMap;
        }).collect(Collectors.toList());
    }
} 