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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static org.apache.logging.log4j.util.Strings.isNotBlank;
import java.io.UnsupportedEncodingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 *
 * @author igor
 */
@Component
public class TokenAuthenticationService {

    public static final String AUTH_HEADER_NAME = "Authorization";

    @Autowired
    private TokenHandler tokenHandler;
     private final Logger log = LoggerFactory.getLogger(TokenAuthenticationService.class);

    /**
     * Adiciona no cabeçalho um token válido para o usuário recebido na autenticação.
     *
     * @param response HttpServletResponse onde será inserido o token.
     * @param authentication Autenticação com o usuário autorizado.
     */
    public void addAuthentication(HttpServletResponse response, Authentication authentication) throws UnsupportedEncodingException {
        final SecurityUser user = (SecurityUser) authentication.getPrincipal();
        response.addHeader(AUTH_HEADER_NAME, tokenHandler.createTokenForUser(user));
    }

    /**
     * Pega o token do HttpServletRequest, abre o token e pega o usuário e suas role e devolve em um objeto
     * {@link Authentication}.
     *
     * @param request {@code HttpServletRequest} com o token no cabeçalho.
     * @return Objeto {@link Authentication} com as informações do usuário.
     */
    public Authentication getAuthentication(HttpServletRequest request) {
        final String token = request.getHeader(AUTH_HEADER_NAME);
        //log.debug(token);
        if (isNotBlank(token)) {
            final SecurityUser user = tokenHandler.parseUserFromToken(token);
            log.debug("criou user");
            if (user != null) {
                Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                return auth;
            }
        }
        return null;
    }
}
