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

package co.cask.wrangler.steps.transformation;

import co.cask.cdap.api.annotation.Description;
import co.cask.cdap.api.annotation.Name;
import co.cask.cdap.api.annotation.Plugin;
import co.cask.wrangler.api.AbstractDirective;
import co.cask.wrangler.api.DirectiveExecutionException;
import co.cask.wrangler.api.RecipeContext;
import co.cask.wrangler.api.Row;
import co.cask.wrangler.api.Usage;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * A step that decodes a column that was encoded as base-32, base-64, or hex.
 */
@Plugin(type = "udd")
@Name("decode")
@Usage("decode <base32|base64|hex> <column>")
@Description("Decodes column values using one of base32, base64, or hex.")
public class Decode extends AbstractDirective {
  private final Base64 base64Encode = new Base64();
  private final Base32 base32Encode = new Base32();
  private final Hex hexEncode = new Hex();
  private final Type type;
  private final String column;

  /**
   * Defines encoding types supported.
   */
  public enum Type {
    BASE64("BASE64"),
    BASE32("BASE32"),
    HEX("HEX");

    private String type;

    Type(String type) {
      this.type = type;
    }

    String getType() {
      return type;
    }
  }

  public Decode(int lineno, String directive, Type type, String column) {
    super(lineno, directive);
    this.type = type;
    this.column = column;
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
    for (Row row : rows) {
      int idx = row.find(column);
      if (idx == -1) {
        continue;
      }

      Object object = row.getValue(idx);
      if (object == null) {
        continue;
      }

      byte[] value = new byte[0];
      if (object instanceof String) {
        value = ((String) object).getBytes();
      } else if (object instanceof byte[]) {
        value = (byte[]) object;
      } else {
        throw new DirectiveExecutionException(
          String.format("%s : Invalid value type '%s' of column '%s'. Should be of type string or byte array, "
            , toString(), value.getClass().getName(), column)
        );
      }

      byte[] out = new byte[0];
      if (type == Type.BASE32) {
        out = base32Encode.decode(value);
      } else if (type == Type.BASE64) {
        out = base64Encode.decode(value);
      } else if (type == Type.HEX) {
        try {
          out = hexEncode.decode(value);
        } catch (DecoderException e) {
          throw new DirectiveExecutionException(
            String.format("%s : Failed to decode hex value.", toString())
          );
        }
      } else {
        throw new DirectiveExecutionException(
          String.format("%s : Invalid type of encoding '%s' specified", toString(), type.toString())
        );
      }

      String obj = new String(out, StandardCharsets.UTF_8);
      row.addOrSet(String.format("%s_decode_%s", column, type.toString().toLowerCase(Locale.ENGLISH)), obj);
    }
    return rows;
  }
}
