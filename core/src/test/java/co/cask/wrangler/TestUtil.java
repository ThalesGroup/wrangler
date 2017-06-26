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

package co.cask.wrangler;

import co.cask.wrangler.api.DirectiveParseException;
import co.cask.wrangler.api.RecipeParser;
import co.cask.wrangler.api.RecipePipeline;
import co.cask.wrangler.api.RecipeException;
import co.cask.wrangler.api.Row;
import co.cask.wrangler.executor.RecipePipelineExecutor;
import co.cask.wrangler.parser.SimpleTextParser;

import java.util.List;

/**
 * Utilities for testing.
 */
public final class TestUtil {

  /**
   * Executes the directives on the record specified.
   *
   * @param directives to be executed.
   * @param rows to be executed on directives.
   * @return transformed directives.
   */
  public static List<Row> run(String[] directives, List<Row> rows)
    throws RecipeException, DirectiveParseException {
    RecipeParser d = new SimpleTextParser(directives);
    RecipePipeline pipeline = new RecipePipelineExecutor();
    pipeline.configure(d, null);
    return pipeline.execute(rows);
  }

}
