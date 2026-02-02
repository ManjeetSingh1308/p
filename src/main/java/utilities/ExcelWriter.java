package utilities;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ExcelWriter {

    public static void writeTable(String outPath, String sheetName,
                                  List<String> headers, List<List<String>> rows) throws Exception {
        Path p = Path.of(outPath).toAbsolutePath();
        Files.createDirectories(p.getParent());

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet(sheetName);
            int r = 0;

            // Header
            Row hr = sh.createRow(r++);
            for (int c = 0; c < headers.size(); c++) {
                Cell cell = hr.createCell(c, CellType.STRING);
                cell.setCellValue(headers.get(c));
            }

            // Rows
            for (List<String> row : rows) {
                Row rr = sh.createRow(r++);
                for (int c = 0; c < row.size(); c++) {
                    rr.createCell(c, CellType.STRING).setCellValue(row.get(c));
                }
            }

            // Autosize
            for (int c = 0; c < headers.size(); c++) sh.autoSizeColumn(c);

            try (FileOutputStream fos = new FileOutputStream(p.toFile())) {
                wb.write(fos);
            }
        }
        System.out.println("Excel written to: " + p);
    }
}
