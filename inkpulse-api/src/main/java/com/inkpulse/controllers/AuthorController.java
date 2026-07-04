package com.inkpulse.controllers;

import an.awesome.pipelinr.Pipeline;
import com.inkpulse.constants.PermissionConstants;
import com.inkpulse.constants.message.BookMessageConstants;
import com.inkpulse.constants.message.AuthorMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.corehelpers.images.UploadFileModel;
import com.inkpulse.features.author.commands.CreateAuthorCommand;
import com.inkpulse.features.author.commands.DeleteAuthorCommand;
import com.inkpulse.features.author.commands.UpdateAuthorCommand;
import com.inkpulse.features.author.dto.AuthorResponse;
import com.inkpulse.features.author.dto.AuthorDetailResponse;
import com.inkpulse.features.author.queries.GetAuthorsQuery;
import com.inkpulse.features.author.queries.GetInternalAuthorsQuery;
import com.inkpulse.features.author.queries.GetAuthorDetailQuery;
import com.inkpulse.features.author.queries.GetInternalAuthorDetailQuery;
import com.inkpulse.models.pagination.PagedList;
import com.inkpulse.models.response.ResultRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AuthorController {

    private final Pipeline pipeline;

    @GetMapping("/api/v1/public/authors")
    public ResponseEntity<ResultRes<PagedList<AuthorResponse>>> getAuthors(GetAuthorsQuery query) {
        log.info("REST request to list/search authors: keyword={}, page={}, size={}",
                query.getSearchKeyword(), query.getPageNumber(), query.getPageSize());

        PagedList<AuthorResponse> result = pipeline.send(query);

        return ResponseEntity.ok(ResultRes.successResult(result, AuthorMessageConstants.LIST_SUCCESS, 200));
    }

    @GetMapping("/api/v1/authors")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Authors.INTERNAL_VIEW + "')")
    public ResponseEntity<ResultRes<PagedList<AuthorResponse>>> getInternalAuthors(GetInternalAuthorsQuery query) {
        log.info("REST request to list internal authors: keyword={}, page={}, size={}",
                query.getSearchKeyword(), query.getPageNumber(), query.getPageSize());

        PagedList<AuthorResponse> result = pipeline.send(query);

        return ResponseEntity.ok(ResultRes.successResult(result, AuthorMessageConstants.INTERNAL_LIST_SUCCESS, 200));
    }

    @GetMapping("/api/v1/public/authors/{id}")
    public ResponseEntity<ResultRes<AuthorDetailResponse>> getAuthorDetail(@PathVariable("id") UUID id) {
        log.info("REST request to get public author detail: ID={}", id);

        GetAuthorDetailQuery query = new GetAuthorDetailQuery(id);
        AuthorDetailResponse result = pipeline.send(query);

        return ResponseEntity.ok(ResultRes.successResult(result, AuthorMessageConstants.DETAIL_SUCCESS, 200));
    }

    @GetMapping("/api/v1/authors/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Authors.INTERNAL_VIEW + "')")
    public ResponseEntity<ResultRes<AuthorDetailResponse>> getInternalAuthorDetail(@PathVariable("id") UUID id) {
        log.info("REST request to get internal author detail: ID={}", id);

        GetInternalAuthorDetailQuery query = new GetInternalAuthorDetailQuery(id);
        AuthorDetailResponse result = pipeline.send(query);

        return ResponseEntity.ok(ResultRes.successResult(result, AuthorMessageConstants.DETAIL_SUCCESS, 200));
    }

    @PostMapping(value = "/api/v1/authors", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('" + PermissionConstants.Authors.CREATE + "')")
    public ResponseEntity<ResultRes<AuthorResponse>> createAuthor(
            @RequestParam("name") String name,
            @RequestParam(value = "biography", required = false) String biography,
            @RequestPart(value = "avatarFile", required = false) MultipartFile avatarFile) {

        log.info("REST request to create author: {}", name);

        UploadFileModel avatarModel = null;
        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                avatarModel = new UploadFileModel(
                        avatarFile.getInputStream(),
                        avatarFile.getOriginalFilename(),
                        avatarFile.getContentType(),
                        avatarFile.getSize()
                );
            } catch (IOException e) {
                throw new BusinessValidationException(BookMessageConstants.AVATAR_READ_ERROR, BookMessageConstants.CODE_AVATAR_READ_ERROR);
            }
        }

        CreateAuthorCommand cmd = new CreateAuthorCommand(name, biography, avatarModel);
        AuthorResponse result = pipeline.send(cmd);

        return ResponseEntity.ok(ResultRes.successResult(result, AuthorMessageConstants.CREATE_SUCCESS, 200));
    }

    @PutMapping(value = "/api/v1/authors/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('" + PermissionConstants.Authors.EDIT + "')")
    public ResponseEntity<ResultRes<AuthorResponse>> updateAuthor(
            @PathVariable("id") UUID id,
            @RequestParam("name") String name,
            @RequestParam(value = "biography", required = false) String biography,
            @RequestPart(value = "avatarFile", required = false) MultipartFile avatarFile) {

        log.info("REST request to update author: ID={}, name={}", id, name);

        UploadFileModel avatarModel = null;
        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                avatarModel = new UploadFileModel(
                        avatarFile.getInputStream(),
                        avatarFile.getOriginalFilename(),
                        avatarFile.getContentType(),
                        avatarFile.getSize()
                );
            } catch (IOException e) {
                throw new BusinessValidationException(BookMessageConstants.AVATAR_READ_ERROR, BookMessageConstants.CODE_AVATAR_READ_ERROR);
            }
        }

        UpdateAuthorCommand cmd = new UpdateAuthorCommand(id, name, biography, avatarModel);
        AuthorResponse result = pipeline.send(cmd);

        return ResponseEntity.ok(ResultRes.successResult(result, AuthorMessageConstants.UPDATE_SUCCESS, 200));
    }

    @DeleteMapping("/api/v1/authors/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Authors.DELETE + "')")
    public ResponseEntity<ResultRes<Void>> deleteAuthor(@PathVariable("id") UUID id) {
        log.info("REST request to delete author: ID={}", id);

        DeleteAuthorCommand cmd = new DeleteAuthorCommand(id);
        pipeline.send(cmd);

        return ResponseEntity.ok(ResultRes.successResult(null, AuthorMessageConstants.DELETE_SUCCESS, 200));
    }
}
