package com.netgrif.workflow.elastic.service;

import com.google.common.collect.ImmutableMap;
import com.netgrif.workflow.auth.domain.LoggedUser;
import com.netgrif.workflow.elastic.domain.ElasticCase;
import com.netgrif.workflow.elastic.domain.ElasticCaseRepository;
import com.netgrif.workflow.elastic.web.CaseSearchRequest;
import com.netgrif.workflow.utils.FullPageRequest;
import com.netgrif.workflow.workflow.domain.Case;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

import static org.elasticsearch.index.query.QueryBuilders.*;

@Service
public class ElasticCaseService implements IElasticCaseService {

    private static final Logger log = LoggerFactory.getLogger(ElasticCaseService.class);

    @Autowired
    private ElasticCaseRepository repository;

    @Autowired
    private ElasticsearchTemplate template;

    private Map<String, Float> fullTextFieldMap = ImmutableMap.of(
            "title", 2f,
            "authorName", 1f,
            "authorEmail", 1f
    );

    /**
     * See {@link QueryStringQueryBuilder#fields(Map)}
     *
     * @return map where keys are ElasticCase field names and values are boosts of these fields
     */
    @Override
    public Map<String, Float> fullTextFields() {
        return fullTextFieldMap;
    }


    @Async
    @Override
    public void index(Case useCase) {
        ElasticCase elasticCase = new ElasticCase(useCase);

        repository.save(elasticCase);

        log.info("[" + useCase.getStringId() + "]: Case \"" + useCase.getTitle() + "\" indexed");
    }

    @Override
    public void indexNow(Case useCase) {
        ElasticCase elasticCase = new ElasticCase(useCase);

        repository.save(elasticCase);

        log.info("[" + useCase.getStringId() + "]: Case \"" + useCase.getTitle() + "\" indexed");
    }

    @Override
    public Page<ElasticCase> search(CaseSearchRequest request, LoggedUser user, Pageable pageable) {
        if (request == null) {
            throw new IllegalArgumentException("Request can not be null!");
        }

        SearchQuery query = buildQuery(request, user, pageable);

        return template.queryForPage(query, ElasticCase.class);
    }

    @Override
    public long count(CaseSearchRequest request, LoggedUser user) {
        if (request == null) {
            throw new IllegalArgumentException("Request can not be null!");
        }

        SearchQuery query = buildQuery(request, user, new FullPageRequest());

        return template.count(query);
    }

    private SearchQuery buildQuery(CaseSearchRequest request, LoggedUser user, Pageable pageable) {
        NativeSearchQueryBuilder builder = new NativeSearchQueryBuilder();
        BoolQueryBuilder query = boolQuery();

        buildPetriNetQuery(request, user, query);
        buildAuthorQuery(request, query);
        buildTaskQuery(request, query);
        buildRoleQuery(request, query);
        buildDataQuery(request, query);
        buildFullTextQuery(request, query);
        buildStringQuery(request, query);

        return builder
                .withQuery(query)
                .withPageable(pageable)
                .build();
    }

    /**
     * Cases with processIdentifier "id" <br>
     * <pre>
     * {
     *     "petriNet": {
     *         "identifier": "id"
     *     }
     * }</pre><br>
     * <p>
     * Cases with processIdentifiers "1" OR "2" <br>
     * <pre>
     * {
     *     "petriNet": [
     *         {
     *             "identifier": "1"
     *         },
     *         {
     *             "identifier": "2"
     *         }
     *     ]
     * }
     * </pre>
     */
    private void buildPetriNetQuery(CaseSearchRequest request, LoggedUser user, BoolQueryBuilder query) {
        if (request.petriNet == null || request.petriNet.isEmpty()) {
            return;
        }

        BoolQueryBuilder petriNetQuery = boolQuery();

        for (CaseSearchRequest.PetriNet petriNet : request.petriNet) {
            if (petriNet.identifier != null) {
                petriNetQuery.should(termQuery("processIdentifier", petriNet.identifier));
            }
        }

        query.filter(petriNetQuery);
    }

    /**
     * <pre>
     * {
     *     "author": {
     *         "email": "user@customer.com"
     *     }
     * }
     * </pre><br>
     *
     * Cases with author with (id 1 AND email "user@customer.com") OR (id 2)
     * <pre>
     * {
     *     "author": [{
     *         "id": 1
     *         "email": "user@customer.com"
     *     }, {
     *         "id": 2
     *     }
     *     ]
     * }
     * </pre><br>
     */
    private void buildAuthorQuery(CaseSearchRequest request, BoolQueryBuilder query) {
        if (request.author == null || request.author.isEmpty()) {
            return;
        }

        BoolQueryBuilder authorsQuery = boolQuery();
        for (CaseSearchRequest.Author author : request.author) {
            BoolQueryBuilder authorQuery = boolQuery();
            if (author.email != null) {
                authorQuery.must(termQuery("authorEmail", author.email));
            }
            if (author.id != null) {
                authorQuery.must(matchQuery("authorName", author.id));
            }
            if (author.name != null) {
                authorQuery.must(termQuery("author", author.name));
            }
            authorsQuery.should(authorQuery);
        }

        query.filter(authorsQuery);
    }

    /**
     * Cases with tasks with import Id "nova_uloha"
     * <pre>
     * {
     *     "task": "nova_uloha"
     * }
     * </pre>
     * <p>
     * Cases with tasks with import Id "nova_uloha" OR "kontrola"
     * <pre>
     * {
     *     "task": [
     *         "nova_uloha",
     *         "kontrola"
     *     ]
     * }
     * </pre>
     */
    private void buildTaskQuery(CaseSearchRequest request, BoolQueryBuilder query) {
        if (request.task == null || request.task.isEmpty()) {
            return;
        }

        BoolQueryBuilder taskQuery = boolQuery();
        for (String taskImportId : request.task) {
            taskQuery.should(termQuery("taskIds", taskImportId));
        }

        query.filter(taskQuery);
    }

    /**
     * Cases with active role "5cb07b6ff05be15f0b972c36"
     * <pre>
     * {
     *     "role": "5cb07b6ff05be15f0b972c36"
     * }
     * </pre>
     *
     * Cases with active role "5cb07b6ff05be15f0b972c36" OR "5cb07b6ff05be15f0b972c31"
     * <pre>
     * {
     *     "role" [
     *         "5cb07b6ff05be15f0b972c36",
     *         "5cb07b6ff05be15f0b972c31"
     *     ]
     * }
     * </pre>
     */
    private void buildRoleQuery(CaseSearchRequest request, BoolQueryBuilder query) {
        if (request.role == null || request.role.isEmpty()) {
            return;
        }

        BoolQueryBuilder roleQuery = boolQuery();
        for (String roleId : request.role) {
            roleQuery.should(termQuery("enabledRoles", roleId));
        }

        query.filter(roleQuery);
    }

    /**
     * Cases where "text_field" has value "text" AND "number_field" has value 125.<br>
     * <pre>
     * {
     *     "data": {
     *         "text_field": "text",
     *         "number_field": "125"
     *     }
     * }
     * </pre>
     */
    private void buildDataQuery(CaseSearchRequest request, BoolQueryBuilder query) {
        if (request.data == null || request.data.isEmpty()) {
            return;
        }

        BoolQueryBuilder dataQuery = boolQuery();
        for (Map.Entry<String, String> field : request.data.entrySet()) {
            dataQuery.must(matchQuery("dataSet." + field.getKey(), field.getValue()));
        }

        query.filter(dataQuery);
    }

    /**
     * Full text search on fields defined by {@link #fullTextFields()}.
     */
    private void buildFullTextQuery(CaseSearchRequest request, BoolQueryBuilder query) {
        if (request.fullText == null || request.fullText.isEmpty()) {
            return;
        }

        QueryBuilder fullTextQuery = queryStringQuery("*" + request.fullText + "*").fields(fullTextFields());
        query.must(fullTextQuery);
    }

    /**
     * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-query-string-query.html">Query String Query</a>
     */
    private void buildStringQuery(CaseSearchRequest request, BoolQueryBuilder query) {
        if (request.query == null || request.query.isEmpty()) {
            return;
        }

        query.must(queryStringQuery(request.query));
    }
}