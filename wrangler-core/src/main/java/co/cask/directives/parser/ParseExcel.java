/*
 *  Copyright © 2017 Cask Data, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package co.cask.directives.parser;

import co.cask.cdap.api.annotation.Description;
import co.cask.cdap.api.annotation.Name;
import co.cask.cdap.api.annotation.Plugin;
import co.cask.functions.Types;
import co.cask.wrangler.api.Arguments;
import co.cask.wrangler.api.Directive;
import co.cask.wrangler.api.DirectiveExecutionException;
import co.cask.wrangler.api.DirectiveParseException;
import co.cask.wrangler.api.ErrorRowException;
import co.cask.wrangler.api.ExecutorContext;
import co.cask.wrangler.api.Optional;
import co.cask.wrangler.api.Row;
import co.cask.wrangler.api.annotations.Categories;
import co.cask.wrangler.api.parser.ColumnName;
import co.cask.wrangler.api.parser.Text;
import co.cask.wrangler.api.parser.TokenType;
import co.cask.wrangler.api.parser.UsageDefinition;
import com.google.common.io.Closeables;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A step to parse Excel files.
 */
@Plugin(type = Directive.Type)
@Name("parse-as-excel")
@Categories(categories = { "parser", "excel"})
@Description("Parses column as Excel file.")
public class ParseExcel implements Directive {
  public static final String NAME = "parse-as-excel";
  private static final Logger LOG = LoggerFactory.getLogger(ParseExcel.class);
  private String column;
  private String sheet;
  private boolean firstRowAsHeader = false;
  private boolean excelDataType = false;

  @Override
  public UsageDefinition define() {
    UsageDefinition.Builder builder = UsageDefinition.builder(NAME);
    builder.define("column", TokenType.COLUMN_NAME);
    builder.define("sheet", TokenType.TEXT, Optional.TRUE);
    builder.define("first-row-as-header", TokenType.BOOLEAN, Optional.TRUE);
    builder.define("use-excel-datatype", TokenType.BOOLEAN, Optional.FALSE);

    return builder.build();
  }

  @Override
  public void initialize(Arguments args) throws DirectiveParseException {
    this.column = ((ColumnName) args.value("column")).value();
    if (args.contains("sheet")) {
      this.sheet = ((Text) args.value("sheet")).value();
    } else {
      this.sheet = "0";
    }
    if (args.contains("first-row-as-header")) {
      this.firstRowAsHeader = ((Boolean) args.value("first-row-as-header").value());
    }

    if (args.contains("use-excel-datatype")) {
      this.excelDataType = ((Boolean) args.value("use-excel-datatype").value());
    }
  }

  @Override
  public void destroy() {
    // no-op
  }

  @Override
  public List<Row> execute(List<Row> records, final ExecutorContext context)
    throws DirectiveExecutionException, ErrorRowException {
    List<Row> results = new ArrayList<>();
    ByteArrayInputStream input = null;
    try {
      for (Row record : records) {
        int idx = record.find(column);
        if (idx != -1) {
          Object object = record.getValue(idx);
          byte[] bytes = null;
          if (object instanceof byte[]) {
            bytes = (byte[]) object;
          } else if (object instanceof ByteBuffer) {
            ByteBuffer buffer = (ByteBuffer) object;
            bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
          } else {
            throw new DirectiveExecutionException(toString() + " : column " + column + " is not byte array or byte buffer.");
          }

          if (bytes != null) {
            input = new ByteArrayInputStream(bytes);
            XSSFWorkbook book = new XSSFWorkbook(input);
            XSSFSheet excelsheet;
            if (Types.isInteger(sheet)) {
              excelsheet = book.getSheetAt(Integer.parseInt(sheet));
            } else {
              excelsheet = book.getSheet(sheet);
            }

            if (excelsheet == null) {
              throw new DirectiveExecutionException(
                String.format("Failed to extract sheet '%s' from the excel. Sheet '%s' does not exist.", sheet, sheet)
              );
            }

            Map<Integer, String> columnNames = new TreeMap<>();
            Iterator<org.apache.poi.ss.usermodel.Row> it = excelsheet.iterator();
            int rows = 0;
            while (it.hasNext()) {
              org.apache.poi.ss.usermodel.Row row = it.next();
              Iterator<Cell> cellIterator = row.cellIterator();
              if(checkIfRowIsEmpty(row)) {
                continue;
              }

              Row newRow = new Row();
              newRow.add("fwd", rows);

              while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();
                String name = columnName(cell.getAddress().getColumn());
                if (firstRowAsHeader && rows > 0) {
                  String value = columnNames.get(cell.getAddress().getColumn());
                  if (value != null) {
                    name = value;
                  }
                }
                String value = "";
                switch (cell.getCellTypeEnum()) {

                  case NUMERIC:
                    if (HSSFDateUtil.isCellDateFormatted(cell)) {
                      if(excelDataType) {
                        newRow.add(name, cell.getDateCellValue());
                      }else{
                        newRow.add(name, cell.getDateCellValue().toString());
                      }
                      value = cell.getDateCellValue().toString();
                    } else {
                      if(excelDataType) {
                        newRow.add(name, cell.getNumericCellValue());
                      }else{
                        newRow.add(name, String.valueOf(cell.getNumericCellValue()));
                      }
                      value = String.valueOf(cell.getNumericCellValue());
                    }
                    break;

                  case STRING:
                    newRow.add(name, cell.getStringCellValue());
                    value = cell.getStringCellValue();
                    break;

                  case BOOLEAN:
                    if(excelDataType){
                      newRow.add(name, cell.getBooleanCellValue());
                    }else {
                      newRow.add(name, String.valueOf(cell.getBooleanCellValue()));
                    }
                    value = String.valueOf(cell.getBooleanCellValue());
                    break;
                }

                if (rows == 0 && firstRowAsHeader) {
                  columnNames.put(cell.getAddress().getColumn(), value);
                }
              }

              if (firstRowAsHeader && rows == 0) {
                rows++;
                continue;
              }
              results.add(newRow);
              rows++;
            }

            if (firstRowAsHeader) {
              rows = rows - 1;
            }

            for (int i = rows - 1; i >= 0; --i) {
              results.get(rows - i - 1).addOrSetAtIndex(1, "bkd", i); // fwd - 0, bkd - 1.
            }
          }
        }
      }
    } catch (Exception e) {
      LOG.info("parsing error :{}",e.getCause());
      throw new ErrorRowException(e.getMessage(), 1);
    } finally {
      if (input != null) {
        Closeables.closeQuietly(input);
      }
    }
    return results;
  }

  private String getFormulaValue(CellValue cellValue, Row newRow, String name, Cell cell) {
      String value = "";
      if (cellValue == null) {
        return null;
      }
      
	  switch (cellValue.getCellTypeEnum()) {
      case STRING:
        newRow.add(name, cellValue.getStringValue());
        value = cellValue.getStringValue();
        break;

      case NUMERIC:
        if (HSSFDateUtil.isValidExcelDate(cellValue.getNumberValue()) && HSSFDateUtil.isInternalDateFormat(cell.getCellStyle().getDataFormat())) {
          value = HSSFDateUtil.getJavaDate(cellValue.getNumberValue()).toString();
          newRow.add(name, value);
    	} else {
          if(excelDataType) {
            newRow.add(name, cellValue.getNumberValue());
          }else{
            newRow.add(name, String.valueOf(cellValue.getNumberValue()));
          }
          value = String.valueOf(cellValue.getNumberValue());
    	}
        break;
      case BOOLEAN:
        if(excelDataType){
          newRow.add(name, cellValue.getBooleanValue());
        }else {
          newRow.add(name, String.valueOf(cellValue.getBooleanValue()));
        }
        value = String.valueOf(cellValue.getBooleanValue());
        break;
      default:
        break;
    }
    return value;
  }

  private boolean checkIfRowIsEmpty(org.apache.poi.ss.usermodel.Row row) {
    if (row == null) {
      return true;
    }
    if (row.getLastCellNum() <= 0) {
      return true;
    }
    for (int cellNum = row.getFirstCellNum(); cellNum < row.getLastCellNum(); cellNum++) {
      Cell cell = row.getCell(cellNum);
      if (cell != null && cell.getCellTypeEnum() != CellType.BLANK && StringUtils.isNotBlank(cell.toString())) {
        return false;
      }
    }
    return true;
  }

  private String columnName(int number) {
    final StringBuilder sb = new StringBuilder();

    int num = number;
    while (num >=  0) {
      int numChar = (num % 26)  + 65;
      sb.append((char)numChar);
      num = (num  / 26) - 1;
    }
    return sb.reverse().toString();
  }
}
