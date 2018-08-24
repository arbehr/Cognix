/*
 * /*******************************************************************************
 *  * Copyright (c) 2016 Cognitiva Brasil - Tecnologias educacionais.
 *  * All rights reserved. This program and the accompanying materials
 *  * are made available either under the terms of the GNU Public License v3
 *  * which accompanies this distribution, and is available at
 *  * http://www.gnu.org/licenses/gpl.html or for any other uses contact
 *  * contato@cognitivabrasil.com.br for information.
 *  ******************************************************************************/

package com.cognitivabrasil.cognix.entities.validators;


import com.cognitivabrasil.cognix.entities.dto.UserDto;
import com.cognitivabrasil.cognix.services.UserService;
import static org.apache.commons.lang3.StringUtils.isBlank;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

/**
 *
 * @author marcos
 */
public class UserEditValidator extends UserValidator{

    public UserEditValidator(UserService userService) {
        super(userService);
    }

    @Override
    public void validate(Object target, Errors errors) {
        super.validate(target, errors);

        UserDto u = (UserDto) target;

        if(!(isBlank(u.getPassword()) && isBlank(u.getConfirmPass()))) {
            ValidationUtils.rejectIfEmpty(errors, "password", "required.password", "Informe uma senha de no mínimo 5 dígitos");
            ValidationUtils.rejectIfEmpty(errors, "confirmPass", "required.confirmPass", "Confirme a senha");
        }
    }
}
