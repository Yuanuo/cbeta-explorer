package org.appxi.cbeta.explorer.dao;

import org.appxi.cbeta.explorer.AppContext;
import org.appxi.util.StringHelper;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.result.FacetAndHighlightPage;
import org.springframework.data.solr.repository.SolrCrudRepository;

import java.util.Collection;

@NoRepositoryBean
public interface PieceRepository extends SolrCrudRepository<Piece, String> {

    void deleteAllByProjectAndVersion(String project, String version);

    default FacetAndHighlightPage<Piece> query(String project, final String input,
                                               Collection<String> categories, boolean facet, Pageable pageable) {
        final SimpleFacetAndHighlightQuery query = new SimpleFacetAndHighlightQuery();
        query.setDefType("edismax");
        query.setDefaultOperator(Query.Operator.AND);
        query.setPageRequest(pageable);
        query.addProjectionOnFields("id", "score", "path_s", "type_s", "title_s",
                "field_book_s", "field_authors_s", "field_location_s", "field_anchor_s",
                "text_txt_cjk_substr");

        if (null != project)
            query.addFilterQuery(new SimpleFilterQuery(Criteria.where("project_s").is(project)));
        query.addFilterQuery(new SimpleFilterQuery(Criteria.where("type_s").is("article")));

        final String queryString;
        if (null == input || input.isBlank())
            queryString = "text_txt_cjk:*";
        else if (input.contains(":"))
            queryString = input;
        else queryString = "text_txt_cjk:($0) OR field_title_txt_cjk:($0)^30".replace("$0", input);
        query.addCriteria(new SimpleStringCriteria(queryString));

        if (null != categories && !categories.isEmpty()) {
            query.addFilterQuery(new SimpleFilterQuery(Criteria.where("category_ss").is(categories)));
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
            highlightOptions.addField("text_txt_cjk");
            query.setHighlightOptions(highlightOptions);
        }

        SolrTemplate solrTemplate = AppContext.beans().getBean(SolrTemplate.class);
        return solrTemplate.queryForFacetAndHighlightPage("pieces", query, Piece.class);
    }

//    //"{!type=edismax bf=\"sum(linear(boosting_${locale}_f,1000,0))\" qf=\"text_txt_cjk\"}${indexQstr}"
//    @Query(defType = "edismax", value = "text_txt_cjk:(?0) field_title_txt_cjk:(?0)^30",
//            defaultOperator = org.springframework.data.solr.core.query.Query.Operator.OR,
//            fields = {"id", "score", "path_s", "type_s", "title_s",
//                    "field_book_s", "field_authors_s", "field_location_s", "field_anchor_s",
//                    "text_txt_cjk"},
//            filters = {"type_s:(article)"}
//    )
//    @Facet(fields = {"category_ss"}, limit = 150, prefix = "std/")
//    @Highlight(prefix = "§§hl#pre§§", postfix = "§§hl#end§§", fields = "text_txt_cjk", fragsize = 100, snipplets = 3)
//    FacetAndHighlightPage<Piece> query(String input, Pageable page);
}
