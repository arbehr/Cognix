/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cognitivabrasil.cognix.repositories;

import com.cognitivabrasil.cognix.entities.User;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;

/**
 *
 * @author Marcos Freitas Nunes <marcos@cognitivabrasil.com.br>
 */
@RunWith(SpringRunner.class)
@DataJpaTest
public class UserRepositoryITest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository service;

    @Test
    public void testGetByLogin() {
        User u = new User();
        u.setUsername("mfnunes");
        u.setName("Marcos Nunes");
        entityManager.persist(u);
        entityManager.flush();

        User result = service.findByUsername(u.getUsername());
        assertThat(result.getName(), equalTo(u.getName()));
    }
}
