package com.free.es.config;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * es7.x配置类
 *
 * @author dinghao
 * @date 2021/3/9
 */
@Configuration
@AllArgsConstructor
@EnableConfigurationProperties(ElasticsearchProperties.class)
public class ElasticSearchConfig {

    private ElasticsearchProperties elasticsearchProperties;

    private List<HttpHost> httpHosts = new ArrayList<>();

    @Bean
    @ConditionalOnMissingBean
    public RestHighLevelClient restHighLevelClient() {
        List<String> clusterNodes = elasticsearchProperties.getClusterNodes();
        if (clusterNodes.isEmpty()) {
            throw new RuntimeException("集群节点不允许为空");
        }
        clusterNodes.forEach(node -> {
            try {
                String[] parts = StringUtils.split(node, ":");
                Assert.notNull(parts, "Must defined");
                Assert.state(parts.length == 2, "Must be defined as 'host:port'");
                httpHosts.add(new HttpHost(parts[0], Integer.parseInt(parts[1]), elasticsearchProperties.getSchema()));
            } catch (Exception e) {
                throw new IllegalStateException("Invalid ES nodes " + "property '" + node + "'", e);
            }
        });
        RestClientBuilder builder = RestClient.builder(httpHosts.toArray(new HttpHost[0]));
        return getRestHighLevelClient(builder, elasticsearchProperties);
    }

    /**
     * get restHistLevelClient
     * @return
     */
    private static RestHighLevelClient getRestHighLevelClient(RestClientBuilder builder, ElasticsearchProperties elasticsearchProperties) {
        // Callback used the default {@link RequestConfig} being set to the {@link CloseableHttpClient}
        builder.setRequestConfigCallback(requestConfigBuilder -> {
            requestConfigBuilder.setConnectTimeout(elasticsearchProperties.getConnectTimeout());
            requestConfigBuilder.setSocketTimeout(elasticsearchProperties.getSocketTimeout());
            requestConfigBuilder.setConnectionRequestTimeout(elasticsearchProperties.getConnectionRequestTimeout());
            return requestConfigBuilder;
        });
        // Callback used to customize the {@link CloseableHttpClient} instance used by a {@link RestClient} instance.
        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            httpClientBuilder.setMaxConnTotal(elasticsearchProperties.getMaxConnectTotal());
            httpClientBuilder.setMaxConnPerRoute(elasticsearchProperties.getMaxConnectPerRoute());
            return httpClientBuilder;
        });
        // Callback used the basic credential auth
        ElasticsearchProperties.Account account = elasticsearchProperties.getAccount();
        if (!StringUtils.isEmpty(account.getUsername()) && !StringUtils.isEmpty(account.getUsername())) {
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(account.getUsername(), account.getPassword()));
        }
        return new RestHighLevelClient(builder);
    }
}
