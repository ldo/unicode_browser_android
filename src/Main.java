package nz.gen.geek_central.unicode_selector;

import android.view.View;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.AdapterView;
public class Main extends android.app.Activity
  {

    private CategoryItemAdapter CategoryList;
    private android.widget.ListView CharListView;
    private CharItemAdapter CharList;
    private int ShowCategory = Unicode.CategoryCodes.get("Lowercase Latin alphabet");

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
            ShowCategory = CategoryList.getItem(Position).Code;
            RebuildCharList();
          } /*onClick*/

        public void onNothingSelected
          (
            AdapterView<?> Parent
          )
          {
          /* can't think of anything to do */
          } /*onNothingSelected*/

      } /*CategorySelect*/;

    class CategoryItemAdapter extends android.widget.ArrayAdapter<CategoryItem>
      {
        static final int ResID = android.R.layout.simple_dropdown_item_1line;
        final LayoutInflater TemplateInflater;

        public CategoryItemAdapter
          (
            LayoutInflater TemplateInflater
          )
          {
            super(Main.this, ResID);
            this.TemplateInflater = TemplateInflater;
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

    class CharItemAdapter extends android.widget.ArrayAdapter<CharItem>
      {
        final int ResID;
        final LayoutInflater TemplateInflater;

        CharItemAdapter
          (
            int ResID,
            LayoutInflater TemplateInflater
          )
          {
            super(Main.this, ResID);
            this.ResID = ResID;
            this.TemplateInflater = TemplateInflater;
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
            ((TextView)TheView.findViewById(R.id.code)).setText
              (
                String.format("U+%04X", ThisItem.CharCode)
              );
            ((TextView)TheView.findViewById(R.id.literal)).setText
              (
                new String
                  (
                    ThisItem.CharCode > 0xffff ?
                        new char[]
                            { /* encode as UTF-16, as Java requires */
                                (char)(ThisItem.CharCode >> 10 & 0x3ff | 0xd800),
                                (char)(ThisItem.CharCode & 0x3ff | 0xdc00),
                            }
                    :
                        new char[] {(char)ThisItem.CharCode}
                  )
              );
            ((TextView)TheView.findViewById(R.id.name)).setText
              (
                ThisItem.Info.Name
              );
          /* more TBD */
            return
                TheView;
          } /*getView*/

      } /*CharItemAdapter*/;

    private void RebuildCharList()
      {
        CharList.clear();
        for (int i = 0; i < Unicode.Chars.size(); ++i)
          {
            final int CharCode = Unicode.Chars.keyAt(i);
            final Unicode.CharInfo ThisChar = Unicode.Chars.valueAt(i);
            if (ThisChar.Category == ShowCategory)
              {
                CharList.add(new CharItem(CharCode, ThisChar));
              } /*if*/
          } /*for*/
        CharList.notifyDataSetChanged();
      } /*RebuildCharList*/

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
            final android.widget.Spinner CategoryListView = (android.widget.Spinner)findViewById(R.id.show_selector);
            CategoryList = new CategoryItemAdapter(getLayoutInflater());
            for (int i = 0; i < Unicode.CategoryNames.size(); ++i)
              {
                final int CategoryCode = Unicode.CategoryNames.keyAt(i);
                if (CategoryCode == ShowCategory)
                  {
                    Selected = CategoryList.getCount();
                  } /*if*/
                CategoryList.add(new CategoryItem(CategoryCode, Unicode.CategoryNames.get(CategoryCode)));
              } /*for*/
            CategoryListView.setAdapter(CategoryList);
            CategoryListView.setOnItemSelectedListener(new CategorySelect());
            CategoryListView.setSelection(Selected);
          }
        CharList = new CharItemAdapter(R.layout.main_list_item, getLayoutInflater());
        CharListView = (android.widget.ListView)findViewById(R.id.list);
        CharListView.setAdapter(CharList);
        RebuildCharList();
      } /*onCreate*/

  } /*Main*/;
