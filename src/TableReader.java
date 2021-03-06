package nz.gen.geek_central.unicode_browser;
/*
    Unicode Browser app--loading of character table.

    The character table file is created by the util/get_codes
    script; see there for a description of the file format.

    Copyright 2013, 2014 Lawrence D'Oliveiro <ldo@geek-central.gen.nz>.

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

import android.util.SparseArray;
public class TableReader
  {

    public static class UnicodeTable
      {
        private final byte[] Contents;
        private final java.nio.ByteBuffer ContentsBuf;
        public final int NrChars, NrCharCategories;
        private final int NrCharRuns;
        private final int CharCodesOffset, CharNamesOffset, CharCategoriesOffset, AltNamesOffset, LikeCharsOffset;
        private final int CategoryNamesOffset, CategoryOrderOffset, CategoryCodesOffset, CategoryLowBoundsOffset, CategoryHighBoundsOffset, CharRunsStart;
        private final int NameStringsStart;
        private final int AltNamesStart, LikeCharsStart;
        public final String Version;

        public UnicodeTable
          (
            android.content.res.AssetFileDescriptor TableFile
          )
          {
            final int ContentsLength = (int)TableFile.getLength();
            Contents = new byte[ContentsLength];
            try
              {
                final int BytesRead = TableFile.createInputStream().read(Contents);
                if (BytesRead != ContentsLength)
                  {
                    throw new RuntimeException
                      (
                        String.format("expected %d bytes, got %d", ContentsLength, BytesRead)
                      );
                  } /*if*/
              }
            catch (java.io.IOException Failed)
              {
                throw new RuntimeException(Failed.toString());
              } /*try*/
            ContentsBuf = java.nio.ByteBuffer.wrap(Contents);
            ContentsBuf.order(java.nio.ByteOrder.LITTLE_ENDIAN);
            CharCodesOffset = ContentsBuf.getInt(0) + 4;
            NrChars = ContentsBuf.getInt(CharCodesOffset - 4);
            CharNamesOffset = CharCodesOffset + NrChars * 4;
            CharCategoriesOffset = CharNamesOffset + NrChars * 4;
            AltNamesOffset = CharCategoriesOffset + NrChars * 4;
            LikeCharsOffset = AltNamesOffset + NrChars * 4;
            CategoryNamesOffset = ContentsBuf.getInt(4) + 4;
            NrCharCategories = ContentsBuf.getInt(CategoryNamesOffset - 4);
            CategoryOrderOffset = CategoryNamesOffset + NrCharCategories * 4;
            CategoryCodesOffset = CategoryOrderOffset + NrCharCategories * 4;
            CategoryLowBoundsOffset = CategoryCodesOffset + NrCharCategories * 4;
            CategoryHighBoundsOffset = CategoryLowBoundsOffset + NrCharCategories * 4;
            NameStringsStart = ContentsBuf.getInt(12);
            CharRunsStart = ContentsBuf.getInt(8) + 4;
            NrCharRuns = ContentsBuf.getInt(CharRunsStart - 4);
            AltNamesStart = ContentsBuf.getInt(16);
            LikeCharsStart = ContentsBuf.getInt(20);
            Version = GetString(ContentsBuf.getInt(24));
          } /*UnicodeTable*/

        private String GetString
          (
            int Offset
          )
          /* extracts the string at the specified offset within the names table. */
          {
            try
              {
                return
                    new String
                      (
                        /*data =*/ Contents,
                        /*offset =*/ NameStringsStart + Offset + 1,
                        /*byteCount =*/ (int)Contents[NameStringsStart + Offset] & 255,
                        /*charSetName =*/ "utf-8"
                      );
              }
            catch (java.io.UnsupportedEncodingException Huh)
              {
                throw new RuntimeException(Huh.toString());
              } /*try*/
          } /*GetString*/

        public String GetCategoryName
          (
            int CategoryCode /* must be in [0 .. NrCharCategories - 1] */
          )
          /* returns the name of the category with the specified code. */
          {
            return
                GetString(ContentsBuf.getInt(CategoryNamesOffset + CategoryCode * 4));
          } /*GetCategoryName*/

        public int GetCategoryNameOrdered
          (
            int Index /* must be in [0 .. NrCharCategories - 1] */
          )
          /* returns the category code with the specified alphabetical index. */
          {
            return
                ContentsBuf.getInt(CategoryOrderOffset + Index * 4);
          } /*GetCategoryNameOrdered*/

        public int GetCategoryCodeOrdered
          (
            int Index /* must be in [0 .. NrCharCategories - 1] */
          )
          /* returns the category code with the specified index. Indexes are
            assigned by increasing code block range start. */
          {
            return
                ContentsBuf.getInt(CategoryCodesOffset + Index * 4);
          } /*GetCategoryCodeOrdered*/

        public int GetCategoryLowBoundByCode
          (
            int Index /* must be in [0 .. NrCharCategories - 1] */
          )
          /* returns the lowest character code in the category with the specified index. */
          {
            return
                ContentsBuf.getInt(CategoryLowBoundsOffset + Index * 4);
          } /*GetCategoryLowBoundByCode*/

        public int GetCategoryHighBoundByCode
          (
            int Index /* must be in [0 .. NrCharCategories - 1] */
          )
          /* returns the highest character code in the category with the specified index. */
          {
            return
                ContentsBuf.getInt(CategoryHighBoundsOffset + Index * 4);
          } /*GetCategoryHighBoundByCode*/

        public int GetCharIndex
          (
            int CharCode,
            boolean MustExist /* throw exception instead of returning not-found index */
          )
          /* returns the index within the character table of the entry with the
            specified character code, or -1 if not found. */
          {
            int Result;
            for (int i = 0;;)
              {
                if (i == NrCharRuns)
                  {
                    if (MustExist)
                      {
                        throw new RuntimeException
                          (
                            String.format("UnicodeTable: undefined char %#X", CharCode)
                          );
                      } /*if*/
                    Result = -1;
                    break;
                  } /*if*/
                final int Base = CharRunsStart + i * 12;
                if (CharCode >= ContentsBuf.getInt(Base) && CharCode <= ContentsBuf.getInt(Base + 4))
                  {
                    Result = ContentsBuf.getInt(Base + 8) + CharCode - ContentsBuf.getInt(Base);
                    break;
                  } /*if*/
                ++i;
              } /*for*/
            return
                Result;
          } /*GetCharIndex*/

      /* following routines return information about a character given its index
        as returned by GetCharIndex, not its character code */

        public int GetCharCode
          (
            int CharIndex
          )
          /* returns the character code. */
          {
            return
                ContentsBuf.getInt(CharCodesOffset + CharIndex * 4);
          } /*GetCharCode*/

        public String GetCharName
          (
            int CharIndex
          )
          /* returns the character name. */
          {
            return
                GetString(ContentsBuf.getInt(CharNamesOffset + CharIndex * 4));
          } /*GetCharName*/

        public int GetCharCategory
          (
            int CharIndex
          )
          /* returns the character's category code. */
          {
            return
                ContentsBuf.getInt(CharCategoriesOffset + CharIndex * 4);
          } /*GetCharCategory*/

        public String[] GetCharOtherNames
          (
            int CharIndex
          )
          /* returns the alternative names for the character. */
          {
            final int Base = AltNamesStart + ContentsBuf.getInt(AltNamesOffset + CharIndex * 4);
            final int NrOtherNames = ContentsBuf.getInt(Base);
            final String[] Result = new String[NrOtherNames];
            for (int i = 1; i <= NrOtherNames; ++i)
              {
                Result[i - 1] = GetString(ContentsBuf.getInt(Base + i * 4));
              } /*for*/
            return
                Result;
          } /*GetCharOtherNames*/

        public int[] GetCharLikeChars
          (
            int CharIndex
          )
          /* returns the codes of other, similar characters. */
          {
            final int Base = LikeCharsStart + ContentsBuf.getInt(LikeCharsOffset + CharIndex * 4);
            final int NrLikeChars = ContentsBuf.getInt(Base);
            final int[] Result = new int[NrLikeChars];
            for (int i = 1; i <= NrLikeChars; ++i)
              {
                Result[i - 1] = ContentsBuf.getInt(Base + i * 4);
              } /*for*/
            return
                Result;
          } /*GetCharLikeChars*/

      } /*UnicodeTable*/;

    public static UnicodeTable Unicode;
    public static SparseArray<CharInfo> CharsByIndex;
    public static SparseArray<String[]> CharNamesSplitByCode;

    public static void Load
      (
        android.content.Context ctx
      )
      /* must be called before using anything else in this class. */
      {
        if (Unicode == null)
          {
            Unicode = new UnicodeTable(ctx.getResources().openRawResourceFd(R.raw.unicode));
          } /*if*/
      } /*Load*/

    public static String[] SplitWords
      (
        String s
      )
      /* returns the components of s split at spaces, and converted to lower case. */
      {
        final java.util.ArrayList<String> Result = new java.util.ArrayList<String>();
        StringBuilder CurComponent = null;
        for (int i = 0;;)
          {
            if (i == s.length() || s.charAt(i) == ' ')
              {
                if (CurComponent != null)
                  {
                    Result.add(CurComponent.toString().toLowerCase());
                    CurComponent = null;
                  } /*if*/
                if (i == s.length())
                    break;
              } /*if*/
            if (s.charAt(i) != ' ')
              {
                if (CurComponent == null)
                  {
                    CurComponent = new StringBuilder();
                  } /*if*/
                CurComponent.append(s.charAt(i));
              } /*if*/
            ++i;
          } /*for*/
        return
            Result.toArray(new String[Result.size()]);
      } /*SplitWords*/

    public static class CharInfo
      {
        public final int Code;
        public final String Name;
        public final int Category; /* index into CategoryNames table */
        public final String[] OtherNames;
        public final int[] LikeChars; /* codes for other similar chars */

        public CharInfo
          (
            int Code,
            String Name,
            int Category,
            String[] OtherNames,
            int[] LikeChars
          )
          {
            this.Code = Code;
            this.Name = Name.intern();
            this.Category = Category;
            this.OtherNames = OtherNames;
            this.LikeChars = LikeChars;
          } /*CharInfo*/
      } /*CharInfo*/;

    public static CharInfo GetCharByIndex
      (
        int CharIndex
      )
      {
        CharInfo Result = CharsByIndex != null ? CharsByIndex.get(CharIndex) : null;
        if (Result == null)
          {
            Result = new CharInfo
              (
                /*Code =*/ Unicode.GetCharCode(CharIndex),
                /*Name =*/ Unicode.GetCharName(CharIndex),
                /*Category =*/ Unicode.GetCharCategory(CharIndex),
                /*OtherNames =*/ Unicode.GetCharOtherNames(CharIndex),
                /*LikeChars =*/ Unicode.GetCharLikeChars(CharIndex)
              );
            if (CharsByIndex == null)
              {
                CharsByIndex = new SparseArray<CharInfo>(Unicode.NrChars);
              } /*if*/
            CharsByIndex.put(CharIndex, Result);
          } /*if*/
        return
            Result;
      } /*GetCharByIndex*/

    public static CharInfo GetCharByCode
      (
        int CharCode,
        boolean MustExist
      )
      {
        final int CharIndex = Unicode.GetCharIndex(CharCode, MustExist);
        return
            CharIndex >= 0 ?
                GetCharByIndex(CharIndex)
            :
                null;
      } /*GetCharByCode*/

    public static boolean CharNameMatches
      (
        int CharCode,
        String[] MatchWords /* as returned by SplitWords on your search string */
      )
      /* does the character with the specified code have a name matching
        the given strings. */
      {
        String[] NameWords =
            CharNamesSplitByCode != null ?
                CharNamesSplitByCode.get(CharCode)
            :
                null;
        if (NameWords == null)
          {
            if (CharNamesSplitByCode == null)
              {
                CharNamesSplitByCode = new SparseArray<String[]>(Unicode.NrChars);
              } /*if*/
            NameWords = SplitWords(GetCharByCode(CharCode, true).Name);
            CharNamesSplitByCode.put(CharCode, NameWords);
          } /*if*/
        boolean Matching;
        for (int i = 0, j = 0;;)
          {
            if (i == MatchWords.length)
              {
                Matching = true;
                break;
              } /*if*/
            if (j == NameWords.length)
              {
                Matching = false;
                break;
              } /*if*/
            if (NameWords[j].contains(MatchWords[i]))
              {
                ++i;
              } /*if*/
            ++j;
          } /*for*/
        return
            Matching;
      } /*CharNameMatches*/

  } /*TableReader*/;
