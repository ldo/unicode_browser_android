package nz.gen.geek_central.unicode_browser; /* must be in same package as app in order to find resources */
/*
    let the user choose a file to load

    Copyright 2011-2014 by Lawrence D'Oliveiro <ldo@geek-central.gen.nz>.

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

import android.os.Bundle;
import android.content.Intent;
import android.view.View;

public class Picker extends android.app.Activity
  {
  /* extra information passed in launch intent: */
    public static final String LookInID = "nz.gen.geek_central.android.useful.Picker.LookIn";
      /* (required) array of strings representing names of directories in which to look for files */
      /* ones not beginning with “/” are interpreted as subdirectories in external storage */
    public static final String ExtensionID = "nz.gen.geek_central.android.useful.Picker.Extension";
      /* (required) extension of filenames to show */
    public static final String SpecialItemsID = "nz.gen.geek_central.android.useful.Picker.SpecialItems";
      /* optional array of Parcelables (which must be Bundles) representing special items to
        display at top of list. Each item Bundle has both “key” and “value” items, whose
        values are Strings; the “value” is shown to the user, and the “key” is returned
        to the caller if the user picks that item. */
    public static final String PreviousItemID = "nz.gen.geek_central.android.useful.Picker.Previous";
      /* if present, pathname of previously-selected item to preselect in list */
    public static final String PreviousSpecialItemID = "nz.gen.geek_central.android.useful.Picker.PreviousSpecial";
     /* if present, key of previously-selected special item to preselect in list */

    public static final String SpecialItemSelectedID = "nz.gen.geek_central.android.useful.Picker.SpecialItem";
      /* will be inserted into result Intent instead of regular data if a special item
        was selected; value will be key from selected special item. */

    public static final String SpecialItemKeyID = "key";
    public static final String SpecialItemValueID = "value";

    android.widget.ListView PickerListView;
    SelectedItemAdapter PickerList;

    static class PickerItem
      {
        final boolean Special;
        final String SpecialKey, SpecialValue, FullPath;
        boolean Selected;

        public PickerItem
          (
            boolean Special,
            String SpecialKey,
            String SpecialValue,
            String FullPath,
            boolean Selected
          )
          {
            this.Special = Special;
            this.SpecialKey = SpecialKey;
            this.SpecialValue = SpecialValue;
            this.FullPath = FullPath;
            this.Selected = Selected;
          } /*PickerItem*/

        public String toString()
          /* returns the display name for the item. I use
            the unqualified filename for non-special items. */
          {
            return
                Special ?
                    SpecialValue
                :
                    new java.io.File(FullPath).getName();
          } /*toString*/

      } /*PickerItem*/;

    class SelectedItemAdapter extends android.widget.ArrayAdapter<PickerItem>
      {
        final int ResID;
        final android.view.LayoutInflater TemplateInflater;
        PickerItem CurSelected;
        android.widget.RadioButton LastChecked;

        class OnSetCheck implements View.OnClickListener
          {
            final PickerItem MyItem;

            public OnSetCheck
              (
                PickerItem TheItem
              )
              {
                MyItem = TheItem;
              } /*OnSetCheck*/

            public void onClick
              (
                View TheView
              )
              {
                if (MyItem != CurSelected)
                  {
                  /* only allow one item to be selected at a time */
                    if (CurSelected != null)
                      {
                        CurSelected.Selected = false;
                        LastChecked.setChecked(false);
                      } /*if*/
                    LastChecked =
                        TheView instanceof android.widget.RadioButton ?
                            (android.widget.RadioButton)TheView
                        :
                            (android.widget.RadioButton)
                            ((android.view.ViewGroup)TheView).findViewById(R.id.file_item_checked);
                    CurSelected = MyItem;
                    MyItem.Selected = true;
                    LastChecked.setChecked(true);
                  } /*if*/
              } /*onClick*/
          } /*OnSetCheck*/

        SelectedItemAdapter
          (
            android.content.Context TheContext,
            int ResID,
            android.view.LayoutInflater TemplateInflater
          )
          {
            super(TheContext, ResID);
            this.ResID = ResID;
            this.TemplateInflater = TemplateInflater;
            CurSelected = null;
            LastChecked = null;
          } /*SelectedItemAdapter*/

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
            final PickerItem ThisItem = this.getItem(Position);
            ((android.widget.TextView)TheView.findViewById(R.id.select_file_name))
                .setText(ThisItem.toString());
            final android.widget.RadioButton ThisChecked =
                (android.widget.RadioButton)TheView.findViewById(R.id.file_item_checked);
            ThisChecked.setChecked(ThisItem.Selected);
            if (ThisItem.Selected)
              {
                CurSelected = ThisItem;
                LastChecked = ThisChecked;
              } /*if*/
            final OnSetCheck ThisSetCheck = new OnSetCheck(ThisItem);
            ThisChecked.setOnClickListener(ThisSetCheck);
              /* otherwise radio button can get checked but I don't notice */
            TheView.setOnClickListener(ThisSetCheck);
            return
                TheView;
          } /*getView*/

      } /*SelectedItemAdapter*/;

    @Override
    public void onCreate
      (
        Bundle ToRestore
      )
      {
        super.onCreate(ToRestore);
        setContentView(R.layout.picker);
        PickerList = new SelectedItemAdapter(this, R.layout.picker_item, getLayoutInflater());
        PickerListView = (android.widget.ListView)findViewById(R.id.item_list);
        PickerListView.setAdapter(PickerList);
        PickerList.setNotifyOnChange(false);
        PickerList.clear();
        final Intent LaunchIntent = getIntent();
        int ScrollToItem = -1;
        final String PreviousSpecialItem = LaunchIntent.getStringExtra(PreviousSpecialItemID);
        final String PreviousItem = LaunchIntent.getStringExtra(PreviousItemID);
          {
          /* specials, if any, go at top of list */
            final android.os.Parcelable[] SpecialItems =
                LaunchIntent.getParcelableArrayExtra(SpecialItemsID);
            if (SpecialItems != null)
              {
                for (int i = 0; i < SpecialItems.length; ++i)
                  {
                    final Bundle ThisSpecial = (Bundle)SpecialItems[i];
                    final String ThisKey = ThisSpecial.getString(SpecialItemKeyID);
                    final boolean Selected =
                        PreviousSpecialItem != null && ThisKey.equals(PreviousSpecialItem);
                    if (Selected)
                      {
                        ScrollToItem = PickerList.getCount();
                      } /*if*/
                    PickerList.add
                      (
                        new PickerItem
                          (
                            /*Special =*/ true,
                            /*SpecialKey =*/ ThisKey,
                            /*SpecialValue =*/ ThisSpecial.getString(SpecialItemValueID),
                            /*FullPath =*/ null,
                            /*Selected =*/ Selected
                          )
                      );
                  } /*for*/
              } /*if*/
          }
          {
            final String ExternalStorage =
                android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
            final String Extension = LaunchIntent.getStringExtra(ExtensionID);
            for (String Here : LaunchIntent.getStringArrayExtra(LookInID))
              {
                final java.io.File ThisDir = new java.io.File
                  (
                        (Here.startsWith("/") ?
                            ""
                        :
                            ExternalStorage + "/"
                        )
                    +
                        Here
                  );
                if (ThisDir.isDirectory())
                  {
                    for (java.io.File Item : ThisDir.listFiles())
                      {
                        if (Item.getName().endsWith(Extension))
                          {
                            final boolean Selected =
                                    ScrollToItem < 0
                                &&
                                    PreviousItem != null
                                &&
                                    Item.getPath().equals(PreviousItem);
                            if (Selected)
                              {
                                ScrollToItem = PickerList.getCount();
                              } /*if*/
                            PickerList.add
                              (
                                new PickerItem
                                  (
                                    /*Special =*/ false,
                                    /*SpecialKey =*/ null,
                                    /*SpecialValue =*/ null,
                                    /*FullPath =*/ Item.getAbsolutePath(),
                                    /*Selected =*/ Selected
                                  )
                              );
                          } /*if*/
                      } /*for*/
                  } /* if*/
              } /*for*/
          }
        PickerList.notifyDataSetChanged();
        if (ScrollToItem >= 0)
          {
            final int ScrollTo = ScrollToItem;
            PickerListView.post
              (
                new Runnable()
                  {
                    public void run()
                      {
                        PickerListView.smoothScrollToPosition(ScrollTo);
                      } /*run*/
                  } /*Runnable*/
              );
          } /*if*/
        ((android.widget.Button)findViewById(R.id.item_select)).setOnClickListener
          (
            new View.OnClickListener()
              {
                public void onClick
                  (
                    View TheView
                  )
                  {
                    PickerItem Selected = null;
                    for (int i = 0;;)
                      {
                        if (i == PickerList.getCount())
                            break;
                        final PickerItem ThisItem =
                            (PickerItem)PickerListView.getItemAtPosition(i);
                        if (ThisItem.Selected)
                          {
                            Selected = ThisItem;
                            break;
                          } /*if*/
                        ++i;
                      } /*for*/
                    if (Selected != null)
                      {
                        final Intent Result = new Intent();
                        if (Selected.Special)
                          {
                            Result.putExtra(SpecialItemSelectedID, Selected.SpecialKey);
                          }
                        else
                          {
                            Result.setData
                              (
                                android.net.Uri.fromFile
                                  (
                                    new java.io.File(Selected.FullPath)
                                  )
                              );
                          } /*if*/
                        setResult(android.app.Activity.RESULT_OK, Result);
                        finish();
                      } /*if*/
                  } /*onClick*/
              } /*OnClickListener*/
          );
      } /*onCreate*/

  } /*Picker*/;
