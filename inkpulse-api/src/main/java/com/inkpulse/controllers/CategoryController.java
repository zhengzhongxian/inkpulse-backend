package com.inkpulse.controllers;

import an.awesome.pipelinr.Pipeline;
import com.inkpulse.constants.PermissionConstants;
import com.inkpulse.features.category.commands.CreateCategoryCommand;
import com.inkpulse.features.category.commands.UpdateCategoryCommand;
import com.inkpulse.features.category.commands.DeleteCategoryCommand;
import com.inkpulse.features.category.queries.GetInternalCategoriesQuery;
import com.inkpulse.models.response.category.CategoryResponse;
import com.inkpulse.features.category.queries.GetCategoriesQuery;
import com.inkpulse.models.response.ResultRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.inkpulse.constants.message.CategoryMessageConstants;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CategoryController {

    private final Pipeline pipeline;

    @GetMapping("/api/v1/public/categories")
    public ResponseEntity<ResultRes<List<CategoryResponse>>> getCategories() {
        log.info("REST request to list categories");
        List<CategoryResponse> result = pipeline.send(new GetCategoriesQuery());
        return ResponseEntity.ok(ResultRes.successResult(result, CategoryMessageConstants.GET_CATEGORIES_SUCCESS, 200));
    }

    @GetMapping("/api/v1/categories")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Categories.INTERNAL_VIEW + "')")
    public ResponseEntity<ResultRes<List<CategoryResponse>>> getInternalCategories() {
        log.info("REST request to list internal categories");
        List<CategoryResponse> result = pipeline.send(new GetInternalCategoriesQuery());
        return ResponseEntity.ok(ResultRes.successResult(result, CategoryMessageConstants.LIST_SUCCESS, 200));
    }

    @PostMapping("/api/v1/categories")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Categories.CREATE + "')")
    public ResponseEntity<ResultRes<CategoryResponse>> createCategory(@RequestBody CreateCategoryCommand command) {
        log.info("REST request to create category: {}", command.getName());
        CategoryResponse result = pipeline.send(command);
        return ResponseEntity.ok(ResultRes.successResult(result, CategoryMessageConstants.CREATE_SUCCESS, 200));
    }

    @PutMapping("/api/v1/categories/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Categories.EDIT + "')")
    public ResponseEntity<ResultRes<CategoryResponse>> updateCategory(
            @PathVariable("id") UUID id,
            @RequestBody UpdateCategoryCommand command) {
        log.info("REST request to update category: {}", id);
        command.setId(id);
        CategoryResponse result = pipeline.send(command);
        return ResponseEntity.ok(ResultRes.successResult(result, CategoryMessageConstants.UPDATE_SUCCESS, 200));
    }

    @DeleteMapping("/api/v1/categories/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Categories.DELETE + "')")
    public ResponseEntity<ResultRes<Boolean>> deleteCategory(@PathVariable("id") UUID id) {
        log.info("REST request to delete category: {}", id);
        Boolean result = pipeline.send(new DeleteCategoryCommand(id));
        return ResponseEntity.ok(ResultRes.successResult(result, CategoryMessageConstants.DELETE_SUCCESS, 200));
    }
}
