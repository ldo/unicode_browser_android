package nz.gen.geek_central.unicode_browser;
/*
    Unicode Browser app--mainline.

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

import java.util.ArrayList;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.Button;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Spinner;
import nz.gen.geek_central.android_useful.UnicodeUseful;
import nz.gen.geek_central.android_useful.JSONPrefs;
import nz.gen.geek_central.android_useful.PopupMenu;
import static nz.gen.geek_central.unicode_browser.TableReader.CharInfo;
import static nz.gen.geek_central.unicode_browser.TableReader.Unicode;

public class Main extends ActionActivity
  {

  /* request codes, all arbitrarily assigned */
    static final int ChooseFontRequest = 1;

    interface RequestResponseAction /* response to an activity result */
      {
        public void Run
          (
            int ResultCode,
            Intent Data
          );
      } /*RequestResponseAction*/;

    java.util.Map<Integer, RequestResponseAction> ActivityResultActions;

    private final android.os.Handler BGRunner = new android.os.Handler();

    private abstract class BGTask implements Runnable
      {
        public BGTask()
          /* must be invoked by subclass! */
          {
            CurrentBG = this; /* effectively cancel any previous task */
            BGRunner.post(CurrentBG);
          } /*BGTask*/

        public abstract boolean Run();
          /* return true iff want to be invoked again */

        public void run()
          {
            if (CurrentBG == this)
              {
                if (Run())
                  {
                    BGRunner.post(this);
                  }
                else
                  {
                    CancelBG();
                  } /*if*/
              } /*if*/
          } /*run*/

      } /*BGTask*/;

    private void CancelBG()
      {
        Progress.setVisibility(View.INVISIBLE);
        CurrentBG = null;
      } /*CancelBG*/

    private BGTask CurrentBG = null;

    abstract class CommonItemAdapter<ItemType> extends android.widget.ArrayAdapter<ItemType>
      {
        final int ResID;
        final LayoutInflater TemplateInflater = Main.this.getLayoutInflater();

        public CommonItemAdapter
          (
            int ResID
          )
          {
            super(Main.this, ResID);
            this.ResID = ResID;
          } /*CommonItemAdapter*/

        public abstract void SetupView
          (
            ItemType TheItem,
            View TheView,
            boolean DropDown
          );
          /* puts relevant information from TheItem into TheView. */

        @Override
        public View getView
          (
            int Position,
            View ReuseView,
            android.view.ViewGroup Parent
          )
          {
            View TheView = ReuseView;
            if (TheView == null)
              {
                TheView = TemplateInflater.inflate(ResID, null);
              } /*if*/
            SetupView((ItemType)getItem(Position), TheView, false);
            return
                TheView;
          } /*getView*/

        @Override
        public View getDropDownView
          (
            int Position,
            View ReuseView,
            android.view.ViewGroup Parent
          )
          {
            View TheView = ReuseView;
            if (TheView == null)
              {
                TheView = TemplateInflater.inflate(ResID, null);
              } /*if*/
            SetupView((ItemType)getItem(Position), TheView, true);
            return
                TheView;
          } /*getDropDownView*/

      } /*CommonItemAdapter*/;

    private java.util.Map<String, Integer> CategoryCodes;

    private android.text.ClipboardManager Clipboard;

    private int[] Favourites = null;
    private Spinner ShowSelector, CategoryListView, CodeBlockListView;
    private AdapterView.OnItemSelectedListener
        ShowSelectorListener, CategoryListViewListener, CodeBlockListViewListener;
    private CategoryItemAdapter CategoryList;
    private CodeBlockItemAdapter CodeBlockList;
    private android.widget.EditText SearchEntry;
    private android.widget.ProgressBar Progress;
    private ShowModeEnum NowShowing;
    private int ShowCategory;
    private ListView CharListView, OtherNamesView, LikeCharsView;
    private CharItemAdapter MainCharList, LikeCharList;
    private NameItemAdapter OtherNamesList;
    private TextView NoCharsDisplay, LiteralDisplay, DetailsDisplay, DetailCategoryDisplay;
    private int CurChar = -1;
    private int DetailCategory = -1;
    private Button AddToText, DeleteFromText;
    private TextView CollectedTextView;
    private final ArrayList<Integer> CollectedText = new ArrayList<Integer>();

    static class CategoryItem
      {
        public final int Code;
        public final String Name;

        public CategoryItem
          (
            int Code,
            String Name
          )
          {
            this.Code = Code;
            this.Name = Name.intern();
          } /*CategoryItem*/

      } /*CategoryItem*/;

    static class CodeBlockItem
      {
        public final int Code, Low, High;

        public CodeBlockItem
          (
            int Code,
            int Low,
            int High
          )
          {
            this.Code = Code;
            this.Low = Low;
            this.High = High;
          } /*CodeBlockItem*/

      } /*CodeBlockItem*/;

    private void SetShowingCategory
      (
        int NewCategory,
        boolean CodeBlock
      )
      {
        ShowCategory = NewCategory;
        for (boolean DoingCodeBlock = false;;)
          {
            final Spinner TheListView = DoingCodeBlock ? CodeBlockListView : CategoryListView;
            final AdapterView.OnItemSelectedListener SaveListener =
                TheListView.getOnItemSelectedListener();
            TheListView.setOnItemSelectedListener(null); /* avoid reentrant calls */
            ((CategorySortItemAdapter<?>)TheListView.getAdapter())
                .SetSelection(TheListView, ShowCategory);
            TheListView.post /* so it runs after OnItemSelectedListener would be triggered */
              (
                new Runnable()
                  {
                    public void run()
                      {
                        TheListView.setOnItemSelectedListener(SaveListener);
                      } /*run*/
                  } /*Runnable*/
              );
            if (DoingCodeBlock)
                break;
            DoingCodeBlock = true;
          } /*for*/
        SetShowingMode(CodeBlock ? ShowModeEnum.CodeBlocks : ShowModeEnum.Categories);
        RebuildMainCharList();
      } /*SetShowingCategory*/

    abstract class CommonOnItemSelectedListener implements AdapterView.OnItemSelectedListener
      /* just so I can implement useless onNothingSelected method in one place */
      {

        public void onNothingSelected
          (
            AdapterView<?> Parent
          )
          {
          /* can't think of anything to do */
          } /*onNothingSelected*/

      } /*CommonOnItemSelectedListener*/;

    class CategorySelect extends CommonOnItemSelectedListener
      {

        public void onItemSelected
          (
            AdapterView<?> Parent,
            View ItemView,
            int Position,
            long ID
          )
          {
            SetShowingCategory(CategoryList.getItem(Position).Code, false);
          } /*onItemSelected*/

      } /*CategorySelect*/;

    abstract class CategorySortItemAdapter<CategoryItem> extends CommonItemAdapter<CategoryItem>
      /* allows selection of list items by category code. */
      {
        final android.util.SparseArray<Integer> CodeToIndex =
            new android.util.SparseArray<Integer>();

        public CategorySortItemAdapter
          (
            int ResID
          )
          {
            super(ResID);
          } /*CategorySortItemAdapter*/

        public void Add
          (
            int CategoryCode,
            CategoryItem Item
          )
          /* Use this instead of add to populate CodeToIndex table. */
          {
            CodeToIndex.put(CategoryCode, getCount());
            add(Item);
          } /*Add*/

        public void SetSelection
          (
            Spinner Parent,
            int CategoryCode
          )
          /* Use this instead of Parent.setSelection to select list item by category code. */
          {
            final Integer CodeIndex = CodeToIndex.get(CategoryCode);
            if (CodeIndex != null)
              {
                Parent.setSelection(CodeIndex);
              } /*if*/
          } /*SetSelection*/

      } /*CategorySortItemAdapter*/;

    class CategoryItemAdapter extends CategorySortItemAdapter<CategoryItem>
      {

        public CategoryItemAdapter()
          {
            super(R.layout.dropdown_item);
          } /*CategoryItemAdapter*/

        @Override
        public void SetupView
          (
            CategoryItem TheItem,
            View TheView,
            boolean DropDown
          )
          {
            ((TextView)TheView.findViewById(android.R.id.text1)).setText
              (
                TheItem.Name
              );
          } /*SetupView*/

      } /*CategoryItemAdapter*/;

    class CodeBlockSelect extends CommonOnItemSelectedListener
      {

        public void onItemSelected
          (
            AdapterView<?> Parent,
            View ItemView,
            int Position,
            long ID
          )
          {
            SetShowingCategory(CodeBlockList.getItem(Position).Code, true);
          } /*onItemSelected*/

      } /*CodeBlockSelect*/;

    class CodeBlockItemAdapter extends CategorySortItemAdapter<CodeBlockItem>
      {

        public CodeBlockItemAdapter()
          {
            super(R.layout.dropdown_item);
          } /*CodeBlockItemAdapter*/

        @Override
        public void SetupView
          (
            CodeBlockItem TheItem,
            View TheView,
            boolean DropDown
          )
          {
            ((TextView)TheView.findViewById(android.R.id.text1)).setText
              (
                String.format
                  (
                    UnicodeUseful.NoLocale,
                    getString(R.string.code_range),
                    UnicodeUseful.FormatCharCode(TheItem.Low),
                    UnicodeUseful.FormatCharCode(TheItem.High),
                    Unicode.GetCategoryName(TheItem.Code)
                  )
              );
          } /*SetupView*/

      } /*CodeBlockItemAdapter*/;

    enum ShowModeEnum
      {
        Categories(0, R.string.category_no_chars),
        CodeBlocks(1, R.string.category_no_chars),
        Searching(2, R.string.search_no_chars),
        Favourites(3, R.string.faves_no_chars),
        ;

        public final int Index;
        public final int EmptyStringID; /* ID of string to show when character list is empty */

        private ShowModeEnum
          (
            int Index,
            int EmptyStringID
          )
          {
            this.Index = Index;
            this.EmptyStringID = EmptyStringID;
          } /*ShowModeEnum*/

        public static ShowModeEnum Val
          (
            int Index
          )
          {
            final ShowModeEnum Result;
            for (int i = 0;;)
              {
                if (values()[i].Index == Index)
                  {
                    Result = values()[i];
                    break;
                  } /*if*/
                ++i;
              } /*for*/
            return
                Result;
          } /*Val*/

      } /*ShowModeEnum*/;

    class ShowingSelect extends CommonOnItemSelectedListener
      {

        public void onItemSelected
          (
            AdapterView<?> Parent,
            View ItemView,
            int Position,
            long ID
          )
          {
            SetShowingMode(((ShowModeItem)Parent.getAdapter().getItem(Position)).ModeEnum);
          } /*onItemSelected*/

      } /*ShowingSelect*/;

    class ShowModeItem
      /* just to hold toString method which cannot go in ShowModeEnum because
        latter is static */
      {
        public final ShowModeEnum ModeEnum;
        public final int PromptResID;
          /* string that appears in spinner when this item is selected */
        public final int ItemResID;
          /* string to appear in popup to select new item */

        public ShowModeItem
          (
            ShowModeEnum ModeEnum,
            int PromptResID,
            int ItemResID
          )
          {
            this.ModeEnum = ModeEnum;
            this.PromptResID = PromptResID;
            this.ItemResID = ItemResID;
          } /*ShowModeEnum*/

        @Override
        public String toString()
          {
            return
                getString(ItemResID);
          } /*toString*/

      } /*ShowModeItem*/;

    class ShowItemAdapter extends CommonItemAdapter<ShowModeItem>
      {

        public ShowItemAdapter()
          {
            super(R.layout.dropdown_item);
          } /*ShowItemAdapter*/

        @Override
        public void SetupView
          (
            ShowModeItem TheItem,
            View TheView,
            boolean DropDown
          )
          {
            ((TextView)TheView.findViewById(android.R.id.text1)).setText
              (
                DropDown ? TheItem.ItemResID : TheItem.PromptResID
              );
          } /*SetupView*/

      } /*ShowItemAdapter*/;

    void SetShowingMode
      (
        ShowModeEnum What
      )
      {
        ShowSelector.setOnItemSelectedListener(null); /* avoid reentrant calls */
        NowShowing = What;
        if (What != ShowModeEnum.Searching)
          {
            CancelBG();
          } /*if*/
        CategoryListView.setVisibility
          (
            What == ShowModeEnum.Categories ?
                View.VISIBLE
            :
                View.INVISIBLE
          );
        CodeBlockListView.setVisibility
          (
            What == ShowModeEnum.CodeBlocks ?
                View.VISIBLE
            :
                View.INVISIBLE
          );
        SearchEntry.setVisibility
          (
            What == ShowModeEnum.Searching ?
                View.VISIBLE
            :
                View.INVISIBLE
          );
        NoCharsDisplay.setText(NowShowing.EmptyStringID);
        ShowSelector.setSelection(NowShowing.Index);
        ShowSelector.post /* so it runs after OnItemSelectedListener would be triggered */
          (
            new Runnable()
              {
                public void run()
                  {
                    ShowSelector.setOnItemSelectedListener(ShowSelectorListener);
                  } /*run*/
              } /*Runnable*/
          );
        if (NowShowing == ShowModeEnum.Searching)
          {
            QueueRebuildMainCharList(SearchEntry.getText().toString());
          }
        else
          {
            RebuildMainCharList(null, false);
          } /*if*/
      } /*SetShowingMode*/

    class CharItemAdapter extends CommonItemAdapter<CharInfo>
      {

        CharItemAdapter
          (
            int ResID
          )
          {
            super(ResID);
          } /*CharItemAdapter*/

        @Override
        public void SetupView
          (
            CharInfo TheItem,
            View TheView,
            boolean DropDown
          )
          {
            ((TextView)TheView.findViewById(R.id.code)).setText(UnicodeUseful.FormatCharCode(TheItem.Code));
            final TextView LiteralView = (TextView)TheView.findViewById(R.id.literal);
            LiteralView.setText(UnicodeUseful.CharToString(TheItem.Code));
            LiteralView.setTypeface(CurFont, Typeface.NORMAL); /* if not already done */
            ((TextView)TheView.findViewById(R.id.name)).setText(TheItem.Name);
          } /*SetupView*/

      } /*CharItemAdapter*/;

    class NameItemAdapter extends CommonItemAdapter<String>
      {

        NameItemAdapter()
          {
            super(R.layout.name_list_item);
          } /*NameItemAdapter*/

        @Override
        public void SetupView
          (
            String Item,
            View TheView,
            boolean DropDown
          )
          {
            ((TextView)TheView.findViewById(R.id.text1)).setText(Item);
          } /*SetupView*/

      } /*NameItemAdapter*/;

    Typeface CurFont = null;
    String CurFontName = null;

    void SetFontName
      (
        String NewFontName /* null for system default */
      )
      {
        final Typeface NewFont =
            NewFontName != null ?
                Typeface.createFromFile(NewFontName)
            :
                Typeface.DEFAULT;
        if (NewFont != null)
          {
            CurFont = NewFont;
            CurFontName = NewFontName;
            for
              (
                int FieldID : new int[]
                  {
                    R.id.big_literal,
                    R.id.collected_text,
                  }
              )
              {
                ((TextView)findViewById(FieldID)).setTypeface(CurFont, Typeface.NORMAL);
              } /*for*/
          /* need to ensure literals in lists use new font as well: */
            MainCharList.notifyDataSetChanged();
            LikeCharList.notifyDataSetChanged();
          } /*if*/
      } /*SetFontName*/

    class CharSelect implements AdapterView.OnItemClickListener
      {
        public final boolean ScrollOnSelect;

        public CharSelect
          (
            boolean ScrollOnSelect
          )
          {
            this.ScrollOnSelect = ScrollOnSelect;
          } /*CharSelect*/

        public void onItemClick
          (
            AdapterView<?> Parent,
            View ItemView,
            int Position,
            long ID
          )
          {
            ShowCharDetails((CharInfo)Parent.getAdapter().getItem(Position), ScrollOnSelect);
          } /*onItemClick*/

      } /*CharSelect*/;

    private void RebuildMainCharList
      (
        String Matching, /* null if not doing matching */
        boolean ShrinkMatch
      )
      {
        CancelBG();
        NoCharsDisplay.setText(R.string.search_no_chars);
        MainCharList.setNotifyOnChange(false);
        if (Matching == null || !ShrinkMatch)
          {
            MainCharList.clear();
          } /*if*/
        if (Matching != null)
          {
            final String[] MatchWords = TableReader.SplitWords(Matching);
            if (ShrinkMatch)
              {
                for (int i = 0;;)
                  {
                    if (i == MainCharList.getCount())
                        break;
                    if (TableReader.CharNameMatches(MainCharList.getItem(i).Code, MatchWords))
                      {
                        ++i;
                      }
                    else
                      {
                        MainCharList.remove(MainCharList.getItem(i));
                      } /*if*/
                  } /*for*/
              }
            else
              {
              /* this can be slow, which is why caller should use BGCharListRebuilder instead
                for this case */
                for (int i = 0; i < Unicode.NrChars; ++i)
                  {
                    if (TableReader.CharNameMatches(MainCharList.getItem(i).Code, MatchWords))
                      {
                        MainCharList.add(TableReader.GetCharByIndex(i));
                      } /*if*/
                  } /*for*/
              } /*if*/
          }
        else if (NowShowing == ShowModeEnum.Favourites)
          {
            if (Favourites != null)
              {
                for (int i = 0; i < Favourites.length; ++i)
                  {
                    final int CharIndex = Unicode.GetCharIndex(Favourites[i], false);
                    if (CharIndex >= 0)
                      {
                        MainCharList.add(TableReader.GetCharByIndex(CharIndex));
                      } /*if*/
                  } /*for*/
              } /*if*/
          }
        else
          {
            for (int i = 0; i < Unicode.NrChars; ++i)
              {
                if (Unicode.GetCharCategory(i) == ShowCategory)
                  {
                    MainCharList.add(TableReader.GetCharByIndex(i));
                  } /*if*/
              } /*for*/
          } /*if*/
        MainCharList.notifyDataSetChanged();
        CharListView.setSelection(0); /* only works after notifyDataSetChanged! */
      } /*RebuildMainCharList*/

    private final int MaxPerBGRun = 100;

    private class BGCharListRebuilder extends BGTask
      {
      /* for doing time-consuming character matches in background to keep interface responsive */
        private final String[] Matching;
        private int CurIndex;
        private final long StartTime;
        private boolean FirstCall;

        public BGCharListRebuilder
          (
            String Matching
          )
          {
            super();
            this.Matching = TableReader.SplitWords(Matching);
            CurIndex = 0;
            MainCharList.clear();
            StartTime = System.currentTimeMillis();
            FirstCall = true;
          } /*BGCharListRebuilder*/

        @Override
        public boolean Run()
          {
            if (System.currentTimeMillis() - StartTime > 500)
              {
                Progress.setVisibility(View.VISIBLE);
              } /*if*/
            final int DoThisRun = Math.min(Unicode.NrChars - CurIndex, MaxPerBGRun);
            MainCharList.setNotifyOnChange(false);
            for
              (
                int i = 0;
                i < MaxPerBGRun && CurIndex < Unicode.NrChars;
                ++i, ++CurIndex
              )
              {
                if (TableReader.CharNameMatches(Unicode.GetCharCode(CurIndex), Matching))
                  {
                    MainCharList.add(TableReader.GetCharByIndex(CurIndex));
                  } /*if*/
              } /*for*/
            MainCharList.notifyDataSetChanged();
            if (FirstCall)
              {
                CharListView.setSelection(0); /* only works after notifyDataSetChanged! */
                FirstCall = false;
              } /*if*/
            final boolean KeepGoing = CurIndex < Unicode.NrChars;
            if (!KeepGoing)
              {
                NoCharsDisplay.setText(R.string.search_no_chars);
              } /*if*/
            return
                KeepGoing;
          } /*Run*/

      } /*BGCharListRebuilder*/;

    private void QueueRebuildMainCharList
      (
        String Matching /* won't be null */
      )
      {
        NoCharsDisplay.setText(R.string.searching);
        new BGCharListRebuilder(Matching);
      } /*QueueRebuildMainCharList*/

    private void RebuildMainCharList()
      {
        RebuildMainCharList(null, false);
      } /*RebuildMainCharList*/

    private class BGCharSelector extends BGTask
      {
        private final int CharCode;
        private int CurIndex;

        public BGCharSelector
          (
            int CharCode
          )
          {
            super();
            this.CharCode = CharCode;
            CurIndex = 0;
          } /*BGCharSelector*/

        @Override
        public boolean Run()
          {
            boolean Found = false;
            for (int i = 0;;)
              {
                if (i == MaxPerBGRun || CurIndex == MainCharList.getCount())
                    break;
                if (MainCharList.getItem(CurIndex).Code == CharCode)
                  {
                    final int CharIndex = CurIndex;
                    CharListView.post
                      (
                        new Runnable()
                          {
                            public void run()
                              {
                                CharListView.setSelection(CharIndex);
                              } /*run*/
                          } /*Runnable*/
                      );
                    Found = true;
                    break;
                  } /*if*/
                ++i;
                ++CurIndex;
              } /*for*/
            return
                !Found && CurIndex < MainCharList.getCount();
          } /*Run*/

      } /*BGCharSelector*/;

    private void ShowCharDetails
      (
        CharInfo TheChar,
        boolean ScrollToIt
      )
      {
        boolean ScrollToChar = false;
        if (TheChar != null)
          {
            CurChar = TheChar.Code;
            LiteralDisplay.setText(UnicodeUseful.CharToString(TheChar.Code));
            DetailsDisplay.setText
              (
                String.format
                  (
                    UnicodeUseful.NoLocale,
                    "%s %s",
                    UnicodeUseful.FormatCharCode(TheChar.Code),
                    TheChar.Name
                  )
              );
            DetailCategoryDisplay.setText(Unicode.GetCategoryName(TheChar.Category));
            DetailCategory = TheChar.Category;
            OtherNamesList.setNotifyOnChange(false);
            OtherNamesList.clear();
            for (String Name : TheChar.OtherNames)
              {
                OtherNamesList.add(Name);
              } /*for*/
            OtherNamesList.notifyDataSetChanged();
            OtherNamesView.setSelection(0); /* only works after notifyDataSetChanged! */
            LikeCharList.setNotifyOnChange(false);
            LikeCharList.clear();
            for (int Code : TheChar.LikeChars)
              {
                LikeCharList.add(TableReader.GetCharByCode(Code, true));
              } /*for*/
            LikeCharList.notifyDataSetChanged();
            LikeCharsView.setSelection(0); /* only works after notifyDataSetChanged! */
            if (ScrollToIt && NowShowing == ShowModeEnum.Categories)
              {
                ScrollToChar = true;
              } /*if*/
          }
        else
          {
            CurChar = -1;
            LiteralDisplay.setText("");
            DetailsDisplay.setText("");
            DetailCategoryDisplay.setText("");
            DetailCategory = -1;
            OtherNamesList.clear();
            LikeCharList.clear();
          } /*if*/
        if (ScrollToChar && CurrentBG == null)
          {
            new BGCharSelector(CurChar);
          } /*if*/
      } /*ShowCharDetails*/

    private int[] GetCollectedText()
      {
        return
            UnicodeUseful.ToArray(CollectedText);
      } /*GetCollectedText*/

    private String CollectedTextToString()
      {
        return
            UnicodeUseful.CharsToString(GetCollectedText());
      } /*CollectedTextToString*/

    private void SetCollectedText
      (
        int[] CharCodes
      )
      {
        CollectedText.clear();
        for (int i = 0; i < CharCodes.length; ++i)
          {
            CollectedText.add(CharCodes[i]);
          } /*for*/
        CollectedTextView.setText(CollectedTextToString());
      } /*SetCollectedText*/

    class DetailClickListener implements View.OnClickListener
      {

        public void onClick
          (
            View TheView
          )
          {
            if (CurChar >= 0)
              {
                final int TheChar = CurChar;
                final PopupMenu Popup = new PopupMenu(Main.this, LiteralDisplay);
                boolean InFaves = false;
                if (Favourites != null)
                  {
                    for (int i = 0;;)
                      {
                        if (i == Favourites.length)
                          {
                            InFaves = false;
                            break;
                          } /*if*/
                        if (Favourites[i] == CurChar)
                          {
                            InFaves = true;
                            break;
                          } /*if*/
                        ++i;
                      } /*for*/
                  } /*if*/
                if (InFaves)
                  {
                    Popup.AddItem
                      (
                        R.string.remove_from_faves,
                        new Runnable()
                          {
                            public void run()
                              {
                                final int[] NewFavourites =
                                    Favourites != null && Favourites.length > 1 ?
                                        new int[Favourites.length - 1]
                                    :
                                        null;
                                boolean Removed = NewFavourites == null;
                                if (NewFavourites != null)
                                  {
                                    for (int i = 0, j = 0;;)
                                      {
                                        if (i == Favourites.length)
                                            break;
                                        if (Favourites[i] != TheChar)
                                          {
                                            if (j == NewFavourites.length)
                                                break;
                                            NewFavourites[j++] = Favourites[i];
                                          }
                                        else
                                          {
                                            Removed = true;
                                          } /*if*/
                                        ++i;
                                      } /*for*/
                                  } /*if*/
                                if (Removed)
                                  {
                                    Favourites = NewFavourites;
                                    if (NowShowing == ShowModeEnum.Favourites)
                                      {
                                        RebuildMainCharList();
                                      } /*if*/
                                  } /*if*/
                              } /*run*/
                          } /*Runnable*/
                      );
                  }
                else
                  {
                    Popup.AddItem
                      (
                        R.string.add_to_faves,
                        new Runnable()
                          {
                            public void run()
                              {
                                final java.util.TreeSet<Integer> NewFaves =
                                  /* keep them sorted by character code */
                                    new java.util.TreeSet<Integer>
                                      (
                                        new java.util.Comparator<Integer>()
                                          {
                                            @Override
                                            public int compare
                                              (
                                                Integer A,
                                                Integer B
                                              )
                                              {
                                                return
                                                    A.compareTo(B);
                                              } /*compare*/
                                          } /*Comparator*/
                                      );
                                if (Favourites != null)
                                  {
                                    for (int i = 0; i < Favourites.length; ++i)
                                      {
                                        NewFaves.add(Favourites[i]);
                                      } /*for*/
                                  } /*if*/
                                NewFaves.add(TheChar);
                                if (NewFaves.size() > (Favourites != null ? Favourites.length : 0))
                                  {
                                    Favourites = new int[NewFaves.size()];
                                    int i = 0;
                                    for (Integer CharCode : NewFaves)
                                      {
                                        Favourites[i++] = CharCode;
                                      } /*for*/
                                    if (NowShowing == ShowModeEnum.Favourites)
                                      {
                                        RebuildMainCharList();
                                      } /*if*/
                                  } /*if*/
                              } /*run*/
                          } /*Runnable*/
                      );
                  } /*if*/
                Popup.Show();
              }
            else
              {
                android.widget.Toast.makeText
                  (
                    /*context =*/ Main.this,
                    /*text =*/ getString(R.string.no_char_action),
                    /*duration =*/ android.widget.Toast.LENGTH_SHORT
                  ).show();
              } /*if*/
          } /*onClick*/

      } /*DetailClickListener*/;

    private static final String StateFileName = "state.txt";

    private void SaveState()
      /* saves state (currently just favourites) to persistent storage. */
      {
        new JSONPrefs(this, StateFileName)
            .PutIntArray("favourites", Favourites)
            .Save(); /* ignore errors! */
      } /*SaveState*/

    private void RestoreState()
      /* restores state (currently just favourites) from persistent storage. */
      {
        Favourites =
            new JSONPrefs(this, StateFileName)
            .Load()
            .GetIntArray("favourites");
      } /*RestoreState*/

    class TextClickListener implements View.OnClickListener
      {

        public void onClick
          (
            View TheView
          )
          {
            final PopupMenu Popup = new PopupMenu(Main.this, CollectedTextView);
            if (CollectedText.size() != 0)
              {
                Popup.AddItem
                  (
                    R.string.copy_text,
                    new Runnable()
                      {
                        public void run()
                          {
                            Clipboard.setText(CollectedTextToString());
                          } /*run*/
                      } /*Runnable*/
                  );
              } /*if*/
            if (Clipboard.hasText())
              {
                Popup.AddItem
                  (
                    R.string.paste_text,
                    new Runnable()
                      {
                        public void run()
                          {
                            if (Clipboard.hasText())
                              {
                                SetCollectedText
                                  (
                                    UnicodeUseful.CharSequenceToChars(Clipboard.getText())
                                  );
                                CharInfo TheChar = null;
                                if (CollectedText.size() > 0)
                                  {
                                    TheChar = TableReader.GetCharByCode(CollectedText.get(0), false);
                                  } /*if*/
                                ShowCharDetails(TheChar, true);
                              } /*if*/
                          } /*run*/
                      } /*Runnable*/
                  );
              } /*if*/
            if (Popup.NrItems() != 0)
              {
                Popup.Show();
              }
            else
              {
                android.widget.Toast.makeText
                  (
                    /*context =*/ Main.this,
                    /*text =*/ getString(R.string.no_text_action),
                    /*duration =*/ android.widget.Toast.LENGTH_SHORT
                  ).show();
              } /*if*/
          } /*onClick*/

      } /*TextClickListener*/;

    @Override
    public boolean dispatchKeyEvent
      (
        android.view.KeyEvent TheEvent
      )
      {
        boolean Handled = false;
        if
          (
                TheEvent.getAction() == android.view.KeyEvent.ACTION_UP
            &&
                TheEvent.getKeyCode() == android.view.KeyEvent.KEYCODE_SEARCH
          )
          {
            if (NowShowing != ShowModeEnum.Searching)
              {
                SetShowingMode(ShowModeEnum.Searching);
              } /*if*/
            Handled = true;
          } /*if*/
        if (!Handled)
          {
            Handled = super.dispatchKeyEvent(TheEvent);
          } /*if*/
        return
            Handled;
      } /*dispatchKeyEvent*/

    void BuildActivityResultActions()
      {
        ActivityResultActions = new java.util.HashMap<Integer, RequestResponseAction>();
        ActivityResultActions.put
          (
            ChooseFontRequest,
            new RequestResponseAction()
              {
                public void Run
                  (
                    int ResultCode,
                    Intent TheIntent
                  )
                  {
                  /* Unfortunately I can't send this Intent directly from the Picker
                    without using some odd launch-mode settings to avoid another instance
                    of Main being created. Which is why I do it here. */
                    TheIntent.setClass(Main.this, Main.class)
                        .setAction(Intent.ACTION_VIEW)
                        .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(TheIntent);
                  } /*Run*/
              } /*RequestResponseAction*/
          );
      } /*BuildActivityResultActions*/

    @Override
    public void onCreate
      (
        Bundle ToRestore
      )
      {
        TableReader.Load(this);
        Clipboard = (android.text.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        CategoryCodes = new java.util.HashMap<String, Integer>(Unicode.NrCharCategories);
        for (int i = 0; i < Unicode.NrCharCategories; ++i)
          {
            CategoryCodes.put(Unicode.GetCategoryName(i), i);
          } /*for*/
        super.onCreate(ToRestore);
        BuildActivityResultActions();
        setContentView(R.layout.main);
          {
            ShowSelector = (Spinner)findViewById(R.id.show_prompt);
            final ShowItemAdapter ToShow = new ShowItemAdapter();
            ToShow.add
              (
                new ShowModeItem
                  (
                    /*ModeEnum =*/ ShowModeEnum.Categories,
                    /*PromptResID =*/ R.string.category_prompt,
                    /*ItemResID =*/ R.string.categories_item
                  )
              );
            ToShow.add
              (
                new ShowModeItem
                  (
                    /*ModeEnum =*/ ShowModeEnum.CodeBlocks,
                    /*PromptResID =*/ R.string.code_block_prompt,
                    /*ItemResID =*/ R.string.code_blocks_item
                  )
              );
            ToShow.add
              (
                new ShowModeItem
                  (
                    /*ModeEnum =*/ ShowModeEnum.Searching,
                    /*PromptResID =*/ R.string.search_prompt,
                    /*ItemResID =*/ R.string.search_item
                  )
              );
            ToShow.add
              (
                new ShowModeItem
                  (
                    /*ModeEnum =*/ ShowModeEnum.Favourites,
                    /*PromptResID =*/ R.string.faves_prompt,
                    /*ItemResID =*/ R.string.faves_item
                  )
              );
            ShowSelector.setAdapter(ToShow);
            ShowSelectorListener = new ShowingSelect();
            ShowSelector.setOnItemSelectedListener(ShowSelectorListener);
          }
        if (ToRestore == null)
          {
            ShowCategory = CategoryCodes.get("Basic Latin"); /* default */
          } /*if*/
          {
            CategoryListView = (Spinner)findViewById(R.id.category_selector);
            CategoryList = new CategoryItemAdapter();
            for (int CategoryIndex = 0; CategoryIndex < Unicode.NrCharCategories; ++CategoryIndex)
              {
                final int CategoryCode = Unicode.GetCategoryNameOrdered(CategoryIndex);
                CategoryList.Add
                  (
                    CategoryCode,
                    new CategoryItem(CategoryCode, Unicode.GetCategoryName(CategoryCode))
                  );
              } /*for*/
            CategoryListView.setAdapter(CategoryList);
            if (ToRestore == null)
              {
                CategoryList.SetSelection(CategoryListView, ShowCategory);
              } /*if*/
            CategoryListViewListener = new CategorySelect();
            CategoryListView.setOnItemSelectedListener(CategoryListViewListener);
          }
          {
            CodeBlockListView = (Spinner)findViewById(R.id.code_block_selector);
            CodeBlockList = new CodeBlockItemAdapter();
            for (int CategoryIndex = 0; CategoryIndex < Unicode.NrCharCategories; ++CategoryIndex)
              {
                final int CategoryCode = Unicode.GetCategoryCodeOrdered(CategoryIndex);
                CodeBlockList.Add
                  (
                    CategoryCode,
                    new CodeBlockItem
                      (
                        /*Code =*/ CategoryCode,
                        /*Low =*/ Unicode.GetCategoryLowBoundByCode(CategoryIndex),
                        /*High =*/ Unicode.GetCategoryHighBoundByCode(CategoryIndex)
                      )
                  );
              } /*for*/
            CodeBlockListView.setAdapter(CodeBlockList);
            if (ToRestore == null)
              {
                CodeBlockList.SetSelection(CodeBlockListView, ShowCategory);
              } /*if*/
            CodeBlockListViewListener = new CodeBlockSelect();
            CodeBlockListView.setOnItemSelectedListener(CodeBlockListViewListener);
          }
        SearchEntry = (android.widget.EditText)findViewById(R.id.search_entry);
        SearchEntry.addTextChangedListener
          (
            new android.text.TextWatcher()
              {
                private String Before = SearchEntry.getText().toString();

                public void afterTextChanged
                  (
                    android.text.Editable TheField
                  )
                  {
                  /* have to do the work here, because CharSequence arg to beforeTextChanged and
                    onTextChanged may not represent entire field contents */
                    final String After = TheField.toString();
                    if
                      (
                            CurrentBG == null
                        &&
                            After.contains(Before)
                        &&
                            MainCharList.getCount() <= MaxPerBGRun
                      )
                      {
                        RebuildMainCharList(After, true);
                      }
                    else
                      {
                        QueueRebuildMainCharList(After);
                      } /*if*/
                    Before = After;
                  } /*afterTextChanged*/

                public void beforeTextChanged
                  (
                    CharSequence FieldContents,
                    int Start,
                    int BeforeCount,
                    int AfterCount
                  )
                  {
                  /* all done in afterTextChanged */
                  } /*beforeTextChanged*/

                public void onTextChanged
                  (
                    CharSequence NewContents,
                    int Start,
                    int BeforeCount,
                    int AfterCount
                  )
                  {
                  /* all done in afterTextChanged */
                  } /*TextChanged*/
              } /*TextWatcher*/
          );
        Progress = (android.widget.ProgressBar)findViewById(R.id.progress);
        CharListView = (ListView)findViewById(R.id.main_list);
        NoCharsDisplay = (TextView)findViewById(R.id.main_list_empty);
        CharListView.setEmptyView(NoCharsDisplay);
        MainCharList = new CharItemAdapter(R.layout.char_list_item);
        CharListView.setAdapter(MainCharList);
        CharListView.setOnItemClickListener(new CharSelect(false));
        OtherNamesList = new NameItemAdapter();
        OtherNamesView = (ListView)findViewById(R.id.names_list);
        OtherNamesView.setAdapter(OtherNamesList);
        LikeCharsView = (ListView)findViewById(R.id.like_list);
        LikeCharList = new CharItemAdapter(R.layout.also_char_list_item);
        LikeCharsView.setAdapter(LikeCharList);
        LikeCharsView.setOnItemClickListener(new CharSelect(true));
        LiteralDisplay = (TextView)findViewById(R.id.big_literal);
        DetailsDisplay = (TextView)findViewById(R.id.details);
          {
            final DetailClickListener OnDetailClick = new DetailClickListener();
            LiteralDisplay.setOnClickListener(OnDetailClick);
            DetailsDisplay.setOnClickListener(OnDetailClick);
          }
        DetailCategoryDisplay = (TextView)findViewById(R.id.category);
        DetailCategoryDisplay.setOnClickListener
          (
            new View.OnClickListener()
              {
                public void onClick
                  (
                    View TheView
                  )
                  {
                    if (DetailCategory >= 0)
                      {
                        SetShowingCategory(DetailCategory, false);
                        if (CurrentBG == null)
                          {
                            final int TheChar = CurChar;
                            CharListView.post /* ensure it runs after ShowingSelect.onItemSelected */
                              (
                                new Runnable()
                                  {
                                    public void run()
                                      {
                                        new BGCharSelector(TheChar);
                                      } /*run*/
                                  } /*Runnable*/
                              );
                          } /*if*/
                      } /*if*/
                  } /*onClick*/
              }
          );
        CollectedTextView = (TextView)findViewById(R.id.collected_text);
        CollectedTextView.setOnClickListener(new TextClickListener());
        AddToText = (Button)findViewById(R.id.add_char);
        AddToText.setOnClickListener
          (
            new View.OnClickListener()
              {
                public void onClick
                  (
                    View TheView
                  )
                  {
                    if (CurChar >= 0)
                      {
                        CollectedText.add(CurChar);
                        CollectedTextView.setText(CollectedTextToString());
                      }
                    else
                      {
                        android.widget.Toast.makeText
                          (
                            /*context =*/ Main.this,
                            /*text =*/ getString(R.string.no_char_action),
                            /*duration =*/ android.widget.Toast.LENGTH_SHORT
                          ).show();
                      } /*if*/
                  } /*onClick*/
              }
          );
        DeleteFromText = (Button)findViewById(R.id.delete_char);
        DeleteFromText.setOnClickListener
          (
            new View.OnClickListener()
              {
                public void onClick
                  (
                    View TheView
                  )
                  {
                    if (CollectedText.size() > 0)
                      {
                        CollectedText.remove(CollectedText.size() - 1);
                        CollectedTextView.setText(CollectedTextToString());
                      } /*if*/
                  } /*onClick*/
              }
          );
        RestoreState();
        if (ToRestore == null)
          {
            SetShowingMode(ShowModeEnum.Categories);
          } /*if*/
      } /*onCreate*/

    @Override
    public void onPostCreate
      (
        Bundle ToRestore
      )
      {
        super.onPostCreate(ToRestore);
      /* for some reason setting of window title has to be done here, won’t work in onCreate */
        getWindow().setTitle(String.format(getString(R.string.window_title), Unicode.Version));
      } /*onPostCreate*/

    @Override
    protected void OnCreateOptionsMenu()
      {
        AddOptionsMenuItem
          (
            /*StringID =*/ R.string.choose_font,
            /*IconID =*/ R.drawable.ic_font,
            /*ActionBarUsage =*/ android.view.MenuItem.SHOW_AS_ACTION_IF_ROOM,
            /*Action =*/
                new Runnable()
                  {
                    public void run()
                      {
                        final Intent LaunchPicker = new Intent(Intent.ACTION_PICK)
                            .setClass(Main.this, Picker.class)
                            .putExtra(Picker.ExtensionID, ".ttf")
                            .putExtra
                              (
                                Picker.LookInID,
                                new String[]
                                    {
                                        "/system/fonts",
                                        "Fonts",
                                        "Download",
                                    }
                              );
                          {
                            final Bundle ChooseSystemFont = new Bundle();
                            ChooseSystemFont.putString(Picker.SpecialItemKeyID, "system"); /* actual value ignored */
                            ChooseSystemFont.putString(Picker.SpecialItemValueID, getString(R.string.system_font));
                            LaunchPicker
                                .putExtra
                                  (
                                    Picker.SpecialItemsID,
                                    new android.os.Parcelable[]
                                      {
                                        ChooseSystemFont,
                                      }
                                  );
                          }
                        if (CurFontName == null)
                          {
                            LaunchPicker.putExtra(Picker.PreviousSpecialItemID, "system");
                          }
                        else
                          {
                            LaunchPicker.putExtra(Picker.PreviousItemID, CurFontName);
                          } /*if*/
                        startActivityForResult(LaunchPicker, ChooseFontRequest);
                      } /*run*/
                  } /*Runnable*/
          );
      } /*onCreateOptionsMenu*/

    @Override
    protected void onNewIntent
      (
        Intent TheIntent
      )
      {
        String Action = TheIntent.getAction();
        if (Action != null)
          {
            Action = Action.intern();
          } /*if*/
        if (Action == Intent.ACTION_VIEW)
          {
            final String SpecialFontName = TheIntent.getStringExtra(Picker.SpecialItemSelectedID);
            if (SpecialFontName != null)
              {
              /* only one possible special entry, so don't bother actually checking returned key */
                SetFontName(null);
              }
            else
              {
                SetFontName(TheIntent.getData().getPath());
              } /*if*/
          } /*if*/
      } /*onnewIntent*/

    @Override
    public void onActivityResult
      (
        int RequestCode,
        int ResultCode,
        Intent Data
      )
      {
        if (ResultCode != android.app.Activity.RESULT_CANCELED)
          {
            final RequestResponseAction Action = ActivityResultActions.get(RequestCode);
            if (Action != null)
              {
                Action.Run(ResultCode, Data);
              } /*if*/
          } /*if*/
      } /*onActivityResult*/

    @Override
    public void onSaveInstanceState
      (
        Bundle ToSave
      )
      {
        super.onSaveInstanceState(ToSave);
        ToSave.putInt("display_mode", NowShowing.Index);
        ToSave.putInt("category", ShowCategory);
        ToSave.putInt("char", CurChar);
        ToSave.putIntArray("input_text", GetCollectedText());
        if (CurFontName != null)
          {
            ToSave.putString("font_pathname", CurFontName);
          } /*if*/
      } /*onSaveInstanceState*/

    @Override
    public void onRestoreInstanceState
      (
        Bundle ToRestore
      )
      {
        super.onRestoreInstanceState(ToRestore);
        SetFontName(ToRestore.getString("font_pathname"));
        SetCollectedText(ToRestore.getIntArray("input_text"));
          {
            final int CharIndex = Unicode.GetCharIndex(ToRestore.getInt("char"), false);
            ShowCharDetails(CharIndex >= 0 ? TableReader.GetCharByIndex(CharIndex) : null, true);
          }
        SetShowingCategory(ToRestore.getInt("category"), false); /* doesn't matter how it calls SetShowingMode ... */
        SetShowingMode(ShowModeEnum.Val(ToRestore.getInt("display_mode"))); /* ... because I do */
      } /*onRestoreInstanceState*/

    @Override
    public void onPause()
      {
        SaveState(); /* good place to do this */
        super.onPause();
      } /*onPause*/

  } /*Main*/;
