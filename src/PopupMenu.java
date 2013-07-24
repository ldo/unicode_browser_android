package nz.gen.geek_central.android_useful;
/*
    Implementation of simple popup action menus. Tapping on an item
    performs the action and dismisses the menu; or the user may press
    the back key to dismiss the menu without performing an action.

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
