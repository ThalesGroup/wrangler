/*
 * Copyright © 2016 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.wrangler.executor;

import co.cask.cdap.api.data.format.StructuredRecord;
import co.cask.cdap.api.data.schema.Schema;
import co.cask.wrangler.api.DirectiveParseException;
import co.cask.wrangler.api.RecipeParser;
import co.cask.wrangler.api.ErrorRecordException;
import co.cask.wrangler.api.RecipePipeline;
import co.cask.wrangler.api.RecipeContext;
import co.cask.wrangler.api.RecipeException;
import co.cask.wrangler.api.Row;
import co.cask.wrangler.api.Directive;
import co.cask.wrangler.api.DirectiveExecutionException;
import co.cask.wrangler.utils.RecordConvertor;
import co.cask.wrangler.utils.RecordConvertorException;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrangle RecipePipeline executes stepRegistry in the order they are specified.
 */
public final class RecipePipelineExecutor implements RecipePipeline<Row, StructuredRecord, ErrorRecord> {
  private RecipeContext context;
  private List<Directive> directives;
  private final ErrorRecordCollector collector = new ErrorRecordCollector();
  private RecordConvertor convertor = new RecordConvertor();

  /**
   * Configures the pipeline based on the directives.
   *
   * @param parser Wrangle directives parser.
   */
  @Override
  public void configure(RecipeParser parser, RecipeContext context) throws RecipeException {
    this.context = context;
    try {
      this.directives = parser.parse();
    } catch (DirectiveParseException e) {
      throw new RecipeException(
        String.format(e.getMessage())
      );
    }
  }

  /**
   * Executes the pipeline on the input.
   *
   * @param rows List of Input record of type I.
   * @param schema Schema to which the output should be mapped.
   * @return Parsed output list of record of type O
   */
  @Override
  public List<StructuredRecord> execute(List<Row> rows, Schema schema)
    throws RecipeException {
    rows = execute(rows);
    try {
      List<StructuredRecord> output = convertor.toStructureRecord(rows, schema);
      return output;
    } catch (RecordConvertorException e) {
      throw new RecipeException("Problem converting into output record. Reason : " + e.getMessage());
    }
  }

  /**
   * Executes the pipeline on the input.
   *
   * @param rows List of input record of type I.
   * @return Parsed output list of record of type I
   */
  @Override
  public List<Row> execute(List<Row> rows) throws RecipeException {
    List<Row> results = Lists.newArrayList();
    try {
      int i = 0;
      collector.reset();
      while (i < rows.size()) {
        List<Row> newRows = rows.subList(i, i+1);
        try {
          for (Directive directive : directives) {
            newRows = directive.execute(newRows, context);
            if (newRows.size() < 1) {
              break;
            }
          }
          if(newRows.size() > 0) {
            results.addAll(newRows);
          }
        } catch (ErrorRecordException e) {
          collector.add(new ErrorRecord(newRows.get(0), e.getMessage(), e.getCode()));
        }
        i++;
      }
    } catch (DirectiveExecutionException e) {
      throw new RecipeException(e);
    }
    return results;
  }

  /**
   * Returns records that are errored out.
   *
   * @return records that have errored out.
   */
  @Override
  public List<ErrorRecord> errors() throws RecipeException {
    return collector.get();
  }

  /**
   * Converts a {@link Row} to a {@link StructuredRecord}.
   *
   * @param rows {@link Row} to be converted
   * @param schema Schema of the {@link StructuredRecord} to be created.
   * @return A {@link StructuredRecord} from record.
   */
  private List<StructuredRecord> toStructuredRecord(List<Row> rows, Schema schema) {
    List<StructuredRecord> results = new ArrayList<>();
    for (Row row : rows) {
      StructuredRecord.Builder builder = StructuredRecord.builder(schema);
      List<Schema.Field> fields = schema.getFields();
      for (Schema.Field field : fields) {
        String name = field.getName();
        Object value = row.getValue(name);
        if (value != null) {
          builder.set(name, value);
        }
      }
      results.add(builder.build());
    }
    return results;
  }
}
