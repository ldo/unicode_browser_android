package nz.gen.geek_central.unicode_browser;
/*
    Unicode Browser app--custom fixed-max-width text display, which
    adjusts the text horizontal scaling so the entire text remains
    visible without truncation.

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
