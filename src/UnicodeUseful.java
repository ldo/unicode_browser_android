package nz.gen.geek_central.android_useful;
/*
    Useful Unicode-related stuff. This includes handling of UTF-16
    encoding/decoding as required by Java strings.

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

import java.util.ArrayList;
public class UnicodeUseful
  {

    public static String FormatCharCode
      (
        int Code
      )
      /* returns the conventional U+xxxx representation for a Unicode character code. */
      {
        return
            String.format(java.util.Locale.US, "U+%04X", Code);
      } /*FormatCharCode*/

    public static String CharToString
      (
        int Code
      )
      /* returns a literal string containing the single specified character. */
      {
        return
            new String
              (
                Code > 0xffff ?
                    new char[]
                        { /* encode as UTF-16 */
                            (char)(Code - 0x10000 >> 10 & 0x3ff | 0xd800),
                            (char)(Code & 0x3ff | 0xdc00),
                        }
                :
                    new char[] {(char)Code}
              );
      } /*CharToString*/

    public static String CharsToString
      (
        int[] Codes
      )
      {
        final StringBuilder Result = new StringBuilder(Codes.length);
        for (int i = 0; i < Codes.length; ++i)
          {
            final int Code = Codes[i];
            if (Code > 0xffff)
              {
                Result.append((char)(Code - 0x10000 >> 10 & 0x3ff | 0xd800));
                Result.append((char)(Code & 0x3ff | 0xdc00));
              }
            else
              {
                Result.append((char)Code);
              } /*if*/
          } /*for*/
        return
            Result.toString();
      } /*CharsToString*/

    public static int[] CharSequenceToChars
      (
        CharSequence Text
      )
      {
        ArrayList<Integer> Result = new ArrayList<Integer>();
        for (int i = 0;;)
          {
            if (i == Text.length())
                break;
            int Val = (int)Text.charAt(i) & 65535;
            if ((Val & 0xfffffc00) == 0xd800 && Text.length() - i > 1)
              {
                final int CharCode2 = (int)Text.charAt(i + 1) & 65535;
                if ((CharCode2 & 0xfffffc00) == 0xdc00) /* decode UTF-16 */
                  {
                    ++i;
                    Val = ((Val & 0x3ff) << 10 | CharCode2 & 0x3ff) + 0x10000;
                  } /*if*/
              } /*if*/
            Result.add(Val);
            ++i;
          } /*for*/
        return
            ToArray(Result);
      } /*CharSequenceToChars*/

    public static int[] ToArray
      (
        ArrayList<Integer> TheList
      )
      /* because Java doesn't allow direct conversion to an int array... */
      {
        final Integer[] Temp = TheList.toArray(new Integer[TheList.size()]);
        final int[] Result = new int[Temp.length];
        for (int i = 0; i < Result.length; ++i)
          {
            Result[i] = Temp[i];
          } /*for*/
        return
            Result;
      } /*ToArray*/

  } /*UnicodeUseful*/;
