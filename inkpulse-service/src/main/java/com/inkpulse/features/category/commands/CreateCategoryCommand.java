package com.inkpulse.features.category.commands;

import com.inkpulse.cqrs.Command;
import com.inkpulse.models.response.CategoryResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCategoryCommand implements Command<CategoryResponse> {
    private String name;
    private String slug;
    private UUID parentId;
}