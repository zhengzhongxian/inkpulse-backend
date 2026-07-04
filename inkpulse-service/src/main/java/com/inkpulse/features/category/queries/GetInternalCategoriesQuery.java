package com.inkpulse.features.category.queries;

import com.inkpulse.cqrs.Query;
import com.inkpulse.models.response.CategoryResponse;
import lombok.Value;

import java.util.List;

@Value
public class GetInternalCategoriesQuery implements Query<List<CategoryResponse>> {
}