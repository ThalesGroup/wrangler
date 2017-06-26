/*
 * Copyright © 2017 Cask Data, Inc.
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

package co.cask.wrangler.steps.row;

import co.cask.cdap.api.annotation.Description;
import co.cask.cdap.api.annotation.Name;
import co.cask.cdap.api.annotation.Plugin;
import co.cask.wrangler.api.AbstractDirective;
import co.cask.wrangler.api.RecipeContext;
import co.cask.wrangler.api.Row;
import co.cask.wrangler.api.DirectiveExecutionException;
import co.cask.wrangler.api.Usage;

import java.util.ArrayList;
import java.util.List;

/**
 * Filters records if they don't have all the columns specified or they have null values or combination.
 */
@Plugin(type = "udd")
@Name("filter-rows-on")
@Usage("filter-rows-on empty-or-null-columns <column>[,<column>*]")
@Description("Filters row that have empty or null columns.")
public class RecordMissingOrNullFilter extends AbstractDirective {
  private final String[] columns;

  public RecordMissingOrNullFilter(int lineno, String directive, String[] columns) {
    super(lineno, directive);
    this.columns = columns;
  }

  /**
   * Executes a wrangle step on single {@link Row} and return an array of wrangled {@link Row}.
   *
   * @param rows List of input {@link Row} to be wrangled by this step.
   * @param context {@link RecipeContext} passed to each step.
   * @return Wrangled List of {@link Row}.
   */
  @Override
  public List<Row> execute(List<Row> rows, RecipeContext context) throws DirectiveExecutionException {
    List<Row> results = new ArrayList<>();
    for (Row row : rows) {
      boolean missingOrNull = true;
      for (String column : columns) {
        int idx = row.find(column.trim());
        if (idx != -1) {
          Object value = row.getValue(idx);
          if (value != null) {
            missingOrNull = false;
          }
        }
      }
      if (!missingOrNull) {
        results.add(row);
      }
    }
    return results;
  }
}
