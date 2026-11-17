package ru.personal.alfafinance;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.*;
import java.util.*;
import java.util.zip.*;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilderFactory;

/** Restricted XLSX reader for ruble statements with known column headings. */
public final class BankStatement {
 static final int MAX_XML=16*1024*1024;
 static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("dd.MM.uuuu",Locale.ROOT).withResolverStyle(ResolverStyle.STRICT);
 public static final class Row {
  public final LocalDate date,posted; public final String code,category,description; public final long amount;
  Row(LocalDate date,LocalDate posted,String code,String category,String description,long amount){this.date=date;this.posted=posted;this.code=code;this.category=category;this.description=description;this.amount=amount;}
  public String fingerprint(){return date+"|"+posted+"|"+code+"|"+amount+"|"+description;}
  public boolean needsReview(){String s=description.toLowerCase(Locale.ROOT);return s.contains("перевод между счетами")||s.contains("между своими счетами")||s.contains("между собственными счетами")||s.contains("внесение средств через устройство")||s.contains("внесение наличных")||s.contains("снятие наличных")||s.contains("выдача наличных")||s.startsWith("категория: перевод")||s.contains("перевод клиенту");}
 }
 public final List<Row> rows=new ArrayList<>();
 public String accountNumber=""; public long incoming,outgoing; public Long reportedBalance,pending;
 static String normalized(String s){return s.replace('\u00a0',' ').replace('\u202f',' ').trim();}
 static long amount(String s){String n=normalized(s).replace("RUR","").replace("RUB","").replace("₽","").replace(" ","").replace(',','.');long a=new BigDecimal(n).movePointRight(2).longValueExact();if(a < -Ledger.MAX||a>Ledger.MAX)throw new IllegalArgumentException("Сумма вне допустимого диапазона");return a;}
 static Document xml(byte[] raw)throws Exception{
  String s=new String(raw,StandardCharsets.UTF_8);String upper=s.toUpperCase(Locale.ROOT);
  if(upper.contains("<!DOCTYPE")||upper.contains("<!ENTITY")||s.indexOf(0)>=0)throw new IllegalArgumentException("Недопустимый XML");
  DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();f.setNamespaceAware(true);f.setExpandEntityReferences(false);
  return f.newDocumentBuilder().parse(new InputSource(new StringReader(s)));
 }
 static NodeList tags(Node n,String name){return n instanceof Document?((Document)n).getElementsByTagNameNS("*",name):((Element)n).getElementsByTagNameNS("*",name);}
 static String first(Element e,String name){NodeList l=tags(e,name);return l.getLength()==0?"":l.item(0).getTextContent();}
 static String strings(Element e){StringBuilder b=new StringBuilder();NodeList t=tags(e,"t");for(int i=0;i<t.getLength();i++)b.append(t.item(i).getTextContent());return b.toString();}
 static Map<String,byte[]> extract(byte[] bytes)throws Exception{
  Map<String,byte[]> files=new HashMap<>();int total=0;
  try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(bytes))){ZipEntry e;byte[] buf=new byte[8192];int entries=0;while((e=zip.getNextEntry())!=null){
   if(++entries>2000)throw new IllegalArgumentException("Слишком много частей XLSX");
   String name=e.getName();boolean keep=name.equals("xl/sharedStrings.xml")||name.equals("xl/worksheets/sheet1.xml");
   int n;ByteArrayOutputStream out=keep?new ByteArrayOutputStream():null;
   while((n=zip.read(buf))!=-1){total+=n;if(total>32*1024*1024)throw new IllegalArgumentException("Слишком большой XLSX");if(keep){if(out.size()+n>MAX_XML)throw new IllegalArgumentException("Слишком большой лист");out.write(buf,0,n);}}
   if(keep&&files.put(name,out.toByteArray())!=null)throw new IllegalArgumentException("Повторяющаяся часть XLSX");
  }}return files;
 }
 static Map<String,String> cells(Element row,List<String> shared){Map<String,String> r=new HashMap<>();NodeList c=tags(row,"c");for(int j=0;j<c.getLength();j++){Element e=(Element)c.item(j);String address=e.getAttribute("r").replaceAll("[0-9]","");if(address.isEmpty())continue;String t=e.getAttribute("t"),v=first(e,"v");if("s".equals(t)&&!v.isEmpty())v=shared.get(Integer.parseInt(v));else if("inlineStr".equals(t))v=strings(e);r.put(address,v);}return r;}
 static String value(Map<String,String> row,String col){return row.getOrDefault(col,"");}
 static String find(Map<String,String> row,String label){for(Map.Entry<String,String> e:row.entrySet())if(normalized(e.getValue()).equalsIgnoreCase(label))return e.getKey();return null;}
 public static BankStatement parse(byte[] bytes)throws Exception{
  Map<String,byte[]> files=extract(bytes);if(!files.containsKey("xl/worksheets/sheet1.xml"))throw new IllegalArgumentException("Не найден первый лист XLSX");
  List<String> shared=new ArrayList<>();if(files.containsKey("xl/sharedStrings.xml")){NodeList items=tags(xml(files.get("xl/sharedStrings.xml")),"si");for(int i=0;i<items.getLength();i++)shared.add(strings((Element)items.item(i)));}
  BankStatement b=new BankStatement();NodeList sheetRows=tags(xml(files.get("xl/worksheets/sheet1.xml")),"row");
  String date=null,posted=null,code=null,category=null,description=null,amount=null,status=null;Long expectedIn=null,expectedOut=null;boolean rubles=false,finished=false;
  for(int i=0;i<sheetRows.getLength();i++){
   Map<String,String> r=cells((Element)sheetRows.item(i),shared);
   if(date==null){
    if(find(r,"Номер счета")!=null||find(r,"Номер счёта")!=null)b.accountNumber=normalized(value(r,"C")).replace(" ","");
    if(find(r,"Валюта счета")!=null){String currency=normalized(value(r,"C"));rubles=currency.equals("RUR")||currency.equals("RUB");}
    if(find(r,"Поступления")!=null)expectedIn=amount(value(r,"N"));
    if(find(r,"Расходы")!=null)expectedOut=Math.abs(amount(value(r,"N")));
    if(find(r,"Текущий баланс")!=null)b.reportedBalance=amount(value(r,"N"));
    if(find(r,"Неподтвержденные операции")!=null)b.pending=amount(value(r,"N"));
    String d=find(r,"Дата операции");if(d!=null){date=d;posted=find(r,"Дата проводки");code=find(r,"Код");category=find(r,"Категория");description=find(r,"Описание");amount=find(r,"Сумма в валюте счета");status=find(r,"Статус");if(posted==null||code==null||category==null||description==null||amount==null||status==null)throw new IllegalArgumentException("Неизвестный формат столбцов выписки");}continue;
   }
   String ds=normalized(value(r,date)),a=normalized(value(r,amount));
   if(ds.isEmpty()||"0".equals(ds)){if(!a.isEmpty()&&!a.equals("0"))throw new IllegalArgumentException("Операция без даты");if(!b.rows.isEmpty())finished=true;continue;}
   if(!ds.matches("[0-9]{2}\\.[0-9]{2}\\.[0-9]{4}")){if(!a.isEmpty()&&!a.equals("0"))throw new IllegalArgumentException("Неизвестная дата операции");if(!b.rows.isEmpty())finished=true;continue;}
   if(finished)throw new IllegalArgumentException("Неожиданные операции после конца таблицы");
   String ps=normalized(value(r,posted)),st=normalized(value(r,status));if(!st.isEmpty()&&!st.equalsIgnoreCase("Выполнен"))throw new IllegalArgumentException("Неизвестный статус операции: "+st);
   if(ps.isEmpty())throw new IllegalArgumentException("Операция без даты проводки");
   long n=amount(a);if(n==0)throw new IllegalArgumentException("Нулевая операция");
   Row entry=new Row(LocalDate.parse(ds,DATE),LocalDate.parse(ps,DATE),value(r,code),value(r,category),value(r,description),n);
   b.rows.add(entry);if(n>0)b.incoming=Math.addExact(b.incoming,n);else b.outgoing=Math.addExact(b.outgoing,-n);
   if(b.rows.size()>20000)throw new IllegalArgumentException("Слишком много операций");
  }
  if(!rubles||b.rows.isEmpty()||expectedIn==null||expectedOut==null)throw new IllegalArgumentException("Нужна рублёвая выписка поддерживаемого формата");
  if(expectedIn!=b.incoming||expectedOut!=b.outgoing)throw new IllegalArgumentException("Суммы операций не совпадают с итогами выписки. Импорт отменён.");
  return b;
 }
}
