package nz.gen.geek_central.unicode_browser;
/*
    Unicode Browser app--decoder for character table.

    The character table file is created by the util/get_codes
    script; see there for a description of the file format.

    Copyright 2013 Lawrence D'Oliveiro <ldo@geek-central.gen.nz>.

    This program is free software: you can redistribute it and/or
    modify it under the terms of the GNU General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

public class TableReader
  {
    public static class Unicode
      {
        private final byte[] Contents;
        private final java.nio.ByteBuffer ContentsBuf;
        public final int NrChars, NrCharCategories;
        private final int NrCharRuns;
        private final int CharCodesOffset, CharNamesOffset, CharCategoriesOffset, AltNamesOffset, LikeCharsOffset;
        private final int CategoryNamesOffset, CharRunsStart;
        private final int NameStringsStart;
        private final int AltNamesStart, LikeCharsStart;
        public final String Version;

        public Unicode
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
                    throw new RuntimeException(String.format("expected %d bytes, got %d", ContentsLength, BytesRead));
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
            NameStringsStart = ContentsBuf.getInt(12);
            CategoryNamesOffset = ContentsBuf.getInt(4) + 4;
            NrCharCategories = ContentsBuf.getInt(CategoryNamesOffset - 4);
            CharRunsStart = ContentsBuf.getInt(8) + 4;
            NrCharRuns = ContentsBuf.getInt(CharRunsStart - 4);
            AltNamesStart = ContentsBuf.getInt(16);
            LikeCharsStart = ContentsBuf.getInt(20);
            Version = GetString(ContentsBuf.getInt(24));
          } /*Unicode*/

        private String GetString
          (
            int Offset
          )
          /* extracts the string at the specified offset within the names table. */
          {
            final int Strlen = (int)ContentsBuf.get(NameStringsStart + Offset) & 255;
            final StringBuilder Result = new StringBuilder(Strlen);
            Result.setLength(Strlen);
            for (int i = 1; i <= Strlen; ++i)
              {
              /* Characters will be 7-bit ASCII only */
                Result.setCharAt(i - 1, (char)((short)ContentsBuf.get(NameStringsStart + Offset + i) & 255));
              } /*for*/
            return
                Result.toString();
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

        public int GetCharIndex
          (
            int CharCode
          )
          /* returns the index within the character table of the entry with the
            specified character code. */
          {
            int Result;
            for (int i = 0;;)
              {
                if (i == NrCharRuns)
                  {
                    throw new RuntimeException
                      (
                        String.format("TableReader.Unicode: undefined char %#X", CharCode)
                      );
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

      } /*Unicode*/;

    public static Unicode Load
      (
        android.content.Context ctx
      )
      {
        return
            new Unicode(ctx.getResources().openRawResourceFd(R.raw.unicode));
      } /*Load*/

  } /*TableReader*/;