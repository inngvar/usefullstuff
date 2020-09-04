package org.stuff.service.dm;


import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.enterprise.context.ApplicationScoped;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class SvodService {

    /**
     * Проанализировать и свести файл SVOD
     *
     * @param source           исходные данные
     * @param fileOutputStream поток куда записать данные
     */
    public void convert(InputStream source, OutputStream fileOutputStream) throws IOException {
        List<SvodOrganization> svodOrganizations = Lists.newArrayList();
        Workbook workbook = new XSSFWorkbook(source);
        Sheet datatypeSheet = workbook.getSheetAt(0);

        for (Row currentRow : datatypeSheet) {

            Iterator<Cell> cellIterator = currentRow.iterator();

            int counter = 0;
            List<Cell> entityValues = Lists.newArrayListWithCapacity(3);
            while (cellIterator.hasNext()) {
                counter++;
                if (counter > 6) {
                    break;
                    //throw new RuntimeException("Error parsing document, to many cell in a row");
                }
                Cell currentCell = cellIterator.next();
                entityValues.add(currentCell);
                if (counter % 3 == 0) {
                    final String name = entityValues.get(0).getStringCellValue();
                    if (StringUtils.isEmpty(name)) {
                        SvodOrganization org = new SvodOrganization();
                        org.empty = true;
                    } else {
                        SvodOrganization org = new SvodOrganization();
                        org.name = name;
                        org.code = Integer.parseInt(entityValues.get(1).getStringCellValue());
                        org.sum = BigDecimal.valueOf(entityValues.get(2).getNumericCellValue()).setScale(2, RoundingMode.HALF_UP);
                        svodOrganizations.add(org);
                    }
                    entityValues.clear();
                }
            }
        }
        List<SvodPair> svodPairs = Lists.newArrayList();
        for (int i = 0; i < svodOrganizations.size() - 1; i = i + 2) {
            final SvodOrganization left = svodOrganizations.get(i);
            final SvodOrganization right = svodOrganizations.get(i + 1);
            SvodPair sv = new SvodPair();
            sv.left = left;
            sv.right = right;
            sv.eq = left.equals(right);
            svodPairs.add(sv);
        }
        final Map<Boolean, List<SvodPair>> groupedByEq = svodPairs.stream().collect(Collectors.groupingBy(f -> f.eq));


        XSSFWorkbook svodBook = new XSSFWorkbook();
        XSSFSheet sheet = svodBook.createSheet("Свод отчёт");
        int rowNum = 0;
        Row row = sheet.createRow(rowNum++);
        Cell cell = row.createCell(0);
        cell.setCellValue("Сошлись");
        rowNum = drawPairGroup(sheet, rowNum, groupedByEq.get(true));
        row = sheet.createRow(rowNum++);
        cell = row.createCell(0);
        cell.setCellValue("Несошлись");
        drawPairGroup(sheet, rowNum, groupedByEq.get(false));
        svodBook.write(fileOutputStream);
    }

    private int drawPairGroup(XSSFSheet sheet, int rowNum, List<SvodPair> svodPairs) {
        if (svodPairs == null) {
            return rowNum;
        }
        for (SvodPair p : svodPairs) {
            rowNum = drawPair(sheet, rowNum, p);
        }
        return rowNum;
    }

    private int drawPair(XSSFSheet sheet, int rowNum, SvodPair p) {
        Integer cellNum = 0;
        Row row;
        row = sheet.createRow(rowNum++);
        cellNum = drowOrganization(cellNum, row, p.left);
        drowOrganization(cellNum, row, p.right);
        return rowNum;
    }

    private Integer drowOrganization(Integer cellNum, Row row, SvodOrganization organization) {
        cellNum = drawCell(row, cellNum, organization.name);
        cellNum = drawCell(row, cellNum, organization.code);
        cellNum = drawCell(row, cellNum, organization.sum);
        return cellNum;
    }

    private Integer drawCell(Row row, Integer cellNum, String value) {
        Cell cell;
        cell = row.createCell(cellNum++);
        cell.setCellValue(value);
        return cellNum;
    }

    private Integer drawCell(Row row, Integer cellNum, Integer value) {
        Cell cell;
        cell = row.createCell(cellNum++);
        cell.setCellValue(value);
        return cellNum;
    }

    private Integer drawCell(Row row, Integer cellNum, BigDecimal value) {
        Cell cell;
        cell = row.createCell(cellNum++);
        cell.setCellValue(value.doubleValue());
        return cellNum;
    }

    public static class SvodPair {
        public SvodOrganization left;
        public SvodOrganization right;
        public boolean eq;
    }

    public static class SvodOrganization {
        public String name;
        public Integer code;
        public BigDecimal sum;
        public boolean empty = false;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SvodOrganization that = (SvodOrganization) o;
            return Objects.equal(name, that.name) &&
                Objects.equal(code, that.code) &&
                Objects.equal(sum, that.sum);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(name, code, sum);
        }
    }

}
