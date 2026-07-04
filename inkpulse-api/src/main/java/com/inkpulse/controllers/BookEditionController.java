package com.inkpulse.controllers;

import an.awesome.pipelinr.Pipeline;
import com.inkpulse.constants.PermissionConstants;
import com.inkpulse.constants.message.BookMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.corehelpers.images.UploadFileModel;
import com.inkpulse.features.book.commands.CreateBookEditionCommand;
import com.inkpulse.features.book.commands.UpdateBookEditionCommand;
import com.inkpulse.features.book.commands.DeleteBookEditionCommand;
import com.inkpulse.features.book.dto.BookEditionResponse;
import com.inkpulse.features.book.dto.PublicBookEditionDetailResponse;
import com.inkpulse.features.book.dto.InternalBookEditionDetailResponse;
import com.inkpulse.features.book.queries.GetPublicBookEditionDetailQuery;
import com.inkpulse.features.book.queries.GetInternalBookEditionDetailQuery;
import com.inkpulse.models.response.ResultRes;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class BookEditionController {

    private final Pipeline pipeline;
    private final Validator validator;

    @PostMapping(value = "/api/v1/book-editions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('" + PermissionConstants.Books.CREATE + "')")
    public ResponseEntity<ResultRes<BookEditionResponse>> createBookEdition(
            @RequestParam("bookId") UUID bookId,
            @RequestParam("isbn") String isbn,
            @RequestParam("price") BigDecimal price,
            @RequestParam(value = "oldPrice", required = false) BigDecimal oldPrice,
            @RequestParam(value = "stockQuantity", defaultValue = "0") int stockQuantity,
            @RequestParam(value = "editionNumber", defaultValue = "1") int editionNumber,
            @RequestParam(value = "coverType", required = false) String coverType,
            @RequestParam(value = "pageCount", required = false) Integer pageCount,
            @RequestParam(value = "publicationYear", required = false) Integer publicationYear,
            @RequestParam(value = "dimensions", required = false) String dimensions,
            @RequestParam(value = "language", required = false) String language,
            @RequestParam(value = "publisherId", required = false) UUID publisherId,
            @RequestParam(value = "badgeIds", required = false) List<String> badgeIds,
            @RequestPart(value = "coverFile", required = false) MultipartFile coverFile,
            @RequestPart(value = "pdfFile", required = false) MultipartFile pdfFile,
            @RequestPart(value = "additionalImages", required = false) MultipartFile[] additionalImages) {

        log.info("REST request to create book edition for bookId: {}, ISBN: {}", bookId, isbn);

        UploadFileModel coverModel = null;
        if (coverFile != null && !coverFile.isEmpty()) {
            try {
                coverModel = new UploadFileModel(
                        coverFile.getInputStream(),
                        coverFile.getOriginalFilename(),
                        coverFile.getContentType(),
                        coverFile.getSize());
            } catch (java.io.IOException e) {
                throw new BusinessValidationException(
                        BookMessageConstants.READ_COVER_ERROR,
                        BookMessageConstants.CODE_READ_COVER_ERROR);
            }
        }

        UploadFileModel pdfModel = null;
        if (pdfFile != null && !pdfFile.isEmpty()) {
            try {
                pdfModel = new UploadFileModel(
                        pdfFile.getInputStream(),
                        pdfFile.getOriginalFilename(),
                        pdfFile.getContentType(),
                        pdfFile.getSize());
            } catch (java.io.IOException e) {
                throw new BusinessValidationException(
                        BookMessageConstants.PDF_READ_ERROR,
                        BookMessageConstants.CODE_PDF_READ_ERROR);
            }
        }

        List<UploadFileModel> additionalImageModels = new ArrayList<>();
        if (additionalImages != null && additionalImages.length > 0) {
            for (MultipartFile file : additionalImages) {
                if (file != null && !file.isEmpty()) {
                    try {
                        additionalImageModels.add(new UploadFileModel(
                                file.getInputStream(),
                                file.getOriginalFilename(),
                                file.getContentType(),
                                file.getSize()));
                    } catch (java.io.IOException e) {
                        throw new BusinessValidationException(
                                BookMessageConstants.GALLERY_READ_ERROR,
                                BookMessageConstants.CODE_GALLERY_READ_ERROR);
                    }
                }
            }
        }

        List<UUID> badgeUuids = null;
        if (badgeIds != null) {
            badgeUuids = badgeIds.stream()
                    .filter(id -> id != null && !id.trim().isEmpty())
                    .map(UUID::fromString)
                    .toList();
        }

        CreateBookEditionCommand cmd = CreateBookEditionCommand.builder()
                .bookId(bookId)
                .isbn(isbn)
                .price(price)
                .oldPrice(oldPrice)
                .stockQuantity(stockQuantity)
                .editionNumber(editionNumber)
                .coverType(coverType)
                .pageCount(pageCount)
                .publicationYear(publicationYear)
                .dimensions(dimensions)
                .language(language)
                .publisherId(publisherId)
                .badgeIds(badgeUuids)
                .coverFile(coverModel)
                .pdfFile(pdfModel)
                .additionalImages(additionalImageModels)
                .build();

        var violations = validator.validate(cmd);
        if (!violations.isEmpty()) {
            var msg = violations.stream().map(v -> v.getMessage()).collect(Collectors.joining("; "));
            throw new BusinessValidationException(msg, "VALIDATION_ERROR");
        }

        BookEditionResponse result = pipeline.send(cmd);

        return ResponseEntity.ok(ResultRes.successResult(result, BookMessageConstants.CREATE_EDITION_SUCCESS, 200));
    }

    @GetMapping("/api/v1/public/book-editions/{id}")
    public ResponseEntity<ResultRes<PublicBookEditionDetailResponse>> getPublicBookEditionDetail(
            @PathVariable("id") UUID id) {
        log.info("REST request to get public book edition detail: id={}", id);
        PublicBookEditionDetailResponse result = pipeline.send(new GetPublicBookEditionDetailQuery(id));
        return ResponseEntity.ok(ResultRes.successResult(result, BookMessageConstants.DETAIL_SUCCESS, 200));
    }

    @GetMapping("/api/v1/book-editions/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Books.INTERNAL_VIEW + "')")
    public ResponseEntity<ResultRes<InternalBookEditionDetailResponse>> getInternalBookEditionDetail(
            @PathVariable("id") UUID id) {
        log.info("REST request to get internal book edition detail: id={}", id);
        InternalBookEditionDetailResponse result = pipeline.send(new GetInternalBookEditionDetailQuery(id));
        return ResponseEntity.ok(ResultRes.successResult(result, BookMessageConstants.DETAIL_SUCCESS, 200));
    }

    @PatchMapping(value = "/api/v1/book-editions/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('" + PermissionConstants.Books.EDIT + "')")
    public ResponseEntity<ResultRes<BookEditionResponse>> updateBookEdition(
            @PathVariable("id") UUID id,
            @RequestParam(value = "isbn", required = false) String isbn,
            @RequestParam(value = "price", required = false) BigDecimal price,
            @RequestParam(value = "oldPrice", required = false) BigDecimal oldPrice,
            @RequestParam(value = "stockQuantity", required = false) Integer stockQuantity,
            @RequestParam(value = "editionNumber", required = false) Integer editionNumber,
            @RequestParam(value = "coverType", required = false) String coverType,
            @RequestParam(value = "pageCount", required = false) Integer pageCount,
            @RequestParam(value = "publicationYear", required = false) Integer publicationYear,
            @RequestParam(value = "dimensions", required = false) String dimensions,
            @RequestParam(value = "language", required = false) String language,
            @RequestParam(value = "publisherId", required = false) UUID publisherId,
            @RequestParam(value = "badgeIds", required = false) List<String> badgeIds,
            @RequestPart(value = "coverFile", required = false) MultipartFile coverFile,
            @RequestPart(value = "pdfFile", required = false) MultipartFile pdfFile,
            @RequestPart(value = "additionalImages", required = false) MultipartFile[] additionalImages,
            @RequestParam(value = "retainImageUrls", required = false) List<String> retainImageUrls) {

        log.info("REST request to update book edition: id={}, isbn={}", id, isbn);

        UploadFileModel coverModel = null;
        if (coverFile != null && !coverFile.isEmpty()) {
            try {
                coverModel = new UploadFileModel(
                        coverFile.getInputStream(),
                        coverFile.getOriginalFilename(),
                        coverFile.getContentType(),
                        coverFile.getSize());
            } catch (java.io.IOException e) {
                throw new BusinessValidationException(
                        BookMessageConstants.READ_COVER_ERROR,
                        BookMessageConstants.CODE_READ_COVER_ERROR);
            }
        }

        UploadFileModel pdfModel = null;
        if (pdfFile != null && !pdfFile.isEmpty()) {
            try {
                pdfModel = new UploadFileModel(
                        pdfFile.getInputStream(),
                        pdfFile.getOriginalFilename(),
                        pdfFile.getContentType(),
                        pdfFile.getSize());
            } catch (java.io.IOException e) {
                throw new BusinessValidationException(
                        BookMessageConstants.PDF_READ_ERROR,
                        BookMessageConstants.CODE_PDF_READ_ERROR);
            }
        }

        List<UploadFileModel> additionalImageModels = null;
        if (additionalImages != null) {
            additionalImageModels = new ArrayList<>();
            for (MultipartFile file : additionalImages) {
                if (file != null && !file.isEmpty()) {
                    try {
                        additionalImageModels.add(new UploadFileModel(
                                file.getInputStream(),
                                file.getOriginalFilename(),
                                file.getContentType(),
                                file.getSize()));
                    } catch (java.io.IOException e) {
                        throw new BusinessValidationException(
                                BookMessageConstants.GALLERY_READ_ERROR,
                                BookMessageConstants.CODE_GALLERY_READ_ERROR);
                    }
                }
            }
        }

        List<UUID> badgeUuids = null;
        if (badgeIds != null) {
            badgeUuids = badgeIds.stream()
                    .filter(idStr -> idStr != null && !idStr.trim().isEmpty())
                    .map(UUID::fromString)
                    .toList();
        }

        UpdateBookEditionCommand cmd = UpdateBookEditionCommand.builder()
                .id(id)
                .isbn(isbn)
                .price(price)
                .oldPrice(oldPrice)
                .stockQuantity(stockQuantity)
                .editionNumber(editionNumber)
                .coverType(coverType)
                .pageCount(pageCount)
                .publicationYear(publicationYear)
                .dimensions(dimensions)
                .language(language)
                .publisherId(publisherId)
                .badgeIds(badgeUuids)
                .coverFile(coverModel)
                .pdfFile(pdfModel)
                .additionalImages(additionalImageModels)
                .retainImageUrls(retainImageUrls)
                .build();

        var violations = validator.validate(cmd);
        if (!violations.isEmpty()) {
            var msg = violations.stream().map(v -> v.getMessage()).collect(Collectors.joining("; "));
            throw new BusinessValidationException(msg, "VALIDATION_ERROR");
        }

        BookEditionResponse result = pipeline.send(cmd);
        return ResponseEntity.ok(ResultRes.successResult(result, BookMessageConstants.UPDATE_EDITION_SUCCESS, 200));
    }

    @DeleteMapping("/api/v1/book-editions/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Books.DELETE + "')")
    public ResponseEntity<ResultRes<Boolean>> deleteBookEdition(@PathVariable("id") UUID id) {
        log.info("REST request to delete book edition: id={}", id);
        Boolean result = pipeline.send(new DeleteBookEditionCommand(id));
        return ResponseEntity.ok(ResultRes.successResult(result, BookMessageConstants.DELETE_EDITION_SUCCESS, 200));
    }
}
