package nz.gen.geek_central.unicode_selector;

import android.view.View;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.ListView;
public class Main extends android.app.Activity
  {

    private android.widget.Spinner CategoryListView;
    private CategoryItemAdapter CategoryList;
    private int ShowCategory = Unicode.CategoryCodes.get("Lowercase Latin alphabet");
    private CharItemAdapter MainCharList, LikeCharList;
    private ArrayAdapter<String> OtherNamesList;
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

    private void SetShowingCategory
      (
        int NewCategory
      )
      {
        ShowCategory = NewCategory;
          {
            int Selected;
            for (int i = 0;;)
              {
                if (Unicode.CategoryNames.keyAt(i) == ShowCategory)
                  {
                    Selected = i;
                    break;
                  } /*if*/
                ++i;
              } /*for*/
            CategoryListView.setSelection(Selected);
          }
        RebuildMainCharList();
      } /*SetShowingCategory*/

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
          } /*onClick*/

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

    static class CharItem
      {
        public final int CharCode;
        public final Unicode.CharInfo Info;

        public CharItem
          (
            int CharCode,
            Unicode.CharInfo Info
          )
          {
            this.CharCode = CharCode;
            this.Info = Info;
          } /*CharItem*/

      } /*CharItem*/;

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

    class CharItemAdapter extends ArrayAdapter<CharItem>
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
            final CharItem ThisItem = getItem(Position);
            ((TextView)TheView.findViewById(R.id.code)).setText(FormatCharCode(ThisItem.CharCode));
            ((TextView)TheView.findViewById(R.id.literal)).setText(CharToString(ThisItem.CharCode));
            ((TextView)TheView.findViewById(R.id.name)).setText(ThisItem.Info.Name);
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
            ShowCharDetails((CharItem)Parent.getAdapter().getItem(Position));
          } /*onItemClick*/

      } /*CharSelect*/;

    private void RebuildMainCharList()
      {
        MainCharList.clear();
        for (int i = 0; i < Unicode.Chars.size(); ++i)
          {
            final int CharCode = Unicode.Chars.keyAt(i);
            final Unicode.CharInfo ThisChar = Unicode.Chars.valueAt(i);
            if (ThisChar.Category == ShowCategory)
              {
                MainCharList.add(new CharItem(CharCode, ThisChar));
              } /*if*/
          } /*for*/
        MainCharList.notifyDataSetChanged();
      } /*RebuildMainCharList*/

    private void ShowCharDetails
      (
        CharItem TheChar
      )
      {
        ((TextView)findViewById(R.id.details)).setText
          (
            String.format
              (
                java.util.Locale.US,
                "%s “%s” %s",
                FormatCharCode(TheChar.CharCode),
                CharToString(TheChar.CharCode),
                TheChar.Info.Name
              )
          );
        DetailCategoryButton.setText
          (
            String.format
              (
                getString(R.string.show_button_format),
                Unicode.CategoryNames.get(TheChar.Info.Category)
              )
          );
        DetailCategory = TheChar.Info.Category;
        OtherNamesList.clear();
        for (String Name : TheChar.Info.OtherNames)
          {
            OtherNamesList.add(Name);
          } /*for*/
        OtherNamesList.notifyDataSetChanged();
        LikeCharList.clear();
        for (int Code : TheChar.Info.LikeChars)
          {
            LikeCharList.add(new CharItem(Code, Unicode.Chars.get(Code)));
          } /*for*/
        LikeCharList.notifyDataSetChanged();
      } /*ShowCharDetails*/

    @Override
    public void onCreate
      (
        android.os.Bundle ToRestore
      )
      {
        super.onCreate(ToRestore);
        setContentView(R.layout.main);
          {
            int Selected = 0;
            CategoryListView = (android.widget.Spinner)findViewById(R.id.show_selector);
            CategoryList = new CategoryItemAdapter();
            for (int i = 0; i < Unicode.CategoryNames.size(); ++i)
              {
                final int CategoryCode = Unicode.CategoryNames.keyAt(i);
                if (CategoryCode == ShowCategory)
                  {
                    Selected = CategoryList.getCount();
                  } /*if*/
                CategoryList.add
                  (
                    new CategoryItem(CategoryCode, Unicode.CategoryNames.valueAt(i))
                  );
              } /*for*/
            CategoryListView.setAdapter(CategoryList);
            CategoryListView.setOnItemSelectedListener(new CategorySelect());
            CategoryListView.setSelection(Selected);
          }
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
        DetailCategoryButton = (android.widget.Button)findViewById(R.id.category);
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
        RebuildMainCharList();
      } /*onCreate*/

  } /*Main*/;
