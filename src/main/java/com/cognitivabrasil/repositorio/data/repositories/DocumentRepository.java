/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cognitivabrasil.repositorio.data.repositories;

import com.cognitivabrasil.repositorio.data.entities.Document;
import com.cognitivabrasil.repositorio.data.entities.Subject;
import com.cognitivabrasil.repositorio.data.entities.User;
import java.util.List;
import org.joda.time.DateTime;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author Marcos Freitas Nunes <marcos@cognitivabrasil.com.br>
 */
public interface DocumentRepository extends JpaRepository<Document, Integer> {

    @Override
    public List<Document> findAll();
    
    public List<Document> findBySubjectAndDeletedIsFalseAndObaaXmlNotNullOrderByCreatedDesc(Subject s);
    
    public List<Document> findByDeletedIsFalseAndObaaXmlNotNullOrderByCreatedDesc();
    
    public Document findByObaaEntry(String entry);
    
    public List<Document> findByCreatedLessThanAndObaaXmlIsNullAndDeletedIsFalse(DateTime d);
    
    public long countByOwnerAndDeletedIsFalse(User u);
}
