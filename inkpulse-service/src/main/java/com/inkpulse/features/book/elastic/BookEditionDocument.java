package com.inkpulse.features.book.elastic;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Document(indexName = "inkpulse_books")
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookEditionDocument {

    @Id
    @JsonProperty("id")
    private String id; // maps to _id (Edition ID)

    @Field(name = "sku", type = FieldType.Keyword)
    @JsonProperty("sku")
    private String isbn;

    @Field(type = FieldType.Double)
    @JsonProperty("price")
    private BigDecimal price;

    @Field(name = "old_price", type = FieldType.Double)
    @JsonProperty("old_price")
    private BigDecimal oldPrice;

    @Field(name = "stock_quantity", type = FieldType.Integer)
    @JsonProperty("stock_quantity")
    private int stockQuantity;

    @Field(name = "edition_number", type = FieldType.Integer)
    @JsonProperty("edition_number")
    private int editionNumber;

    @Field(name = "edition_thumbnail_url", type = FieldType.Keyword)
    @JsonProperty("edition_thumbnail_url")
    private String thumbnailUrl;

    @Field(name = "file_path_pdf", type = FieldType.Keyword)
    @JsonProperty("file_path_pdf")
    private String filePathPdf;

    @Field(name = "cover_type", type = FieldType.Keyword)
    @JsonProperty("cover_type")
    private String coverType;

    @Field(name = "page_count", type = FieldType.Integer)
    @JsonProperty("page_count")
    private Integer pageCount;

    @Field(name = "publication_year", type = FieldType.Integer)
    @JsonProperty("publication_year")
    private Integer publicationYear;

    @Field(name = "weight_gram", type = FieldType.Integer)
    @JsonProperty("weight_gram")
    private int weightGram;

    @Field(name = "width_cm", type = FieldType.Integer)
    @JsonProperty("width_cm")
    private int widthCm;

    @Field(name = "height_cm", type = FieldType.Integer)
    @JsonProperty("height_cm")
    private int heightCm;

    @Field(name = "length_cm", type = FieldType.Integer)
    @JsonProperty("length_cm")
    private int lengthCm;

    @Field(name = "language", type = FieldType.Keyword)
    @JsonProperty("language")
    private String language;

    @Field(name = "publisher_name", type = FieldType.Keyword)
    @JsonProperty("publisher_name")
    private String publisherName;

    // Gallery Images
    @Field(name = "image_urls", type = FieldType.Keyword)
    @JsonProperty("image_urls")
    private List<String> imageUrls;

    // --- Parent Book Fields (Denormalized) ---
    @Field(name = "book_id", type = FieldType.Keyword)
    @JsonProperty("book_id")
    private String bookId;

    @Field(name = "book_title", type = FieldType.Text)
    @JsonProperty("book_title")
    private String title;

    @Field(name = "book_thumbnail_url", type = FieldType.Keyword)
    @JsonProperty("book_thumbnail_url")
    private String bookThumbnailUrl;

    @Field(type = FieldType.Text)
    @JsonProperty("introduce")
    private String introduce;

    @Field(type = FieldType.Text)
    @JsonProperty("description")
    private String description;

    @Field(name = "author", type = FieldType.Text)
    @JsonProperty("author")
    private String authorName; // Joined list of author names (e.g. "Robert C. Martin")

    @Field(name = "badge_text", type = FieldType.Keyword)
    @JsonProperty("badge_text")
    private String badgeText;

    @Field(name = "badge_text_color", type = FieldType.Keyword)
    @JsonProperty("badge_text_color")
    private String badgeTextColor;

    @Field(name = "badge_bg_color", type = FieldType.Keyword)
    @JsonProperty("badge_bg_color")
    private String badgeBgColor;

    @Field(name = "is_active", type = FieldType.Boolean)
    @JsonProperty("is_active")
    private boolean active;

    @Field(name = "is_deleted", type = FieldType.Boolean)
    @JsonProperty("is_deleted")
    private boolean deleted;

    @Field(name = "category_slugs", type = FieldType.Keyword)
    @JsonProperty("category_slugs")
    private List<String> categorySlugs;

    @Field(name = "badges", type = FieldType.Object)
    @JsonProperty("badges")
    private List<BadgeInfo> badges;

    @Field(name = "publisher_id", type = FieldType.Keyword)
    @JsonProperty("publisher_id")
    private String publisherId;

    @Field(name = "author_ids", type = FieldType.Keyword)
    @JsonProperty("author_ids")
    private List<String> authorIds;

    @Field(name = "badge_ids", type = FieldType.Keyword)
    @JsonProperty("badge_ids")
    private List<String> badgeIds;

    @Field(name = "sold_count", type = FieldType.Integer)
    @JsonProperty("sold_count")
    private int soldCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BadgeInfo {
        @JsonProperty("text")
        private String text;
        @JsonProperty("textColor")
        private String textColor;
        @JsonProperty("bgColor")
        private String bgColor;
        @JsonProperty("shape")
        private String shape;
    }
}
