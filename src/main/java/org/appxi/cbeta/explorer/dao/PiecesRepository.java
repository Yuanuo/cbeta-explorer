package org.appxi.cbeta.explorer.dao;

import org.appxi.cbeta.explorer.AppContext;
import org.appxi.search.solr.Piece;
import org.appxi.search.solr.PieceRepository;
import org.appxi.util.StringHelper;
import org.springframework.data.domain.Pageable;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.*;
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
            scopes.forEach(s -> {
                if (s.startsWith("nav/")) {
                    query.addFilterQuery(new SimpleFilterQuery(
                            new SimpleStringCriteria("category_ss:" + PieceRepository.wrapWhitespace(s) + "*")));
                } else {
                    query.addFilterQuery(new SimpleFilterQuery(
                            new SimpleStringCriteria("field_book_s:" + s)));
                }
            });
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
            query.addFilterQuery(new SimpleFilterQuery(new SimpleStringCriteria("category_ss:(" +
                    categories.stream().map(PieceRepository::wrapWhitespace).collect(Collectors.joining(" OR "))
                    + ")")));
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
