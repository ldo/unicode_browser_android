package nz.gen.geek_central.unicode_browser;

import java.nio.ByteBuffer;
public class TableReader
  {
    public static class Unicode
      {
        private final byte[] Contents;
        private final ByteBuffer ContentsBuf;
        public final int NrChars, NrCharCategories, NrCharRuns;
        public final int CharCodesOffset, CharNamesOffset, CharCategoriesOffset, AltNamesOffset, LikeCharsOffset;
        public final int CategoryNamesOffset, CharRunsStart;
        public final int NameStringsStart;
        public final int AltNamesStart, LikeCharsStart;
        public final String Version;

        public Unicode
          (
            android.content.res.AssetFileDescriptor TableFile
          )
          {
            final int ContentsLength = (int)TableFile.getLength();
            System.out.printf("TableReader: Size of unicode table: %d\n", ContentsLength); /* debug */
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
            ContentsBuf = ByteBuffer.wrap(Contents);
            ContentsBuf.order(java.nio.ByteOrder.LITTLE_ENDIAN);
            System.out.printf("TableReader: char codes at %#x, categories at %#x, char runs at %#x, name strings at %#x, alt names at %#x, like chars at %#x, version at %#x\n", ContentsBuf.getInt(0), ContentsBuf.getInt(4), ContentsBuf.getInt(8), ContentsBuf.getInt(12), ContentsBuf.getInt(16), ContentsBuf.getInt(20), ContentsBuf.getInt(24)); /* debug */
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
            System.out.printf("TableReader: nr chars = %d, categories = %d, runs = %d, version = %s\n", NrChars, NrCharCategories, NrCharRuns, Version); /* debug */
          } /*Unicode*/

        public String GetString
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
                    throw new RuntimeException(String.format("undefined char %#X", CharCode));
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

        public int GetCharCode
          (
            int CharIndex
          )
          {
            return
                ContentsBuf.getInt(CharCodesOffset + CharIndex * 4);
          } /*GetCharCode*/

        public String GetCharName
          (
            int CharIndex
          )
          {
            return
                GetString(ContentsBuf.getInt(CharNamesOffset + CharIndex * 4));
          } /*GetCharName*/

        public int GetCharCategory
          (
            int CharIndex
          )
          {
            return
                ContentsBuf.getInt(CharCategoriesOffset + CharIndex * 4);
          } /*GetCharCategory*/

        public String[] GetCharOtherNames
          (
            int CharIndex
          )
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

        public String GetCategoryName
          (
            int CategoryCode
          )
          {
            return
                GetString(ContentsBuf.getInt(CategoryNamesOffset + CategoryCode * 4));
          } /*GetCategoryName*/

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
