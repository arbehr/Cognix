/*
 * /*******************************************************************************
 *  * Copyright (c) 2016 Cognitiva Brasil - Tecnologias educacionais.
 *  * All rights reserved. This program and the accompanying materials
 *  * are made available either under the terms of the GNU Public License v3
 *  * which accompanies this distribution, and is available at
 *  * http://www.gnu.org/licenses/gpl.html or for any other uses contact
 *  * contato@cognitivabrasil.com.br for information.
 *  ******************************************************************************/
package com.cognitivabrasil.cognix.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 *
 * @author Marcos Freitas Nunes <marcos@cognitivabrasil.com.br>
 */
@Component
public class Config {

    @Autowired
    private Environment env;

    public static final String FILE_PATH = "/var/seathings/repositorio/";

    public String getUrl() {
        String port = env.getProperty("repository.port", "8080");
        return ("http://"
                + env.getProperty("repository.hostname")
                + (port.equals("80") ? "" : (":" + port))
                // if port 80, dont put anything
                + env.getProperty("repository.rootPath", "/repositorio")
                + "/documents/");
    }

}
