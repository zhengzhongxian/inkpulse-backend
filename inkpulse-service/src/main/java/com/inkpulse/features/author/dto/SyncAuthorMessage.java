package com.inkpulse.features.author.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncAuthorMessage {
    private UUID id;
    private String name;
    private String biography;
    private String avatarUrl;
    private boolean isDeleted;
}
