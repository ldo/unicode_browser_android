package nz.gen.geek_central.unicode_browser;
/*
    Unicode Browser app--mainline.

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

import android.view.View;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Spinner;
public class Main extends android.app.Activity
  {
    private final android.os.Handler BGTask = new android.os.Handler();
    private Runnable CurrentBG = null;
    private TableReader.Unicode Unicode;

    static class CharInfo
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

    CharInfo GetChar
      (
        int CharIndex
      )
      {
        return
            new CharInfo
              (
                /*Code =*/ Unicode.GetCharCode(CharIndex),
                /*Name =*/ Unicode.GetCharName(CharIndex),
                /*Category =*/ Unicode.GetCharCategory(CharIndex),
                /*OtherNames =*/ Unicode.GetCharOtherNames(CharIndex),
                /*LikeChars =*/ Unicode.GetCharLikeChars(CharIndex)
              );
      } /*GetChar*/

    private java.util.Map<String, Integer> CategoryCodes;

    private android.widget.FrameLayout ShowFrame;
    private Spinner ShowSelector, CategoryListView;
    private CategoryItemAdapter CategoryList;
    private android.widget.EditText SearchEntry;
    private android.widget.ProgressBar Progress;
    private ThingsToShow NowShowing;
    private int ShowCategory;
    private CharItemAdapter MainCharList, LikeCharList;
    private NameItemAdapter OtherNamesList;
    private TextView DetailCategoryDisplay;
    private android.widget.Button DetailCategoryButton;
    private int DetailCategory = -1;

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
        DetailCategoryButton.setVisibility
          (
                    DetailCategory < 0
                ||
                    NowShowing == ThingsToShow.Categories && ShowCategory == DetailCategory
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
        SetShowing(ThingsToShow.Categories);
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
            SetShowing((ThingsToShow)Parent.getAdapter().getItem(Position));
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
                        { /* encode as UTF-16, as Java requires */
                            (char)(Code >> 10 & 0x3ff | 0xd800),
                            (char)(Code & 0x3ff | 0xdc00),
                        }
                :
                    new char[] {(char)Code}
              );
      } /*CharToString*/

    enum ThingsToShow
      {
        Categories(0, R.string.category_prompt),
        Searching(1, R.string.search_prompt),
        ;

        public final int Index, PromptResID;

        private ThingsToShow
          (
            int Index,
            int PromptResID
          )
          {
            this.Index = Index;
            this.PromptResID = PromptResID;
          } /*ThingsToShow*/

      } /*ThingsToShow*/;

    class ShowItemAdapter extends ArrayAdapter<ThingsToShow>
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
            final ThingsToShow ThisItem = getItem(Position);
            ((TextView)TheView.findViewById(android.R.id.text1)).setText
              (
                ThisItem.PromptResID
              );
            return
                TheView;
          } /*getView*/

      } /*ShowItemAdapter*/;

    void SetShowing
      (
        ThingsToShow What
      )
      {
        NowShowing = What;
      /* ShowFrame.setDisplayedChild(NowShowing.Index); */ /* not for FrameLayout */
        if (What != ThingsToShow.Searching)
          {
            CancelBG();
          } /*if*/
        CategoryListView.setVisibility
          (
            What == ThingsToShow.Categories ?
                View.VISIBLE
            :
                View.INVISIBLE
          );
        SearchEntry.setVisibility
          (
            What == ThingsToShow.Searching ?
                View.VISIBLE
            :
                View.INVISIBLE
          );
        ShowSelector.setSelection(NowShowing.Index);
        SetShowDetailCategory();
        if (NowShowing == ThingsToShow.Searching)
          {
            QueueRebuildMainCharList(SearchEntry.getText().toString());
          }
        else
          {
            RebuildMainCharList(null, false);
          } /*if*/
      } /*SetShowing*/

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
            ((TextView)TheView.findViewById(R.id.code)).setText(FormatCharCode(ThisItem.Code));
            ((TextView)TheView.findViewById(R.id.literal)).setText(CharToString(ThisItem.Code));
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

        public void onItemClick
          (
            AdapterView<?> Parent,
            View ItemView,
            int Position,
            long ID
          )
          {
            ShowCharDetails((CharInfo)Parent.getAdapter().getItem(Position));
          } /*onItemClick*/

      } /*CharSelect*/;

    private void RebuildMainCharList
      (
        String Matching, /* null if not doing matching */
        boolean ShrinkMatch
      )
      {
        CancelBG();
        if (Matching == null || !ShrinkMatch)
          {
            MainCharList.clear();
          } /*if*/
        System.err.printf("RebuildMainCharList(%s, %s)\n", Matching, ShrinkMatch); /* debug */
        if (Matching != null)
          {
            Matching = Matching.toLowerCase();
            if (ShrinkMatch)
              {
                for (int i = 0;;)
                  {
                    if (i == MainCharList.getCount())
                        break;
                    if (MainCharList.getItem(i).Name.toLowerCase().contains(Matching))
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
                    if (Unicode.GetCharName(i).toLowerCase().contains(Matching))
                      {
                        MainCharList.add(GetChar(i));
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
                    MainCharList.add(GetChar(i));
                  } /*if*/
              } /*for*/
          } /*if*/
        MainCharList.notifyDataSetChanged();
      } /*RebuildMainCharList*/

    private final int MaxPerBGRun = 100;

    private void CancelBG()
      {
        Progress.setVisibility(View.INVISIBLE);
        CurrentBG = null;
      } /*CancelBG*/

    private class BGCharListRebuilder implements Runnable
      {
      /* for doing time-consuming character matches in background to keep interface responsive */
        private final String Matching;
        private int CurIndex;
        private final long StartTime;

        public BGCharListRebuilder
          (
            String Matching
          )
          {
            this.Matching = Matching.toLowerCase();
            CurIndex = 0;
            MainCharList.clear();
            StartTime = System.currentTimeMillis();
          } /*BGCharListRebuilder*/

        public void run()
          {
            if (CurrentBG == this)
              {
                if (System.currentTimeMillis() - StartTime > 1000)
                  {
                    Progress.setVisibility(View.VISIBLE);
                  } /*if*/
                final int DoThisRun = Math.min(Unicode.NrChars - CurIndex, MaxPerBGRun);
                for
                  (
                    int i = 0;
                    i < MaxPerBGRun && CurIndex < Unicode.NrChars;
                    ++i, ++CurIndex
                  )
                  {
                    if (Unicode.GetCharName(CurIndex).toLowerCase().contains(Matching))
                      {
                        MainCharList.add(GetChar(CurIndex));
                      } /*if*/
                  } /*for*/
                MainCharList.notifyDataSetChanged();
                if (CurIndex < Unicode.NrChars)
                  {
                    BGTask.post(this);
                  }
                else
                  {
                    CancelBG();
                  } /*if*/
              } /*if*/
          } /*run*/

      } /*BGCharListRebuilder*/;

    private void QueueRebuildMainCharList
      (
        final String Matching /* won't be null */
      )
      {
        CurrentBG = new BGCharListRebuilder(Matching);
        BGTask.post(CurrentBG);
      } /*QueueRebuildMainCharList*/

    private void RebuildMainCharList()
      {
        RebuildMainCharList(null, false);
      } /*RebuildMainCharList*/

    private void ShowCharDetails
      (
        CharInfo TheChar
      )
      {
        ((TextView)findViewById(R.id.big_literal)).setText(CharToString(TheChar.Code));
        ((TextView)findViewById(R.id.details)).setText
          (
            String.format
              (
                java.util.Locale.US,
                "%s %s",
                FormatCharCode(TheChar.Code),
                TheChar.Name
              )
          );
        DetailCategoryDisplay.setText(Unicode.GetCategoryName(TheChar.Category));
        DetailCategory = TheChar.Category;
        OtherNamesList.clear();
        for (String Name : TheChar.OtherNames)
          {
            OtherNamesList.add(Name);
          } /*for*/
        OtherNamesList.notifyDataSetChanged();
        LikeCharList.clear();
        for (int Code : TheChar.LikeChars)
          {
            LikeCharList.add(GetChar(Unicode.GetCharIndex(Code)));
          } /*for*/
        LikeCharList.notifyDataSetChanged();
        SetShowDetailCategory();
      } /*ShowCharDetails*/

    @Override
    public void onCreate
      (
        android.os.Bundle ToRestore
      )
      {
        Unicode = TableReader.Load(this);
        CategoryCodes = new java.util.HashMap<String, Integer>(Unicode.NrCharCategories);
        for (int i = 0; i < Unicode.NrCharCategories; ++i)
          {
            CategoryCodes.put(Unicode.GetCategoryName(i), i);
          } /*for*/
        super.onCreate(ToRestore);
        setContentView(R.layout.main);
        ShowFrame = (android.widget.FrameLayout)findViewById(R.id.show_frame);
          {
            ShowSelector = (Spinner)findViewById(R.id.show_prompt);
            final ShowItemAdapter ToShow = new ShowItemAdapter();
            for (ThingsToShow ShowThis : ThingsToShow.values())
              {
                ToShow.add(ShowThis);
              } /*for*/
            ShowSelector.setAdapter(ToShow);
            ShowSelector.setOnItemSelectedListener(new ShowingSelect());
          }
        ShowCategory = CategoryCodes.get("C0 Controls and Basic Latin (Basic Latin)"); /* default */
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
            CategoryListView.setOnItemSelectedListener(new CategorySelect());
            CategoryListView.setSelection(ShowCategory);
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
                    System.err.printf("SearchEntry.afterTextChanged(%s, %s)\n", Before, After); /* debug */
                    if (CurrentBG == null && After.contains(Before) && MainCharList.getCount() <= MaxPerBGRun)
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
          {
            final ListView CharListView = (ListView)findViewById(R.id.main_list);
            MainCharList = new CharItemAdapter(R.layout.char_list_item);
            CharListView.setAdapter(MainCharList);
            CharListView.setOnItemClickListener(new CharSelect());
          }
        OtherNamesList = new NameItemAdapter();
        ((ListView)findViewById(R.id.names_list)).setAdapter(OtherNamesList);
          {
            final ListView CharListView = (ListView)findViewById(R.id.like_list);
            LikeCharList = new CharItemAdapter(R.layout.also_char_list_item);
            CharListView.setAdapter(LikeCharList);
            CharListView.setOnItemClickListener(new CharSelect());
          }
        DetailCategoryDisplay = (TextView)findViewById(R.id.category);
        DetailCategoryButton = (android.widget.Button)findViewById(R.id.show_category);
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
                      } /*if*/
                  } /*onClick*/
              }
          );
        SetShowing(ThingsToShow.Categories);
      } /*onCreate*/

  } /*Main*/;
