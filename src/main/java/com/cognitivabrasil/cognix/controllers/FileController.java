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

import cognitivabrasil.obaa.Technical.Format;
import com.cognitivabrasil.cognix.entities.Document;
import com.cognitivabrasil.cognix.entities.Files;
import com.cognitivabrasil.cognix.entities.dto.MessageDto;
import com.cognitivabrasil.cognix.services.DocumentService;
import com.cognitivabrasil.cognix.services.FileService;
import com.cognitivabrasil.cognix.util.Config;
import com.cognitivabrasil.cognix.web.security.SecurityUser;
import com.cognitivabrasil.cognix.web.security.TokenHandler;
import com.cognitivabrasil.cognix.web.security.TokenAuthenticationService;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author Marcos Freitas Nunes <marcosn@gmail.com>
 */
@RequestMapping("/files")
@RestController
@CrossOrigin(origins = "*")
public class FileController {

    private final Logger LOG = LoggerFactory.getLogger(FileController.class);
    @Autowired
    private FileService fileService;
    @Autowired
    private DocumentService documentsService;
    @Autowired
    private TokenHandler tokenHandler;
    private Files file = null;
    private int chunk;
    private int chunks;
    private static final String RESP_SUCCESS = "{\"jsonrpc\" : \"2.0\", \"result\" : \"success\", \"id\" : \"id\"}";
    private static final String RESP_ERROR = "{\"jsonrpc\" : \"2.0\", \"error\" : {\"code\": 101, \"message\": \"Falha ao abrir o input stream.\"}, \"id\" : \"id\"}";
    public static final String DEFAULT_THUMBNAIL_PATH = "./src/main/resources/default-thumbnail.png";

    @GetMapping(value = "/{id}")
    public void getFile(@PathVariable("id") int id, HttpServletResponse response) throws IOException {
        Files f = fileService.get(id);
        if (f == null) {
            response.sendError(HttpServletResponse.SC_GONE, "O arquivo solicitado não foi encontrado.");
        } else {
            String fileName = f.getLocation();

            try {
                // get your file as InputStream
                InputStream is = new FileInputStream(new File(fileName));

                response.setHeader("Content-Disposition", "attachment; filename=" + f.getName());
                response.setContentType(f.getContentType());
                // copy it to response's OutputStream
                IOUtils.copy(is, response.getOutputStream());

                response.flushBuffer();

            } catch (FileNotFoundException fe) {
                response.sendError(HttpServletResponse.SC_GONE, "O arquivo solicitado não foi encontrado.");
                LOG.error("O arquivo solicitado não foi encontrado.", fe);
            } catch (IOException ex) {
                LOG.error("Error writing file to output stream. Filename was '" + fileName + "'");
                throw ex;
            }
        }
    }

    @DeleteMapping(value = "/{id}")
    private HttpEntity<MessageDto> delete(@PathVariable("id") Integer id) {

        Files f = fileService.get(id);

        if (f == null) {
            MessageDto msgDto = new MessageDto(MessageDto.ERROR, "O arquivo não foi encontrado na base de dados!", "upload");
            return new ResponseEntity(msgDto, HttpStatus.NOT_FOUND);
        }

        Document doc = f.getDocument();
        String fileFormat = f.getContentType();
        Format format = new Format(fileFormat);

        doc.getMetadata().getTechnical().getFormats().remove(format);

        try {
            fileService.deleteFile(f);
        } catch (IOException e) {
            String errorMsg = "O arquivo não foi encontrado no disco, mas foi removido da base de dados!";
            LOG.error(errorMsg, e);

            MessageDto msgDto = new MessageDto(MessageDto.WARN, errorMsg, "upload");
            return new ResponseEntity(msgDto, HttpStatus.OK);
        }
        MessageDto msgDto = new MessageDto(MessageDto.SUCCESS, "Arquivo excluido com sucesso.", "upload");
        return new ResponseEntity(msgDto, HttpStatus.OK);
    }

    @PostMapping(value = "/uploadFile")
    public HttpEntity<String> upload(@RequestParam("file") MultipartFile multipartFile, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, org.apache.commons.fileupload.FileUploadException, Exception {
        
        if (file == null) {
            file = new Files();
            file.setSizeInBytes(0L);
        }
        MessageDto msg;
        Integer docId = null;
        String docPath = null;
        String responseString = RESP_SUCCESS;
        boolean isMultipart = ServletFileUpload.isMultipartContent(request);

        if (isMultipart) {
            try {
                if(request.getHeader(TokenAuthenticationService.AUTH_HEADER_NAME) == null){
                    responseString = RESP_ERROR;
                    LOG.error("Usuário sem token!");
                    file = null;
                    throw new Exception("Usuário sem token!");
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
                if (!multipartFile.isEmpty()) {
                    // InputStream input = multipartFile.getInputStream();
                    Enumeration<String> parameterNames = request.getParameterNames();
 
                    while (parameterNames.hasMoreElements()) {
            
                        String paramName = parameterNames.nextElement();
                        String paramValue = null;
                        
                        String[] paramValues = request.getParameterValues(paramName);
                        for (int i = 0; i < paramValues.length; i++) {
                            paramValue = paramValues[i];                          
                        }

                        switch (paramName) {
                            case "chunks":
                                this.chunks = Integer.parseInt(paramValue);
                                break;
                            case "chunk":
                                this.chunk = Integer.parseInt(paramValue);
                                break;
                            case "filename":
                                if(paramValue.equals("Material")){
                                    file.setName(multipartFile.getOriginalFilename());
                                }
                                if(paramValue.equals("Thumbnail")){
                                    file.setName("thumbnail");
                                }
                                break;
                            case "docId":
                                if (paramValue.isEmpty()) {
                                    throw new org.apache.commons.fileupload.FileUploadException("Não foi informado o id do documento.");
                                }
                                docId = Integer.parseInt(paramValue);
                                docPath = Config.FILE_PATH + docId;
                                File documentPath = new File(docPath);
                                // cria o diretorio
                                if(!documentPath.exists()){
                                    documentPath.mkdirs();
                                }
                                break;
                            default:
                                break;
                        }
                    }

                    
                    try {
                        File uploadFile = null;
                        BufferedOutputStream bufferedOutput = null;
                        byte[] data = null;
                        // copy file (learning object or thumbnail)
                        if (!multipartFile.isEmpty()) {
                            uploadFile = new File(docPath, file.getName());
                            bufferedOutput = new BufferedOutputStream(new FileOutputStream(uploadFile, true));
                            data = multipartFile.getBytes();
                            bufferedOutput.write(data);
                            bufferedOutput.close();
                        }
                    } catch (Exception e) {
                        LOG.error("Erro ao salvar o arquivo.", e);
                        file = null;
                        throw e;
                    } finally {
                        if (!multipartFile.isEmpty()) {
                            if(!file.getName().equals("thumbnail")){
                                file.setName(multipartFile.getOriginalFilename());
                            }
                            file.setContentType(multipartFile.getContentType());
                            file.setPartialSize(multipartFile.getSize());
                        }
                    }
                }

                if ((this.chunk == this.chunks - 1) || this.chunks == 0) {
                    file.setLocation(docPath + "/" + file.getName());
                    if (docId != null) {
                        file.setDocument(documentsService.get(docId));
                    }
                    fileService.save(file);
                    file = null;
                }
            } catch(io.jsonwebtoken.SignatureException e){
                responseString = RESP_ERROR;
                LOG.error("A assinatura é inválida!");
                file = null;
                throw e;
            } catch(io.jsonwebtoken.MalformedJwtException e){
                responseString = RESP_ERROR;
                LOG.error("O token é inválido!");
                file = null;
                throw e;
            } catch (org.apache.commons.fileupload.FileUploadException | IOException | NumberFormatException e) {
                responseString = RESP_ERROR;
                LOG.error("Erro ao salvar o arquivo", e);
                file = null;
                throw e;
            }
        } // Not a multi-part MIME request.
        else {
            responseString = RESP_ERROR;
        }
        response.setContentType("application/json");
        byte[] responseBytes = responseString.getBytes();
        response.setContentLength(responseBytes.length);
        ServletOutputStream output = response.getOutputStream();
        output.write(responseBytes);
        output.flush();
        return new ResponseEntity(responseString, HttpStatus.OK);
    }

    @GetMapping(value = "/{id}/thumbnail")
    public void getThumbnail(@PathVariable("id") Long id, HttpServletResponse response) throws IOException {
        if (id == null || id == 0) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "O arquivo solicitado não foi encontrado.");
        } else {
            String fileName = Config.FILE_PATH + id + "/thumbnail";

            try {
                // get your file as InputStream
                InputStream is = new FileInputStream(new File(fileName));

                response.setHeader("Content-Disposition", "attachment; filename= thumbnail" + id);
                response.setStatus(HttpServletResponse.SC_CREATED);
                // copy it to response's OutputStream
                IOUtils.copy(is, response.getOutputStream());

                response.flushBuffer();

            } catch (FileNotFoundException fe) {
                // get your file as InputStream
                InputStream is = new FileInputStream(new File(DEFAULT_THUMBNAIL_PATH));
                response.setHeader("Content-Disposition", "attachment; filename=default-thumbnail.png");
                response.setContentType(MediaType.IMAGE_PNG_VALUE);
                response.setStatus(HttpServletResponse.SC_CREATED);
                // copy it to response's OutputStream
                IOUtils.copy(is, response.getOutputStream());

                response.flushBuffer();
                LOG.error("Imagen solicitada não foi encontrada.", fe);
            } catch (IOException ex) {
                LOG.error("Error writing file to output stream. Filename was '" + fileName + "'");
                throw ex;
            }
        }
    }

}
