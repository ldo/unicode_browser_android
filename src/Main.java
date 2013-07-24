package nz.gen.geek_central.unicode_browser;
/*
    Unicode Browser app--mainline.

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
import android.os.Bundle;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Spinner;
import nz.gen.geek_central.android_useful.UnicodeUseful;
import nz.gen.geek_central.android_useful.JSONPrefs;
import nz.gen.geek_central.android_useful.PopupMenu;
import static nz.gen.geek_central.unicode_browser.TableReader.CharInfo;
import static nz.gen.geek_central.unicode_browser.TableReader.Unicode;
public class Main extends android.app.Activity
  {
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

    private java.util.Map<String, Integer> CategoryCodes;

    private android.text.ClipboardManager Clipboard;

    private int[] Favourites = null;
    private Spinner ShowSelector, CategoryListView;
    private CategoryItemAdapter CategoryList;
    private android.widget.EditText SearchEntry;
    private android.widget.ProgressBar Progress;
    private ShowModeEnum NowShowing;
    private int ShowCategory;
    private ListView CharListView, OtherNamesView, LikeCharsView;
    private CharItemAdapter MainCharList, LikeCharList;
    private NameItemAdapter OtherNamesList;
    private TextView LiteralDisplay, DetailsDisplay, DetailCategoryDisplay;
    private Button DetailCategoryButton;
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

        public String toString()
          {
            return
                this.Name;
          } /*toString*/

      } /*CategoryItem*/;

    private void SetShowDetailCategory()
      {
        final boolean ShowingChar = DetailCategory >= 0;
        DetailCategoryButton.setVisibility
          (
                    !ShowingChar
                ||
                    NowShowing == ShowModeEnum.Categories && ShowCategory == DetailCategory
            ?
                View.INVISIBLE
            :
                View.VISIBLE
          );
      } /*SetShowDetailCategory*/

    private void SetShowingCategory
      (
        int NewCategory
      )
      {
        ShowCategory = NewCategory;
        CategoryListView.setSelection(ShowCategory);
        SetShowingMode(ShowModeEnum.Categories);
        RebuildMainCharList();
      } /*SetShowingCategory*/

    class ShowingSelect implements AdapterView.OnItemSelectedListener
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

        public void onNothingSelected
          (
            AdapterView<?> Parent
          )
          {
          /* can't think of anything to do */
          } /*onNothingSelected*/

      } /*ShowingSelect*/;

    class CategorySelect implements AdapterView.OnItemSelectedListener
      {

        public void onItemSelected
          (
            AdapterView<?> Parent,
            View ItemView,
            int Position,
            long ID
          )
          {
            SetShowingCategory(CategoryList.getItem(Position).Code);
          } /*onItemSelected*/

        public void onNothingSelected
          (
            AdapterView<?> Parent
          )
          {
          /* can't think of anything to do */
          } /*onNothingSelected*/

      } /*CategorySelect*/;

    class CategoryItemAdapter extends ArrayAdapter<CategoryItem>
      {
        static final int ResID = android.R.layout.simple_dropdown_item_1line;
        final LayoutInflater TemplateInflater = Main.this.getLayoutInflater();

        public CategoryItemAdapter()
          {
            super(Main.this, ResID);
          } /*CategoryItemAdapter*/

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
            final CategoryItem ThisItem = getItem(Position);
            ((TextView)TheView.findViewById(android.R.id.text1)).setText
              (
                ThisItem.Name
              );
            return
                TheView;
          } /*getView*/

      } /*CategoryItemAdapter*/;

    enum ShowModeEnum
      {
        Categories(0),
        Searching(1),
        Favourites(2),
        ;

        public final int Index;

        private ShowModeEnum
          (
            int Index
          )
          {
            this.Index = Index;
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

    class ShowItemAdapter extends ArrayAdapter<ShowModeItem>
      {
        static final int ResID = android.R.layout.simple_dropdown_item_1line;
        final LayoutInflater TemplateInflater = Main.this.getLayoutInflater();

        public ShowItemAdapter()
          {
            super(Main.this, ResID);
          } /*ShowItemAdapter*/

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
            final ShowModeItem ThisItem = getItem(Position);
            ((TextView)TheView.findViewById(android.R.id.text1)).setText
              (
                ThisItem.PromptResID
              );
            return
                TheView;
          } /*getView*/

      } /*ShowItemAdapter*/;

    void SetShowingMode
      (
        ShowModeEnum What
      )
      {
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
        SearchEntry.setVisibility
          (
            What == ShowModeEnum.Searching ?
                View.VISIBLE
            :
                View.INVISIBLE
          );
        ShowSelector.setSelection(NowShowing.Index);
        SetShowDetailCategory();
        if (NowShowing == ShowModeEnum.Searching)
          {
            QueueRebuildMainCharList(SearchEntry.getText().toString());
          }
        else
          {
            RebuildMainCharList(null, false);
          } /*if*/
      } /*SetShowingMode*/

    class CharItemAdapter extends ArrayAdapter<CharInfo>
      {
        final int ResID;
        final LayoutInflater TemplateInflater = Main.this.getLayoutInflater();

        CharItemAdapter
          (
            int ResID
          )
          {
            super(Main.this, ResID);
            this.ResID = ResID;
          } /*CharItemAdapter*/

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
            final CharInfo ThisItem = getItem(Position);
            ((TextView)TheView.findViewById(R.id.code)).setText(UnicodeUseful.FormatCharCode(ThisItem.Code));
            ((TextView)TheView.findViewById(R.id.literal)).setText(UnicodeUseful.CharToString(ThisItem.Code));
            ((TextView)TheView.findViewById(R.id.name)).setText(ThisItem.Name);
            return
                TheView;
          } /*getView*/

      } /*CharItemAdapter*/;

    class NameItemAdapter extends ArrayAdapter<String>
      {
        static final int ResID = R.layout.name_list_item;
        final LayoutInflater TemplateInflater = Main.this.getLayoutInflater();

        NameItemAdapter()
          {
            super(Main.this, ResID);
          } /*NameItemAdapter*/

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
            ((TextView)TheView.findViewById(R.id.text1)).setText
              (
                getItem(Position)
              );
            return
                TheView;
          } /*getView*/

      } /*NameItemAdapter*/;

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
            return
                CurIndex < Unicode.NrChars;
          } /*Run*/

      } /*BGCharListRebuilder*/;

    private void QueueRebuildMainCharList
      (
        String Matching /* won't be null */
      )
      {
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
        if (TheChar != null)
          {
            CurChar = TheChar.Code;
            LiteralDisplay.setText(UnicodeUseful.CharToString(TheChar.Code));
            DetailsDisplay.setText
              (
                String.format
                  (
                    java.util.Locale.US,
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
            if (ScrollToIt && NowShowing == ShowModeEnum.Categories && CurrentBG == null)
              {
                new BGCharSelector(CurChar);
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
        SetShowDetailCategory();
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
                final PopupMenu Popup = new PopupMenu(Main.this);
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
      /* shouldn't be large, so I don't bother compressing it */

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
            final PopupMenu Popup = new PopupMenu(Main.this);
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
        setContentView(R.layout.main);
          {
            ShowSelector = (Spinner)findViewById(R.id.show_prompt);
            final ShowItemAdapter ToShow = new ShowItemAdapter();
            ToShow.add(new ShowModeItem(ShowModeEnum.Categories, R.string.category_prompt, R.string.categories_item));
            ToShow.add(new ShowModeItem(ShowModeEnum.Searching, R.string.search_prompt, R.string.search_item));
            ToShow.add(new ShowModeItem(ShowModeEnum.Favourites, R.string.faves_prompt, R.string.faves_item));
            ShowSelector.setAdapter(ToShow);
            ShowSelector.setOnItemSelectedListener(new ShowingSelect());
          }
        if (ToRestore == null)
          {
            ShowCategory = CategoryCodes.get("Basic Latin"); /* default */
          } /*if*/
          {
            CategoryListView = (Spinner)findViewById(R.id.category_selector);
            CategoryList = new CategoryItemAdapter();
            for (int CategoryCode = 0; CategoryCode < Unicode.NrCharCategories; ++CategoryCode)
              {
                CategoryList.add
                  (
                    new CategoryItem(CategoryCode, Unicode.GetCategoryName(CategoryCode))
                  );
              } /*for*/
            CategoryListView.setAdapter(CategoryList);
            if (ToRestore == null)
              {
                CategoryListView.setSelection(ShowCategory);
              } /*if*/
            CategoryListView.setOnItemSelectedListener(new CategorySelect());
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
                  /* have to do the work here, becauseCharSequence arg to beforeTextChanged and
                    afterTextChanged may not represent entire field contents */
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
        DetailCategoryButton = (Button)findViewById(R.id.show_category);
        DetailCategoryButton.setOnClickListener
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
                        SetShowingCategory(DetailCategory);
                        if (CurrentBG == null)
                          {
                            new BGCharSelector(CurChar);
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
      } /*onSaveInstanceState*/

    @Override
    public void onRestoreInstanceState
      (
        Bundle ToRestore
      )
      {
        super.onRestoreInstanceState(ToRestore);
      /* bit of trickiness to avoid OnItemSelectedListeners being called later
        and overriding restoration of previous selections */
        final AdapterView.OnItemSelectedListener SaveShowingListener =
            ShowSelector.getOnItemSelectedListener();
        final AdapterView.OnItemSelectedListener SaveCategoryListener =
            CategoryListView.getOnItemSelectedListener();
        ShowSelector.setOnItemSelectedListener(null);
        CategoryListView.setOnItemSelectedListener(null);
          /* avoid listener being triggered by restoration of state */
        SetCollectedText(ToRestore.getIntArray("input_text"));
          {
            final int CharIndex = Unicode.GetCharIndex(ToRestore.getInt("char"), false);
            ShowCharDetails(CharIndex >= 0 ? TableReader.GetCharByIndex(CharIndex) : null, true);
          }
        SetShowingCategory(ToRestore.getInt("category"));
        final int ToShow = ToRestore.getInt("display_mode");
        ShowSelector.setSelection(ToShow);
        SetShowingMode(ShowModeEnum.Val(ToShow));
        CategoryListView.post /* so it runs after OnItemSelectedListener would be triggered */
          (
            new Runnable()
              {
                public void run()
                  {
                    CategoryListView.setOnItemSelectedListener(SaveCategoryListener);
                  } /*run*/
              } /*Runnable*/
          );
        ShowSelector.post /* so it runs after OnItemSelectedListener would be triggered */
          (
            new Runnable()
              {
                public void run()
                  {
                    ShowSelector.setOnItemSelectedListener(SaveShowingListener);
                  } /*run*/
              } /*Runnable*/
          );
      } /*onRestoreInstanceState*/

    @Override
    public void onPause()
      {
        SaveState(); /* good place to do this */
        super.onPause();
      } /*onPause*/

  } /*Main*/;
