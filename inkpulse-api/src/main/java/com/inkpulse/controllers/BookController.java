package com.inkpulse.controllers;

import an.awesome.pipelinr.Pipeline;
import com.inkpulse.constants.PermissionConstants;
import com.inkpulse.constants.message.BookMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.features.book.commands.CreateBookCommand;
import com.inkpulse.features.book.dto.BookResponse;
import com.inkpulse.features.book.queries.GetBooksQuery;
import com.inkpulse.features.book.queries.GetInternalBooksQuery;
import com.inkpulse.features.book.queries.GetInternalBookDetailQuery;
import com.inkpulse.features.book.dto.InternalBookDetailResponse;
import com.inkpulse.models.pagination.PagedList;
import com.inkpulse.models.response.ResultRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Validator;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class BookController {

    private final Pipeline pipeline;
    private final Validator validator;

    @GetMapping("/api/v1/public/books")
    public ResponseEntity<ResultRes<PagedList<BookResponse>>> getBooks(GetBooksQuery query) {
        log.info(
                "REST request to list books: page={}, size={}, search={}, category={}, minPrice={}, maxPrice={}, coverType={}, authorName={}",
                query.getPageNumber(), query.getPageSize(), query.getSearchKeyword(), query.getCategorySlug(),
                query.getMinPrice(), query.getMaxPrice(), query.getCoverType(), query.getAuthorName());

        PagedList<BookResponse> result = pipeline.send(query);

        return ResponseEntity.ok(ResultRes.successResult(result, BookMessageConstants.LIST_SUCCESS, 200));
    }

    @GetMapping("/api/v1/books")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Books.INTERNAL_VIEW + "')")
    public ResponseEntity<ResultRes<PagedList<BookResponse>>> getInternalBooks(GetInternalBooksQuery query) {
        log.info("REST request to list internal books: page={}, size={}, search={}, active={}",
                query.getPageNumber(), query.getPageSize(), query.getSearchKeyword(), query.getActive());

        PagedList<BookResponse> result = pipeline.send(query);

        return ResponseEntity.ok(ResultRes.successResult(result, BookMessageConstants.INTERNAL_LIST_SUCCESS, 200));
    }

    @PostMapping(value = "/api/v1/books", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('" + PermissionConstants.Books.CREATE + "')")
    public ResponseEntity<ResultRes<BookResponse>> createBook(
            @RequestParam("title") String title,
            @RequestParam(value = "introduce", required = false) String introduce,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "categoryIds", required = false) Set<UUID> categoryIds,
            @RequestParam(value = "authorIds", required = false) Set<UUID> authorIds,
            @RequestParam(value = "badgeId", required = false) UUID badgeId,
            @RequestPart("coverFile") MultipartFile coverFile) {

        log.info("REST request to create book: title={}", title);

        java.io.InputStream inputStream;
        try {
            inputStream = coverFile.getInputStream();
        } catch (java.io.IOException e) {
            throw new BusinessValidationException(
                    BookMessageConstants.READ_COVER_ERROR,
                    BookMessageConstants.CODE_READ_COVER_ERROR);
        }

        CreateBookCommand cmd = new CreateBookCommand(
                title,
                introduce,
                description,
                categoryIds,
                authorIds,
                badgeId,
                inputStream,
                coverFile.getOriginalFilename(),
                coverFile.getContentType(),
                coverFile.getSize());

        var violations = validator.validate(cmd);
        if (!violations.isEmpty()) {
            var msg = violations.stream().map(v -> v.getMessage()).collect(Collectors.joining("; "));
            throw new BusinessValidationException(msg, "VALIDATION_ERROR");
        }

        BookResponse result = pipeline.send(cmd);

        return ResponseEntity.ok(ResultRes.successResult(result, BookMessageConstants.CREATE_SUCCESS, 200));
    }

    @PutMapping(value = "/api/v1/books/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('" + PermissionConstants.Books.EDIT + "')")
    public ResponseEntity<ResultRes<BookResponse>> updateBook(
            @PathVariable("id") UUID id,
            @RequestParam("title") String title,
            @RequestParam(value = "introduce", required = false) String introduce,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "categoryIds", required = false) Set<UUID> categoryIds,
            @RequestParam(value = "authorIds", required = false) Set<UUID> authorIds,
            @RequestParam(value = "badgeId", required = false) UUID badgeId,
            @RequestParam(value = "active", required = false) Boolean active,
            @RequestPart(value = "coverFile", required = false) MultipartFile coverFile) {

        log.info("REST request to update book: id={}, title={}, active={}", id, title, active);

        java.io.InputStream inputStream = null;
        String originalFilename = null;
        String contentType = null;
        long size = 0;

        if (coverFile != null && !coverFile.isEmpty()) {
            try {
                inputStream = coverFile.getInputStream();
                originalFilename = coverFile.getOriginalFilename();
                contentType = coverFile.getContentType();
                size = coverFile.getSize();
            } catch (java.io.IOException e) {
                throw new BusinessValidationException(
                        BookMessageConstants.READ_COVER_ERROR,
                        BookMessageConstants.CODE_READ_COVER_ERROR);
            }
        }

        com.inkpulse.features.book.commands.UpdateBookCommand cmd = new com.inkpulse.features.book.commands.UpdateBookCommand(
                id,
                title,
                introduce,
                description,
                categoryIds,
                authorIds,
                badgeId,
                inputStream,
                originalFilename,
                contentType,
                size,
                active);

        var violations = validator.validate(cmd);
        if (!violations.isEmpty()) {
            var msg = violations.stream().map(v -> v.getMessage()).collect(Collectors.joining("; "));
            throw new BusinessValidationException(msg, "VALIDATION_ERROR");
        }

        BookResponse result = pipeline.send(cmd);

        return ResponseEntity.ok(ResultRes.successResult(result, "Cập nhật thông tin sách thành công!", 200));
    }

    @DeleteMapping("/api/v1/books/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Books.DELETE + "')")
    public ResponseEntity<ResultRes<Boolean>> deleteBook(@PathVariable("id") UUID id) {
        log.info("REST request to delete book: id={}", id);

        com.inkpulse.features.book.commands.DeleteBookCommand cmd = new com.inkpulse.features.book.commands.DeleteBookCommand(
                id);
        Boolean result = pipeline.send(cmd);

        return ResponseEntity.ok(ResultRes.successResult(result, "Xóa sách thành công!", 200));
    }

    @GetMapping("/api/v1/books/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Books.INTERNAL_VIEW + "')")
    public ResponseEntity<ResultRes<InternalBookDetailResponse>> getInternalBookDetail(@PathVariable("id") UUID id) {
        log.info("REST request to get internal book detail: id={}", id);
        InternalBookDetailResponse result = pipeline.send(new GetInternalBookDetailQuery(id));
        return ResponseEntity.ok(ResultRes.successResult(result, BookMessageConstants.DETAIL_SUCCESS, 200));
    }
}
