package org.appxi.cbeta.explorer;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.appxi.prefs.UserPrefs;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.repository.config.EnableSolrRepositories;

import java.nio.file.Path;

@Configuration
@EnableSolrRepositories("org.appxi.cbeta.explorer.dao")
class AppConfig {
    static SolrClient cachedSolrClient;

    @Bean
    SolrClient solrClient() throws Exception {
        final String coreName = "pieces";
        final Path solrHome = UserPrefs.dataDir().resolve(".solr");
        final Path confHome = UserPrefs.appDir().resolve("template");

        final NodeConfig config = new NodeConfig.NodeConfigBuilder(coreName, solrHome)
                .setConfigSetBaseDirectory(confHome.toString())
                .build();

        final EmbeddedSolrServer solrClient = new EmbeddedSolrServer(config, coreName);
        if (null != solrClient.getCoreContainer() && solrClient.getCoreContainer().getCoreDescriptor(coreName) == null) {
            final CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
            createRequest.setCoreName(coreName);
            createRequest.setConfigSet(coreName);
            solrClient.request(createRequest);
        }

        return cachedSolrClient = solrClient;
    }

//    @Bean
//    public SolrClient solrClient() {
//        return new Http2SolrClient.Builder("http://localhost:8983/solr/").build();
//    }

    @Bean
    SolrTemplate solrTemplate(SolrClient client) throws Exception {
        return new SolrTemplate(client);
    }
}
