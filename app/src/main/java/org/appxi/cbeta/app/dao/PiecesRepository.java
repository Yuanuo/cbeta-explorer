package org.appxi.cbeta.app.dao;

import org.appxi.cbeta.app.AppContext;
import org.appxi.search.solr.Piece;
import org.appxi.search.solr.PieceRepository;
import org.appxi.util.StringHelper;
import org.springframework.data.domain.Pageable;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.FacetOptions;
import org.springframework.data.solr.core.query.HighlightOptions;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleFacetAndHighlightQuery;
import org.springframework.data.solr.core.query.SimpleFilterQuery;
import org.springframework.data.solr.core.query.SimpleStringCriteria;
import org.springframework.data.solr.core.query.result.FacetAndHighlightPage;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.stream.Collectors;

@Repository
public interface PiecesRepository extends PieceRepository {
    default FacetAndHighlightPage<Piece> search(String profile, Collection<String> scopes,
                                                final String input,
                                                Collection<String> categories, boolean facet, Pageable pageable) {
        final SimpleFacetAndHighlightQuery query = new SimpleFacetAndHighlightQuery();
        query.setDefType("edismax");
        query.setDefaultOperator(Query.Operator.AND);
        query.setPageRequest(pageable);
        query.addProjectionOnFields("id", "score", "field_file_s", "type_s", "title_s",
                "field_book_s", "field_author_txt_aio", "field_location_s", "field_anchor_s", "category_ss",
                "text_txt_aio_sub");

        if (null != profile)
            query.addFilterQuery(new SimpleFilterQuery(Criteria.where("project_ss").is(profile)));

        //
        if (null != scopes && !scopes.isEmpty()) {
            Criteria criteria = null;
            for (String scope : scopes) {
                scope = PieceRepository.wrapWhitespace(scope);
                if (null == criteria) {
                    if (scope.startsWith("nav/")) {
                        criteria = Criteria.where("category_ss").startsWith(scope);
                    } else {
                        criteria = Criteria.where("field_book_s").is(scope);
                    }
                } else {
                    if (scope.startsWith("nav/")) {
                        criteria = criteria.or("category_ss").startsWith(scope);
                    } else {
                        criteria = criteria.or("field_book_s").is(scope);
                    }
                }
            }
            query.addFilterQuery(new SimpleFilterQuery(criteria));
        }
        //
        query.addFilterQuery(new SimpleFilterQuery(Criteria.where("type_s").is("article")));

        final String queryString;
        if (null == input || input.isBlank())
            queryString = "text_txt_aio:*";
        else if (input.contains(":"))
            queryString = input;
        else queryString = "text_txt_aio:($0) OR field_title_txt_aio:($0)^30".replace("$0", input);
        query.addCriteria(new SimpleStringCriteria(queryString));

        if (null != categories && !categories.isEmpty()) {
            query.addFilterQuery(new SimpleFilterQuery(new SimpleStringCriteria(
                    "category_ss:(" +
                    categories.stream().map(PieceRepository::wrapWhitespace).collect(Collectors.joining(" OR "))
                    + ")"
            )));
        }

        if (facet) {
            FacetOptions facetOptions = new FacetOptions();
            facetOptions.addFacetOnField("category_ss").setFacetLimit(4000);
            query.setFacetOptions(facetOptions);
        }

        if (StringHelper.isNotBlank(input)) {
            HighlightOptions highlightOptions = new HighlightOptions();
            highlightOptions.setSimplePrefix("§§hl#pre§§").setSimplePostfix("§§hl#end§§");
            highlightOptions.setFragsize(100).setNrSnipplets(3);
            highlightOptions.addField("text_txt_aio");
            query.setHighlightOptions(highlightOptions);
        }

        try {
            SolrTemplate solrTemplate = AppContext.getBean(SolrTemplate.class);
            return null == solrTemplate ? null
                    : solrTemplate.queryForFacetAndHighlightPage(Piece.REPO, query, Piece.class);
        } catch (Throwable e) {
            return null;
        }
    }
}
