/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cognitivabrasil.repositorio.data.services;

import cognitivabrasil.obaa.General.General;
import com.cognitivabrasil.repositorio.data.entities.Document;
import com.cognitivabrasil.repositorio.data.entities.Files;
import com.cognitivabrasil.repositorio.data.entities.Subject;
import com.cognitivabrasil.repositorio.data.repositories.DocumentRepository;
import com.cognitivabrasil.repositorio.services.DocumentService;
import com.cognitivabrasil.repositorio.services.FileService;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import org.joda.time.LocalDate;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionConfiguration;

/**
 * Integration tests of the DocumentService
 *
 * @author Marcos Freitas Nunes <marcos@cognitivabrasil.com.br>
 * @author Paulo Schreiner <paulo@cognitivabrasil.com.br>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "file:src/main/webapp/WEB-INF/spring/root-context.xml")
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class})
@TransactionConfiguration(transactionManager = "transactionManager", defaultRollback = true)
public class DocumentServiceIT extends AbstractTransactionalJUnit4SpringContextTests {

    @Autowired
    private DocumentService docService;
    @Autowired
    private DocumentRepository docRep;
    @Autowired
    private FileService fService;
    @PersistenceContext
    private EntityManager em;

    @Test
    public void testGetDocument() {
        Document d = docService.get(1);
        assertThat(d.getCreated().toLocalDate(), equalTo(new LocalDate(2013, 5, 8)));
        assertThat(d.getMetadata(), notNullValue());
        assertThat(d.getObaaEntry(), equalTo("entry1"));

        General obaaGeneral = d.getMetadata().getGeneral();
        assertThat(obaaGeneral.getTitles(), hasSize(1));
        assertThat(obaaGeneral.getTitles().get(0), equalTo("Ataque a o TCP - Mitnick"));
        assertThat(obaaGeneral.getKeywords(), hasSize(1));
        assertThat(obaaGeneral.getKeywords().get(0), equalTo("TCP"));

        List<Files> sf = d.getFiles();
        assertThat(sf, hasSize(2));
        Files f = fService.get(1);
        assertThat(sf.contains(f), equalTo(true));
        Files f2 = fService.get(2);
        assertThat(sf.contains(f2), equalTo(true));
    }

    @Test
    public void testDeleteDocument() {
        int before = docService.getAll().size();
        int filesBefore = fService.getAll().size();

        Document d = docService.get(1);
        int numFiles = d.getFiles().size();

        docService.delete(d);

        em.flush();
        em.clear();

        assertThat(docService.getAll(), hasSize(before - 1));
        assertThat(fService.getAll(), hasSize(filesBefore - numFiles));
    }

    @Test
    public void testDeletedDocument() {
        Document d = docService.get(1);
        docService.delete(d);

        em.flush();
        em.clear();

        d = docService.get(1);
        assertThat(d.isDeleted(), equalTo(true));
        assertThat(d.getFiles(), hasSize(0));
        assertThat(d.getObaaXml(), nullValue());
    }

    @Test
    public void testOrderFiles() {
        Document d = docService.get(1);

        assertThat(d.getFiles().get(0).getName(), equalTo("teste.txt"));
        assertThat(d.getFiles().get(1).getName(), equalTo("teste2.txt"));
    }

    @Test
    public void testDeleteALLDocuments() {

        docService.deleteAll();

        assertThat(docService.getAll(), hasSize(0));
        assertThat(fService.getAll(), hasSize(0));
    }

    @Test
    public void testGetByObaaEntry() {
        Document d = docService.get("entry2");
        assertThat(d, notNullValue());
        assertThat(d.getId(), equalTo(2));
    }

    @Test
    public void testSave() {
        Document d = new Document();
        d.setObaaEntry("jorjao");

        List<Files> h = new ArrayList<>();

        Files f1 = new Files();
        f1.setLocation("someplace");
        f1.setName("file.pdf");
        h.add(f1);
        f1.setDocument(d);

        d.setFiles(h);

        docService.save(d);

        em.flush();

        Document d2 = docService.get("jorjao");
        assertThat(d2, notNullValue());

        assertThat(d2.getFiles(), notNullValue());
        Files f2 = null;
        for (Files f : d2.getFiles()) {
            f2 = f;
        }
        assertEquals(f2.getLocation(), "someplace");

    }

    @Test
    public void testOwner() {
        Document d = docService.get(1);
        assertThat(d.getOwner().getName(), equalTo("Marcos Nunes"));
    }

    @Test
    public void testDeleteEmpty() {
        int before;
        before = docRep.findAll().size(); //tem que ser com rep pq o service retorna apenas os que não foram deletados
        docService.deleteEmpty();
        assertThat(docRep.findAll().size(), equalTo(before - 1));
    }

    @Test
    public void testSubject() {
        Document d = docService.get(5);
        assertThat(d.getSubject().getName(), equalTo("ciencias"));
    }

    @Test
    public void testGetBySubject() {
        Subject subject = new Subject();
        subject.setName("ciencias");
        subject.setId(1);
        List<Document> docs = docService.getBySubject(subject);

        assertThat(docs, hasSize(1));
        assertThat(docs.get(0).getId(), equalTo(5));
    }

    @Test
    public void testDeleteWithFile() throws IOException {
        String location = "./src/test/resources/file.test";
        File testFile = new File(location);

        try (PrintWriter gravador = new PrintWriter(new FileWriter(testFile))) {
            gravador.print("Arquivo criado pelo teste. Pode ser apagado sem problemas.");
        }
        Document d = docService.get(3);
        d.getFiles().get(0).setLocation(location);
        docService.save(d);

        docService.delete(d);
    }
}
