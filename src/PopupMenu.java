package nz.gen.geek_central.unicode_browser;
/*
    Unicode Browser app--implementation of popup action menus.
    Tapping on an item performs the action and dismisses the menu;
    or the user may press the back key to dismiss the menu without
    performing an action.

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

import android.widget.ArrayAdapter;
public class PopupMenu
  {
    private final android.content.Context ctx;
    private final ArrayAdapter<PopupAction> MenuItems;

    private class PopupAction
      {
        public final String Name;
        public final Runnable Action;

        public PopupAction
          (
            String Name,
            Runnable Action
          )
          {
            this.Name = Name;
            this.Action = Action;
          } /*PopupAction*/

        public String toString()
          {
            return
                Name;
          } /*toString*/

        public void Invoke()
          {
            Action.run();
          } /*Invoke*/

      } /*PopupAction*/;

    public PopupMenu
      (
        android.content.Context ctx
      )
      {
        this.ctx = ctx;
        this.MenuItems = new ArrayAdapter<PopupAction>
          (
            ctx,
            android.R.layout.simple_dropdown_item_1line
          );
      } /*PopupMenu*/

    public PopupMenu AddItem
      (
        String Name,
        Runnable Action
      )
      /* adds another item to the menu. */
      {
        MenuItems.add(new PopupAction(Name, Action));
        return
            this; /* for convenient chaining of calls */
      } /*AddItem*/

    public PopupMenu AddItem
      (
        int NameRes,
        Runnable Action
      )
      /* adds another item to the menu. */
      {
        return
            AddItem(ctx.getString(NameRes), Action);
      } /*AddItem*/

    public int NrItems()
      /* returns the number of items in the menu so far. */
      {
        return
            MenuItems.getCount();
      } /*NrItems*/

    public void Show()
      /* actually shows the menu and lets the user perform an action. */
      {
        new android.app.AlertDialog.Builder(ctx)
            .setSingleChoiceItems
              (
                /*adapter =*/ MenuItems,
                /*checkedItem =*/ -1,
                /*listener =*/
                    new android.content.DialogInterface.OnClickListener()
                      {
                        public void onClick
                          (
                            android.content.DialogInterface Popup,
                            int WhichItem
                          )
                          {
                            MenuItems.getItem(WhichItem).Invoke();
                            Popup.dismiss();
                          } /*onClick*/
                      } /*DialogInterface.OnClickListener*/
              )
            .show();
      } /*Show*/

  } /*PopupMenu*/;
