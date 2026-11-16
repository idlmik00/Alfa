package ru.personal.alfafinance;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
 static final int BG=0xff080809, CARD=0xff151517, LINE=0xff29292e, RED=0xffff353c, WHITE=0xfff7f7f8, MUTED=0xffa0a0a8;
 static final Locale RU=new Locale("ru","RU");
 static final String[] TYPES={"Карта","Наличные","Счёт"};
 static final String[] CATEGORIES={"Продукты","Транспорт","Кафе и рестораны","Жильё и ЖКХ","Здоровье","Связь","Покупки","Развлечения","Прочее"};
 Ledger ledger=new Ledger(); SharedPreferences prefs; LinearLayout root; YearMonth month=YearMonth.now();
 String page="home",accountFilter=null,importAccount=null; int historyLimit=100; boolean hidden,secure,storageLocked;
 @Override public void onCreate(Bundle saved){
  super.onCreate(saved);prefs=getSharedPreferences("local",MODE_PRIVATE);hidden=prefs.getBoolean("hidden",false);secure=prefs.getBoolean("secure",true);
  getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);getWindow().getDecorView().setSystemUiVisibility(0);security();
  try{String state=prefs.getString("ledger_v2",null);if(state!=null)ledger=decode(state);}catch(Exception e){storageLocked=true;}
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
 void menu(LinearLayout p,String icon,String title,String right,Runnable r){LinearLayout h=horizontal();h.setPadding(dp(14),dp(13),dp(14),dp(13));h.setBackground(bg(CARD,16));TextView i=text(icon,22,RED,true);i.setGravity(Gravity.CENTER);h.addView(i,new LinearLayout.LayoutParams(dp(38),dp(38)));LinearLayout c=col();c.setPadding(dp(12),0,dp(6),0);label(c,title,16,WHITE,true);weight(h,c);if(right!=null)h.addView(text(right,14,MUTED,false));h.addView(text("  ›",21,MUTED,false));h.setOnClickListener(v->r.run());p.addView(h);space(p,9);}
 void nav(String target){page=target;accountFilter=null;historyLimit=100;show();}
 void show(){
  LinearLayout outer=col();outer.setBackgroundColor(BG);outer.setLayoutParams(new ViewGroup.LayoutParams(-1,-1));
  ScrollView sc=new ScrollView(this);sc.setFillViewport(true);root=col();root.setPadding(dp(20),dp(25),dp(20),dp(23));sc.addView(root,new ScrollView.LayoutParams(-1,-2));outer.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
  line(outer);LinearLayout bar=horizontal();bar.setPadding(dp(3),dp(7),dp(3),dp(4));bar.setBackgroundColor(BG);
  String[] labels={"⌂\nГлавная","▥\nАналитика","⚙\nНастройки"};String[] ids={"home","analytics","settings"};
  for(int n=0;n<ids.length;n++){final String to=ids[n];boolean active=page.equals(to)||to.equals("home")&&(page.equals("accounts")||page.equals("history"));TextView t=text(labels[n],14,active?RED:MUTED,active);t.setGravity(Gravity.CENTER);t.setMinHeight(dp(56));t.setOnClickListener(v->nav(to));weight(bar,t);}
  outer.addView(bar,new LinearLayout.LayoutParams(-1,-2));setContentView(outer);
  switch(page){case "accounts":accounts();break;case "history":history();break;case "analytics":analytics();break;case "settings":settings();break;default:home();}
 }
 void title(String s){label(root,s,30,WHITE,true);space(root,22);}
 void home(){
  title("Мои финансы");
  LinearLayout hero=col();hero.setBackground(gradient());hero.setPadding(dp(22),dp(22),dp(22),dp(22));hero.setMinimumHeight(dp(156));
  LinearLayout top=horizontal();weight(top,text("Общий баланс",16,0xffe9d2d4,false));TextView eye=text(hidden?"Показать":"Скрыть",13,WHITE,true);eye.setPadding(dp(10),0,0,0);eye.setOnClickListener(v->{hidden=!hidden;prefs.edit().putBoolean("hidden",hidden).apply();show();});top.addView(eye);hero.addView(top);space(hero,16);
  label(hero,hidden?"••••••":money(ledger.total()),34,WHITE,true);space(hero,10);label(hero,ledger.accounts.isEmpty()?"Добавьте карту, наличные или счёт":"По внесённым данным",13,0xffe9d2d4,false);root.addView(hero);space(root,14);
  pair(root,action("+  Доход",false,()->entryDialog(Ledger.IN,null)),action("−  Расход",false,()->entryDialog(Ledger.OUT,null)));space(root,16);
  menu(root,"▤","Карты, наличные, счета",String.valueOf(ledger.accounts.size()),()->nav("accounts"));
  menu(root,"⇄","Перевод между счетами",null,()->entryDialog(Ledger.MOVE,null));
  space(root,12);heading("Последние операции","Все",()->{month=YearMonth.now();nav("history");});
  List<Ledger.Entry> all=ledger.sorted();if(all.isEmpty()){label(root,"Пока нет операций",15,MUTED,false);space(root,8);label(root,"Добавьте доход или расход — здесь появится история.",14,MUTED,false);}else for(int i=0;i<Math.min(5,all.size());i++)entryRow(all.get(i));
 }
 void accounts(){
  title("Счета");label(root,"Всего: "+(hidden?"••••••":money(ledger.total())),22,WHITE,true);space(root,18);
  for(Ledger.Account a:ledger.accounts.values()){
   LinearLayout l=col();LinearLayout h=horizontal();LinearLayout left=col();label(left,a.name,17,WHITE,true);space(left,4);label(left,a.type,13,MUTED,false);weight(h,left);h.addView(text(hidden?"••••":money(ledger.balance(a.id)),18,WHITE,true));l.addView(h);space(l,12);
   pair(l,action("Операции",false,()->{page="history";accountFilter=a.id;month=YearMonth.now();historyLimit=100;show();}),action("Настроить",false,()->accountDialog(a)));card(root,l);space(root,10);
  }
  if(ledger.accounts.isEmpty()){label(root,"Создайте первый кошелёк и укажите остаток до первой операции.",15,MUTED,false);space(root,15);}
  button(root,"+  Добавить счёт",true,()->accountDialog(null));label(root,"Если несколько карт привязаны к одному банковскому счёту, учитывайте его как один кошелёк. Иначе общий баланс будет завышен.",13,MUTED,false);
 }
 void monthControl(){LinearLayout h=horizontal();h.addView(action("‹",false,()->{month=month.minusMonths(1);historyLimit=100;show();}),new LinearLayout.LayoutParams(dp(48),dp(48)));TextView t=text(monthName(),17,WHITE,true);t.setGravity(Gravity.CENTER);weight(h,t);h.addView(action("›",false,()->{month=month.plusMonths(1);historyLimit=100;show();}),new LinearLayout.LayoutParams(dp(48),dp(48)));root.addView(h);space(root,18);}
 void history(){
  title(accountFilter==null?"Операции":accountName(accountFilter));monthControl();if(accountFilter!=null)button(root,"Все счета",false,()->{accountFilter=null;show();});
  int count=0;for(Ledger.Entry e:ledger.sorted()){if(!YearMonth.from(e.date).equals(month)||accountFilter!=null&&!accountFilter.equals(e.from)&&!accountFilter.equals(e.to))continue;count++;if(count<=historyLimit)entryRow(e);}
  if(count==0)label(root,"Нет операций за выбранный месяц",15,MUTED,false);
  if(count>historyLimit)button(root,"Показать ещё",false,()->{historyLimit+=100;show();});
  space(root,12);button(root,"+  Добавить операцию",true,()->chooseType());
 }
 void chooseType(){new AlertDialog.Builder(this).setTitle("Тип операции").setItems(new String[]{"Доход","Расход","Перевод"},(d,i)->entryDialog(new String[]{Ledger.IN,Ledger.OUT,Ledger.MOVE}[i],null)).show();}
 void entryRow(Ledger.Entry e){
  boolean transfer=Ledger.MOVE.equals(e.type), income=Ledger.IN.equals(e.type);LinearLayout h=horizontal();h.setPadding(0,dp(12),0,dp(12));h.setMinimumHeight(dp(66));
  TextView ic=text(transfer?"⇄":income?"+":"−",23,RED,true);ic.setGravity(Gravity.CENTER);ic.setBackground(bg(CARD,13));h.addView(ic,new LinearLayout.LayoutParams(dp(44),dp(44)));
  LinearLayout left=col();left.setPadding(dp(12),0,dp(6),0);label(left,transfer?"Перевод":e.category,16,WHITE,true);space(left,5);
  String details=e.date.format(DateTimeFormatter.ofPattern("d MMM",RU))+" · "+(transfer?accountName(e.from)+" → "+accountName(e.to):accountName(income?e.to:e.from));if(!e.note.isEmpty())details+=" · "+e.note;TextView dt=text(details,12,MUTED,false);dt.setMaxLines(2);dt.setEllipsize(android.text.TextUtils.TruncateAt.END);left.addView(dt);weight(h,left);
  TextView sum=text(hidden?"••••":transfer?money(e.amount):signed(income?e.amount:-e.amount),16,income?WHITE:transfer?MUTED:RED,true);h.addView(sum);h.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Операция").setItems(new String[]{"Изменить","Удалить"},(d,i)->{if(i==0)entryDialog(e.type,e);else if(writable())new AlertDialog.Builder(this).setMessage("Удалить операцию? Балансы и отчёты пересчитаются.").setNegativeButton("Отмена",null).setPositiveButton("Удалить",(a,b)->{ledger.entries.remove(e.id);save();show();}).show();}).show());root.addView(h);line(root);
 }
 void progress(LinearLayout l,long spent,long limit){ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(1000);bar.setProgress(limit<=0?0:(int)Math.min(1000,Math.max(0,spent*1000.0/limit)));bar.setProgressTintList(ColorStateList.valueOf(RED));bar.setProgressBackgroundTintList(ColorStateList.valueOf(LINE));l.addView(bar,new LinearLayout.LayoutParams(-1,dp(10)));}
 void analytics(){
  title("Аналитика");monthControl();long in=ledger.sum(month,Ledger.IN),out=ledger.sum(month,Ledger.OUT),lim=ledger.limit(month,"");
  LinearLayout c=col();label(c,"Расходы за месяц",14,MUTED,false);space(c,7);label(c,money(out),30,WHITE,true);space(c,12);label(c,"Доходы  "+money(in),15,WHITE,false);space(c,6);label(c,"Разница  "+signed(in-out),15,MUTED,false);card(root,c);space(root,15);
  heading("Бюджет","Настроить",()->limitDialog(""));LinearLayout b=col();label(b,lim==0?"Лимит не задан":"Осталось",14,MUTED,false);space(b,8);label(b,lim==0?"—":money(lim-out),26,lim>0&&out>lim?RED:WHITE,true);space(b,10);if(lim>0){progress(b,out,lim);space(b,8);label(b,money(out)+" из "+money(lim),13,MUTED,false);if(month.equals(YearMonth.now())){space(b,8);label(b,"Лимит до конца месяца: "+money(ledger.daily(month,LocalDate.now()))+" / день",14,WHITE,false);}}else label(b,"Установите сумму, которую планируете потратить.",14,MUTED,false);card(root,b);space(root,18);
  heading("Категории","+ Лимит",()->chooseCategoryLimit());SortedMap<String,Long> by=ledger.categories(month);Set<String> cats=new LinkedHashSet<>(by.keySet());String prefix=month+"|";for(String k:ledger.limits.keySet())if(k.startsWith(prefix)&&k.length()>prefix.length())cats.add(k.substring(prefix.length()));
  ArrayList<String> sorted=new ArrayList<>(cats);sorted.sort((a,d)->Long.compare(by.getOrDefault(d,0L),by.getOrDefault(a,0L)));
  for(String name:sorted){long spent=by.getOrDefault(name,0L),limit=ledger.limit(month,name);LinearLayout x=col();LinearLayout h=horizontal();weight(h,text(name,15,WHITE,true));h.addView(text(money(spent),15,RED,true));x.addView(h);space(x,6);progress(x,spent,limit>0?limit:out);space(x,5);label(x,limit>0?"Лимит "+money(limit)+(spent>limit?" · Перерасход":""):(out>0?String.format(RU,"%.0f%% расходов",100.0*spent/out):"Лимит не задан"),12,MUTED,false);x.setOnClickListener(v->limitDialog(name));card(root,x);space(root,8);}
  if(cats.isEmpty())label(root,"Здесь появятся категории ваших расходов.",14,MUTED,false);
  space(root,17);menu(root,"✦","Финансовый помощник",null,()->advice());
 }
 void settings(){
  title("Настройки");menu(root,"▤","Управление счетами",null,()->nav("accounts"));
  menu(root,"⇣","Импорт CSV",null,()->pickImportAccount());
  menu(root,"⇡","Сохранить резервную копию",null,()->exportBackup());
  menu(root,"↻","Восстановить копию",null,()->openDocument(12));
  LinearLayout l=col();Switch s=new Switch(this);s.setText("Защищать экран от скриншотов");s.setTextColor(WHITE);s.setChecked(secure);s.setThumbTintList(ColorStateList.valueOf(RED));s.setOnCheckedChangeListener((v,on)->{secure=on;prefs.edit().putBoolean("secure",secure).apply();security();});l.addView(s);space(l,12);label(l,"Данные хранятся на устройстве. Интернет и банковские разрешения не запрашиваются.",13,MUTED,false);card(root,l);space(root,16);label(root,"Мои финансы · v0.2",14,MUTED,false);space(root,8);label(root,"Подключение банков и нейросети — будущие этапы.",13,MUTED,false);
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
 String category(String s,boolean income){s=s.toLowerCase(RU);if(income)return s.contains("зарплат")?"Зарплата":"Прочий доход";if(s.matches(".*(продукт|магазин|супермаркет|пятероч|пятёроч|магнит).*"))return "Продукты";if(s.matches(".*(такси|метро|азс|транспорт|автобус).*"))return "Транспорт";if(s.matches(".*(кафе|ресторан|кофе).*"))return "Кафе и рестораны";if(s.matches(".*(аптек|здоров).*"))return "Здоровье";return "Прочее";}
 void entryDialog(String kind,Ledger.Entry old){
  if(!writable())return;if(ledger.accounts.isEmpty()){new AlertDialog.Builder(this).setTitle("Сначала создайте счёт").setMessage("Так приложение будет знать, где находятся деньги.").setNegativeButton("Отмена",null).setPositiveButton("Добавить",(d,w)->accountDialog(null)).show();return;}
  boolean move=kind.equals(Ledger.MOVE),income=kind.equals(Ledger.IN);if(move&&ledger.accounts.size()<2){toast("Для перевода нужны как минимум два счёта");return;}
  ArrayList<String> ids=new ArrayList<>(ledger.accounts.keySet()),names=new ArrayList<>();for(String id:ids)names.add(accountName(id)+" · "+ledger.accounts.get(id).type);
  LinearLayout l=panel();int selected=Math.max(0,ids.indexOf(old==null?accountFilter:income?old.to:old.from));Spinner first=spinner(l,move?"Откуда":income?"Куда поступили деньги":"Откуда списаны деньги",names,selected);
  Spinner second=move?spinner(l,"Куда",names,old==null?(selected+1)%ids.size():ids.indexOf(old.to)):null;
  EditText sum=field(l,"Сумма, ₽",old==null?"":number(old.amount),true);
  EditText cat=move?null:field(l,income?"Категория дохода (необязательно)":"Категория расхода (необязательно)",old==null?"":old.category,false);
  EditText note=field(l,"Комментарий",old==null?"":old.note,false);
  EditText date=field(l,"Дата (ГГГГ-ММ-ДД)",old==null?LocalDate.now().toString():old.date.toString(),false);
  formDialog(old==null?(move?"Перевод":income?"Доход":"Расход"):"Изменить операцию",l,null,null,()->{
   long n=amount(value(sum));String a=ids.get(first.getSelectedItemPosition()),other=move?ids.get(second.getSelectedItemPosition()):null,desc=value(note),c=move?"":value(cat);if(!move&&c.isEmpty())c=category(desc,income);
   ledger.put(new Ledger.Entry(old==null?uid():old.id,kind,income?null:a,move?other:income?a:null,c,desc,LocalDate.parse(value(date)),n));save();show();
  });
 }
 void chooseCategoryLimit(){if(!writable())return;LinkedHashSet<String> set=new LinkedHashSet<>(Arrays.asList(CATEGORIES));set.addAll(ledger.categories(month).keySet());ArrayList<String> list=new ArrayList<>(set);list.add("Своя категория");new AlertDialog.Builder(this).setTitle("Категория бюджета").setItems(list.toArray(new String[0]),(d,i)->{if(i==list.size()-1){LinearLayout l=panel();EditText e=field(l,"Название категории","",false);formDialog("Новая категория",l,null,null,()->{String c=value(e);if(c.isEmpty())throw new IllegalArgumentException("Введите название");l.post(()->limitDialog(c));});}else limitDialog(list.get(i));}).show();}
 void limitDialog(String category){if(!writable())return;LinearLayout l=panel();long lim=ledger.limit(month,category);EditText e=field(l,"Лимит в рублях (0 — убрать)",lim==0?"":number(lim),true);formDialog(category.isEmpty()?"Бюджет на "+monthName():category,l,null,null,()->{ledger.setLimit(month,category,amount(value(e)));save();show();});}
 void advice(){long out=ledger.sum(month,Ledger.OUT),in=ledger.sum(month,Ledger.IN),lim=ledger.limit(month,"");StringBuilder s=new StringBuilder("За "+monthName().toLowerCase(RU)+" учтено "+money(out)+" расходов.\n\n");if(lim==0)s.append("Задайте общий лимит, чтобы отслеживать бюджет. ");else if(out>lim)s.append("Бюджет превышен на "+money(out-lim)+". Проверьте необязательные траты. ");else{s.append("Осталось "+money(lim-out)+". ");if(month.equals(YearMonth.now()))s.append("До конца месяца можно расходовать не более "+money(ledger.daily(month,LocalDate.now()))+" в день по плану. ");}
  if(out>in)s.append("\n\nРасходы выше учтённых доходов. Проверьте полноту записей.");SortedMap<String,Long> by=ledger.categories(month);if(!by.isEmpty()){Map.Entry<String,Long> max=Collections.max(by.entrySet(),Map.Entry.comparingByValue());s.append("\n\nСамая крупная категория: "+max.getKey()+" ("+money(max.getValue())+").");}for(Map.Entry<String,Long> c:by.entrySet()){long n=ledger.limit(month,c.getKey());if(n>0&&c.getValue()>n)s.append("\nЛимит «"+c.getKey()+"» превышен на "+money(c.getValue()-n)+".");}
  s.append("\n\nЭто локальные расчёты, а не нейросеть. Лимит бюджета не равен доступным деньгам.");new AlertDialog.Builder(this).setTitle("Финансовый помощник").setMessage(s.toString()).setPositiveButton("Понятно",null).show();
 }
 String encode(Ledger model)throws Exception{JSONObject obj=new JSONObject();obj.put("schema",2);JSONArray a=new JSONArray(),e=new JSONArray(),im=new JSONArray();JSONObject limits=new JSONObject();for(Ledger.Account x:model.accounts.values()){JSONObject o=new JSONObject();o.put("id",x.id).put("name",x.name).put("type",x.type).put("opening",x.opening);a.put(o);}for(Ledger.Entry x:model.entries.values()){JSONObject o=new JSONObject();o.put("id",x.id).put("type",x.type).put("from",x.from==null?JSONObject.NULL:x.from).put("to",x.to==null?JSONObject.NULL:x.to).put("category",x.category).put("note",x.note).put("date",x.date.toString()).put("amount",x.amount);e.put(o);}for(Map.Entry<String,Long> x:model.limits.entrySet())limits.put(x.getKey(),x.getValue());for(String x:model.imports)im.put(x);obj.put("accounts",a).put("entries",e).put("limits",limits).put("imports",im);return obj.toString();}
 String nullable(JSONObject o,String k)throws Exception{return o.isNull(k)?null:o.getString(k);}
 Ledger decode(String raw)throws Exception{JSONObject obj=new JSONObject(raw);if(obj.getInt("schema")!=2)throw new IllegalArgumentException("Неподдерживаемый формат копии");Ledger m=new Ledger();JSONArray a=obj.getJSONArray("accounts"),e=obj.getJSONArray("entries");for(int i=0;i<a.length();i++){JSONObject x=a.getJSONObject(i);String id=x.getString("id");if(m.accounts.containsKey(id))throw new IllegalArgumentException("Повтор счёта");m.put(new Ledger.Account(id,x.getString("name"),x.getString("type"),x.getLong("opening")));}for(int i=0;i<e.length();i++){JSONObject x=e.getJSONObject(i);String id=x.getString("id");if(m.entries.containsKey(id))throw new IllegalArgumentException("Повтор операции");m.put(new Ledger.Entry(id,x.getString("type"),nullable(x,"from"),nullable(x,"to"),x.getString("category"),x.getString("note"),LocalDate.parse(x.getString("date")),x.getLong("amount")));}JSONObject limits=obj.getJSONObject("limits");Iterator<String> keys=limits.keys();while(keys.hasNext()){String k=keys.next();int at=k.indexOf('|');if(at<0)throw new IllegalArgumentException("Неверный бюджет");m.setLimit(YearMonth.parse(k.substring(0,at)),k.substring(at+1),limits.getLong(k));}JSONArray im=obj.optJSONArray("imports");if(im!=null)for(int i=0;i<im.length();i++)m.imports.add(im.getString(i));m.total();return m;}
 void save(){try{if(!prefs.edit().putString("ledger_v2",encode(ledger)).commit())throw new IOException();}catch(Exception e){storageLocked=true;toast("Не удалось сохранить изменения. Не закрывайте приложение; попробуйте сохранить резервную копию.");}}
 void openDocument(int req){Intent it=new Intent(Intent.ACTION_OPEN_DOCUMENT);it.addCategory(Intent.CATEGORY_OPENABLE);it.setType("*/*");startActivityForResult(it,req);}
 void exportBackup(){new AlertDialog.Builder(this).setTitle("Резервная копия").setMessage("JSON содержит суммы и описания операций без дополнительного шифрования. Сохраните файл в защищённом месте; не отправляйте его посторонним.").setNegativeButton("Отмена",null).setPositiveButton("Продолжить",(d,w)->{Intent it=new Intent(Intent.ACTION_CREATE_DOCUMENT);it.addCategory(Intent.CATEGORY_OPENABLE);it.setType("application/json");it.putExtra(Intent.EXTRA_TITLE,"Мои-финансы-"+LocalDate.now()+".json");startActivityForResult(it,11);}).show();}
 void pickImportAccount(){if(!writable())return;if(ledger.accounts.isEmpty()){toast("Сначала добавьте счёт для импорта");return;}ArrayList<String> ids=new ArrayList<>(ledger.accounts.keySet()),names=new ArrayList<>();for(String id:ids)names.add(accountName(id));new AlertDialog.Builder(this).setTitle("Импорт на счёт").setItems(names.toArray(new String[0]),(d,i)->{importAccount=ids.get(i);openDocument(13);}).show();}
 byte[] read(Uri uri)throws Exception{try(InputStream in=getContentResolver().openInputStream(uri);ByteArrayOutputStream buffer=new ByteArrayOutputStream()){if(in==null)throw new IOException();byte[] part=new byte[8192];int n;while((n=in.read(part))!=-1){if(buffer.size()+n>16*1024*1024)throw new IllegalArgumentException("Файл больше 16 МБ");buffer.write(part,0,n);}return buffer.toByteArray();}}
 String hash(byte[] raw)throws Exception{byte[] digest=MessageDigest.getInstance("SHA-256").digest(raw);StringBuilder b=new StringBuilder();for(byte x:digest)b.append(String.format(Locale.ROOT,"%02x",x&255));return b.toString();}
 void importCsv(byte[] data)throws Exception{
  if(!writable()||!ledger.accounts.containsKey(importAccount))throw new IllegalArgumentException("Выберите счёт");final String account=importAccount;String key=account+":"+hash(data);if(ledger.imports.contains(key))throw new IllegalArgumentException("Этот файл уже импортирован на счёт");ArrayList<Ledger.Entry> pending=new ArrayList<>();
  try(BufferedReader r=new BufferedReader(new StringReader(new String(data,StandardCharsets.UTF_8)))){String line=r.readLine();if(line==null||!line.replace("\ufeff","").equals("date;amount;description"))throw new IllegalArgumentException("Нужен CSV с заголовком date;amount;description");while((line=r.readLine())!=null){if(line.trim().isEmpty())continue;String[] p=line.split(";",3);if(p.length!=3)throw new IllegalArgumentException("Неверная строка CSV");long a=amount(p[1]);boolean income=a>0;pending.add(new Ledger.Entry(uid(),income?Ledger.IN:Ledger.OUT,income?null:account,income?account:null,category(p[2],income),p[2],LocalDate.parse(p[0]),Math.abs(a)));if(pending.size()>20000||ledger.entries.size()+pending.size()>100000)throw new IllegalArgumentException("Слишком много операций");}}
  if(pending.isEmpty())throw new IllegalArgumentException("Файл пуст");
  new AlertDialog.Builder(this).setTitle("Импорт: "+pending.size()+" операций").setMessage("Счёт: "+accountName(account)+". Повтор точно такого же файла блокируется, но пересекающиеся выписки могут содержать дубликаты. Переводы нужно проверить вручную: этот формат не различает их.").setNegativeButton("Отмена",null).setPositiveButton("Импортировать",(d,w)->{if(!writable()||!ledger.accounts.containsKey(account)||ledger.imports.contains(key)||ledger.entries.size()+pending.size()>100000)return;for(Ledger.Entry e:pending)ledger.put(e);ledger.imports.add(key);save();nav("history");}).show();
 }
 @Override protected void onActivityResult(int rq,int rc,Intent data){super.onActivityResult(rq,rc,data);if(rc!=RESULT_OK||data==null||data.getData()==null)return;try{Uri uri=data.getData();if(rq==11){if(storageLocked&&prefs.contains("ledger_v2"))throw new IllegalArgumentException("Исходное хранилище не перезаписано; восстановите корректную копию");try(OutputStream out=getContentResolver().openOutputStream(uri)){if(out==null)throw new IOException();out.write(encode(ledger).getBytes(StandardCharsets.UTF_8));}toast("Резервная копия сохранена");}else if(rq==12){Ledger candidate=decode(new String(read(uri),StandardCharsets.UTF_8));new AlertDialog.Builder(this).setTitle("Заменить локальные данные?").setMessage("В копии "+candidate.accounts.size()+" счетов и "+candidate.entries.size()+" операций. Текущие данные будут полностью заменены.").setNegativeButton("Отмена",null).setPositiveButton("Восстановить",(d,w)->{ledger=candidate;storageLocked=false;save();nav("home");}).show();}else if(rq==13)importCsv(read(uri));}catch(Exception e){error(e);}}
 @Override public void onBackPressed(){if(!page.equals("home"))nav("home");else super.onBackPressed();}
}
