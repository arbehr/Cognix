package com.cognitivabrasil.cognix.services;

import com.cognitivabrasil.cognix.services.UserService;
import com.cognitivabrasil.cognix.web.security.SecurityUser;
import com.cognitivabrasil.cognix.web.security.TokenHandler;
import java.io.UnsupportedEncodingException;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.HttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;


@Service
public class SolrServiceImpl implements SolrService {

    @Autowired
    private Environment env;

    @Autowired
    UserService userService;

    @Autowired
    private TokenHandler tokenHandler;

    private static final Logger log = LoggerFactory.getLogger(SolrServiceImpl.class);

    private String getSolrToken(){
        SecurityUser securityUser = new SecurityUser(userService.get("admin"));
        try {
            String token = tokenHandler.createTokenForUser(securityUser);
            return token;
        } catch(UnsupportedEncodingException e) {
            return "";
        }
    }

    @Override
    public boolean createUserSolr(String username){
        String solrPath = env.getProperty("solr.path", "http://localhost:8983");
        HttpClient httpClient = HttpClientBuilder.create().build();
        try {
            HttpPost request = new HttpPost(solrPath + "/solr/admin/authorization");
            StringEntity params = new StringEntity("{\"set-user-role\" : { \"" + username + "\": [\"solr-update\",\"query\"] } }");
            request.addHeader("Content-type", "application/json");
            request.addHeader("Authorization", "Bearer " + getSolrToken());
            request.setEntity(params);
            HttpResponse response = httpClient.execute(request);
            
            if(response.getStatusLine().getStatusCode() == 200) {
                return true;
            }
            
            log.error("Erro ao criar usuário no Solr.");
            return false;
        } catch (Exception ex) {
            log.error("Erro ao criar usuário no Solr.");
            log.error(ex.toString());
            return false;
        }
    }
}