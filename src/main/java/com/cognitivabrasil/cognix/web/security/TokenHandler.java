/*
 * Copyright (c) 2019 Cognitiva Brasil Tecnologias Educacionais
 * http://www.cognitivabrasil.com.br
 *
 * All rights reserved. This program and the accompanying materials
 * are made available either under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html or for any other uses contact
 * contato@cognitivabrasil.com.br for information.
 */
package com.cognitivabrasil.cognix.web.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Clock;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.DefaultClock;
import javax.xml.bind.DatatypeConverter;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * @author igor
 */
@Component
public class TokenHandler {
    //To Solr needs to be Base64 encoded
    private static final String KEY_TOKEN = "Yn2kjibddFAWtnPJ2AFlL8WXmohJMCvigQggaEypa5E=";

    private final int MILIS_IN_A_SECOND = 1000;
    private Clock clock = DefaultClock.INSTANCE;

    private int hoursToExpire = 5;
     
    private Long expiration= (long)604800;

    private final Logger log = LoggerFactory.getLogger(TokenHandler.class);

    public SecurityUser parseUserFromToken(String token) {
        Claims body = Jwts.parser()
                .setSigningKey(KEY_TOKEN)
                .parseClaimsJws(token)
                .getBody();
        String username = body.getSubject();
        List<String> roles = body.get("roles", List.class);
        return new SecurityUser(username, roles);
    }
        
    public String createTokenForUser(SecurityUser user) throws UnsupportedEncodingException {
        final Date createdDate = clock.now();
        final Date expirationDate = calculateExpirationDate(createdDate);
        byte[] decodedSecret = Base64.getDecoder().decode(KEY_TOKEN);
        Map map = new HashMap<String,Object>();
        map.put("alg","HS256");
        map.put("typ","JWT");
        
        return Jwts.builder()
                .setHeader(map)
                .setSubject(user.getUsername())
                .setExpiration(expirationDate)
                .setIssuer("edumar.uac.pt")
                .setAudience("Solr")
                .claim("roles", user.getRoles())
                .claim("name", user.getName())
                .signWith(SignatureAlgorithm.HS256, decodedSecret)
                .compact();
    }
        private Date calculateExpirationDate(Date createdDate) {
        return new Date(createdDate.getTime() + expiration * MILIS_IN_A_SECOND);
    }
    
    
}
