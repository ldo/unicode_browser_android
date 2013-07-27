package nz.gen.geek_central.android_useful;
/*
    Management of persistent settings in JSON format. There is support
    for only limited data types at present--only what I've needed.
    The assumption is the data will not be large, so the file is not
    compressed.

    Copyright 2013 Lawrence D'Oliveiro <ldo@geek-central.gen.nz>.

    Licensed under the Apache License, Version 2.0 (the "License"); you may not
    use this file except in compliance with the License. You may obtain a copy of
    the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
    License for the specific language governing permissions and limitations under
    the License.
*/

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
public class JSONPrefs
  {
    private final android.content.Context ctx;
    private final String PrefsFileName;
    private JSONObject Contents = null;

    public JSONPrefs
      (
        android.content.Context ctx,
        String PrefsFileName
      )
      {
        this.ctx = ctx;
        this.PrefsFileName = PrefsFileName;
      } /*JSONPrefs*/

    public JSONPrefs Clear()
      {
        Contents = null;
        return
            this; /* for convenient chaining of calls */
      } /*Clear*/

    public JSONPrefs PutIntArray
      (
        String Key,
        int[] Value /* can be null for empty array */
      )
      {
        if (Contents == null)
          {
            Contents = new JSONObject();
          } /*if*/
        try
          {
            final JSONArray Item = new JSONArray();
            if (Value != null)
              {
                for (int i = 0; i < Value.length; ++i)
                  {
                    Item.put(Value[i]);
                  } /*for*/
              } /*if*/
            Contents.put(Key, Item);
          }
        catch (JSONException Bug)
          {
            throw new RuntimeException(Bug.toString());
          } /*try*/
        return
            this; /* for convenient chaining of calls */
      } /*PutIntArray*/

    public boolean Save()
      /* saves the contents to the prefs file. Returns success/failure. */
      {
        if (Contents == null)
          {
            Contents = new JSONObject();
          } /*if*/
        boolean Success = false;
        try
          {
            ctx.deleteFile(PrefsFileName); /* don't bother doing any fancy atomic stuff */
            final java.io.FileOutputStream OutFile =
                ctx.openFileOutput(PrefsFileName, ctx.MODE_WORLD_READABLE);
            OutFile.write(Contents.toString().getBytes("utf-8"));
            OutFile.flush();
            OutFile.close();
            Success = true;
          }
        catch (java.io.IOException Bad)
          {
          } /*try*/
        if (!Success)
          {
            ctx.deleteFile(PrefsFileName); /* ensure no leftover partial state */
          } /*if*/
        return
            Success;
      } /*Save*/

    public JSONPrefs Load()
      /* loads the existing contents of the prefs file, if any. */
      {
        Contents = null; /* in case of error */
        try
          {
            final java.io.FileInputStream InFile = ctx.openFileInput(PrefsFileName);
            byte[] Buf = new byte[512];
            int BufLength = 0;
            for (;;)
              {
                if (BufLength == Buf.length)
                  {
                    final byte[] NewBuf = new byte[Buf.length * 2];
                    System.arraycopy(Buf, 0, NewBuf, 0, BufLength);
                    Buf = NewBuf;
                  } /*if*/
                final int MoreBytes =
                    InFile.read(Buf, BufLength, Buf.length - BufLength);
                if (MoreBytes <= 0)
                    break;
                BufLength += MoreBytes;
              } /*for*/
            InFile.close();
            Contents = new JSONObject(new String(Buf, 0, BufLength, "utf-8"));
          }
        catch (java.io.IOException Bad)
          {
          }
        catch (JSONException Corrupted)
          {
          } /*try*/
        return
            this; /* for convenient chaining of calls */
      } /*Load*/

    public boolean GotContents()
      {
        return
            Contents != null;
      } /*GotContents*/

    public int[] GetIntArray
      (
        String Key
      )
      /* returns an int array stored under the specified key, or null if none. */
      {
        int[] Result = null;
        try
          {
            if (Contents != null)
              {
                final JSONArray Item = Contents.optJSONArray(Key);
                if (Item != null && Item.length() != 0)
                  {
                    Result = new int[Item.length()];
                    for (int i = 0; i < Result.length; ++i)
                      {
                        Result[i] = Item.getInt(i);
                      } /*for*/
                  } /*if*/
              } /*if*/
          }
        catch (JSONException Corrupted)
          {
            Result = null;
          } /*try*/
        return
            Result;
      } /*GetIntArray*/

  } /*JSONPrefs*/;
