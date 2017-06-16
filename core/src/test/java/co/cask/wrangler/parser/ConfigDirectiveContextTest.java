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

package co.cask.wrangler.parser;

import co.cask.wrangler.api.DirectiveParseException;
import co.cask.wrangler.api.Directives;
import co.cask.wrangler.api.Step;
import co.cask.wrangler.config.Config;
import com.google.gson.Gson;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Tests {@link NoOpDirectiveContext}
 */
public class ConfigDirectiveContextTest {

  private static final String CONFIG = "{\n" +
    "\t\"exclusions\" : [\n" +
    "\t\t\"parse-as-csv\",\n" +
    "\t\t\"parse-as-excel\",\n" +
    "\t\t\"set\",\n" +
    "\t\t\"invoke-http\",\n" +
    "\t\t\"js-parser\"\n" +
    "\t],\n" +
    "\n" +
    "\t\"aliases\" : {\n" +
    "\t\t\"json-parser\" : \"parse-as-json\",\n" +
    "\t\t\"js-parser\" : \"parse-as-json\"\n" +
    "\t}\n" +
    "}";

  private static final String EMPTY = "{}";

  @Test(expected = DirectiveParseException.class)
  public void testBasicExclude() throws Exception {
    String[] text = new String[] {
      "parse-as-csv body , true"
    };

    Gson gson = new Gson();
    Config config = gson.fromJson(CONFIG, Config.class);

    Directives directives = new TextDirectives(text);
    directives.initialize(new ConfigDirectiveContext(config));
    directives.getSteps();
  }

  @Test(expected = DirectiveParseException.class)
  public void testAliasedAndExcluded() throws Exception {
    String[] text = new String[] {
      "js-parser body"
    };

    Gson gson = new Gson();
    Config config = gson.fromJson(CONFIG, Config.class);

    Directives directives = new TextDirectives(text);
    directives.initialize(new ConfigDirectiveContext(config));
    directives.getSteps();
  }

  @Test
  public void testAliasing() throws Exception {
    String[] text = new String[] {
      "json-parser body"
    };

    Gson gson = new Gson();
    Config config = gson.fromJson(CONFIG, Config.class);

    Directives directives = new TextDirectives(text);
    directives.initialize(new ConfigDirectiveContext(config));

    List<Step> steps = directives.getSteps();
    Assert.assertEquals(1, steps.size());
  }

  @Test(expected = DirectiveParseException.class)
  public void testEmptyAliasingShouldFail() throws Exception {
    String[] text = new String[] {
      "json-parser body"
    };

    Gson gson = new Gson();
    Config config = gson.fromJson(EMPTY, Config.class);

    Directives directives = new TextDirectives(text);
    directives.initialize(new ConfigDirectiveContext(config));

    List<Step> steps = directives.getSteps();
    Assert.assertEquals(1, steps.size());
  }

  @Test
  public void testWithNoAliasingNoExclusion() throws Exception {
    String[] text = new String[] {
      "parse-as-json body"
    };

    Gson gson = new Gson();
    Config config = gson.fromJson(EMPTY, Config.class);

    Directives directives = new TextDirectives(text);
    directives.initialize(new ConfigDirectiveContext(config));

    List<Step> steps = directives.getSteps();
    Assert.assertEquals(1, steps.size());
  }

}