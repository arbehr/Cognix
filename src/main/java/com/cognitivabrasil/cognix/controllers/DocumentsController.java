/*
 * Copyright (c) 2016 Cognitiva Brasil - Tecnologias educacionais.
 * All rights reserved. This program and the accompanying materials
 * are made available either under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html or for any other uses contact
 * contato@cognitivabrasil.com.br for information.
 */
package com.cognitivabrasil.cognix.controllers;

import cognitivabrasil.obaa.Accessibility.Accessibility;
import cognitivabrasil.obaa.Accessibility.Primary;
import cognitivabrasil.obaa.Accessibility.ResourceDescription;
import cognitivabrasil.obaa.Educational.Context;
import cognitivabrasil.obaa.Educational.Educational;
import cognitivabrasil.obaa.Educational.IntendedEndUserRole;
import cognitivabrasil.obaa.Educational.Interaction;
import cognitivabrasil.obaa.Educational.InteractionType;
import cognitivabrasil.obaa.Educational.InteractivityLevel;
import cognitivabrasil.obaa.Educational.InteractivityType;
import cognitivabrasil.obaa.Educational.LearningContentType;
import cognitivabrasil.obaa.Educational.LearningResourceType;
import cognitivabrasil.obaa.Educational.Perception;
import cognitivabrasil.obaa.Educational.Reciprocity;
import cognitivabrasil.obaa.General.General;
import cognitivabrasil.obaa.General.Identifier;
import cognitivabrasil.obaa.General.Keyword;
import cognitivabrasil.obaa.General.Structure;
import cognitivabrasil.obaa.LifeCycle.Entity;
import cognitivabrasil.obaa.LifeCycle.LifeCycle;
import cognitivabrasil.obaa.LifeCycle.Role;
import cognitivabrasil.obaa.LifeCycle.Status;
import cognitivabrasil.obaa.Metametadata.Contribute;
import cognitivabrasil.obaa.Metametadata.Language;
import cognitivabrasil.obaa.Metametadata.MetadataSchema;
import cognitivabrasil.obaa.Metametadata.Metametadata;
import cognitivabrasil.obaa.OBAA;
import cognitivabrasil.obaa.Relation.Kind;
import cognitivabrasil.obaa.Relation.Relation;
import cognitivabrasil.obaa.Relation.Resource;
import cognitivabrasil.obaa.Rights.Rights;
import cognitivabrasil.obaa.Technical.Location;
import cognitivabrasil.obaa.Technical.Name;
import cognitivabrasil.obaa.Technical.OrComposite;
import cognitivabrasil.obaa.Technical.Requirement;
import cognitivabrasil.obaa.Technical.Size;
import cognitivabrasil.obaa.Technical.SupportedPlatform;
import cognitivabrasil.obaa.Technical.Technical;
import cognitivabrasil.obaa.Technical.Type;
import com.cognitivabrasil.cognix.entities.Document;
import com.cognitivabrasil.cognix.entities.User;
import com.cognitivabrasil.cognix.entities.Files;
import com.cognitivabrasil.cognix.entities.dto.DocumentDto;
import com.cognitivabrasil.cognix.entities.dto.DocumentTinyDto;
import com.cognitivabrasil.cognix.entities.dto.MessageDto;
import com.cognitivabrasil.cognix.services.DocumentService;
import com.cognitivabrasil.cognix.services.UserService;
import com.cognitivabrasil.cognix.util.Config;
import com.cognitivabrasil.cognix.web.security.SecurityUser;
import com.cognitivabrasil.cognix.web.security.TokenHandler;
import com.cognitivabrasil.cognix.web.security.TokenAuthenticationService;
import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.HashMap;
import java.util.stream.Collectors;
import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import static org.springframework.util.FileCopyUtils.copy;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.core.env.Environment;

/**
 * Document's controller to get, put and update Cognix documents.
 *
 * @author Marcos Freitas Nunes <marcos@cognitivabrasil.com.br>
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/documents")
public class DocumentsController {

    @Autowired
    private DocumentService docService;

    @Autowired
    private UserService userService;

    @Autowired
    private TokenHandler tokenHandler;

    @Autowired
    private Config config;

    @Autowired
    private Environment env;

    @Autowired
    private JavaMailSender mailSender;

    private final Logger log = LoggerFactory.getLogger(DocumentsController.class);

    private static final String RESP_SUCCESS = "{\"jsonrpc\" : \"2.0\", \"result\" : \"success\", \"id\" : \"id\"}";
    private static final String RESP_ERROR = "{\"jsonrpc\" : \"2.0\", \"error\" : {\"code\": 101, \"message\": \"Falha ao abrir o input stream.\"}, \"id\" : \"id\"}";

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<MessageDto> handleException() {
        MessageDto error = new MessageDto(MessageDto.ERROR, "O documento solicitado não foi encontrado.");
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /**
     * Lista os documentos de forma paginada, com possibilidade de informar o tamanho de cada página. O documento é
     * resumido, tem apenas as informações necessárias para a lista de documentos no frontend.
     *
     * @param page Número da página.
     * @param size Tamanho da página, ou seja, quantos resultados serão retornados.
     * @return
     */
    @GetMapping
    public HttpEntity<Page<Document>> getAll(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        log.trace("Buscando documentos, pagina: {}, tamanho: {}", page, size);
        Page<Document> docs = docService.getPage(PageRequest.of(page, size));
        List<DocumentTinyDto> result = docs.stream().map(DocumentTinyDto::new).collect(Collectors.toList());
        return new ResponseEntity(result, HttpStatus.OK);
    }

    /**
     * Busca um documento pelo id. Seta o idioma do obaa para pt-BR, então os metadados serão traduzidos.
     *
     * @param id Identificador do documento que será buscado.
     * @return Dto com os dados do documento solicitado.
     */
    @GetMapping("/{id}")
    public HttpEntity<DocumentDto> get(@PathVariable Integer id) {

        Document d = docService.get(id);
        if (d.isDeleted()) {
            return new ResponseEntity<>(HttpStatus.MOVED_PERMANENTLY);
        }
        d.getMetadata().setLocale("pt-BR");
        DocumentDto dto = new DocumentDto(d);
        //mostrar a relação para o usuário
        if (d.getMetadata() != null && !d.getMetadata().getRelations().isEmpty()) {
            for (Relation rel : d.getMetadata().getRelations()) {
                switch (rel.getKind().getText()) {
                    case Kind.IS_VERSION_OF:
                        if (!rel.getResource().getIdentifier().isEmpty()) {
                            dto.setIsVersion(rel.getResource().getIdentifier().get(0).getEntry());
                        }
                        break;
                    case Kind.HAS_VERSION:
                        if (!rel.getResource().getIdentifier().isEmpty()) {
                            dto.setHasVersion(rel.getResource().getIdentifier().get(0).getEntry());
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        return new ResponseEntity(dto, HttpStatus.OK);
    }

    /**
     * Deleta o documento informado.
     *
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    private HttpEntity<MessageDto> delete(@PathVariable("id") int id, HttpServletRequest request) {
        MessageDto msg;

        log.info("Deletando o objeto: " + id);
        try {
            Document d = docService.get(id);

            if (d.isDeleted()) {
                msg = new MessageDto(MessageDto.ERROR, "O documento solicitado já foi deletado.");
                return new ResponseEntity(msg, HttpStatus.MOVED_PERMANENTLY);
            }

            final String token = request.getHeader(TokenAuthenticationService.AUTH_HEADER_NAME);
            final SecurityUser user = tokenHandler.parseUserFromToken(token);

            log.info(user.getRoles().toString());
            boolean roleChecked = false;
            for(String role : user.getRoles()){
                if(role.equals("tech_reviewer") || role.equals("pedag_reviewer")) {
                    roleChecked = true;
                }
            }

            if(!roleChecked) {
                msg = new MessageDto(MessageDto.ERROR, "Acesso negado! Você não ter permissão para deletar este documento.");
                return new ResponseEntity(msg, HttpStatus.FORBIDDEN);
            }

            docService.delete(d);
            msg = new MessageDto(MessageDto.SUCCESS, "Documento excluido com sucesso");
        } catch (IOException io) {
            msg = new MessageDto(MessageDto.SUCCESS, "Documento excluido com sucesso, mas os seus arquivos não foram encontrados");
            log.warn("Documento excluido com sucesso, mas os seus arquivos não foram encontrados", io);
        } catch (DataAccessException e) {
            log.error("Não foi possivel excluir o documento.", e);
            msg = new MessageDto(MessageDto.ERROR, "Erro ao excluir o documento.", "");
            return new ResponseEntity(msg, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity(msg, HttpStatus.OK);
    }

    @PostMapping("/{id}/edit")
    private HttpEntity<DocumentDto> edit(@PathVariable("id") Integer id, @RequestBody DocumentDto dto, HttpServletRequest request) throws IOException, Exception {
        String responseString = RESP_SUCCESS;
        Document d = docService.get(id);

        MessageDto msg;
        // log.info("entrei no edit, id=" + id);

        try {
            if(request.getHeader(TokenAuthenticationService.AUTH_HEADER_NAME) == null){
                responseString = RESP_ERROR;
                log.error("Usuário sem token!");
                throw new Exception("Usuário sem token!");
            }                
    
            final String token = request.getHeader(TokenAuthenticationService.AUTH_HEADER_NAME);
            final SecurityUser user = tokenHandler.parseUserFromToken(token);

            log.info(user.getRoles().toString());
            boolean roleChecked = false;
            for(String role : user.getRoles()){
                if(role.equals("tech_reviewer") || role.equals("pedag_reviewer")) {
                    roleChecked = true;
                }
            }

            if(!roleChecked) {
                msg = new MessageDto(MessageDto.ERROR, "Acesso negado! Você não ter permissão para editar este documento.");
                return new ResponseEntity(msg, HttpStatus.FORBIDDEN);
            }

            setOBAAFiles(d, dto.getMetadata());

            msg = new MessageDto(MessageDto.SUCCESS, "Documento salvo com sucesso");

            SimpleMailMessage message = new SimpleMailMessage();

            if(user.getRoles().toString().contains("tech_reviewer")) {
                message.setSubject("OA submetido para revisão pedagógica");
                message.setText("Um novo OA foi submetido: " + 
                "\n\nTítulo: " + dto.getMetadata().getGeneral().getTitles() +
                "\nUsuário: " + user.getUsername() + 
                "\n\nVerifique seu perfil no EduMar!");
                message.setTo(env.getProperty("email.pedagogical.reviewer"));
                message.setFrom(env.getProperty("administrator.email"));
            }
            if(user.getRoles().toString().contains("pedag_reviewer")) {
                message.setSubject("Seu OA está disponível no EduMar!");
                message.setText("Comunicamos que seu OA " + 
                dto.getMetadata().getGeneral().getTitles() +
                " já se encontra disponível no Edumar: " +
                "https://" + env.getProperty("repository.hostname") + "/documents/" + dto.getId());
                message.setTo(user.getUsername());
                message.setFrom(env.getProperty("administrator.email"));
            }
            
            mailSender.send(message);

        } catch(io.jsonwebtoken.SignatureException e){
            responseString = RESP_ERROR;
            log.error("A assinatura é inválida!");
            throw e;
        } catch(io.jsonwebtoken.MalformedJwtException e){
            responseString = RESP_ERROR;
            log.error("O token é inválido!");
            throw e;
        }
        return new ResponseEntity<>(new DocumentDto(d), HttpStatus.OK);
    }

    /**
     * Edita o documento.
     *
     * @param id Identificador do documento que será editado.
     * @param dto Dados do documento.
     * @return
     * @throws IOException
     */
    @PutMapping("/{id}")
    private HttpEntity<MessageDto> editDo(@PathVariable("id") Integer id, @RequestBody DocumentDto dto, HttpServletRequest request) throws IOException {
        log.debug("Editing document: {}", id);
        MessageDto msg;
        Document d = docService.get(id);
 
        final String token = request.getHeader(TokenAuthenticationService.AUTH_HEADER_NAME);
        final SecurityUser user = tokenHandler.parseUserFromToken(token);

        log.info(user.getRoles().toString());
        boolean roleChecked = false;
        for(String role : user.getRoles()){
            if(role.equals("tech_reviewer") || role.equals("pedag_reviewer")) {
                roleChecked = true;
            }
        }

        if(!roleChecked) {
            msg = new MessageDto(MessageDto.ERROR, "Acesso negado! Você não ter permissão para editar este documento.");
            return new ResponseEntity(msg, HttpStatus.FORBIDDEN);
        }

        setOBAAFiles(d, dto.getMetadata());
        msg = new MessageDto(MessageDto.SUCCESS, "Documento editado com sucesso");
        return new ResponseEntity<>(msg, HttpStatus.OK);
    }

    private void setOBAAFiles(Document d, OBAA obaa) {
        log.debug("Trying to save");

        // split the keywords
        List<Keyword> splittedKeywords = new ArrayList<>();
        if (!obaa.getGeneral().getKeywords().isEmpty()) {

            obaa.getGeneral().getKeywords().forEach((k) -> {
                for (String nk : k.split("\\s*[,;]\\s*")) {
                    splittedKeywords.add(new Keyword(nk));
                }
            });
            obaa.getGeneral().setKeywords(splittedKeywords);
        }

        log.debug("Title: " + obaa.getGeneral().getTitles());

        Technical t = obaa.getTechnical();

        Long size;

        size = 0L;
        for (Files f : d.getFiles()) {
            size += f.getSizeInBytes();
        }

        // somatorio to tamanho de todos os arquivos
        t.setSize(new Size(size));
        obaa.setTechnical(t);
        d.setMetadata(obaa);

        if (obaa.getTechnical() == null) {
            log.warn("Technical was null");
            obaa.setTechnical(new Technical());
        }
        List<Location> l = obaa.getTechnical().getLocation();

        //if doesn't have location, an entry based is created
        if (l == null || l.isEmpty()) {
            obaa.getTechnical().addLocation(obaa.getGeneral().getIdentifiers().get(0).getEntry());
        }

        // Preenchimento dos metametadados
        Metametadata meta = new Metametadata();

        meta.setLanguage(new Language("pt-BR"));

        String userName = d.getOwner().getUsername();
        Contribute c = new Contribute();

        // Quando fizer o cadastro dos usuários do sistema cuidar para que possa por os dados do vcard
        Entity e = new Entity();
        e.setName(userName, "");

        c.addEntity(e);
        c.setRole(Role.AUTHOR);

        // today date
        Date date = new Date();
        DateFormat dateFormat;
        dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        c.setDate(dateFormat.format(date));
        meta.addContribute(c);

        Identifier i = new Identifier("URI", "http://www.w3.org/2001/XMLSchema-instance");

        meta.addIdentifier(i);

        //default metadataSchema
        MetadataSchema metaSchema = new MetadataSchema();
        meta.addSchema(metaSchema);

        d.getMetadata().setMetametadata(meta);
        // log.debug("OBAA entry = " + obaa.getGeneral().getIdentifiers().get(0).getEntry());
        // log.debug(d.getObaaEntry());
        // d.setObaaEntry(obaa.getGeneral().getIdentifiers().get(0).getEntry());

        //Se o documento tem uma relação is_version_of, é testado se o outro
        //documento tem o Has_version, se não tiver a relação é criada.
        for (Identifier id : obaa.getRelationsWithKind(Kind.IS_VERSION_OF)) {
            if (id.getCatalog().equalsIgnoreCase("URI")) {
                Document docVersion = docService.get(id.getEntry());
                if (docVersion.getMetadata() != null) {
                    //testa se o documento ja tem tem a versao cadastrada
                    if (!docVersion.getMetadata().hasRelationWith(Kind.HAS_VERSION, d.getObaaEntry())) {
                        //Cria relação de versão no orginial
                        Relation originalRelation = new Relation();
                        originalRelation.setKind(Kind.HAS_VERSION);
                        originalRelation.setResource(new Resource());
                        originalRelation.getResource().addIdentifier(new Identifier("URI", d.getObaaEntry()));
                        List<Relation> relationsList = new ArrayList<>();
                        relationsList.add(originalRelation);
                        docVersion.getMetadata().setRelations(relationsList);
                        //salva o documento com a nova relaçao
                        docService.save(docVersion);
                    }
                }
            }
        }

        d.setMetadata(obaa);
        d.setActive(true);
        docService.save(d);
    }

    private String createUri(Document d) {
        return config.getUrl() + d.getId();
    }

    /**
     * Works only if the files are uploaded and not in a remote location
     *
     * @param d
     * @return
     */
    private OBAA metadataFromFile(Document d) {

        OBAA suggestions = new OBAA();
        List<Files> files = d.getFiles();

        boolean empty = false;

        /*Images*/
        boolean allImg = true;
        final String IMAGE = "image";

        /*Applications*/
        boolean allPdf = true;
        final String PDF_MIMETYPE = "application/pdf";
        boolean allDoc = true;
        final String DOC_MIMETYPE = "application/msword";

        /*Empty verification*/
        if (files.isEmpty()) {
            empty = true;
        }

        String mime = "";

        for (Files file : files) {

            mime = file.getContentType();
            log.debug("MIME Type: " + mime);

            if (!mime.startsWith(IMAGE)) {
                allImg = false;
            }
            if (!mime.equals(PDF_MIMETYPE)) {
                allPdf = false;
            }
            if (!mime.equals(DOC_MIMETYPE)) {
                allDoc = false;
            }

        }

        if (allImg && !empty) {
            //all image
            suggestions = this.allImg(mime);

        } else if (allPdf && !empty) {
            //all PDF
            suggestions = this.allPdf();

        } else if (allDoc && !empty) {
            //all DOC
            suggestions = this.allDoc();
        }
        General general = new General();
        //Title Suggestion
        if (files.size() != 1 || files.size() >= 2) {
            general.addTitle("");
        } else {
            String fileName = files.get(0).getName().replaceAll("_", " ");

            // to remove the file extension
            if (fileName.contains(".")) {
                general.addTitle(fileName.substring(0, fileName.lastIndexOf('.')));
            } else {
                general.addTitle(fileName);
            }
        }
        suggestions.setGeneral(general);

        return suggestions;
    }

    /**
     * Esses métodos foram feitos privado e aqui e não na classe ObaaDto, por ela ser uma classe apenas para
     * tranferência de dados.
     */
    private OBAA allImg(String mime) {
        OBAA imgObj = new OBAA();

        log.debug("Gerenation suggestion metadata from Image");

        //General
        General general = new General();
        general.setStructure(Structure.fromText(Structure.ATOMIC));
        general.setAggregationLevel(1);
        imgObj.setGeneral(general);

        //Educational
        Educational educational = new Educational();
        educational.setInteractivityType(InteractivityType.EXPOSITIVE);
        Interaction interaction = new Interaction();
        interaction.setPerception(Perception.VISUAL);
        interaction.setSynchronism(false);
        interaction.setCoPresence(false);
        interaction.setReciprocity(Reciprocity.ONE_ONE);
        educational.setInteraction(interaction);
        educational.setInteractivityLevel(InteractivityLevel.VERY_LOW);
        imgObj.setEducational(educational);

        //Accessibility
        Accessibility accessibility = new Accessibility();
        ResourceDescription resourceDescription = new ResourceDescription();
        Primary primary = new Primary();
        primary.setVisual(true);
        primary.setAuditory(false);
        primary.setTactile(false);
        primary.setText(false);
        resourceDescription.setPrimary(primary);
        accessibility.setResourceDescription(resourceDescription);
        imgObj.setAccessibility(accessibility);

        //Technical
        Technical technical = new Technical();
        technical.addSupportedPlatforms(SupportedPlatform.WEB);
        technical.addSupportedPlatforms(SupportedPlatform.MOBILE);
        technical.addSupportedPlatforms(SupportedPlatform.DTV);
        imgObj.setTechnical(technical);

        if (mime.endsWith("jpeg") || mime.endsWith("jpg") || mime.endsWith("png") || mime.endsWith("gif")) {
            Requirement requirement = new Requirement();
            OrComposite orComposite = new OrComposite();
            orComposite.setName(Name.ANY);
            orComposite.setType(Type.OPERATING_SYSTEM);
            requirement.addOrComposite(orComposite);
            technical.addRequirement(requirement);
        }
        return imgObj;
    }

    private OBAA allPdf() {
        OBAA pdfObj = new OBAA();

        log.debug("Gerenation suggestion metadata from  PDF");

        //General
        General general = new General();
        general.setStructure(Structure.fromText(Structure.ATOMIC));
        general.setAggregationLevel(1);
        pdfObj.setGeneral(general);

        //Educational
        Educational educational = new Educational();
        educational.setInteractivityType(InteractivityType.EXPOSITIVE);
        Interaction interaction = new Interaction();
        interaction.setPerception(Perception.VISUAL);
        interaction.setSynchronism(false);
        interaction.setCoPresence(false);
        interaction.setReciprocity(Reciprocity.ONE_ONE);
        educational.setInteraction(interaction);
        educational.setInteractivityLevel(InteractivityLevel.VERY_LOW);
        pdfObj.setEducational(educational);

        //Accessibility
        Accessibility accessibility = new Accessibility();
        ResourceDescription resourceDescription = new ResourceDescription();
        Primary primary = new Primary();
        primary.setVisual(true);
        primary.setAuditory(false);
        primary.setTactile(false);
        primary.setText(true);
        resourceDescription.setPrimary(primary);
        accessibility.setResourceDescription(resourceDescription);
        pdfObj.setAccessibility(accessibility);

        //Technical
        Technical technical = new Technical();
        technical.addSupportedPlatforms(SupportedPlatform.WEB);
        technical.addSupportedPlatforms(SupportedPlatform.MOBILE);

        technical.setOtherPlatformRequirements("É necessário um programa como o Adobe Reader para ver esse arquivo.");

        Requirement requirement = new Requirement();
        OrComposite orComposite = new OrComposite();
        orComposite.setName(Name.ANY);
        orComposite.setType(Type.OPERATING_SYSTEM);
        requirement.addOrComposite(orComposite);
        technical.addRequirement(requirement);

        pdfObj.setTechnical(technical);

        return pdfObj;
    }

    private OBAA allDoc() {
        OBAA docObj = new OBAA();

        log.debug("Gerenation suggestion metadata from Doc");

        //General
        General general = new General();
        general.setStructure(Structure.fromText(Structure.ATOMIC));
        general.setAggregationLevel(1);
        docObj.setGeneral(general);

        //Educational
        Educational educational = new Educational();
        educational.setInteractivityType(InteractivityType.EXPOSITIVE);
        Interaction interaction = new Interaction();
        interaction.setPerception(Perception.VISUAL);
        interaction.setSynchronism(false);
        interaction.setCoPresence(false);
        interaction.setReciprocity(Reciprocity.ONE_ONE);
        educational.setInteraction(interaction);
        educational.setInteractivityLevel(InteractivityLevel.VERY_LOW);
        docObj.setEducational(educational);

        //Accessibility
        Accessibility accessibility = new Accessibility();
        ResourceDescription resourceDescription = new ResourceDescription();
        Primary primary = new Primary();
        primary.setVisual(true);
        primary.setAuditory(false);
        primary.setTactile(false);
        primary.setText(true);
        resourceDescription.setPrimary(primary);
        accessibility.setResourceDescription(resourceDescription);
        docObj.setAccessibility(accessibility);

        //Technical
        Technical technical = new Technical();
        technical.addSupportedPlatforms(SupportedPlatform.WEB);
        technical.addSupportedPlatforms(SupportedPlatform.MOBILE);

        technical.setOtherPlatformRequirements("É necessário um programa como o Microsoft Word para ver esse arquivo.");

        Requirement requirement = new Requirement();
        OrComposite orComposite = new OrComposite();
        orComposite.setName(Name.ANY);
        orComposite.setType(Type.OPERATING_SYSTEM);
        requirement.addOrComposite(orComposite);
        technical.addRequirement(requirement);
        docObj.setTechnical(technical);

        return docObj;
    }

    /**
     * Cria um documento versão de outro existente.
     *
     * @param versionOf id do documento de origem deste.
     * @return
     */
    @PostMapping(value = "/new/versionOf/{versionOf}", params = "versionOf")
    private HttpEntity<DocumentDto> newVersionOf(@PathVariable Integer versionOf, HttpServletRequest request) {
        MessageDto msg;
        if(request.getHeader(TokenAuthenticationService.AUTH_HEADER_NAME) == null){
            log.error("Usuário sem token!");
            msg = new MessageDto(MessageDto.ERROR, "O usuário não possui token.");
            return new ResponseEntity(msg, HttpStatus.INTERNAL_SERVER_ERROR);
        }                
        

        final String token = request.getHeader(TokenAuthenticationService.AUTH_HEADER_NAME);
        final SecurityUser user = tokenHandler.parseUserFromToken(token);

        boolean roleChecked = false;
        for(String role : user.getRoles()){
            if(role.equals("author")) {
                roleChecked = true;
            }
        }

        if(!roleChecked) {
            msg = new MessageDto(MessageDto.ERROR, "Acesso negado! Você não ter permissão para enviar documentos.");
            return new ResponseEntity(msg, HttpStatus.FORBIDDEN);
        }

        //Criação de nova versão
        Document d = docService.get(versionOf);
        Document dv = new Document();
        dv.setCreated(new DateTime());
        //o documento precisa ser salvo para gerar um id da base
        docService.save(dv);
        //copia o original
        OBAA originalObaa = d.getMetadata();
        OBAA versionObaa = originalObaa.clone();

        //altera o id
        String versionUri = createUri(dv);
        dv.setObaaEntry(versionUri);

        Identifier versionId = new Identifier("URI", versionUri);
        versionObaa.getGeneral().getIdentifiers().clear();

        //esvaziar o location para gerar um novo
        versionObaa.getTechnical().getLocation().clear();

        //seta o identifier na versao
        versionObaa.getGeneral().addIdentifier(versionId);

        //Cria relação de versão no orginial
        Relation originalRelation = new Relation();
        originalRelation.setKind(Kind.HAS_VERSION);
        Resource r = new Resource();
        r.addIdentifier(versionId);
        originalRelation.setResource(r);
        List<Relation> relationsList = new ArrayList<>();
        relationsList.add(originalRelation);
        originalObaa.setRelations(relationsList);

        //Cria relação de versão no novo objeto
        Relation versionRelation = new Relation();

        versionRelation.setKind(Kind.IS_VERSION_OF);
        Identifier id = originalObaa.getGeneral().getIdentifiers().get(0);
        Resource r2 = new Resource();
        r2.addIdentifier(id);
        versionRelation.setResource(r2);
        List<Relation> relations2List = new ArrayList<>();
        relations2List.add(versionRelation);
        versionObaa.setRelations(relations2List);

        docService.save(d);
        dv.setMetadata(versionObaa);

        return new ResponseEntity(new DocumentDto(d), HttpStatus.OK);
    }

    /**
     * Cria um documento, salva na base e devolve para o frontend um novo documento com id na base e identificador.
     *
     * @return
     */
    @PostMapping(value = "/new")
    public HttpEntity<DocumentDto> newShow(HttpServletRequest request) {
        MessageDto msg;
        Document d = new Document();
        try{
            if(request.getHeader(TokenAuthenticationService.AUTH_HEADER_NAME) == null){
                log.error("Usuário sem token!");
                msg = new MessageDto(MessageDto.ERROR, "O usuário não possui token.");
                return new ResponseEntity(msg, HttpStatus.INTERNAL_SERVER_ERROR);
            }                
            

            final String token = request.getHeader(TokenAuthenticationService.AUTH_HEADER_NAME);
            final SecurityUser user = tokenHandler.parseUserFromToken(token);

            log.info(user.getRoles().toString());
            boolean roleChecked = false;
            for(String role : user.getRoles()){
                if(role.equals("author")) {
                    roleChecked = true;
                }
            }

            if(!roleChecked) {
                msg = new MessageDto(MessageDto.ERROR, "Acesso negado! Você não ter permissão para enviar documentos.");
                return new ResponseEntity(msg, HttpStatus.FORBIDDEN);
            }

            d.setCreated(new DateTime());

            log.info("Carregando documento para o usuário: " + user.getUsername());

            d.setOwner(userService.get(user.getUsername()));

            //o documento precisa ser salvo para gerar um id da base
            docService.save(d);

            String uri = createUri(d);
            d.setObaaEntry(uri);
            docService.save(d);
            OBAA obaa = new OBAA();

            obaa.setGeneral(new General());
            obaa.setLifeCycle(new LifeCycle());
            obaa.setEducational(new Educational());
            obaa.setRights(new Rights());

            List<Identifier> identifiers = new ArrayList<>();
            Identifier i = new Identifier();
            i.setEntry(uri);
            i.setCatalog("URI");

            identifiers.add(i);
            obaa.getGeneral().setIdentifiers(identifiers);

            d.setMetadata(obaa);
        }
        catch(io.jsonwebtoken.SignatureException e){
            log.error("A assinatura é inválida!");
            msg = new MessageDto(MessageDto.ERROR, "A assinatura não foi encontrada através do token.");
            return new ResponseEntity(msg, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        catch(io.jsonwebtoken.MalformedJwtException e){
            log.error("O token é inválido!");
            msg = new MessageDto(MessageDto.ERROR, "O usuário não foi encontrado através do token.");
            return new ResponseEntity(msg, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity(new DocumentDto(d), HttpStatus.OK);
    }

    @GetMapping(value = "/model/classPlan")
    private HttpEntity<DocumentDto> newClassPlan() {

        //inicializa com o new basico
        HttpEntity<DocumentDto> httpEntityDocDto = newShow(null);
        DocumentDto docDto = httpEntityDocDto.getBody();

//        metadados para planos de aula
        OBAA lo = docDto.getMetadata();
        General general = lo.getGeneral();
        general.addLanguage("pt-BR");
        Structure s = new Structure();
        s.setText(Structure.COLLECTION);
        general.setStructure(s);
        general.setAggregationLevel(3);
        general.addKeyword("Plano de Aula");
        general.addKeyword("");
        lo.setGeneral(general);

        LifeCycle lifeCycle = new LifeCycle();
        lifeCycle.setVersion("1");
        lifeCycle.setStatus(Status.FINALIZED);

        cognitivabrasil.obaa.LifeCycle.Contribute contribute = new cognitivabrasil.obaa.LifeCycle.Contribute();

        Entity e = new Entity();
        e.setName("Ministério da Educação", "do Brasil");

        contribute.addEntity(e);
        contribute.setRole(Role.PUBLISHER);

        // today date
        Date date = new Date();
        DateFormat dateFormat;
        dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        contribute.setDate(dateFormat.format(date));
        lifeCycle.addContribute(contribute);

        lo.setLifeCycle(lifeCycle);

        Technical technical = new Technical();

        Requirement requirement = new Requirement();
        OrComposite orComposite = new OrComposite();
        orComposite.setType(Type.OPERATING_SYSTEM);
        orComposite.setName(Name.MULTI_OS);

        OrComposite orComposite2 = new OrComposite();
        orComposite2.setType(Type.BROWSER);
        orComposite2.setName(Name.ANY);

        requirement.addOrComposite(orComposite);
        requirement.addOrComposite(orComposite2);
        technical.addRequirement(requirement);

        List<Location> location = new ArrayList<>();
        technical.setLocation(location);

        technical.setOtherPlatformRequirements("É necessário um programa como o acrobat reader que permite a leitura de arquivos no formato PDF.");
        technical.addSupportedPlatforms(SupportedPlatform.WEB);

        lo.setTechnical(technical);

        Educational educational = new Educational();
        educational.setInteractivityType(InteractivityType.EXPOSITIVE);
        educational.addLearningResourceType(LearningResourceType.LECTURE);
        educational.setInteractivityLevel(InteractivityLevel.VERY_LOW);
        educational.addDescription("Plano de aula envolvendo o uso do computador ou recursos alternativos.");
        educational.addLanguage("pt-BR");
        educational.setLearningContentType(LearningContentType.PROCEDIMENTAL);
        educational.addContext(Context.SCHOOL);

        educational.addIntendedEndUserRole(IntendedEndUserRole.TEACHER);

        Interaction interaction = new Interaction();
        interaction.setInteractionType(InteractionType.OBJECT_INDIVIDUAL);
        interaction.setCoPresence(false);
        interaction.setSynchronism(false);
        interaction.setPerception(Perception.VISUAL);
        interaction.setReciprocity(Reciprocity.ONE_N);

        educational.setInteraction(interaction);

        lo.setEducational(educational);

        Rights rights = new Rights();
        rights.setCost(false);

        lo.setRights(rights);

        Accessibility accessibility = new Accessibility();
        ResourceDescription resourceDescription = new ResourceDescription();
        Primary primary = new Primary();
        primary.setVisual(true);
        primary.setAuditory(false);
        primary.setText(true);
        primary.setTactile(false);

        resourceDescription.setPrimary(primary);
        accessibility.setResourceDescription(resourceDescription);

        lo.setAccessibility(accessibility);

//        docDto.setMetadata(lo);
        return httpEntityDocDto;
    }

    @PostMapping("/{id}")
    private HttpEntity<MessageDto> newDo(@PathVariable Integer id, @RequestBody DocumentDto dto, HttpServletRequest request) {
        MessageDto msg;
        try {            
            if(request.getHeader(TokenAuthenticationService.AUTH_HEADER_NAME) == null){
                log.error("Usuário sem token!");
                msg = new MessageDto(MessageDto.ERROR, "O usuário não possui token.");
                return new ResponseEntity(msg, HttpStatus.INTERNAL_SERVER_ERROR);
            }                
            
            final String token = request.getHeader(TokenAuthenticationService.AUTH_HEADER_NAME);
            final SecurityUser user = tokenHandler.parseUserFromToken(token);

            log.info(user.getRoles().toString());
            boolean roleChecked = false;
            for(String role : user.getRoles()){
                if(role.equals("author")) {
                    roleChecked = true;
                }
            }

            Document doc = docService.get(id);
            
            log.info("Salvando documento...");
            //TODO: pegar aqui o email do usuário logado? Já foi pego no new
            //doc.setOwner(UsersController.getCurrentUser());
            setOBAAFiles(doc, dto.getMetadata());

            SimpleMailMessage message = new SimpleMailMessage();
            message.setSubject("OA submetido para revisão técnica");
            message.setText("Um novo OA foi submetido: " + 
            "\n\nTítulo: " + dto.getMetadata().getGeneral().getTitles() +
            "\nUsuário: " + user.getUsername() + 
            "\n\nVerifique seu perfil no EduMar!");
            message.setTo(env.getProperty("email.technical.reviewer"));
            message.setFrom(env.getProperty("administrator.email"));
            
            mailSender.send(message);

            msg = new MessageDto(MessageDto.SUCCESS, "Documento salvo com sucesso");
        } catch (DataAccessException e) {
            log.error("Não foi possivel salvar o documento.", e);
            msg = new MessageDto(MessageDto.ERROR, "Erro ao salvar o documento.", "");
        } catch(io.jsonwebtoken.SignatureException e){
            log.error("A assinatura do token é inválida.", e);
            msg = new MessageDto(MessageDto.ERROR, "A assinatura é inválida!");
        } catch(io.jsonwebtoken.MalformedJwtException e){
            log.error("O token é inválido.", e);
            msg = new MessageDto(MessageDto.ERROR, "O token é inválido!");
        }
        return new ResponseEntity(msg, HttpStatus.OK);
    }

    /**
     * Gera metadados a partir do arquivo do documento.
     *
     * @param id
     * @return
     */
    @PostMapping(value = "/new/generateMetadata")
    private HttpEntity<OBAA> generateMetadata(int id) {
        Document doc = docService.get(id);

        return new ResponseEntity(metadataFromFile(doc), HttpStatus.OK);
    }

    /**
     * Criado apenas para salvar em um diretório todos os objetos da base com o seu respectivo metadado.
     *
     * @return
     * @throws IOException
     * @throws Exception
     */
    //TODO: Para acessar este controller deve ser super usuário.
    // @PostMapping("/saveFilesToDisk")
    private HttpEntity recallFiles()
            throws IOException {
        String location = Config.FILE_PATH + "old/";

        List<Document> docs = docService.getAll();

        for (Document doc : docs) {
            log.trace("\n doc " + doc.getId());

            String destinationPath = Config.FILE_PATH + doc.getId();
            File destinationDocFiles = new File(destinationPath);
            destinationDocFiles.mkdir();

            List<Files> files = doc.getFiles();
            int numberFiles = 0;
            for (Files f : files) {
                if (f.getLocation().isEmpty()) {
                    throw new IOException("A localização do documento está em branco.");
                }
                File sourceFile = new File(location + f.getId());
                File destinationFile = new File(destinationPath + "/" + f.getName());

                copy(sourceFile, destinationFile);
                numberFiles++;
            }
            if (numberFiles == 0) {
                throw new IOException("O Documento " + doc.getId() + " não possui nenhum arquivo!");
            }
        }
        return new ResponseEntity(HttpStatus.OK);
    }

}
