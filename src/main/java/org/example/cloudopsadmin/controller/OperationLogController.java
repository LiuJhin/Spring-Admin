package org.example.cloudopsadmin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.cloudopsadmin.common.ApiResponse;
import org.example.cloudopsadmin.entity.OperationLog;
import org.example.cloudopsadmin.service.OperationLogService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/operation-logs")
@RequiredArgsConstructor
@Tag(name = "Operation Logs", description = "操作日志列表")
public class OperationLogController {

    private final OperationLogService operationLogService;

    @GetMapping
    @Operation(summary = "获取操作日志列表", description = "分页获取操作日志，显示谁对什么内容进行了操作以及时间")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(name = "operator", required = false) String operator,
            @RequestParam(name = "target_type", required = false) String targetType,
            @RequestParam(name = "action", required = false) String action,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "sort_order", defaultValue = "desc") String sortOrder
    ) {
        Page<OperationLog> logPage = operationLogService.list(page, pageSize, operator, targetType, action, search, sortOrder);

        List<Map<String, Object>> list = logPage.getContent().stream().map(log -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", log.getId());
            map.put("operator_email", log.getOperatorEmail());
            map.put("operator_name", log.getOperatorName());
            map.put("action", log.getAction());
            map.put("target_type", log.getTargetType());
            map.put("target_id", log.getTargetId());
            map.put("description", log.getDescription());
            map.put("created_at", log.getCreatedAt());
            return map;
        }).toList();

        Map<String, Object> data = new HashMap<>();
        data.put("total", logPage.getTotalElements());
        data.put("page", logPage.getNumber() + 1);
        data.put("page_size", logPage.getSize());
        data.put("list", list);

        return ApiResponse.success("success", data);
    }
}

