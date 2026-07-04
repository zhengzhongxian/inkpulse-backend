package com.inkpulse.controllers;

import an.awesome.pipelinr.Pipeline;
import com.inkpulse.constants.PermissionConstants;
import com.inkpulse.constants.message.PublisherMessageConstants;
import com.inkpulse.features.publisher.commands.CreatePublisherCommand;
import com.inkpulse.features.publisher.commands.UpdatePublisherCommand;
import com.inkpulse.features.publisher.commands.DeletePublisherCommand;
import com.inkpulse.features.publisher.dto.PublisherResponse;
import com.inkpulse.features.publisher.queries.GetPublisherByIdQuery;
import com.inkpulse.features.publisher.queries.GetPublishersQuery;
import com.inkpulse.features.publisher.queries.GetPagedPublishersQuery;
import com.inkpulse.models.pagination.PagedList;
import com.inkpulse.models.response.ResultRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PublisherController {

    private final Pipeline pipeline;

    @GetMapping("/api/v1/publishers")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Publishers.VIEW + "')")
    public ResponseEntity<ResultRes<List<PublisherResponse>>> getPublishers() {
        log.info("REST request to get publishers list");
        List<PublisherResponse> result = pipeline.send(new GetPublishersQuery());
        return ResponseEntity.ok(ResultRes.successResult(result, PublisherMessageConstants.LIST_SUCCESS, 200));
    }

    @GetMapping("/api/v1/publishers/paged")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Publishers.INTERNAL_VIEW + "')")
    public ResponseEntity<ResultRes<PagedList<PublisherResponse>>> getPagedPublishers(
            @RequestParam(value = "pageNumber", defaultValue = "1") int pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "searchKeyword", required = false) String searchKeyword) {
        log.info("REST request to get paged publishers: page={}, size={}, search={}", pageNumber, pageSize, searchKeyword);
        GetPagedPublishersQuery query = new GetPagedPublishersQuery();
        query.setPageNumber(pageNumber);
        query.setPageSize(pageSize);
        query.setSearchKeyword(searchKeyword);
        PagedList<PublisherResponse> result = pipeline.send(query);
        return ResponseEntity.ok(ResultRes.successResult(result, PublisherMessageConstants.LIST_SUCCESS, 200));
    }

    @PostMapping("/api/v1/publishers")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Publishers.CREATE + "')")
    public ResponseEntity<ResultRes<PublisherResponse>> createPublisher(@RequestBody CreatePublisherCommand command) {
        log.info("REST request to create publisher: {}", command.getName());
        PublisherResponse result = pipeline.send(command);
        return ResponseEntity.ok(ResultRes.successResult(result, PublisherMessageConstants.CREATE_SUCCESS, 200));
    }

    @GetMapping("/api/v1/publishers/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Publishers.VIEW + "')")
    public ResponseEntity<ResultRes<PublisherResponse>> getPublisherById(@PathVariable("id") UUID id) {
        log.info("REST request to get publisher details: {}", id);
        PublisherResponse result = pipeline.send(new GetPublisherByIdQuery(id));
        return ResponseEntity.ok(ResultRes.successResult(result, PublisherMessageConstants.GET_SUCCESS, 200));
    }

    @PutMapping("/api/v1/publishers/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Publishers.EDIT + "')")
    public ResponseEntity<ResultRes<PublisherResponse>> updatePublisher(
            @PathVariable("id") UUID id,
            @RequestBody UpdatePublisherCommand command) {
        log.info("REST request to update publisher: {}", id);
        command.setId(id);
        PublisherResponse result = pipeline.send(command);
        return ResponseEntity.ok(ResultRes.successResult(result, PublisherMessageConstants.UPDATE_SUCCESS, 200));
    }

    @DeleteMapping("/api/v1/publishers/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.Publishers.DELETE + "')")
    public ResponseEntity<ResultRes<Boolean>> deletePublisher(@PathVariable("id") UUID id) {
        log.info("REST request to delete publisher: {}", id);
        Boolean result = pipeline.send(new DeletePublisherCommand(id));
        return ResponseEntity.ok(ResultRes.successResult(result, PublisherMessageConstants.DELETE_SUCCESS, 200));
    }
}
