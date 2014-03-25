package nz.gen.geek_central.unicode_browser;
/*
    Support for context menu and action bar, where available.

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

import android.view.Menu;
import android.view.MenuItem;

public abstract class ActionActivity extends android.app.Activity
  {

    protected java.util.Map<MenuItem, Runnable> OptionsMenu;
    protected java.util.Map<MenuItem, Runnable> ContextMenu;
    protected Menu TheOptionsMenu;
    protected android.view.ContextMenu TheContextMenu;

    public static boolean ClassHasMethod
      (
        String ClassName,
        String MethodName,
        Class<?>... ArgTypes
      )
      /* does the named class have a method with the specified argument types. */
      {
        boolean HasIt;
        try
          {
            HasIt =
                    Class.forName(ClassName)
                        .getDeclaredMethod(MethodName, ArgTypes)
                !=
                    null;
          }
        catch (NoSuchMethodException Nope)
          {
            HasIt = false;
          }
        catch (ClassNotFoundException Huh)
          {
            throw new RuntimeException(Huh.toString());
          } /*try*/
        return
            HasIt;
      } /*ClassHasMethod*/

    public static final boolean HasActionBar =
        ClassHasMethod
          (
            /*ClassName =*/ "android.view.MenuItem",
            /*MethodName = */ "setShowAsAction",
            /*ArgTypes =*/ Integer.TYPE
          );

    protected abstract void OnCreateOptionsMenu();
      /* Do actual creation of options menu here. */

    protected void AddOptionsMenuItem
      (
        int StringID,
        int IconID,
        int ActionBarUsage, /* post-Gingerbread only */
        Runnable Action
      )
      /* Call from within OnCreateOptionsMenu to define menu items. */
      {
        final MenuItem TheItem = TheOptionsMenu.add(StringID);
        if (IconID != 0)
          {
            TheItem.setIcon(IconID);
          } /*if*/
        if (HasActionBar)
          {
            TheItem.setShowAsAction(ActionBarUsage);
          } /*if*/
        OptionsMenu.put(TheItem, Action);
      } /*AddOptionsMenuItem*/

    @Override
    public boolean onCreateOptionsMenu
      (
        Menu TheMenu
      )
      /* takes care of calling your OnCreateOptionsMenu. */
      {
        TheOptionsMenu = TheMenu;
        OptionsMenu = new java.util.HashMap<MenuItem, Runnable>();
        OnCreateOptionsMenu();
        return
            true;
      } /*onCreateOptionsMenu*/

    protected void InitContextMenu
      (
        android.view.ContextMenu TheMenu
      )
      /* must be called at start of setup of context menu. */
      {
        TheContextMenu = TheMenu;
        ContextMenu = new java.util.HashMap<MenuItem, Runnable>();
      } /*InitContextMenu*/

    protected void AddContextMenuItem
      (
        String Name,
        Runnable Action
      )
      /* call to add another item to context menu. */
      {
        ContextMenu.put(TheContextMenu.add(Name), Action);
      } /*AddContextMenuItem*/

    protected void AddContextMenuItem
      (
        int StringID,
        Runnable Action
      )
      /* call to add another item to context menu. */
      {
        AddContextMenuItem(getString(StringID), Action);
      } /*AddContextMenuItem*/

    @Override
    public boolean onOptionsItemSelected
      (
        MenuItem TheItem
      )
      {
        boolean Handled = false;
        final Runnable Action = OptionsMenu != null ? OptionsMenu.get(TheItem) : null;
        if (Action != null)
          {
            Action.run();
            Handled = true;
          } /*if*/
        return
            Handled;
      } /*onOptionsItemSelected*/

    @Override
    public boolean onContextItemSelected
      (
        MenuItem TheItem
      )
      {
        boolean Handled = false;
        final Runnable Action = ContextMenu != null ? ContextMenu.get(TheItem) : null;
        if (Action != null)
          {
            Action.run();
            Handled = true;
          } /*if*/
        return
            Handled;
      } /*onContextItemSelected*/

  } /*ActionActivity*/;
