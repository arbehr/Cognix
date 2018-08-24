/*
 *
 *  * Copyright (c) 2016 Cognitiva Brasil - Tecnologias educacionais.
 *  * All rights reserved. This program and the accompanying materials
 *  * are made available either under the terms of the GNU Public License v3
 *  * which accompanies this distribution, and is available at
 *  * http://www.gnu.org/licenses/gpl.html or for any other uses contact
 *  * contato@cognitivabrasil.com.br for information.
 *  ******************************************************************************
 *
 */
package com.cognitivabrasil.cognix.entities;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import org.junit.Test;

/**
 *
 * @author Marcos Freitas Nunes <marcos@cognitivabrasil.com.br>
 */
public class FilesTest {

    @Test
    public void testIncrementSize() {
        Files f = new Files();
        f.setPartialSize(300L);
        assertThat(f.getSizeFormatted(), equalTo("300 Bytes"));

        f.setPartialSize(1000L);
        assertThat(f.getSizeFormatted(), equalTo("1,3 KB"));
    }

    @Test
    public void testFormattedSizeError() {
        Files f = new Files();
        assertThat(f.getSizeFormatted(), equalTo("Tamanho não definido"));
    }

    @Test
    public void testFileIsInFileList() {
        FileItem f = new DiskFileItem("marcos", "text/plain", true, "marcos", 10000, null);
        List<Files> l = new ArrayList<>();

        assertThat(Files.isFileItemInFileList(l, f), equalTo(false));

        Files f1 = new Files();
        f1.setName("nunes");
        l.add(f1);
        assertThat(Files.isFileItemInFileList(l, f), equalTo(false));

        Files f2 = new Files();
        f2.setName("marcos");
        l.add(f2);
        assertThat(Files.isFileItemInFileList(l, f), equalTo(true));
    }
}
