package ru.personal.alfafinance;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.NumberFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MainActivity extends Activity {
 static final int BG=0xff050506, CARD=0xff101011, LINE=0xff222224, RED=0xffff3038, WHITE=0xfff7f7f8, MUTED=0xff939399;
 static final Locale RU=new Locale("ru","RU");
 static final String[] TYPES={"Карта","Наличные","Счёт"};
 static final String[] CATEGORIES=CategoryCatalog.names(false).toArray(new String[0]);
 Ledger ledger=new Ledger(); SharedPreferences prefs; LinearLayout root; YearMonth month=YearMonth.now();
 String page="home",accountFilter=null,importAccount=null;
 String reportMode="month",reportType=Ledger.OUT,detailCategory=null;LocalDate anchor=LocalDate.now(),customStart=LocalDate.now().withDayOfMonth(1),customEnd=LocalDate.now(),detailDay=null;YearMonth calendarMonth=YearMonth.now();
 boolean selecting=false;final Set<String> selectedEntries=new LinkedHashSet<>();List<Ledger.Entry> visibleEntries=new ArrayList<>();
 final int[] palette={0xffff343d,0xffe7a447,0xff6197ef,0xffa68ae6,0xff46c4a8,0xffdc78a5,0xffa4bc62,0xff65c4e0,0xfff07952,0xffa4a4ad,0xff7b73d8,0xffd6c46c}; int historyLimit=100; boolean hidden,secure,storageLocked,loadFailed;
 @Override public void onCreate(Bundle saved){
  super.onCreate(saved);prefs=getSharedPreferences("local",MODE_PRIVATE);hidden=prefs.getBoolean("hidden",false);secure=prefs.getBoolean("secure",true);
  getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);getWindow().getDecorView().setSystemUiVisibility(0);security();
  try{String state=prefs.getString("ledger_v2",null);if(state!=null)ledger=decode(state);}catch(Exception e){storageLocked=true;loadFailed=true;}
  if(saved!=null){page=saved.getString("page","home");accountFilter=saved.getString("filter");importAccount=saved.getString("import");try{month=YearMonth.parse(saved.getString("month"));}catch(Exception ignored){}}
  show();
  if(storageLocked)new AlertDialog.Builder(this).setTitle("Не удалось прочитать локальные данные").setMessage("Исходные данные сохранены. Изменения отключены, чтобы не перезаписать их. Восстановите проверенную резервную копию в настройках.").setPositiveButton("ОК",null).show();
 }
 @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putString("page",page);b.putString("filter",accountFilter);b.putString("month",month.toString());b.putString("import",importAccount);}
 void security(){if(secure)getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);}
 int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
 String money(long n){NumberFormat f=NumberFormat.getNumberInstance(RU);f.setMinimumFractionDigits(n%100==0?0:2);f.setMaximumFractionDigits(2);return f.format(BigDecimal.valueOf(n,2))+" ₽";}
 String signed(long n){return (n>=0?"+":"−")+money(Math.abs(n));}
 String number(long n){return BigDecimal.valueOf(n,2).stripTrailingZeros().toPlainString();}
 long amount(String s){long n=new BigDecimal(s.trim().replace(" ","").replace("\u00a0","").replace("\u202f","").replace(',','.')).movePointRight(2).longValueExact();if(n < -Ledger.MAX||n>Ledger.MAX)throw new IllegalArgumentException("Сумма слишком велика");return n;}
 String uid(){return UUID.randomUUID().toString();}
 String accountName(String id){Ledger.Account a=ledger.accounts.get(id);return a==null?"—":a.name;}
 String monthName(){String s=month.format(DateTimeFormatter.ofPattern("LLLL yyyy",RU));return s.substring(0,1).toUpperCase(RU)+s.substring(1);}
 void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
 void error(Exception e){toast(e instanceof IllegalArgumentException&&e.getMessage()!=null?e.getMessage():"Проверьте введённые данные");}
 boolean writable(){if(storageLocked){toast("Сначала восстановите данные, изменения заблокированы");return false;}return true;}
 GradientDrawable bg(int color,float radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
 GradientDrawable border(int color,float radius,int stroke){GradientDrawable d=bg(color,radius);d.setStroke(dp(1),stroke);return d;}
 GradientDrawable gradient(){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff8c0d19,0xff36080c,0xff09090b,0xffa40c18});d.setCornerRadius(dp(22));return d;}
 TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextColor(color);t.setTextSize(size);t.setFontFeatureSettings("tnum");if(bold)t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));t.setIncludeFontPadding(false);return t;}
 LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
 LinearLayout horizontal(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
 void space(LinearLayout l,int n){View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,dp(n)));}
 void line(LinearLayout l){View v=new View(this);v.setBackgroundColor(LINE);l.addView(v,new LinearLayout.LayoutParams(-1,dp(1)));}
 void label(LinearLayout l,String s,int size,int color,boolean bold){l.addView(text(s,size,color,bold));}
 void weight(LinearLayout l,View v){l.addView(v,new LinearLayout.LayoutParams(0,-2,1));}
 void card(LinearLayout parent,LinearLayout l){l.setPadding(dp(16),dp(16),dp(16),dp(16));l.setBackground(bg(CARD,18));parent.addView(l,new LinearLayout.LayoutParams(-1,-2));}
 TextView action(String s,boolean filled,Runnable r){TextView t=text(s,15,filled?Color.WHITE:RED,true);t.setGravity(Gravity.CENTER);t.setMinHeight(dp(49));t.setPadding(dp(12),dp(11),dp(12),dp(11));t.setBackground(bg(filled?0xffd62330:CARD,14));t.setClickable(true);t.setFocusable(true);t.setOnClickListener(v->r.run());return t;}
 void button(LinearLayout l,String s,boolean filled,Runnable r){l.addView(action(s,filled,r),new LinearLayout.LayoutParams(-1,-2));space(l,10);}
 void pair(LinearLayout parent,TextView a,TextView b){LinearLayout h=horizontal();weight(h,a);View gap=new View(this);h.addView(gap,new LinearLayout.LayoutParams(dp(10),1));weight(h,b);parent.addView(h);}
 void heading(String s,String link,Runnable r){LinearLayout h=horizontal();weight(h,text(s,21,WHITE,true));if(link!=null){TextView x=text(link,15,RED,true);x.setPadding(dp(12),dp(12),0,dp(12));x.setOnClickListener(v->r.run());h.addView(x);}root.addView(h);space(root,12);}
 String menuIcon(String key){switch(key){case "▤":return "wallet";case "⇄":return "transfer";case "⇣":return "download";case "⇡":return "upload";case "↻":return "refresh";case "✦":return "star";default:return key;}}
 SvgIcon icon(String key,int color,int size){return new SvgIcon(this,menuIcon(key),color,size);}
 FrameLayout iconTile(String key,int color,int size,int tile){FrameLayout b=new FrameLayout(this);b.setBackground(bg(0xff151516,13));SvgIcon v=icon(key,color,size);b.addView(v,new FrameLayout.LayoutParams(-1,-1));b.setLayoutParams(new LinearLayout.LayoutParams(dp(tile),dp(tile)));return b;}
 void menu(LinearLayout p,String key,String title,String right,Runnable r){
  LinearLayout h=horizontal();h.setMinimumHeight(dp(62));h.setPadding(dp(12),dp(9),dp(12),dp(9));h.setBackground(border(CARD,17,0xff151516));
  h.addView(iconTile(key,RED,25,44));TextView t=text(title,17,WHITE,false);t.setPadding(dp(17),0,dp(6),0);weight(h,t);if(right!=null)h.addView(text(right,13,MUTED,false));h.addView(icon("chevron",0xff747477,20),new LinearLayout.LayoutParams(dp(25),dp(38)));
  h.setClickable(true);h.setFocusable(true);h.setOnClickListener(v->r.run());p.addView(h,new LinearLayout.LayoutParams(-1,-2));space(p,11);
 }
 class BalanceArt extends Drawable{
  final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
  @Override public void draw(Canvas canvas){
   RectF r=new RectF(getBounds());float w=r.width(),h=r.height();if(w<=0||h<=0)return;
   canvas.save();canvas.translate(r.left,r.top);RectF rect=new RectF(0,0,w,h);float radius=dp(22);Path clip=new Path();clip.addRoundRect(rect,radius,radius,Path.Direction.CW);canvas.clipPath(clip);
   p.setShader(new LinearGradient(0,0,w,h,new int[]{0xff111112,0xff0b0b0c,0xff21090c},new float[]{0,.62f,1},Shader.TileMode.CLAMP));canvas.drawRect(rect,p);
   p.setShader(new RadialGradient(w*.98f,h*.22f,w*.53f,new int[]{0xffc51621,0xff5d1016,0x00120a0a},new float[]{0,.48f,1},Shader.TileMode.CLAMP));canvas.drawRect(rect,p);
   Path wave=new Path();wave.moveTo(w*.56f,h);wave.cubicTo(w*.72f,h*.93f,w*.74f,h*.65f,w*.87f,h*.57f);wave.cubicTo(w*.93f,h*.52f,w*.96f,h*.48f,w,h*.42f);wave.lineTo(w,h);wave.close();
   p.setShader(new LinearGradient(w*.55f,h*.45f,w,h,new int[]{0xff7e0b15,0xff240d10,0xff0d0b0c},null,Shader.TileMode.CLAMP));canvas.drawPath(wave,p);p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setColor(0xff262021);p.setStrokeWidth(dp(1));canvas.drawRoundRect(rect,radius,radius,p);p.setStyle(Paint.Style.FILL);canvas.restore();
  }
  @Override public void setAlpha(int a){p.setAlpha(a);invalidateSelf();}
  @Override public void setColorFilter(ColorFilter c){p.setColorFilter(c);invalidateSelf();}
  @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
 }
 void tab(LinearLayout bar,String key,String name,String dest,boolean active){
  LinearLayout t=col();t.setGravity(Gravity.CENTER);t.setMinimumHeight(dp(62));int color=active?RED:MUTED;
  t.addView(icon(key,color,23),new LinearLayout.LayoutParams(dp(28),dp(28)));space(t,4);TextView caption=text(name,10,color,active);caption.setGravity(Gravity.CENTER);t.addView(caption);t.setContentDescription(name);t.setClickable(true);t.setFocusable(true);t.setOnClickListener(v->nav(dest));bar.addView(t,new LinearLayout.LayoutParams(0,dp(62),1));
 }
 void bottomBar(LinearLayout outer){
  LinearLayout bar=horizontal();bar.setPadding(dp(5),dp(10),dp(5),dp(6));GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xff17090b,0xff090909,0xff050506});d.setCornerRadii(new float[]{dp(22),dp(22),dp(22),dp(22),0,0,0,0});d.setStroke(dp(1),0xff201619);bar.setBackground(d);
  tab(bar,"home","Главная","home",page.equals("home")||page.equals("accounts"));tab(bar,"chart","Аналитика","analytics",Arrays.asList("analytics","budget","category","calendar","day").contains(page));
  FrameLayout plusSlot=new FrameLayout(this);bar.addView(plusSlot,new LinearLayout.LayoutParams(0,dp(64),1));FrameLayout circle=new FrameLayout(this);circle.setBackground(bg(0xffff2b35,100));circle.addView(icon("plus",WHITE,31),new FrameLayout.LayoutParams(-1,-1));plusSlot.addView(circle,new FrameLayout.LayoutParams(dp(54),dp(54),Gravity.CENTER));circle.setContentDescription("Добавить операцию");circle.setClickable(true);circle.setFocusable(true);circle.setOnClickListener(v->chooseType());
  tab(bar,"list","Операции","history",page.equals("history"));tab(bar,"person","Профиль","settings",page.equals("settings"));outer.addView(bar,new LinearLayout.LayoutParams(-1,-2));
 }

 void nav(String target){page=target;accountFilter=null;historyLimit=100;selecting=false;selectedEntries.clear();detailDay=null;detailCategory=null;show();}
 void show(){
  LinearLayout outer=col();outer.setBackgroundColor(BG);outer.setLayoutParams(new ViewGroup.LayoutParams(-1,-1));
  ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setVerticalScrollBarEnabled(false);
  root=col();int pad=getResources().getConfiguration().screenWidthDp>=600?36:18;root.setPadding(dp(pad),dp(page.equals("home")?26:20),dp(pad),dp(23));sc.addView(root,new ScrollView.LayoutParams(-1,-2));outer.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
  bottomBar(outer);setContentView(outer);
  switch(page){case "accounts":accounts();break;case "history":history();break;case "analytics":analytics();break;case "category":detail();break;case "calendar":calendar();break;case "day":detail();break;case "budget":budgetPage();break;case "settings":settings();break;default:home();}
 }
 void title(String s){LinearLayout h=horizontal();if(!Arrays.asList("home","analytics","history","settings").contains(page)||page.equals("history")&&accountFilter!=null){SvgIcon back=icon("back",MUTED,25);back.setContentDescription("Назад");back.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);back.setOnClickListener(v->backPage());h.addView(back,new LinearLayout.LayoutParams(dp(37),dp(48)));}weight(h,text(s,29,WHITE,true));root.addView(h);space(root,20);}
 void home(){
  LinearLayout header=horizontal();weight(header,text("Мои финансы",29,WHITE,true));SvgIcon settings=icon("settings",0xff929295,24);settings.setContentDescription("Настройки");settings.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);settings.setOnClickListener(v->nav("settings"));header.addView(settings,new LinearLayout.LayoutParams(dp(40),dp(44)));root.addView(header);space(root,31);
  LinearLayout hero=col();hero.setBackground(new BalanceArt());hero.setPadding(dp(22),dp(24),dp(20),dp(24));hero.setMinimumHeight(dp(151));
  LinearLayout top=horizontal();weight(top,text("Общий баланс",15,0xffa9a6a8,false));SvgIcon eye=icon(hidden?"hidden":"eye",0xffa9a6a8,20);eye.setContentDescription(hidden?"Показать баланс":"Скрыть баланс");eye.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);eye.setOnClickListener(v->{hidden=!hidden;prefs.edit().putBoolean("hidden",hidden).apply();show();});top.addView(eye,new LinearLayout.LayoutParams(dp(34),dp(28)));hero.addView(top);space(hero,13);
  TextView sum=text(hidden?"••••••":money(ledger.total()),37,WHITE,true);sum.setSingleLine(true);sum.setAutoSizeTextTypeUniformWithConfiguration(20,37,1,android.util.TypedValue.COMPLEX_UNIT_SP);hero.addView(sum,new LinearLayout.LayoutParams(-1,dp(51)));space(hero,8);
  YearMonth now=YearMonth.now();long delta=ledger.sum(now,Ledger.IN)-ledger.sum(now,Ledger.OUT);long base=ledger.total()-delta;String change=delta==0?"":(delta>0?"▲ ":"▼ ");if(base>0&&delta!=0)change+=signed(delta)+String.format(RU," (%.1f%%)",100.0*delta/base);else change+=signed(delta);String sub=ledger.accounts.isEmpty()?"Добавьте первый счёт":hidden?"Изменение скрыто":change+" за месяц";label(hero,sub,14,ledger.accounts.isEmpty()?MUTED:RED,false);
  root.addView(hero,new LinearLayout.LayoutParams(-1,-2));space(root,25);
  menu(root,"wallet","Счета",null,()->nav("accounts"));
  menu(root,"transfer","Операции",null,()->{month=YearMonth.now();nav("history");});
  menu(root,"chart","Аналитика",null,()->nav("analytics"));
  menu(root,"target","Бюджет",null,()->{month=YearMonth.now();nav("budget");});
 }

 void accounts(){
  title("Счета");label(root,"Всего: "+(hidden?"••••••":money(ledger.total())),22,WHITE,true);space(root,18);
  for(Ledger.Account a:ledger.accounts.values()){
   LinearLayout l=col();LinearLayout h=horizontal();h.addView(iconTile(a.type.equals("Карта")?"card":a.type.equals("Наличные")?"cash":"bank",RED,23,42));LinearLayout left=col();left.setPadding(dp(12),0,dp(6),0);label(left,a.name,17,WHITE,true);space(left,4);String suffix="";for(Map.Entry<String,String> binding:ledger.bankAccounts.entrySet())if(binding.getValue().equals(a.id)){String number=binding.getKey();suffix=" · •••• "+number.substring(Math.max(0,number.length()-4));break;}label(left,a.type+suffix,13,MUTED,false);weight(h,left);h.addView(text(hidden?"••••":money(ledger.balance(a.id)),18,WHITE,true));l.addView(h);space(l,12);
   pair(l,action("Операции",false,()->{page="history";accountFilter=a.id;month=YearMonth.now();historyLimit=100;show();}),action("Настроить",false,()->accountDialog(a)));card(root,l);space(root,10);
  }
  if(ledger.accounts.isEmpty()){label(root,"Создайте первый кошелёк и укажите остаток до первой операции.",15,MUTED,false);space(root,15);}
  button(root,"+  Добавить счёт",true,()->accountDialog(null));label(root,"Если несколько карт привязаны к одному банковскому счёту, учитывайте его как один кошелёк. Иначе общий баланс будет завышен.",13,MUTED,false);
 }
 void monthControl(){LinearLayout h=horizontal();h.addView(action("‹",false,()->{month=month.minusMonths(1);historyLimit=100;show();}),new LinearLayout.LayoutParams(dp(48),dp(48)));TextView t=text(monthName(),17,WHITE,true);t.setGravity(Gravity.CENTER);weight(h,t);h.addView(action("›",false,()->{month=month.plusMonths(1);historyLimit=100;show();}),new LinearLayout.LayoutParams(dp(48),dp(48)));root.addView(h);space(root,18);}
 void history(){
  title(accountFilter==null?"Операции":accountName(accountFilter));monthControl();if(accountFilter!=null)button(root,"Все счета",false,()->{accountFilter=null;show();});
  List<Ledger.Entry> list=new ArrayList<>();for(Ledger.Entry e:ledger.sorted())if(YearMonth.from(e.date).equals(month)&&(accountFilter==null||accountFilter.equals(e.from)||accountFilter.equals(e.to)))list.add(e);
  renderEntries(list);
  if(!selecting){space(root,12);button(root,"+  Добавить операцию",true,()->chooseType());}
 }
 void renderEntries(List<Ledger.Entry> list){
  visibleEntries=list;selectedEntries.retainAll(list.stream().map(e->e.id).collect(java.util.stream.Collectors.toSet()));
  if(list.isEmpty()){label(root,"Нет операций за выбранный период",15,MUTED,false);return;}
  if(selecting){
   pair(root,action("Выбрать все",false,()->{for(Ledger.Entry e:list)selectedEntries.add(e.id);show();}),action("Отмена",false,()->{selecting=false;selectedEntries.clear();show();}));space(root,8);
   button(root,"Изменить категорию · "+selectedEntries.size(),true,()->bulkCategory());
  }else button(root,"Выбрать операции",false,()->{selecting=true;show();});
  LocalDate last=null;int count=0;for(Ledger.Entry e:list){if(count++>=historyLimit)break;if(!e.date.equals(last)){space(root,15);label(root,e.date.format(DateTimeFormatter.ofPattern("d MMMM, EEEE",RU)),13,MUTED,true);space(root,5);last=e.date;}entryRow(e);}
  if(list.size()>historyLimit)button(root,"Показать ещё",false,()->{historyLimit+=100;show();});
 }
 void bulkCategory(){
  if(!writable()||selectedEntries.isEmpty()){toast("Выберите операции");return;}
  final ArrayList<Ledger.Entry> batch=new ArrayList<>();String type=null;
  for(String id:selectedEntries){Ledger.Entry e=ledger.entries.get(id);if(e==null)continue;if(Ledger.MOVE.equals(e.type)){toast("Переводы между своими счетами не имеют категории");return;}if(type!=null&&!type.equals(e.type)){toast("Выберите только доходы или только расходы");return;}type=e.type;batch.add(e);}
  if(batch.isEmpty())return;final boolean incoming=Ledger.IN.equals(type);
  categoryPicker(incoming,"",cat->new AlertDialog.Builder(this).setTitle("Изменить "+batch.size()+" операций?").setMessage("Новая категория: "+cat+". Суммы, счета и даты сохранятся.").setNegativeButton("Отмена",null).setPositiveButton("Применить",(d,w)->{if(!writable())return;for(Ledger.Entry e:batch){ledger.put(new Ledger.Entry(e.id,e.type,e.from,e.to,cat,e.note,e.date,e.amount));learn(e,cat);}save();selecting=false;selectedEntries.clear();show();}).show());
 }
 void backPage(){if(selecting){selecting=false;selectedEntries.clear();show();return;}if(page.equals("day")){page="calendar";show();}else if(page.equals("category")||page.equals("calendar"))nav("analytics");else if(page.equals("history")&&accountFilter!=null)nav("accounts");else nav("home");}

 void chooseType(){
  LinearLayout l=panel();AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Новая операция").setView(l).setNegativeButton("Отмена",null).create();
  menu(l,"minus","Расход",null,()->{dialog.dismiss();entryDialog(Ledger.OUT,null);});menu(l,"plus","Доход",null,()->{dialog.dismiss();entryDialog(Ledger.IN,null);});menu(l,"transfer","Перевод",null,()->{dialog.dismiss();entryDialog(Ledger.MOVE,null);});
  dialog.show();
 }
 void entryRow(Ledger.Entry e){
  boolean transfer=Ledger.MOVE.equals(e.type), income=Ledger.IN.equals(e.type);LinearLayout h=horizontal();h.setPadding(0,dp(12),0,dp(12));h.setMinimumHeight(dp(66));
  h.addView(operationIcon(e));
  LinearLayout left=col();left.setPadding(dp(12),0,dp(6),0);label(left,transfer?"Перевод":e.category,16,WHITE,true);space(left,5);
  String details=e.date.format(DateTimeFormatter.ofPattern("d MMM",RU))+" · "+(transfer?accountName(e.from)+" → "+accountName(e.to):accountName(income?e.to:e.from));if(!e.note.isEmpty())details+=" · "+e.note;TextView dt=text(details,12,MUTED,false);dt.setMaxLines(2);dt.setEllipsize(android.text.TextUtils.TruncateAt.END);left.addView(dt);weight(h,left);
  TextView sum=text(hidden?"••••":transfer?money(e.amount):signed(income?e.amount:-e.amount),16,income?WHITE:transfer?MUTED:RED,true);h.addView(sum);if(selecting){CheckBox cb=new CheckBox(this);cb.setButtonTintList(ColorStateList.valueOf(RED));cb.setChecked(selectedEntries.contains(e.id));cb.setOnClickListener(v->{if(cb.isChecked())selectedEntries.add(e.id);else selectedEntries.remove(e.id);show();});h.addView(cb);h.setOnClickListener(v->{if(!selectedEntries.add(e.id))selectedEntries.remove(e.id);show();});}else h.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Операция").setItems(new String[]{"Изменить","Удалить"},(d,i)->{if(i==0)entryDialog(e.type,e);else if(writable())new AlertDialog.Builder(this).setMessage("Удалить операцию? Балансы и отчёты пересчитаются.").setNegativeButton("Отмена",null).setPositiveButton("Удалить",(a,b)->{ledger.entries.remove(e.id);save();show();}).show();}).show());h.setOnLongClickListener(v->{selecting=true;selectedEntries.add(e.id);show();return true;});root.addView(h);line(root);
 }
 void progress(LinearLayout l,long spent,long limit){ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(1000);bar.setProgress(limit<=0?0:(int)Math.min(1000,Math.max(0,spent*1000.0/limit)));bar.setProgressTintList(ColorStateList.valueOf(RED));bar.setProgressBackgroundTintList(ColorStateList.valueOf(LINE));l.addView(bar,new LinearLayout.LayoutParams(-1,dp(10)));}
 ReportPeriod report(){return ReportPeriod.of(reportMode,anchor,customStart,customEnd);}
 String rangeLabel(ReportPeriod p){if(reportMode.equals("month"))return YearMonth.from(p.start).format(DateTimeFormatter.ofPattern("LLLL yyyy",RU));if(reportMode.equals("year"))return Integer.toString(p.start.getYear());return p.start.format(DateTimeFormatter.ofPattern("dd.MM.yy"))+" — "+p.end.format(DateTimeFormatter.ofPattern("dd.MM.yy"));}
 void pickDate(LocalDate initial,java.util.function.Consumer<LocalDate> cb){DatePickerDialog d=new DatePickerDialog(this,(v,y,m,day)->cb.accept(LocalDate.of(y,m+1,day)),initial.getYear(),initial.getMonthValue()-1,initial.getDayOfMonth());d.show();}
 void setCustom(){pickDate(customStart,start->pickDate(customEnd.isBefore(start)?start:customEnd,end->{if(end.isBefore(start)){toast("Конец периода раньше начала");return;}customStart=start;customEnd=end;reportMode="custom";show();}));}
 void shift(int n){switch(reportMode){case "week":anchor=anchor.plusWeeks(n);break;case "year":anchor=anchor.plusYears(n);break;case "custom":long days=java.time.temporal.ChronoUnit.DAYS.between(customStart,customEnd)+1;customStart=customStart.plusDays(n*days);customEnd=customEnd.plusDays(n*days);break;default:anchor=anchor.plusMonths(n);}show();}
 void periodBar(){
  String[] keys={"week","month","year","custom"},names={"Неделя","Месяц","Год","Период"};LinearLayout h=horizontal();
  for(int i=0;i<keys.length;i++){final String k=keys[i];TextView t=action(names[i],reportMode.equals(k),()->{if(k.equals("custom"))setCustom();else{reportMode=k;show();}});t.setTextSize(12);t.setPadding(dp(3),dp(8),dp(3),dp(8));h.addView(t,new LinearLayout.LayoutParams(0,dp(42),1));}root.addView(h);space(root,12);
  LinearLayout dates=horizontal();dates.addView(action("‹",false,()->shift(-1)),new LinearLayout.LayoutParams(dp(42),dp(44)));TextView t=text(rangeLabel(report()),15,WHITE,true);t.setGravity(Gravity.CENTER);weight(dates,t);dates.addView(action("›",false,()->shift(1)),new LinearLayout.LayoutParams(dp(42),dp(44)));root.addView(dates);space(root,15);
 }
 int categoryColor(String name){List<String> list=CategoryCatalog.names(reportType.equals(Ledger.IN));int i=list.indexOf(name);return palette[Math.floorMod(i<0?name.hashCode():i,palette.length)];}
 class Ring extends View{
  final List<String> names;final Map<String,Long> values;final long total;final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
  Ring(List<String> n,Map<String,Long> v,long sum){super(MainActivity.this);names=n;values=v;total=sum;setContentDescription("Диаграмма "+(reportType.equals(Ledger.OUT)?"расходов":"доходов")+", "+money(total)+". Категории доступны ниже.");}
  @Override protected void onDraw(Canvas c){super.onDraw(c);float x=getWidth()/2f,y=getHeight()/2f,stroke=dp(23),r=Math.min(x,y)-stroke/2-dp(12);if(r<=0)return;RectF rect=new RectF(x-r,y-r,x+r,y+r);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(stroke);p.setColor(LINE);c.drawCircle(x,y,r,p);float angle=-90;for(String name:names){float sweep=total<=0?0:360f*values.get(name)/total;p.setColor(categoryColor(name));float gap=Math.min(2f,sweep*.15f);c.drawArc(rect,angle+gap/2,sweep-gap,false,p);angle+=sweep;}
   p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));p.setColor(MUTED);p.setTextSize(dp(13));c.drawText(reportType.equals(Ledger.OUT)?"Расходы":"Доходы",x,y-dp(13),p);p.setColor(WHITE);p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));p.setTextSize(dp(25));String value=hidden?"••••":money(total);while(p.measureText(value)>r*1.65f&&p.getTextSize()>dp(12))p.setTextSize(p.getTextSize()-1);c.drawText(value,x,y+dp(16),p);
  }
  @Override public boolean onTouchEvent(android.view.MotionEvent e){if(e.getAction()!=MotionEvent.ACTION_UP)return true;float x=e.getX()-getWidth()/2f,y=e.getY()-getHeight()/2f,r=Math.min(getWidth(),getHeight())/2f-dp(23)/2f-dp(12);if(Math.abs(Math.hypot(x,y)-r)>dp(19)||total<=0)return true;double a=(Math.toDegrees(Math.atan2(y,x))+450)%360;double running=0;for(String name:names){running+=360.0*values.get(name)/total;if(a<running){openCategory(name);break;}}return true;}
 }
 void openCategory(String name){detailCategory=name;detailDay=null;page="category";selecting=false;selectedEntries.clear();historyLimit=100;show();}
 void analytics(){
  title("Аналитика");periodBar();pair(root,action("Расходы",reportType.equals(Ledger.OUT),()->{reportType=Ledger.OUT;show();}),action("Доходы",reportType.equals(Ledger.IN),()->{reportType=Ledger.IN;show();}));space(root,8);
  ReportPeriod p=report();Map<String,Long> by=p.categories(ledger,reportType);long total=p.sum(ledger,reportType);
  ArrayList<String> names=new ArrayList<>(by.keySet());names.sort((a,b)->Long.compare(by.get(b),by.get(a)));
  root.addView(new Ring(names,by,total),new LinearLayout.LayoutParams(-1,dp(235)));
  long previous=p.previous(reportMode).sum(ledger,reportType);String compared=reportMode.equals("month")?"К прошлому месяцу":"К предыдущему периоду";label(root,compared+" · "+(hidden?"••••":signed(total-previous)+(previous>0?String.format(RU," (%+.1f%%)",100.0*(total-previous)/previous):"")),13,MUTED,false);space(root,18);
  heading("Категории",null,null);
  for(String name:names){LinearLayout h=horizontal();h.setMinimumHeight(dp(58));h.setPadding(0,dp(5),0,dp(5));int color=categoryColor(name);h.addView(iconTile(CategoryCatalog.icon(name),color,23,40));LinearLayout l=col();l.setPadding(dp(10),0,dp(5),0);label(l,name,15,WHITE,false);space(l,4);label(l,String.format(RU,"%.1f%%",100.0*by.get(name)/total),12,MUTED,false);weight(h,l);h.addView(text(hidden?"••••":money(by.get(name)),15,WHITE,true));h.addView(icon("chevron",MUTED,17),new LinearLayout.LayoutParams(dp(22),dp(38)));h.setOnClickListener(v->openCategory(name));root.addView(h);line(root);}
  if(names.isEmpty())label(root,"Нет операций за выбранный период",14,MUTED,false);
  space(root,16);menu(root,"calendar","Календарь расходов",null,()->{calendarMonth=YearMonth.from(p.start);nav("calendar");});menu(root,"target","Бюджет",null,()->nav("budget"));menu(root,"star","Финансовый помощник",null,()->{month=YearMonth.from(anchor);advice();});
 }
 void detail(){
  boolean day=page.equals("day");ReportPeriod p=day?new ReportPeriod(detailDay,detailDay):report();String type=day?Ledger.OUT:reportType,cat=day?null:detailCategory;
  title(day?detailDay.format(DateTimeFormatter.ofPattern("d MMMM yyyy",RU)):cat);List<Ledger.Entry> list=p.entries(ledger,type,cat);long sum=0;for(Ledger.Entry e:list)sum+=e.amount;
  LinearLayout l=col();label(l,day?"Расходы за день":rangeLabel(p),14,MUTED,false);space(l,7);label(l,hidden?"••••":money(sum),28,WHITE,true);space(l,6);label(l,list.size()+" операций",13,MUTED,false);card(root,l);space(root,12);renderEntries(list);
 }
 void calendar(){
  title("Календарь");LinearLayout h=horizontal();h.addView(action("‹",false,()->{calendarMonth=calendarMonth.minusMonths(1);show();}),new LinearLayout.LayoutParams(dp(44),dp(44)));TextView m=text(calendarMonth.format(DateTimeFormatter.ofPattern("LLLL yyyy",RU)),17,WHITE,true);m.setGravity(Gravity.CENTER);weight(h,m);h.addView(action("›",false,()->{calendarMonth=calendarMonth.plusMonths(1);show();}),new LinearLayout.LayoutParams(dp(44),dp(44)));root.addView(h);space(root,16);
  Map<LocalDate,Long> days=new HashMap<>();for(Ledger.Entry e:ledger.entries.values())if(Ledger.OUT.equals(e.type)&&YearMonth.from(e.date).equals(calendarMonth))days.merge(e.date,e.amount,Long::sum);
  long max=0,total=0;for(long v:days.values()){max=Math.max(max,v);total+=v;}label(root,"Расходы за месяц · "+(hidden?"••••":money(total)),16,WHITE,true);space(root,15);LinearLayout weekdays=horizontal();for(String name:new String[]{"Пн","Вт","Ср","Чт","Пт","Сб","Вс"}){TextView t=text(name,12,MUTED,false);t.setGravity(Gravity.CENTER);weight(weekdays,t);}root.addView(weekdays);space(root,8);
  int offset=calendarMonth.atDay(1).getDayOfWeek().getValue()-1;
  for(int row=0;row<6;row++){if(row*7-offset>=calendarMonth.lengthOfMonth())break;LinearLayout week=horizontal();for(int col=0;col<7;col++){int d=row*7+col-offset+1;LinearLayout cell=col();cell.setGravity(Gravity.CENTER);cell.setMinimumHeight(dp(54));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(58),1);lp.setMargins(dp(2),dp(2),dp(2),dp(2));week.addView(cell,lp);if(d<1||d>calendarMonth.lengthOfMonth())continue;LocalDate date=calendarMonth.atDay(d);long v=days.getOrDefault(date,0L);int red=max==0?30:30+(int)(150.0*v/max);cell.setBackground(bg(v==0?CARD:Color.rgb(red,16,23),9));label(cell,String.valueOf(d),15,WHITE,true);space(cell,4);if(v>0)label(cell,hidden?"••":v>=100000?String.format(RU,"%.1fк",v/100000.0):String.valueOf(v/100),10,WHITE,false);cell.setContentDescription(date+" "+money(v));cell.setOnClickListener(x->{detailDay=date;detailCategory=null;page="day";selecting=false;selectedEntries.clear();historyLimit=100;show();});}root.addView(week);}
  space(root,10);label(root,"Нажмите на день, чтобы посмотреть расходы. Чем ярче цвет, тем больше сумма.",13,MUTED,false);
 }

 void budgetPage(){
  title("Бюджет");monthControl();long out=ledger.sum(month,Ledger.OUT),lim=ledger.limit(month,"");
  heading("Бюджет","Настроить",()->limitDialog(""));LinearLayout b=col();label(b,lim==0?"Лимит не задан":"Осталось",14,MUTED,false);space(b,8);label(b,lim==0?"—":money(lim-out),26,lim>0&&out>lim?RED:WHITE,true);space(b,10);if(lim>0){progress(b,out,lim);space(b,8);label(b,money(out)+" из "+money(lim),13,MUTED,false);if(month.equals(YearMonth.now())){space(b,8);label(b,"Лимит до конца месяца: "+money(ledger.daily(month,LocalDate.now()))+" / день",14,WHITE,false);}}else label(b,"Установите сумму, которую планируете потратить.",14,MUTED,false);card(root,b);space(root,18);
  heading("Категории","+ Лимит",()->chooseCategoryLimit());SortedMap<String,Long> by=ledger.categories(month);Set<String> cats=new LinkedHashSet<>(by.keySet());String prefix=month+"|";for(String k:ledger.limits.keySet())if(k.startsWith(prefix)&&k.length()>prefix.length())cats.add(k.substring(prefix.length()));
  ArrayList<String> sorted=new ArrayList<>(cats);sorted.sort((a,d)->Long.compare(by.getOrDefault(d,0L),by.getOrDefault(a,0L)));
  for(String name:sorted){long spent=by.getOrDefault(name,0L),limit=ledger.limit(month,name);LinearLayout x=col();LinearLayout h=horizontal();weight(h,text(name,15,WHITE,true));h.addView(text(money(spent),15,RED,true));x.addView(h);space(x,6);progress(x,spent,limit>0?limit:out);space(x,5);label(x,limit>0?"Лимит "+money(limit)+(spent>limit?" · Перерасход":""):(out>0?String.format(RU,"%.0f%% расходов",100.0*spent/out):"Лимит не задан"),12,MUTED,false);x.setOnClickListener(v->limitDialog(name));card(root,x);space(root,8);}
  if(cats.isEmpty())label(root,"Здесь появятся категории ваших расходов.",14,MUTED,false);
  space(root,17);menu(root,"✦","Финансовый помощник",null,()->advice());
 }
 void settings(){
  title("Профиль");menu(root,"▤","Управление счетами",null,()->nav("accounts"));
  menu(root,"⇣","Импорт выписки XLSX / CSV",null,()->openDocument(13));
  menu(root,"⇡","Сохранить резервную копию",null,()->exportBackup());
  menu(root,"↻","Восстановить копию",null,()->openDocument(12));
  LinearLayout l=col();Switch s=new Switch(this);s.setText("Защищать экран от скриншотов");s.setTextColor(WHITE);s.setChecked(secure);s.setThumbTintList(ColorStateList.valueOf(RED));s.setOnCheckedChangeListener((v,on)->{secure=on;prefs.edit().putBoolean("secure",secure).apply();security();});l.addView(s);space(l,12);label(l,"Данные хранятся на устройстве. Интернет и банковские разрешения не запрашиваются.",13,MUTED,false);card(root,l);space(root,16);label(root,"Мои финансы · v0.7",14,MUTED,false);space(root,8);label(root,"Подключение банков и нейросети — будущие этапы.",13,MUTED,false);
 }
 LinearLayout panel(){LinearLayout l=col();l.setPadding(dp(22),dp(6),dp(22),dp(8));return l;}
 EditText field(LinearLayout l,String title,String value,boolean numeric){space(l,9);label(l,title,13,MUTED,false);EditText e=new EditText(this);e.setSingleLine(true);e.setText(value);if(numeric)e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL|android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);l.addView(e);return e;}
 Spinner spinner(LinearLayout l,String label,List<String> items,int selected){space(l,9);label(l,label,13,MUTED,false);Spinner sp=new Spinner(this);ArrayAdapter<String> ad=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,items);ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);sp.setAdapter(ad);sp.setSelection(Math.max(0,Math.min(items.size()-1,selected)));l.addView(sp,new LinearLayout.LayoutParams(-1,dp(48)));return sp;}
 View dialogView(LinearLayout l){ScrollView sc=new ScrollView(this);sc.addView(l);return sc;}
 String value(EditText e){return e.getText().toString().trim();}
 interface Checked {void run() throws Exception;}
 void formDialog(String title,LinearLayout l,String extra,Runnable extraAction,Checked submit){AlertDialog.Builder b=new AlertDialog.Builder(this).setTitle(title).setView(dialogView(l)).setNegativeButton("Отмена",null).setPositiveButton("Сохранить",null);if(extra!=null)b.setNeutralButton(extra,(d,w)->extraAction.run());AlertDialog a=b.create();a.setOnShowListener(d->a.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{submit.run();a.dismiss();}catch(Exception ex){error(ex);}}));a.show();}
 void accountDialog(Ledger.Account old){
  if(!writable())return;LinearLayout l=panel();EditText name=field(l,"Название",old==null?"":old.name,false);Spinner type=spinner(l,"Тип",Arrays.asList(TYPES),old==null?0:Arrays.asList(TYPES).indexOf(old.type));EditText start=field(l,"Остаток до первой операции, ₽",old==null?"0":number(old.opening),true);space(l,7);label(l,"Начальный остаток не входит в доходы. Для долга можно указать отрицательную сумму.",12,MUTED,false);
  formDialog(old==null?"Добавить счёт":"Настроить счёт",l,old==null?null:"Удалить",()->new AlertDialog.Builder(this).setMessage("Удалить счёт без операций?").setNegativeButton("Отмена",null).setPositiveButton("Удалить",(d,w)->{try{ledger.removeAccount(old.id);save();show();}catch(Exception e){error(e);}}).show(),()->{ledger.put(new Ledger.Account(old==null?uid():old.id,value(name),TYPES[type.getSelectedItemPosition()],amount(value(start))));save();show();});
 }
 String category(String s,boolean income){s=s.toLowerCase(RU);if(income){if(s.contains("зарплат"))return "Зарплата";if(s.contains("кешбэк")||s.contains("кэшбэк"))return "Кешбэк";if(s.contains("преми"))return "Премия";if(s.contains("аванс"))return "Аванс";return "Прочий доход";}if(s.matches(".*(продукт|магазин|супермаркет|пятероч|пятёроч|магнит).*"))return "Продукты";if(s.matches(".*(такси|метро|азс|транспорт|автобус).*"))return "Транспорт";if(s.matches(".*(кафе|ресторан|кофе).*"))return "Кафе и рестораны";if(s.matches(".*(аптек|здоров).*"))return "Здоровье";if(s.matches(".*(жкх|аренд|электрич).*"))return "Жильё и ЖКХ";if(s.matches(".*(интернет|мобильн|связь).*"))return "Связь и интернет";return "Прочее";}
 void categoryPicker(boolean income,String selected,java.util.function.Consumer<String> choose){
  LinearLayout l=panel();AlertDialog d=new AlertDialog.Builder(this).setTitle(income?"Категория дохода":"Категория расхода").setView(dialogView(l)).setNegativeButton("Отмена",null).create();
  Set<String> known=new LinkedHashSet<>();String[][] groups=income?CategoryCatalog.INCOME:CategoryCatalog.EXPENSE;
  for(String[] group:groups){label(l,group[0],13,MUTED,true);space(l,8);for(int i=1;i<group.length;i++){final String name=group[i];known.add(name);menu(l,CategoryCatalog.icon(name),name,selected.equals(name)?"✓":null,()->{d.dismiss();choose.accept(name);});}space(l,8);}
  Set<String> custom=new LinkedHashSet<>();for(Ledger.Entry e:ledger.entries.values())if((income?Ledger.IN:Ledger.OUT).equals(e.type)&&!known.contains(e.category))custom.add(e.category);if(!selected.isEmpty()&&!known.contains(selected))custom.add(selected);
  if(!custom.isEmpty()){label(l,"Мои категории",13,MUTED,true);space(l,8);for(String name:custom)menu(l,CategoryCatalog.icon(name),name,name.equals(selected)?"✓":null,()->{d.dismiss();choose.accept(name);});}
  button(l,"+  Своя категория",false,()->{d.dismiss();LinearLayout p=panel();EditText e=field(p,"Название","",false);formDialog("Своя категория",p,null,null,()->{String n=value(e);if(n.isEmpty()||n.length()>60)throw new IllegalArgumentException("Введите название до 60 символов");choose.accept(n);});});d.show();
 }
 void entryDialog(String kind,Ledger.Entry old){
  if(!writable())return;
  if(ledger.accounts.isEmpty()){
   new AlertDialog.Builder(this).setTitle("Добавьте первый счёт").setMessage("Выберите, где находятся деньги: карта, наличные или счёт.").setNegativeButton("Отмена",null).setPositiveButton("Добавить",(d,w)->accountDialog(null)).show();return;
  }
  final boolean move=kind.equals(Ledger.MOVE),income=kind.equals(Ledger.IN);
  if(move&&ledger.accounts.size()<2){toast("Для перевода нужны два разных счёта");return;}
  ArrayList<String> ids=new ArrayList<>(ledger.accounts.keySet()),names=new ArrayList<>();for(String id:ids){Ledger.Account ac=ledger.accounts.get(id);names.add(ac.name+" • "+(hidden?"••••":money(ledger.balance(id))));}
  LinearLayout shell=col();shell.setBackgroundColor(BG);LinearLayout l=col();l.setPadding(dp(20),dp(18),dp(20),dp(24));
  Dialog d=new Dialog(this);d.requestWindowFeature(Window.FEATURE_NO_TITLE);
  LinearLayout head=horizontal();head.setPadding(dp(14),dp(12),dp(14),dp(4));SvgIcon back=icon("back",MUTED,25);back.setContentDescription("Отмена");back.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);back.setOnClickListener(v->d.dismiss());head.addView(back,new LinearLayout.LayoutParams(dp(44),dp(48)));weight(head,text(old==null?(move?"Перевод":income?"Доход":"Расход"):"Изменить операцию",24,WHITE,true));shell.addView(head);
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(l);shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout amountCard=col();amountCard.setPadding(dp(18),dp(18),dp(18),dp(12));amountCard.setBackground(new BalanceArt());label(amountCard,"Сумма, ₽",14,0xffded0d0,false);space(amountCard,8);
  EditText sum=new EditText(this);sum.setSingleLine(true);sum.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);sum.setTextSize(36);sum.setTextColor(WHITE);sum.setHintTextColor(0xffa08b8e);sum.setBackgroundColor(Color.TRANSPARENT);sum.setHint("0");sum.setText(old==null?"":number(old.amount));sum.setPadding(0,dp(6),0,dp(6));sum.setSelectAllOnFocus(true);amountCard.addView(sum);l.addView(amountCard);space(l,18);
  int selected=Math.max(0,ids.indexOf(old==null?accountFilter:income?old.to:old.from));Spinner first=spinner(l,move?"Со счёта":income?"На счёт":"Счёт списания",names,selected);
  Spinner second=move?spinner(l,"На счёт",names,old==null?(selected+1)%ids.size():ids.indexOf(old.to)):null;
  final String[] categoryValue={old==null||move?"":old.category};
  if(!move){space(l,14);label(l,"Категория",13,MUTED,false);space(l,8);LinearLayout catRow=horizontal();catRow.setPadding(dp(12),dp(8),dp(12),dp(8));catRow.setMinimumHeight(dp(58));catRow.setBackground(bg(CARD,16));FrameLayout tile=iconTile(CategoryCatalog.icon(categoryValue[0]),RED,23,40);catRow.addView(tile);TextView chosen=text(categoryValue[0].isEmpty()?"Выбрать категорию":categoryValue[0],16,WHITE,false);chosen.setPadding(dp(12),0,0,0);weight(catRow,chosen);catRow.addView(icon("chevron",MUTED,19),new LinearLayout.LayoutParams(dp(24),dp(36)));catRow.setOnClickListener(v->categoryPicker(income,categoryValue[0],name->{categoryValue[0]=name;chosen.setText(name);tile.removeAllViews();tile.addView(icon(CategoryCatalog.icon(name),RED,23),new FrameLayout.LayoutParams(-1,-1));}));l.addView(catRow);}
  final LocalDate[] selectedDate={old==null?LocalDate.now():old.date};space(l,14);label(l,"Дата",13,MUTED,false);space(l,8);LinearLayout day=horizontal();day.setPadding(dp(12),dp(8),dp(12),dp(8));day.setMinimumHeight(dp(54));day.setBackground(bg(CARD,16));day.addView(iconTile("calendar",RED,23,38));TextView dateText=text(selectedDate[0].format(DateTimeFormatter.ofPattern("d MMMM yyyy",RU)),16,WHITE,false);dateText.setPadding(dp(12),0,0,0);weight(day,dateText);day.addView(icon("chevron",MUTED,19),new LinearLayout.LayoutParams(dp(24),dp(36)));day.setOnClickListener(v->{LocalDate current=selectedDate[0];DatePickerDialog picker=new DatePickerDialog(this,(p,y,m,dayOfMonth)->{selectedDate[0]=LocalDate.of(y,m+1,dayOfMonth);dateText.setText(selectedDate[0].format(DateTimeFormatter.ofPattern("d MMMM yyyy",RU)));},current.getYear(),current.getMonthValue()-1,current.getDayOfMonth());picker.getDatePicker().setMaxDate(System.currentTimeMillis());picker.show();});l.addView(day);
  space(l,8);EditText note=field(l,"Комментарий (необязательно)",old==null?"":old.note,false);note.setHint(move?"Например, снятие наличных":"Например, покупка в магазине");space(l,14);if(move)label(l,"Перевод не увеличивает доходы или расходы. Комиссию банка добавьте отдельным расходом.",13,MUTED,false);
  LinearLayout foot=col();foot.setPadding(dp(20),dp(8),dp(20),dp(14));button(foot,old==null?"Сохранить операцию":"Сохранить изменения",true,()->{try{
   long n=amount(value(sum));if(n<=0)throw new IllegalArgumentException("Введите сумму больше нуля");
   String a=ids.get(first.getSelectedItemPosition()),other=move?ids.get(second.getSelectedItemPosition()):null,desc=value(note),c=move?"":categoryValue[0];
   if(!move&&c.isEmpty())throw new IllegalArgumentException("Выберите категорию");
   ledger.put(new Ledger.Entry(old==null?uid():old.id,kind,income?null:a,move?other:income?a:null,c,desc,selectedDate[0],n));save();month=YearMonth.from(selectedDate[0]);d.dismiss();show();
  }catch(Exception e){error(e);}});shell.addView(foot);
  d.setContentView(shell);Window w=d.getWindow();if(w!=null){w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(BG));w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);if(secure)w.addFlags(WindowManager.LayoutParams.FLAG_SECURE);w.setStatusBarColor(BG);w.setNavigationBarColor(BG);}shell.setFocusableInTouchMode(true);shell.requestFocus();d.show();if(w!=null)w.setLayout(-1,-1);
 }

 void chooseCategoryLimit(){if(!writable())return;LinkedHashSet<String> set=new LinkedHashSet<>(Arrays.asList(CATEGORIES));set.addAll(ledger.categories(month).keySet());ArrayList<String> list=new ArrayList<>(set);list.add("Своя категория");new AlertDialog.Builder(this).setTitle("Категория бюджета").setItems(list.toArray(new String[0]),(d,i)->{if(i==list.size()-1){LinearLayout l=panel();EditText e=field(l,"Название категории","",false);formDialog("Новая категория",l,null,null,()->{String c=value(e);if(c.isEmpty())throw new IllegalArgumentException("Введите название");l.post(()->limitDialog(c));});}else limitDialog(list.get(i));}).show();}
 void limitDialog(String category){if(!writable())return;LinearLayout l=panel();long lim=ledger.limit(month,category);EditText e=field(l,"Лимит в рублях (0 — убрать)",lim==0?"":number(lim),true);formDialog(category.isEmpty()?"Бюджет на "+monthName():category,l,null,null,()->{ledger.setLimit(month,category,amount(value(e)));save();show();});}
 void advice(){long out=ledger.sum(month,Ledger.OUT),in=ledger.sum(month,Ledger.IN),lim=ledger.limit(month,"");StringBuilder s=new StringBuilder("За "+monthName().toLowerCase(RU)+" учтено "+money(out)+" расходов.\n\n");if(lim==0)s.append("Задайте общий лимит, чтобы отслеживать бюджет. ");else if(out>lim)s.append("Бюджет превышен на "+money(out-lim)+". Проверьте необязательные траты. ");else{s.append("Осталось "+money(lim-out)+". ");if(month.equals(YearMonth.now()))s.append("До конца месяца можно расходовать не более "+money(ledger.daily(month,LocalDate.now()))+" в день по плану. ");}
  if(out>in)s.append("\n\nРасходы выше учтённых доходов. Проверьте полноту записей.");SortedMap<String,Long> by=ledger.categories(month);if(!by.isEmpty()){Map.Entry<String,Long> max=Collections.max(by.entrySet(),Map.Entry.comparingByValue());s.append("\n\nСамая крупная категория: "+max.getKey()+" ("+money(max.getValue())+").");}for(Map.Entry<String,Long> c:by.entrySet()){long n=ledger.limit(month,c.getKey());if(n>0&&c.getValue()>n)s.append("\nЛимит «"+c.getKey()+"» превышен на "+money(c.getValue()-n)+".");}
  s.append("\n\nЭто локальные расчёты, а не нейросеть. Лимит бюджета не равен доступным деньгам.");new AlertDialog.Builder(this).setTitle("Финансовый помощник").setMessage(s.toString()).setPositiveButton("Понятно",null).show();
 }
 String encode(Ledger model)throws Exception{JSONObject obj=new JSONObject();obj.put("schema",2);JSONArray a=new JSONArray(),e=new JSONArray(),im=new JSONArray();JSONObject limits=new JSONObject();for(Ledger.Account x:model.accounts.values()){JSONObject o=new JSONObject();o.put("id",x.id).put("name",x.name).put("type",x.type).put("opening",x.opening);a.put(o);}for(Ledger.Entry x:model.entries.values()){JSONObject o=new JSONObject();o.put("id",x.id).put("type",x.type).put("from",x.from==null?JSONObject.NULL:x.from).put("to",x.to==null?JSONObject.NULL:x.to).put("category",x.category).put("note",x.note).put("date",x.date.toString()).put("amount",x.amount);e.put(o);}for(Map.Entry<String,Long> x:model.limits.entrySet())limits.put(x.getKey(),x.getValue());for(String x:model.imports)im.put(x);obj.put("accounts",a).put("entries",e).put("limits",limits).put("imports",im).put("bankAccounts",new JSONObject(model.bankAccounts)).put("bankRows",new JSONObject(model.bankRows));return obj.toString();}
 String nullable(JSONObject o,String k)throws Exception{return o.isNull(k)?null:o.getString(k);}
 Ledger decode(String raw)throws Exception{JSONObject obj=new JSONObject(raw);if(obj.getInt("schema")!=2)throw new IllegalArgumentException("Неподдерживаемый формат копии");Ledger m=new Ledger();JSONArray a=obj.getJSONArray("accounts"),e=obj.getJSONArray("entries");for(int i=0;i<a.length();i++){JSONObject x=a.getJSONObject(i);String id=x.getString("id");if(m.accounts.containsKey(id))throw new IllegalArgumentException("Повтор счёта");m.put(new Ledger.Account(id,x.getString("name"),x.getString("type"),x.getLong("opening")));}for(int i=0;i<e.length();i++){JSONObject x=e.getJSONObject(i);String id=x.getString("id");if(m.entries.containsKey(id))throw new IllegalArgumentException("Повтор операции");m.put(new Ledger.Entry(id,x.getString("type"),nullable(x,"from"),nullable(x,"to"),x.getString("category"),x.getString("note"),LocalDate.parse(x.getString("date")),x.getLong("amount")));}JSONObject limits=obj.getJSONObject("limits");Iterator<String> keys=limits.keys();while(keys.hasNext()){String k=keys.next();int at=k.indexOf('|');if(at<0)throw new IllegalArgumentException("Неверный бюджет");m.setLimit(YearMonth.parse(k.substring(0,at)),k.substring(at+1),limits.getLong(k));}JSONArray im=obj.optJSONArray("imports");if(im!=null)for(int i=0;i<im.length();i++)m.imports.add(im.getString(i));JSONObject bindings=obj.optJSONObject("bankAccounts");if(bindings!=null){Iterator<String> it=bindings.keys();while(it.hasNext()){String k=it.next(),v=bindings.getString(k);if(m.accounts.containsKey(v))m.bankAccounts.put(k,v);}}JSONObject bankRows=obj.optJSONObject("bankRows");if(bankRows!=null){Iterator<String> it=bankRows.keys();while(it.hasNext()){String k=it.next();m.bankRows.put(k,bankRows.getString(k));}}m.total();return m;}
 void save(){try{if(!prefs.edit().putString("ledger_v2",encode(ledger)).commit())throw new IOException();}catch(Exception e){storageLocked=true;toast("Не удалось сохранить изменения. Не закрывайте приложение; попробуйте сохранить резервную копию.");}}
 void openDocument(int req){Intent it=new Intent(Intent.ACTION_OPEN_DOCUMENT);it.addCategory(Intent.CATEGORY_OPENABLE);it.setType("*/*");startActivityForResult(it,req);}
 void exportBackup(){new AlertDialog.Builder(this).setTitle("Резервная копия").setMessage("JSON содержит суммы и описания операций без дополнительного шифрования. Сохраните файл в защищённом месте; не отправляйте его посторонним.").setNegativeButton("Отмена",null).setPositiveButton("Продолжить",(d,w)->{Intent it=new Intent(Intent.ACTION_CREATE_DOCUMENT);it.addCategory(Intent.CATEGORY_OPENABLE);it.setType("application/json");it.putExtra(Intent.EXTRA_TITLE,"Мои-финансы-"+LocalDate.now()+".json");startActivityForResult(it,11);}).show();}
 void pickImportAccount(){if(!writable())return;if(ledger.accounts.isEmpty()){toast("Сначала добавьте счёт для импорта");return;}ArrayList<String> ids=new ArrayList<>(ledger.accounts.keySet()),names=new ArrayList<>();for(String id:ids)names.add(accountName(id));new AlertDialog.Builder(this).setTitle("Импорт на счёт").setItems(names.toArray(new String[0]),(d,i)->{importAccount=ids.get(i);openDocument(13);}).show();}
 byte[] read(Uri uri)throws Exception{try(InputStream in=getContentResolver().openInputStream(uri);ByteArrayOutputStream buffer=new ByteArrayOutputStream()){if(in==null)throw new IOException();byte[] part=new byte[8192];int n;while((n=in.read(part))!=-1){if(buffer.size()+n>16*1024*1024)throw new IllegalArgumentException("Файл больше 16 МБ");buffer.write(part,0,n);}return buffer.toByteArray();}}
 String hash(byte[] raw)throws Exception{byte[] digest=MessageDigest.getInstance("SHA-256").digest(raw);StringBuilder b=new StringBuilder();for(byte x:digest)b.append(String.format(Locale.ROOT,"%02x",x&255));return b.toString();}
 String normalizedNote(String s){return s.replaceAll("\\s+"," ").trim();}
 String matchKey(LocalDate date,long signed,String note){return date+"|"+signed+"|"+normalizedNote(note);}
 long signedOn(Ledger.Entry e,String account){return account.equals(e.to)?e.amount:account.equals(e.from)?-e.amount:0;}
 void selectStatement(byte[] raw)throws Exception{
  if(!writable())return;BankStatement statement=BankStatement.parse(raw);
  String number=statement.accountNumber;
  if(!number.matches("\\d{20}"))throw new IllegalArgumentException("В выписке нет полного номера счёта");
  String account=ledger.bankAccounts.get(number);
  if(account!=null&&ledger.accounts.containsKey(account)){previewStatement(statement,account,null);return;}
  ArrayList<String> ids=new ArrayList<>(),names=new ArrayList<>();
  for(Ledger.Account ac:ledger.accounts.values())if(!ledger.bankAccounts.containsValue(ac.id)){ids.add(ac.id);names.add(ac.name+" · "+ac.type);}
  names.add("+ Создать новый счёт");
  new AlertDialog.Builder(this).setTitle("Привязать счёт •••• "+number.substring(16)).setItems(names.toArray(new String[0]),(d,i)->{
   if(i<ids.size())previewStatement(statement,ids.get(i),null);
   else {LinearLayout l=panel();EditText name=field(l,"Название счёта","Альфа-Банк",false);EditText opening=field(l,"Начальный остаток до периода, ₽","0",true);
    formDialog("Новый банковский счёт",l,null,null,()->{Ledger.Account created=new Ledger.Account(uid(),value(name),"Счёт",amount(value(opening)));l.post(()->previewStatement(statement,created.id,created));});}
  }).setNegativeButton("Отмена",null).show();
 }
 void previewStatement(BankStatement statement,String account,Ledger.Account created){
  if(!writable())return;
  Map<String,Deque<Ledger.Entry>> exact=new HashMap<>();Map<String,List<Ledger.Entry>> byAmount=new HashMap<>();Set<String> used=new HashSet<>(ledger.bankRows.values());Set<String> claimed=new HashSet<>();
  // Preclaim stable identities before attempting fallback matching (order-independent).
  for(Ledger.Entry e:ledger.entries.values()){
   long n=signedOn(e,account);if(n==0)continue;
   exact.computeIfAbsent(matchKey(e.date,n,e.note),k->new ArrayDeque<>()).add(e);
   byAmount.computeIfAbsent(e.date+"|"+n,k->new ArrayList<>()).add(e);
  }
  Map<String,Integer> scan=new HashMap<>();for(BankStatement.Row r:statement.rows){String k=BankRules.stable(r);String mapped=ledger.bankRows.get(account+"|"+k+"|"+scan.merge(k,1,Integer::sum));if(mapped!=null)claimed.add(mapped);}
  Map<String,Integer> occurrences=new HashMap<>(),oldOccurrences=new HashMap<>();Map<String,String> mappings=new LinkedHashMap<>();
  ArrayList<Ledger.Entry> pending=new ArrayList<>(),updates=new ArrayList<>();int duplicates=0,review=0,ambiguous=0;
  long net=0;
  for(BankStatement.Row r:statement.rows){
   String stable=BankRules.stable(r);int occurrence=occurrences.merge(stable,1,Integer::sum);
   String key=account+"|"+stable+"|"+occurrence;
   String oldFp=r.fingerprint();int oldN=oldOccurrences.merge(oldFp,1,Integer::sum);
   String oldId;
   try{oldId="bank-"+hash((account+"|"+oldFp+"|"+oldN).getBytes(StandardCharsets.UTF_8));}catch(Exception ex){error(ex);return;}
   if(ledger.bankRows.containsKey(key)){duplicates++;continue;}
   Ledger.Entry found=ledger.entries.get(oldId);
   if(found!=null&&used.contains(found.id))found=null;
   if(found==null){Deque<Ledger.Entry> q=exact.get(matchKey(r.date,r.amount,r.description));while(q!=null&&!q.isEmpty()&&used.contains(q.peek().id))q.remove();if(q!=null&&!q.isEmpty())found=q.remove();}
   if(found==null){Deque<Ledger.Entry> q=exact.get(matchKey(r.date,r.amount,BankRules.clean(r)));while(q!=null&&!q.isEmpty()&&used.contains(q.peek().id))q.remove();if(q!=null&&!q.isEmpty())found=q.remove();}
   boolean income=r.amount>0;String cat=BankRules.category(r),note=BankRules.clean(r);
   if(cat.endsWith("проверить")||cat.endsWith("уточнить"))review++;
   if(found!=null){
    duplicates++;used.add(found.id);claimed.add(found.id);mappings.put(key,found.id);
    if(!Ledger.MOVE.equals(found.type)){
     boolean raw=normalizedNote(found.note).equals(normalizedNote(r.description));
     String oldAuto=category(r.description,income),oldXlsx=category(r.category+" "+r.description,income);
     String corrected=(found.category.equals(oldAuto)||found.category.equals(oldXlsx)||found.category.equals("Переводы — проверить"))?cat:found.category;
     String correctedNote=raw?note:found.note;
     if(!corrected.equals(found.category)||!correctedNote.equals(found.note))updates.add(new Ledger.Entry(found.id,found.type,found.from,found.to,corrected,correctedNote,found.date,found.amount));
    }
    continue;
   }
   List<Ledger.Entry> candidates=byAmount.getOrDefault(r.date+"|"+r.amount,Collections.emptyList());
   boolean uncertain=false;for(Ledger.Entry e:candidates)if(!used.contains(e.id)||!claimed.contains(e.id)&&ledger.bankRows.containsValue(e.id)){uncertain=true;break;}
   if(uncertain){ambiguous++;continue;}
   String id=uid();pending.add(new Ledger.Entry(id,income?Ledger.IN:Ledger.OUT,income?null:account,income?account:null,cat,note,r.date,Math.abs(r.amount)));mappings.put(key,id);net+=r.amount;
  }
  if(ledger.entries.size()+pending.size()>100000){toast("Слишком много операций");return;}
  final int skipped=ambiguous;
  String name=created==null?accountName(account):created.name;
  String msg="Счёт: "+name+" •••• "+statement.accountNumber.substring(16)+"\nСтрок в выписке: "+statement.rows.size()+"\nНовых: "+pending.size()+"\nУже учтено: "+duplicates+"\nИсправить категории / описания: "+updates.size()+"\nНеоднозначных совпадений (не добавляем): "+ambiguous+"\nПереводы и покупки для проверки: "+review+"\nОстаток после импорта: "+money((created==null?ledger.balance(account):created.opening)+net);
  if(statement.reportedBalance!=null)msg+="\nОстаток по выписке: "+money(statement.reportedBalance);
  if(statement.holdCount>0)msg+="\nОперации в обработке (HOLD): "+statement.holdCount+" · "+signed(statement.holdNet)+"\nОни уже входят в суммы выписки и будут учтены один раз как операции по дате покупки. При изменении суммы или даты после подтверждения проверьте историю вручную.";
  msg+="\n\nСуществующие пользовательские категории и комментарии сохраняются, кроме распознанных старых автоматических значений. Переводы требуют проверки. Начальный остаток не меняется автоматически.";
  new AlertDialog.Builder(this).setTitle("Проверка выписки").setMessage(msg).setNegativeButton("Отмена",null).setPositiveButton("Применить",(d,w)->{
   if(!writable())return;
   try{
    if(created!=null)ledger.put(created);
    for(Ledger.Entry e:updates)ledger.put(e);for(Ledger.Entry e:pending)ledger.put(e);
    ledger.bankAccounts.put(statement.accountNumber,account);ledger.bankRows.putAll(mappings);save();
    month=YearMonth.now();nav("history");toast(skipped>0?"Импорт выполнен. Неоднозначные строки пропущены: "+skipped:"Импорт выполнен");
   }catch(Exception ex){error(ex);}
  }).show();
 }
 void importCsv(byte[] data)throws Exception{
  if(!writable()||!ledger.accounts.containsKey(importAccount))throw new IllegalArgumentException("Выберите счёт");final String account=importAccount;String key=account+":"+hash(data);if(ledger.imports.contains(key))throw new IllegalArgumentException("Этот файл уже импортирован на счёт");ArrayList<Ledger.Entry> pending=new ArrayList<>();
  try(BufferedReader r=new BufferedReader(new StringReader(new String(data,StandardCharsets.UTF_8)))){String line=r.readLine();if(line==null||!line.replace("\ufeff","").equals("date;amount;description"))throw new IllegalArgumentException("Нужен CSV с заголовком date;amount;description");while((line=r.readLine())!=null){if(line.trim().isEmpty())continue;String[] p=line.split(";",3);if(p.length!=3)throw new IllegalArgumentException("Неверная строка CSV");long a=amount(p[1]);boolean income=a>0;pending.add(new Ledger.Entry(uid(),income?Ledger.IN:Ledger.OUT,income?null:account,income?account:null,category(p[2],income),p[2],LocalDate.parse(p[0]),Math.abs(a)));if(pending.size()>20000||ledger.entries.size()+pending.size()>100000)throw new IllegalArgumentException("Слишком много операций");}}
  if(pending.isEmpty())throw new IllegalArgumentException("Файл пуст");
  new AlertDialog.Builder(this).setTitle("Импорт: "+pending.size()+" операций").setMessage("Счёт: "+accountName(account)+". Повтор точно такого же файла блокируется, но пересекающиеся выписки могут содержать дубликаты. Переводы нужно проверить вручную: этот формат не различает их.").setNegativeButton("Отмена",null).setPositiveButton("Импортировать",(d,w)->{if(!writable()||!ledger.accounts.containsKey(account)||ledger.imports.contains(key)||ledger.entries.size()+pending.size()>100000)return;for(Ledger.Entry e:pending)ledger.put(e);ledger.imports.add(key);save();nav("history");}).show();
 }
 @Override protected void onActivityResult(int rq,int rc,Intent data){super.onActivityResult(rq,rc,data);if(rc!=RESULT_OK||data==null||data.getData()==null)return;try{Uri uri=data.getData();if(rq==11){if(loadFailed)throw new IllegalArgumentException("Исходное хранилище не перезаписано; восстановите корректную копию");try(OutputStream out=getContentResolver().openOutputStream(uri)){if(out==null)throw new IOException();out.write(encode(ledger).getBytes(StandardCharsets.UTF_8));}toast("Резервная копия сохранена");}else if(rq==12){Ledger candidate=decode(new String(read(uri),StandardCharsets.UTF_8));new AlertDialog.Builder(this).setTitle("Заменить локальные данные?").setMessage("В копии "+candidate.accounts.size()+" счетов и "+candidate.entries.size()+" операций. Текущие данные будут полностью заменены.").setNegativeButton("Отмена",null).setPositiveButton("Восстановить",(d,w)->{ledger=candidate;storageLocked=false;loadFailed=false;save();nav("home");}).show();}else if(rq==13){byte[] raw=read(uri);if(raw.length>=4&&raw[0]==80&&raw[1]==75)selectStatement(raw);else {if(!writable())return;ArrayList<String> ids=new ArrayList<>(ledger.accounts.keySet()),names=new ArrayList<>();for(String id:ids)names.add(accountName(id));if(ids.isEmpty()){toast("Сначала добавьте счёт для CSV");return;}new AlertDialog.Builder(this).setTitle("Счёт для CSV").setItems(names.toArray(new String[0]),(d,i)->{importAccount=ids.get(i);try{importCsv(raw);}catch(Exception ex){error(ex);}}).setNegativeButton("Отмена",null).show();}}}catch(Exception e){if(rq==13){String reason=e instanceof IllegalArgumentException&&!(e instanceof NumberFormatException)&&!(e instanceof java.time.format.DateTimeParseException)?e.getMessage():null;if(reason==null||reason.length()>180)reason="Не удалось прочитать структуру файла ("+e.getClass().getSimpleName()+").";new AlertDialog.Builder(this).setTitle("Импорт не выполнен").setMessage(reason+"\n\nДанные не добавлены. Выберите оригинальную рублёвую выписку XLSX или подготовленный CSV.").setPositiveButton("ОК",null).show();}else error(e);}}
 @Override public void onBackPressed(){if(!page.equals("home")||selecting)backPage();else super.onBackPressed();}
}
