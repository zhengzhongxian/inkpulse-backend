package com.inkpulse.features.author.elastic;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import lombok.*;

@Document(indexName = "inkpulse_authors")
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthorDocument {

    @Id
    @JsonProperty("id")
    private String id; // maps to _id (Author ID)

    @Field(type = FieldType.Text)
    @JsonProperty("name")
    private String name;

    @Field(name = "avatar_url", type = FieldType.Keyword)
    @JsonProperty("avatar_url")
    private String avatarUrl;

    @Field(type = FieldType.Text)
    @JsonProperty("biography")
    private String biography;

    @Field(name = "is_deleted", type = FieldType.Boolean)
    @JsonProperty("is_deleted")
    private boolean deleted;
}
