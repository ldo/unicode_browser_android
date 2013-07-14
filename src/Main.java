package nz.gen.geek_central.unicode_selector;

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

    class CategoryClick implements android.widget.AdapterView.OnItemSelectedListener
      {

        public void onItemSelected
          (
            android.widget.AdapterView<?> Parent,
            android.view.View ItemView,
            int Position,
            long ID
          )
          {
            ShowCategory = CategoryList.getItem(Position).Code;
            RebuildCharList();
          } /*onClick*/

        public void onNothingSelected
          (
            android.widget.AdapterView<?> Parent
          )
          {
          /* can't think of anything to do */
          } /*onNothingSelected*/

      } /*CategoryClick*/;

    class CategoryItemAdapter extends android.widget.ArrayAdapter<CategoryItem>
      {
        static final int ResID = android.R.layout.simple_dropdown_item_1line;
        final android.view.LayoutInflater TemplateInflater;

        public CategoryItemAdapter
          (
            android.view.LayoutInflater TemplateInflater
          )
          {
            super(Main.this, ResID);
            this.TemplateInflater = TemplateInflater;
          } /*CategoryItemAdapter*/

        @Override
        public android.view.View getView
          (
            int Position,
            android.view.View ReuseView,
            android.view.ViewGroup Parent
          )
          {
            android.view.View TheView = ReuseView;
            if (TheView == null)
              {
                TheView = TemplateInflater.inflate(ResID, null);
              } /*if*/
            final CategoryItem ThisItem = getItem(Position);
            ((android.widget.TextView)TheView.findViewById(android.R.id.text1)).setText
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
        final android.view.LayoutInflater TemplateInflater;

        CharItemAdapter
          (
            int ResID,
            android.view.LayoutInflater TemplateInflater
          )
          {
            super(Main.this, ResID);
            this.ResID = ResID;
            this.TemplateInflater = TemplateInflater;
          } /*CharItemAdapter*/

        @Override
        public android.view.View getView
          (
            int Position,
            android.view.View ReuseView,
            android.view.ViewGroup Parent
          )
          {
            android.view.View TheView = ReuseView;
            if (TheView == null)
              {
                TheView = TemplateInflater.inflate(ResID, null);
              } /*if*/
            final CharItem ThisItem = getItem(Position);
            ((android.widget.TextView)TheView.findViewById(R.id.code)).setText
              (
                String.format("U+%04X", ThisItem.CharCode)
              );
            ((android.widget.TextView)TheView.findViewById(R.id.literal)).setText
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
            ((android.widget.TextView)TheView.findViewById(R.id.name)).setText
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
        for (int CharCode : Unicode.Chars.keySet())
          {
            final Unicode.CharInfo ThisChar = Unicode.Chars.get(CharCode);
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
            final android.widget.Spinner SelectShowing = (android.widget.Spinner)findViewById(R.id.show_selector);
            CategoryList = new CategoryItemAdapter(getLayoutInflater());
            for (int CategoryCode : Unicode.CategoryNames.keySet())
              {
                if (CategoryCode == ShowCategory)
                  {
                    Selected = CategoryList.getCount();
                  } /*if*/
                CategoryList.add(new CategoryItem(CategoryCode, Unicode.CategoryNames.get(CategoryCode)));
              } /*for*/
            final android.widget.Spinner CategoryListView = (android.widget.Spinner)findViewById(R.id.show_selector);
            CategoryListView.setAdapter(CategoryList);
            CategoryListView.setOnItemSelectedListener(new CategoryClick());
            CategoryListView.setSelection(Selected);
          }
        CharList = new CharItemAdapter(R.layout.main_list_item, getLayoutInflater());
        CharListView = (android.widget.ListView)findViewById(R.id.list);
        CharListView.setAdapter(CharList);
        RebuildCharList();
      } /*onCreate*/

  } /*Main*/;
