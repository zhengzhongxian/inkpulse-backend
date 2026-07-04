package com.inkpulse.features.book.elastic;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookEditionElasticRepository extends ElasticsearchRepository<BookEditionDocument, String> {
}
