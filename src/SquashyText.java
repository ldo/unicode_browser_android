package nz.gen.geek_central.unicode_browser;
/*
    Unicode Browser app--custom fixed-max-width text display, which
    adjusts the text horizontal scaling so the entire text remains
    visible without truncation.

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

public class SquashyText extends android.widget.TextView
  {

    private void Init()
      {
        setSingleLine(true);
      } /*Init*/

    public SquashyText
      (
        android.content.Context Context
      )
      {
        super(Context);
        Init();
      } /*SquashyText*/

    public SquashyText
      (
        android.content.Context Context,
        android.util.AttributeSet Attributes
      )
      {
        super(Context, Attributes);
        Init();
      } /*SquashyText*/

    public SquashyText
      (
        android.content.Context Context,
        android.util.AttributeSet Attributes,
        int DefaultStyle
      )
      {
        super(Context, Attributes, DefaultStyle);
        Init();
      } /*SquashyText*/

    @Override
    public void onDraw
      (
        android.graphics.Canvas g
      )
      {
        final float SaveTextScaleX = getTextScaleX();
        setTextScaleX(Math.min(getWidth() / getPaint().measureText(getText().toString() + " "), 1.0f));
        super.onDraw(g);
        setTextScaleX(SaveTextScaleX);
      } /*onDraw*/

  } /*SquashyText*/;
