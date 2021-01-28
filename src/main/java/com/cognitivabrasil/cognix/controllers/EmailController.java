package com.cognitivabrasil.cognix.controllers;

import com.cognitivabrasil.cognix.entities.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.core.env.Environment;

@RestController
public class EmailController {

    private final Logger log = LoggerFactory.getLogger(EmailController.class);

    @Autowired private JavaMailSender mailSender;

    @Autowired private Environment env;

    @RequestMapping(path = "/email-send", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity sendMail(@RequestBody Email form) {
        //TODO: require token to send email
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setSubject("Edumar - dúvidas, sugestões e/ou comentários");
        message.setText("Nome: " + form.getName() + 
        "\nEmail: " + form.getEmail() + "\nIP: " + form.getIP() + 
        "\n\nMensagem: \n\n" + form.getMessage());
        message.setTo(env.getProperty("administrator.email"));
        message.setFrom(env.getProperty("administrator.email"));

        try {
            mailSender.send(message);
            String jsonResponse ="{ \"msg\":" + "\""+ "Email enviado com sucesso!" + "\"}";
            return new ResponseEntity <> (jsonResponse, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            String jsonResponse ="{ \"msg\":" + "\""+ "Erro ao enviar email." + "\"}";
            return new ResponseEntity <> (jsonResponse, HttpStatus.OK);
        }
    }
}