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

package co.cask.wrangler.registry;

import co.cask.cdap.api.annotation.Description;
import co.cask.cdap.api.annotation.Name;
import co.cask.wrangler.api.UDD;
import co.cask.wrangler.api.parser.UsageDefinition;
import com.google.gson.JsonObject;

import java.util.Objects;

/**
 * Class description here.
 */
public final class DirectiveInfo {
  private String name;
  private UsageDefinition definition;
  private Class<?> directive;
  private String usage;
  private String description;

  public DirectiveInfo(Class<?> directive) throws IllegalAccessException, InstantiationException {
    this.directive = directive;
    Object object = directive.newInstance();
    this.definition = ((UDD) object).define();
    this.usage = definition.toString();
    this.name = directive.getAnnotation(Name.class).value();
    this.description = directive.getAnnotation(Description.class).value();
  }

  public String name() {
    return name;
  }

  public String usage() {
    return usage;
  }

  public UsageDefinition definition() {
    return definition;
  }

  public UDD instance() throws IllegalAccessException, InstantiationException {
    return (UDD) directive.newInstance();
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if(this.getClass() != obj.getClass()) {
      return false;
    }
    final DirectiveInfo other = (DirectiveInfo) obj;
    return Objects.equals(this.name, other.name);
  }

  public JsonObject toJson() {
    JsonObject response = new JsonObject();
    response.addProperty("name", name);
    response.addProperty("usage", usage);
    response.addProperty("description", description);
    response.addProperty("class", directive.getCanonicalName());
    response.add("definition", definition.toJson());
    return response;
  }
}