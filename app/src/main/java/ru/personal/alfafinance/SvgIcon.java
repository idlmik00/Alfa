package ru.personal.alfafinance;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import org.xmlpull.v1.XmlPullParser;
import android.util.Xml;
import java.util.*;
import java.io.*;
/** Small renderer for this app's own SVG sprite (absolute M/L/Q/C/Z paths only).
 * The paths are drawn at the device's native resolution, never as bitmaps. */
public final class SvgIcon extends View {
 private static Map<String,Path> paths;
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final Path vector; private final int size;
 public SvgIcon(Context context,String key,int color,int sizeDp){
  super(context);size=Math.round(sizeDp*getResources().getDisplayMetrics().density);load(context);
  Path p=paths.get(key);vector=p==null?paths.get("more"):p;
  paint.setColor(color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.8f);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
  setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
 }
 private static synchronized void load(Context c){
  if(paths!=null)return;Map<String,Path> result=new HashMap<>();
  try(InputStream in=c.getAssets().open("icons.svg")){
   XmlPullParser parser=Xml.newPullParser();parser.setInput(in,"UTF-8");String id=null;
   for(int event=parser.getEventType();event!=XmlPullParser.END_DOCUMENT;event=parser.next()){
    if(event==XmlPullParser.START_TAG&&"symbol".equals(parser.getName()))id=parser.getAttributeValue(null,"id");
    if(event==XmlPullParser.START_TAG&&"path".equals(parser.getName())&&id!=null)result.put(id,parse(parser.getAttributeValue(null,"d")));
   }
  }catch(Exception e){android.util.Log.e("SvgIcon","Unable to read bundled vector icons",e);}
  paths=result;
 }
 private static float f(String[] a,int i){return Float.parseFloat(a[i]);}
 private static Path parse(String d){Path p=new Path();String[] t=d.trim().split("\\s+");int i=0;while(i<t.length){switch(t[i++]){
 case "M":p.moveTo(f(t,i),f(t,i+1));i+=2;break;
 case "L":p.lineTo(f(t,i),f(t,i+1));i+=2;break;
 case "Q":p.quadTo(f(t,i),f(t,i+1),f(t,i+2),f(t,i+3));i+=4;break;
 case "C":p.cubicTo(f(t,i),f(t,i+1),f(t,i+2),f(t,i+3),f(t,i+4),f(t,i+5));i+=6;break;
 case "Z":p.close();break;
 default:throw new IllegalArgumentException("Unsupported SVG command");
 }}return p;}
 @Override protected void onDraw(Canvas c){super.onDraw(c);if(vector==null)return;float s=Math.min(size,Math.min(getWidth(),getHeight()));c.save();c.translate((getWidth()-s)/2,(getHeight()-s)/2);c.scale(s/24,s/24);c.drawPath(vector,paint);c.restore();}
}
