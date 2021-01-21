/*
 * /*******************************************************************************
 *  * Copyright (c) 2016 Cognitiva Brasil - Tecnologias educacionais.
 *  * All rights reserved. This program and the accompanying materials
 *  * are made available either under the terms of the GNU Public License v3
 *  * which accompanies this distribution, and is available at
 *  * http://www.gnu.org/licenses/gpl.html or for any other uses contact
 *  * contato@cognitivabrasil.com.br for information.
 *  ******************************************************************************/
package com.cognitivabrasil.cognix.controllers;

import com.cognitivabrasil.cognix.entities.User;
import com.cognitivabrasil.cognix.entities.dto.MessageDto;
import com.cognitivabrasil.cognix.entities.dto.UserDto;
import com.cognitivabrasil.cognix.entities.validators.UserEditPasswordValidator;
import com.cognitivabrasil.cognix.entities.validators.UserEditValidator;
import com.cognitivabrasil.cognix.services.UserService;
import java.io.IOException;
import java.security.Principal;
import java.util.List;
import static java.util.stream.Collectors.toList;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Marcos Nunes <marcosn@gmail.com>
 */
@RequestMapping("/users")
@RestController
public class UsersController {

    private final Logger log = LoggerFactory.getLogger(UsersController.class);

    @Autowired
    private UserService userService;

    /**
     * Lista os usuários ativos no sistema
     *
     * @return
     */
    // @GetMapping
    private HttpEntity<UserDto> list() {
        List<User> l = userService.getAll();
        List<UserDto> usersDto = l.stream().map(UserDto::new).collect(toList());

        return new ResponseEntity(usersDto, HttpStatus.OK);
    }

    /**
     * Salva um novo usuário no sistema.
     *
     * @param userDto Usuário que será salvo.
     * @param bindingResult
     * @return
     */
    @PostMapping
    private HttpEntity<UserDto> save(@Valid @RequestBody UserDto userDto) {

        User u = userDto.toUser();
        userService.save(u);

        return new ResponseEntity(HttpStatus.CREATED);
    }

    /**
     * Busca um usuário no sistema.
     *
     * @param id identificador do usuário que será retornado.
     * @return
     */
    // @GetMapping(value = "/{id}")
    private HttpEntity<UserDto> get(@PathVariable("id") int id) {
        User u = userService.get(id);
        return new ResponseEntity(new UserDto(u), HttpStatus.OK);
    }

    /**
     * Edita o usuário com o id informado.
     *
     * @param id identificador do usuário que será editado.
     * @param userDto Dados do usuário.
     * @param bindingResult
     * @return
     */
    // @PutMapping(value = "/{id}")
    private HttpEntity<UserDto> edit(@PathVariable("id") int id, @RequestBody UserDto userDto, BindingResult bindingResult) {
        User u = userService.get(id);

        UserEditValidator validator = new UserEditValidator(userService);
        validator.validate(userDto, bindingResult);
        if (bindingResult.hasErrors()) {
            return new ResponseEntity(userDto, HttpStatus.BAD_REQUEST);
        }

        userService.save(userDto.updateUser(u));
        return new ResponseEntity(HttpStatus.OK);
    }

    /**
     * Busca o usuário autenticado e devolve como dto.
     *
     * @param id
     * @param model
     * @param logged
     * @return
     * @throws IOException
     */
    // @RequestMapping(value = "/me", method = RequestMethod.GET)
    private HttpEntity<UserDto> editPass(@PathVariable("id") int id, Model model, Principal logged) throws IOException {
        User u = userService.get(logged.getName());
        UserDto userDto = new UserDto(u);
        return new ResponseEntity(userDto, HttpStatus.OK);
    }

    /**
     * Edita a senha do usuário.
     *
     * @param id
     * @param uDto
     * @param bindingResult
     * @param logged
     * @return
     * @throws IOException
     */
    // @PostMapping(value = "/{id}/editPass")
    private HttpEntity<MessageDto> editPass(@PathVariable("id") int id, @RequestBody UserDto uDto,
            BindingResult bindingResult, Principal logged)
            throws IOException {

        User u = userService.get(id);

        if (!u.getUsername().equals(logged.getName())) {
            return new ResponseEntity(new MessageDto(MessageDto.ERROR, "Você não pode pode editar este usuário."),
                    HttpStatus.FORBIDDEN);
        }

        uDto.setName(u.getName());
        uDto.setUsername(u.getUsername());
        uDto.setRole(u.getRole());

        UserEditPasswordValidator validator = new UserEditPasswordValidator(u, userService);
        validator.validate(uDto, bindingResult);
        if (bindingResult.hasErrors()) {
            return new ResponseEntity(new MessageDto(MessageDto.ERROR, "Erro no formulário"),
                    HttpStatus.BAD_REQUEST);
        }

        userService.save(uDto.updateUser(u));
        return new ResponseEntity(new MessageDto(MessageDto.SUCCESS, "Senha alterada com sucesso"), HttpStatus.OK);
    }

    /**
     * Deleta o usuário.
     *
     * @param id
     * @return retorna o objeto {@link MessageDto} com erro ou sucesso.
     */
    // @DeleteMapping(value = "/{id}")
    private HttpEntity<MessageDto> delete(@PathVariable("id") int id) {
        try {
            User u = userService.get(id);
            userService.delete(u);
            return new ResponseEntity(new MessageDto(MessageDto.SUCCESS, "Usuário excluido com sucesso"), HttpStatus.OK);
        } catch (DataAccessException | IllegalStateException e) {
            log.error("Erro ao excluir um usuário.", e);
            return new ResponseEntity(new MessageDto(MessageDto.ERROR, "Erro ao excluir o usuário. Mensagem de erro:"
                    + e.getMessage()), HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Lista todos os usuário deletados.
     *
     * @return Lista de usuários deletados.
     */
    // @GetMapping(value = "/deleted")
    private HttpEntity<List<UserDto>> getDeleted() {

        List<User> users = userService.getDeleted();
        List<UserDto> usersDto = users.stream().map(UserDto::new).collect(toList());

        return new ResponseEntity(usersDto, HttpStatus.OK);
    }

    /**
     * Ativa o usuário com o id informado.
     *
     * @param id Id do usuário desativado que deve ser ativado.
     * @return
     */
    // @PostMapping(value = "/{id}/activate")
    private HttpEntity<MessageDto> activateUserDeleted(@PathVariable("id") int id) {
        try {
            userService.activate(userService.get(id));
            return new ResponseEntity(new MessageDto(MessageDto.SUCCESS, "Usuário ativado com sucesso"), HttpStatus.OK);
        } catch (DataAccessException e) {
            log.error("Erro ao reativar o usuário: {}",id, e);
            MessageDto msg = new MessageDto(MessageDto.ERROR, "Erro ao reativar o usuário");
            return new ResponseEntity(msg, HttpStatus.BAD_REQUEST);
        }
    }

}
