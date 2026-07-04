package com.inkpulse.features.author.elastic;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorElasticRepository extends ElasticsearchRepository<AuthorDocument, String> {
}
