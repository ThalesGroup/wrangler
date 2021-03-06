/*
 *  Copyright © 2017-2018 Cask Data, Inc.
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

package co.cask.wrangler.service;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests {@link FileTypeDetector}
 */
public class FileTypeDetectorTest {

  @Test
  public void testFileTypeExtensions() throws Exception {
    FileTypeDetector detector = new FileTypeDetector();

    String[] filenames = {
      "syslog.dat",
      "syslog.dat.1",
      "syslog.txt",
      "syslog.txt.1",
      "titanic.csv",
      "titanic.csv.1",
      "titanic.csv.1.2",
      "noextension"
    };

    for (String filename : filenames) {
      String mimeType = detector.detectFileType(filename);
      Assert.assertNotEquals("UNKNOWN", mimeType);
      Assert.assertEquals("text/plain", mimeType);
    }
  }
}