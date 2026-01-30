package org.example.cloudopsadmin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.cloudopsadmin.common.ApiResponse;
import org.example.cloudopsadmin.entity.CloudProvider;
import org.example.cloudopsadmin.entity.CreditCard;
import org.example.cloudopsadmin.entity.PartnerBd;
import org.example.cloudopsadmin.service.BasicDataService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/basic-data")
@RequiredArgsConstructor
@Tag(name = "Basic Data", description = "Basic Data Configuration Management")
public class BasicDataController {

    private final BasicDataService basicDataService;

    // --- Cloud Provider Endpoints ---

    @GetMapping("/cloud-providers")
    @Operation(summary = "Get all Cloud Providers")
    public ApiResponse<List<CloudProvider>> getAllCloudProviders() {
        return ApiResponse.success("Success", basicDataService.getAllCloudProviders());
    }

    @PostMapping("/cloud-providers")
    @Operation(summary = "Create Cloud Provider")
    public ApiResponse<CloudProvider> createCloudProvider(@RequestBody CloudProvider provider) {
        return ApiResponse.success("Created successfully", basicDataService.createCloudProvider(provider));
    }

    @PutMapping("/cloud-providers/{id}")
    @Operation(summary = "Update Cloud Provider")
    public ApiResponse<CloudProvider> updateCloudProvider(@PathVariable Long id, @RequestBody CloudProvider provider) {
        return ApiResponse.success("Updated successfully", basicDataService.updateCloudProvider(id, provider));
    }

    @DeleteMapping("/cloud-providers/{id}")
    @Operation(summary = "Delete Cloud Provider")
    public ApiResponse<Void> deleteCloudProvider(@PathVariable Long id) {
        basicDataService.deleteCloudProvider(id);
        return ApiResponse.success("Deleted successfully", null);
    }

    // --- Partner BD Endpoints ---

    @GetMapping("/partner-bds")
    @Operation(summary = "Get all Partner BDs")
    public ApiResponse<List<PartnerBd>> getAllPartnerBds() {
        return ApiResponse.success("Success", basicDataService.getAllPartnerBds());
    }

    @PostMapping("/partner-bds")
    @Operation(summary = "Create Partner BD")
    public ApiResponse<PartnerBd> createPartnerBd(@RequestBody PartnerBd bd) {
        return ApiResponse.success("Created successfully", basicDataService.createPartnerBd(bd));
    }

    @PutMapping("/partner-bds/{id}")
    @Operation(summary = "Update Partner BD")
    public ApiResponse<PartnerBd> updatePartnerBd(@PathVariable Long id, @RequestBody PartnerBd bd) {
        return ApiResponse.success("Updated successfully", basicDataService.updatePartnerBd(id, bd));
    }

    @DeleteMapping("/partner-bds/{id}")
    @Operation(summary = "Delete Partner BD")
    public ApiResponse<Void> deletePartnerBd(@PathVariable Long id) {
        basicDataService.deletePartnerBd(id);
        return ApiResponse.success("Deleted successfully", null);
    }

    // --- Credit Card Endpoints ---

    @GetMapping("/credit-cards")
    @Operation(summary = "Get all Credit Cards")
    public ApiResponse<List<CreditCard>> getAllCreditCards() {
        return ApiResponse.success("Success", basicDataService.getAllCreditCards());
    }

    @PostMapping("/credit-cards")
    @Operation(summary = "Create Credit Card")
    public ApiResponse<CreditCard> createCreditCard(@Valid @RequestBody CreditCard card) {
        return ApiResponse.success("Created successfully", basicDataService.createCreditCard(card));
    }

    @PutMapping("/credit-cards/{id}")
    @Operation(summary = "Update Credit Card")
    public ApiResponse<CreditCard> updateCreditCard(@PathVariable Long id, @Valid @RequestBody CreditCard card) {
        return ApiResponse.success("Updated successfully", basicDataService.updateCreditCard(id, card));
    }

    @DeleteMapping("/credit-cards/{id}")
    @Operation(summary = "Delete Credit Card")
    public ApiResponse<Void> deleteCreditCard(@PathVariable Long id) {
        basicDataService.deleteCreditCard(id);
        return ApiResponse.success("Deleted successfully", null);
    }
}
