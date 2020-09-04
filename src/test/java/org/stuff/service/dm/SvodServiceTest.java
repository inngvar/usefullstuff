package org.stuff.service.dm;

import org.junit.jupiter.api.Test;

import java.io.*;


class SvodServiceTest {

    public SvodService svodService = new SvodService();

    @Test
    void convert() throws IOException {

        final InputStream svod = this.getClass().getClassLoader().getResourceAsStream("svods/SVOD_test.xlsx");
        FileOutputStream fileOutputStream = new FileOutputStream("/home/igorch/svod_test.xlsx");
        svodService.convert(svod, fileOutputStream);

    }
}
