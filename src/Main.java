package nz.gen.geek_central.unicode_selector;

public class Main extends android.app.Activity
  {

    private android.widget.ListView CharListView;
    private CharItemAdapter CharList;

    public static class CharItem
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
            final CharItem ThisItem = (CharItem)getItem(Position);
            ((android.widget.TextView)TheView.findViewById(R.id.text)).setText
              (
                String.format
                  (
                    "U+%#04X -- %s",
                    ThisItem.CharCode,
                    ThisItem.Info.Name
                  )
              );
          /* more TBD */
            return
                TheView;
          } /*getView*/

      } /*CharItemAdapter*/;

    @Override
    public void onCreate
      (
        android.os.Bundle ToRestore
      )
      {
        super.onCreate(ToRestore);
        setContentView(R.layout.main);
        CharList = new CharItemAdapter(R.layout.main_list_item, getLayoutInflater());
        for (int CharCode : Unicode.Chars.keySet()) /* quick-and-dirty listing of all characters for now */
          {
            CharList.add(new CharItem(CharCode, Unicode.Chars.get(CharCode)));
          } /*for*/
        CharListView = (android.widget.ListView)findViewById(R.id.list);
        CharListView.setAdapter(CharList);
      } /*onCreate*/

  } /*Main*/;
