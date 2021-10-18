package org.appxi.cbeta.explorer;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.prefs.UserPrefs;
import org.appxi.search.solr.Piece;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.repository.config.EnableSolrRepositories;

import java.nio.file.Path;
import java.util.Set;

@Configuration
@EnableSolrRepositories("org.appxi.cbeta.explorer.dao")
class SpringConfig {
    @Bean
    SolrClient solrClient() throws Exception {
        final Path solrHome = UserPrefs.dataDir().resolve(".solr");
        final Path confHome = FxHelper.appDir().resolve("template");

        final NodeConfig config = new NodeConfig.NodeConfigBuilder(Piece.REPO, solrHome)
                .setConfigSetBaseDirectory(confHome.toString())
                .setAllowPaths(Set.of(Path.of("_ALL_"), solrHome))
                .build();

        final EmbeddedSolrServer solrClient = new EmbeddedSolrServer(config, Piece.REPO);
        if (null != solrClient.getCoreContainer() && solrClient.getCoreContainer().getCoreDescriptor(Piece.REPO) == null) {
            final CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
            createRequest.setCoreName(Piece.REPO);
            createRequest.setConfigSet(Piece.REPO);
            solrClient.request(createRequest);
        }

        return solrClient;
    }

    @Bean
    SolrTemplate solrTemplate(SolrClient client) throws Exception {
        return new SolrTemplate(client);
    }
}
