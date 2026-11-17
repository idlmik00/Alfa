package ru.personal.alfafinance;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;
public class StatementSmoke {
 static String cell(String col,int row,String value){return "<c r=\""+col+row+"\" t=\"inlineStr\"><is><t>"+value+"</t></is></c>";}
 static String row(int n,String... cells){return "<row r=\""+n+"\">"+String.join("",cells)+"</row>";}
 static byte[] sheet(String summary)throws Exception{
  String xml="<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>"+
   row(1,cell("A",1,"Валюта счета"),cell("C",1,"RUR"))+
   row(7,cell("A",7,"Номер счета"),cell("C",7,"00000000000000000001"))+
   row(2,cell("K",2,"Поступления"),cell("N",2,summary+" RUR"))+
   row(3,cell("K",3,"Расходы"),cell("N",3,"20,50 RUR"))+
   row(4,cell("A",4,"Дата операции"),cell("B",4,"Дата проводки"),cell("D",4,"Код"),cell("E",4,"Категория"),cell("L",4,"Описание"),cell("M",4,"Сумма в валюте счета"),cell("O",4,"Статус "))+
   row(5,cell("A",5,"01.01.2025"),cell("B",5,"02.01.2025"),cell("D",5,"TEST"),cell("E",5,"Прочие операции"),cell("L",5,"Тестовое поступление"),cell("M",5,"100,00"),cell("O",5,"Выполнен"))+
   row(6,cell("A",6,"02.01.2025"),cell("B",6,"03.01.2025"),cell("D",6,"TEST"),cell("E",6,"Прочие операции"),cell("L",6,"Категория: Перевод по СБП"),cell("M",6,"-20,50"))+
   "</sheetData></worksheet>";
  ByteArrayOutputStream out=new ByteArrayOutputStream();try(ZipOutputStream z=new ZipOutputStream(out)){z.putNextEntry(new ZipEntry("xl/worksheets/sheet1.xml"));z.write(xml.getBytes(StandardCharsets.UTF_8));z.closeEntry();}return out.toByteArray();
 }
 public static void main(String[] args)throws Exception{
  BankStatement b=BankStatement.parse(sheet("100,00"));
  if(!b.accountNumber.equals("00000000000000000001"))throw new AssertionError("Account binding missing");
  if(b.rows.size()!=2||b.incoming!=10000||b.outgoing!=2050||!b.rows.get(1).needsReview())throw new AssertionError("Statement totals or review failure");
  boolean rejected=false;try{BankStatement.parse(sheet("101,00"));}catch(IllegalArgumentException e){rejected=true;}if(!rejected)throw new AssertionError("Invalid statement accepted");
  BankStatement.Row shop=new BankStatement.Row(b.rows.get(0).date,b.rows.get(0).posted,"CARDTEST","Прочие операции","Операция по карте: 000000******0000, место совершения операции: RU/CITY/PYATEROCHKA 123, MCC: 5411",-50000);
  if(!BankRules.category(shop).equals("Продукты")||!BankRules.clean(shop).equals("PYATEROCHKA 123")||!BankRules.category(b.rows.get(1)).equals("Переводы — проверить"))throw new AssertionError("Bank categories or clean note failed");
  BankStatement.Row transport=new BankStatement.Row(shop.date,shop.posted,"TEST","Прочие операции","Операция по карте: 000000******0000, место совершения операции: RU/CITY/BUS, MCC: 4111",-100);
  if(!BankRules.category(transport).equals("Транспорт"))throw new AssertionError("Transport category failed");
  System.out.println("Statement and category tests passed");
 }
}
