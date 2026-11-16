package ru.personal.alfafinance;
import java.util.*;
public class CategorySmoke{
 public static void main(String[] args){
  for(boolean income:new boolean[]{false,true}){
   List<String> names=CategoryCatalog.names(income);
   if(names.size()!=new HashSet<>(names).size()||names.size()<10)throw new AssertionError("Invalid category list");
   for(String name:names)if(name.trim().isEmpty()||CategoryCatalog.icon(name).isEmpty())throw new AssertionError("Missing category/icon");
  }
  if(!CategoryCatalog.names(true).contains("Зарплата")||!CategoryCatalog.names(false).contains("Продукты"))throw new AssertionError("Missing main category");
  System.out.println("Category checks passed");
 }
}