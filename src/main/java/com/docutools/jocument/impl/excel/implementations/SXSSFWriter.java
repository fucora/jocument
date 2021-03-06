package com.docutools.jocument.impl.excel.implementations;

import com.docutools.jocument.impl.excel.interfaces.ExcelWriter;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

/**
 * This is a streamed implementation of the {@link com.docutools.jocument.impl.excel.interfaces.ExcelWriter} interface.
 * The streaming is done so memory can be saved.
 * For now, the amount of rows kept in memory is set to the default, 100.
 * SXSSFWriter works by keeping a reference to the current sheet and row being edited, and copying/cloning required
 * values on the creation of new objects.
 * This is why to the `new...`/`add...` methods the original references of the template should be passed.
 * If one would like to use objects created somewhere else directly, a new implementation considering this would have
 * to be created.
 *
 * @author Anton Oellerer
 * @since 2020-04-02
 */
public class SXSSFWriter implements ExcelWriter {
  private static final Logger logger = LogManager.getLogger();

  private final Path path;
  private final SXSSFWorkbook workbook;
  private Sheet currentSheet;
  private Row currentRow;
  private int rowOffset = 0;

  /**
   * Creates a new SXSSFWriter.
   *
   * @param path The path to save the finished report to.
   */
  public SXSSFWriter(Path path) {
    workbook = new SXSSFWorkbook();
    workbook.setForceFormulaRecalculation(true);
    this.path = path;
  }

  @Override
  public void newSheet(Sheet sheet) {
    logger.info("Creating new sheet of {}", sheet.getSheetName());
    currentSheet = workbook.createSheet(sheet.getSheetName());
    currentSheet.setActiveCell(sheet.getActiveCell());
    currentSheet.setAutobreaks(sheet.getAutobreaks());
    Arrays.stream(sheet.getColumnBreaks()).forEach(column -> currentSheet.setColumnBreak(column));
    currentSheet.setDefaultColumnWidth(sheet.getDefaultColumnWidth());
    currentSheet.setDefaultRowHeight(sheet.getDefaultRowHeight());
    currentSheet.setDisplayFormulas(sheet.isDisplayFormulas());
    currentSheet.setDisplayGridlines(sheet.isDisplayGridlines());
    currentSheet.setDisplayGuts(sheet.getDisplayGuts());
    currentSheet.setDisplayRowColHeadings(sheet.isDisplayRowColHeadings());
    currentSheet.setDisplayZeros(sheet.isDisplayZeros());
    currentSheet.setFitToPage(sheet.getFitToPage());
    currentSheet.setHorizontallyCenter(sheet.getHorizontallyCenter());
    currentSheet.setPrintGridlines(sheet.isPrintGridlines());
    currentSheet.setPrintRowAndColumnHeadings(sheet.isPrintRowAndColumnHeadings());
    currentSheet.setRepeatingColumns(sheet.getRepeatingColumns());
    currentSheet.setRepeatingRows(sheet.getRepeatingRows());
    currentSheet.setRightToLeft(sheet.isRightToLeft());
    Arrays.stream(sheet.getRowBreaks()).forEach(row -> currentSheet.setRowBreak(row));
    currentSheet.setRowSumsBelow(sheet.getRowSumsBelow());
    currentSheet.setRowSumsRight(sheet.getRowSumsRight());
    currentSheet.setSelected(sheet.isSelected());
    currentSheet.setVerticallyCenter(sheet.getVerticallyCenter());
  }

  @Override
  public void newRow(Row row) {
    logger.debug("Creating new row {}", row.getRowNum());
    currentRow = currentSheet.createRow(row.getRowNum() + rowOffset);
    currentRow.setHeight(row.getHeight());
    currentRow.setRowStyle(row.getRowStyle());
    currentRow.setZeroHeight(row.getZeroHeight());
  }

  @Override
  public void addCell(Cell cell) {
    logger.debug("Creating new cell {} {}", cell.getColumnIndex(), cell.getRow().getRowNum());
    var newCell = currentRow.createCell(cell.getColumnIndex(), cell.getCellType());
    if (workbook.getCellStyleAt(cell.getCellStyle().getIndex()) == null) {
      copyCellStyle(cell.getCellStyle());
    }
    newCell.setCellComment(cell.getCellComment());
    newCell.setCellStyle(cell.getCellStyle());
    newCell.setHyperlink(cell.getHyperlink());
    currentSheet.setColumnWidth(cell.getColumnIndex(), cell.getSheet().getColumnWidth(cell.getColumnIndex()));
    switch (cell.getCellType()) {
      case _NONE -> {
      }
      case NUMERIC -> newCell.setCellValue(cell.getNumericCellValue());
      case STRING -> newCell.setCellValue(cell.getStringCellValue());
      case FORMULA -> newCell.setCellFormula(cell.getCellFormula());
      case BLANK -> newCell.setBlank();
      case BOOLEAN -> newCell.setCellValue(cell.getBooleanCellValue());
      case ERROR -> newCell.setCellErrorValue(cell.getErrorCellValue());
      default -> {
      }
    }
  }

  @Override
  public void addCell(Cell templateCell, String newCellText) {
    logger.debug("Creating new cell {} {} with text {}",
        templateCell.getColumnIndex(), templateCell.getRow().getRowNum(), newCellText);
    var newCell = currentRow.createCell(templateCell.getColumnIndex(), templateCell.getCellType());
    if (workbook.getCellStyleAt(templateCell.getCellStyle().getIndex()) == null) {
      copyCellStyle(templateCell.getCellStyle());
    }
    newCell.setCellComment(templateCell.getCellComment());
    newCell.setCellStyle(templateCell.getCellStyle());
    newCell.setHyperlink(templateCell.getHyperlink());
    currentSheet.setColumnWidth(templateCell.getColumnIndex(), templateCell.getSheet().getColumnWidth(templateCell.getColumnIndex()));
    newCell.setCellValue(newCellText);
  }

  @Override
  public void complete() throws IOException {
    var outputStream = new BufferedOutputStream(Files.newOutputStream(path));
    workbook.write(outputStream);
    outputStream.close();
    workbook.dispose();
  }

  @Override
  public void addRowOffset(int size) {
    rowOffset += size;
  }

  private void copyCellStyle(CellStyle cellStyle) {
    var newStyle = workbook.createCellStyle();
    newStyle.cloneStyleFrom(cellStyle);
  }
}
