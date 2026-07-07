package com.inkpulse.features.category.commands;

import com.inkpulse.cqrs.Command;
import com.inkpulse.models.response.category.CategoryResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCategoryCommand implements Command<CategoryResponse> {
    private UUID id;
    private String name;
    private String slug;
    private UUID parentId;
}