/*
 * Copyright (c) 2016 Cognitiva Brasil - Tecnologias educacionais.
 * All rights reserved. This program and the accompanying materials
 * are made available either under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html or for any other uses contact
 * contato@cognitivabrasil.com.br for information.
 */

package com.cognitivabrasil.cognix.services;


import com.cognitivabrasil.cognix.entities.Subject;
import com.cognitivabrasil.cognix.repositories.SubjectRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * The Class DocumentServiceImpl.
 *
 * @author Marcos Freitas Nunes <marcos@cognitivabrasil.com.br>
 */
@Service
public class SubjectServiceImpl implements SubjectService {

    @Autowired
    private SubjectRepository subRep;

    @Override
    public Subject getSubjectByName(String name) {
        return subRep.findByName(name);
    }

    @Override
    public List<Subject> getAll() {
        return subRep.findAll();
    }

}
