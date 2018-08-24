/*
 * /*******************************************************************************
 *  * Copyright (c) 2016 Cognitiva Brasil - Tecnologias educacionais.
 *  * All rights reserved. This program and the accompanying materials
 *  * are made available either under the terms of the GNU Public License v3
 *  * which accompanies this distribution, and is available at
 *  * http://www.gnu.org/licenses/gpl.html or for any other uses contact
 *  * contato@cognitivabrasil.com.br for information.
 *  ******************************************************************************/

package com.cognitivabrasil.cognix.services;

import ORG.oclc.oai.server.catalog.OaiDocumentService;
import com.cognitivabrasil.cognix.entities.Document;
import com.cognitivabrasil.cognix.entities.Files;
import com.cognitivabrasil.cognix.entities.Subject;
import com.cognitivabrasil.cognix.repositories.DocumentRepository;
import com.cognitivabrasil.cognix.util.Config;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * The Class DocumentServiceImpl.
 *
 * @author Marcos Freitas Nunes <marcos@cognitivabrasil.com.br>
 * @author Paulo Schreiner <paulo@cognitivabrasil.com.br>
 */
@Service
public class DocumentServiceImpl implements DocumentService, OaiDocumentService {

    @Autowired
    private DocumentRepository docRep;

    private final Logger LOG = LoggerFactory.getLogger(DocumentServiceImpl.class);

    @Override
    public Document get(String e) {
        return docRep.findByObaaEntry(e);
    }

    @Override
    public Document get(int i) {
        return docRep.getOne(i);

    }

    @Override
    public List<Document> getAll() {
        return docRep.findByDeletedIsFalseAndObaaXmlNotNullOrderByCreatedDesc();
    }

    @Override
    public Page<Document> getPage(Pageable pageable) {
        return docRep.findByDeletedIsFalseAndObaaXmlNotNullOrderByCreatedDesc(pageable);
    }

    @Override
    public List<Document> getBySubject(Subject s) {
        return docRep.findBySubjectAndDeletedIsFalseAndObaaXmlNotNullOrderByCreatedDesc(s);
    }

    @Override
    public Page<Document> getPageBySubject(Subject s, Pageable pageable) {
        return docRep.findBySubjectAndDeletedIsFalseAndObaaXmlNotNullOrderByCreatedDesc(s, pageable);
    }

    @Override
    public void delete(Document d) throws IOException {

        try {
            FileUtils.forceDelete(new File(Config.FILE_PATH + d.getId()));
        } catch (IOException io) {
            LOG.warn("Nao foi possivel deletar os arquivos do documento: " + d.getId() + "."
                    + "Mas o documento sera removido da base! " + io.getMessage());
            throw io;
        } finally {
            d.getFiles().clear();
            d.setObaaXml(null);
            d.setCreated(new DateTime());
            d.setDeleted(true);
            docRep.save(d);
        }

    }

    @Override
    public void deleteAll() {
        for (Document d : docRep.findAll()) {
            for (Files f : d.getFiles()) {
                try {
                    f.deleteFile();
                } catch (IOException e) {
                    LOG.error("Could not delete file", e);
                }
            }
            docRep.delete(d);
        }
    }

    @Override
    public void deleteFromDatabase(Document d) {
        docRep.delete(d);
    }


    /*
     * (non-Javadoc)
     *
     * @see modelos.DocumentosDAO#save(OBAA.OBAA, metadata.Header)
     */
    @Override
    public void save(Document d) throws IllegalStateException {
        //para garantir que todas alterações feitas no obaa serão salvar no xml.
        if(!d.getMetadata().isEmpty()){
            d.setObaaXml(d.getMetadata().toXml());
        }
        docRep.save(d);
    }

    @Override
    public void deleteEmpty() {
        DateTime d = DateTime.now();
        List<Document> docs = docRep.findByCreatedLessThanAndActiveIsFalse(d.minusHours(3));
        for (Document doc : docs) {
            deleteFromDatabase(doc);
        }
    }

    @Override
    public Iterator find(Date from, Date until, int oldCount, int maxListSize) {
        PageRequest pr = new PageRequest(oldCount / maxListSize, maxListSize, Sort.Direction.ASC, "created");

        if (from != null && until != null) {
            return docRep.betweenInclusive(new DateTime(from), add1Second(new DateTime(until)), pr).iterator();
        } else if (from != null && until == null) {
            return docRep.from(new DateTime(from), pr).iterator();
        } else if (from == null && until != null) {
            return docRep.until(add1Second(new DateTime(until)), pr).iterator();

        } else {
            return docRep.all(pr).iterator();
        }
    }

    @Override
    public int count(Date from, Date until) {
        if (from != null && until != null) {
            return docRep.countBetweenInclusive(new DateTime(from), add1Second(new DateTime(until)));
        } else if (from != null && until == null) {
            return docRep.countFrom(new DateTime(from));
        } else if (from == null && until != null) {
            return docRep.countUntil(add1Second(new DateTime(until)));

        } else {
            return docRep.countActiveTrue();
        }

    }

    private DateTime add1Second(DateTime dateTime) {
        return dateTime.plusSeconds(1);
    }

    @Override
    public long count(){
        return docRep.countActiveTrueDeletedFalse();
    }

}