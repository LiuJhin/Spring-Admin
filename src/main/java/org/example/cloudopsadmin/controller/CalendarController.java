package org.example.cloudopsadmin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.cloudopsadmin.common.ApiResponse;
import org.example.cloudopsadmin.entity.Schedule;
import org.example.cloudopsadmin.entity.Todo;
import org.example.cloudopsadmin.repository.ScheduleRepository;
import org.example.cloudopsadmin.repository.TodoRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Calendar Management")
@RestController
@RequestMapping("/api/v1/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final ScheduleRepository scheduleRepository;
    private final TodoRepository todoRepository;

    // --- Schedule APIs ---

    @Operation(summary = "Get schedules by range")
    @GetMapping("/schedules")
    public ApiResponse<List<Schedule>> getSchedules(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        return ApiResponse.success("success", scheduleRepository.findByStartTimeBetween(start, end));
    }

    @Operation(summary = "Create schedule")
    @PostMapping("/schedules")
    public ApiResponse<Schedule> createSchedule(@RequestBody Schedule schedule) {
        return ApiResponse.success("success", scheduleRepository.save(schedule));
    }
    
    @Operation(summary = "Update schedule")
    @PutMapping("/schedules/{id}")
    public ApiResponse<Schedule> updateSchedule(@PathVariable Long id, @RequestBody Schedule schedule) {
        return scheduleRepository.findById(id).map(existing -> {
            existing.setTitle(schedule.getTitle());
            existing.setCategory(schedule.getCategory());
            existing.setStartTime(schedule.getStartTime());
            existing.setEndTime(schedule.getEndTime());
            existing.setLocation(schedule.getLocation());
            existing.setDescription(schedule.getDescription());
            return ApiResponse.success("success", scheduleRepository.save(existing));
        }).orElse(ApiResponse.error(404, "Schedule not found"));
    }

    @Operation(summary = "Delete schedule")
    @DeleteMapping("/schedules/{id}")
    public ApiResponse<Void> deleteSchedule(@PathVariable Long id) {
        scheduleRepository.deleteById(id);
        return ApiResponse.success("success", null);
    }

    // --- Todo APIs ---

    @Operation(summary = "Get todos")
    @GetMapping("/todos")
    public ApiResponse<List<Todo>> getTodos() {
        return ApiResponse.success("success", todoRepository.findAll());
    }

    @Operation(summary = "Create todo")
    @PostMapping("/todos")
    public ApiResponse<Todo> createTodo(@RequestBody Todo todo) {
        return ApiResponse.success("success", todoRepository.save(todo));
    }
    
    @Operation(summary = "Update todo status")
    @PatchMapping("/todos/{id}")
    public ApiResponse<Todo> updateTodoStatus(@PathVariable Long id, @RequestBody Todo todo) {
         return todoRepository.findById(id).map(existing -> {
            if (todo.getCompleted() != null) existing.setCompleted(todo.getCompleted());
            if (todo.getContent() != null) existing.setContent(todo.getContent());
            return ApiResponse.success("success", todoRepository.save(existing));
        }).orElse(ApiResponse.error(404, "Todo not found"));
    }
    
    @Operation(summary = "Delete todo")
    @DeleteMapping("/todos/{id}")
    public ApiResponse<Void> deleteTodo(@PathVariable Long id) {
        todoRepository.deleteById(id);
        return ApiResponse.success("success", null);
    }
}
