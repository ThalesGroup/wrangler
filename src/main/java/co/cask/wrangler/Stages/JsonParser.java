package co.cask.wrangler.Stages;

import co.cask.wrangler.WrangleStep;
import co.cask.wrangler.Row;
import co.cask.wrangler.WrangleStepException;

/**
 * Created by nitin on 12/3/16.
 */
public class JsonParser implements WrangleStep {
  private String col;

  public JsonParser(String col) {
    this.col = col;
  }

  @Override
  public Row execute(Row row) throws WrangleStepException {
    return null;
  }
}
