package org.appxi.cbeta.app;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.appxi.cbeta.app.event.GenericEvent;
import org.appxi.javafx.app.BaseApp;
import org.appxi.search.solr.Piece;
import org.appxi.util.FileHelper;
import org.appxi.util.StringHelper;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.repository.config.EnableSolrRepositories;

import java.net.URL;
import java.nio.file.Path;
import java.util.Set;

@Configuration
@EnableSolrRepositories("org.appxi.cbeta.app.dao")
public class SpringConfig {
    static BaseApp app;

    static void setup(BaseApp app) {
        SpringConfig.app = app;
    }

    private static final Object _initBeans = new Object();
    private static AnnotationConfigApplicationContext beans;

    public static AnnotationConfigApplicationContext beans() {
        if (null != beans)
            return beans;
        synchronized (_initBeans) {
            if (null != beans)
                return beans;
            try {
                beans = new AnnotationConfigApplicationContext(SpringConfig.class) {
                    @Override
                    public Resource[] getResources(String locationPattern) {
                        if ("classpath*:org/appxi/cbeta/app/dao/**/*.class".equals(locationPattern)) {
                            URL url = AppContext.class.getResource("/org/appxi/cbeta/app/dao/PiecesRepository.class");
                            return null == url ? new Resource[0] : new Resource[]{new UrlResource(url)};
                        }
                        return new Resource[0];
                    }
                };
                app.eventBus.fireEvent(new GenericEvent(GenericEvent.BEANS_READY));
                app.logger.info(StringHelper.concat("beans init after: ",
                        System.currentTimeMillis() - app.startTime));
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return beans;
    }

    public static <T> T getBean(Class<T> requiredType) {
        try {
            return beans().getBean(requiredType);
        } catch (Throwable ignore) {
            return null;
        }
    }

    @Bean
    SolrClient solrClient() throws Exception {
        final Path solrHome = app.workspace.resolve(".solr");
        final Path confHome = app.appDir().resolve("template");

        System.setProperty("solr.dns.prevent.reverse.lookup", "true");
        System.setProperty("solr.install.dir", solrHome.toString());

        FileHelper.makeDirs(solrHome);
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
    SolrTemplate solrTemplate(SolrClient client) {
        return new SolrTemplate(client);
    }
}
