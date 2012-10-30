/**
 * Copyright (c) 2012-2013, Michael Yang ��� (www.yangfuhai.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.tsz.afinal.db.sqlite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.tsz.afinal.db.table.Id;
import net.tsz.afinal.db.table.KeyValue;
import net.tsz.afinal.db.table.ManyToOne;
import net.tsz.afinal.db.table.Property;
import net.tsz.afinal.db.table.TableInfo;
import net.tsz.afinal.exception.DbException;

public class SqlBuilder {
	
	/**
	 * ��ȡ�����sql���
	 * @param tableInfo
	 * @return
	 */
	public static String getInsertSQL(Object entity){
		
		TableInfo table=TableInfo.get(entity.getClass());
		
		Object idvalue = table.getId().getValue(entity);
		
		List<KeyValue> keyValueList = new ArrayList<KeyValue>();
		
		if(!(idvalue instanceof Integer)){ //���˷�������,����id , �����������Ͳ���Ҫ����id��
			if(idvalue instanceof String && idvalue != null){
				KeyValue kv = new KeyValue(table.getId().getColumn(),idvalue);
				keyValueList.add(kv);
			}
		}
		
		//��������
		Collection<Property> propertys = table.propertyMap.values();
		for(Property property : propertys){
			KeyValue kv = property2KeyValue(property,entity) ;
			if(kv!=null)
				keyValueList.add(kv);
		}
		
		//������������һ��
		Collection<ManyToOne> manyToOnes = table.manyToOneMap.values();
		for(ManyToOne many:manyToOnes){
			KeyValue kv = manyToOne2KeyValue(many,entity);
			if(kv!=null) keyValueList.add(kv);
		}
		
		StringBuffer strSQL=new StringBuffer();
		if(keyValueList!=null && keyValueList.size()>0){
			strSQL.append("INSERT INTO ");
			strSQL.append(table.getTableName());
			strSQL.append(" (");
			for(KeyValue kv : keyValueList){
				strSQL.append(kv.getKey()).append(",");
			}
			strSQL.deleteCharAt(strSQL.length() - 1);
			strSQL.append(") VALUES ( ");
			for(KeyValue kv : keyValueList){
				Object value = kv.getValue();
				if(value instanceof String){
					strSQL.append("'").append(value).append("'").append(",");
				}else{
					strSQL.append(value).append(",");
				}
			}
			strSQL.deleteCharAt(strSQL.length() - 1);
			strSQL.append(")");
		}
		
		return strSQL.toString();
	}
	
	
	private static String getDeleteSqlBytableName(String tableName){
		return "DELETE FROM "+ tableName;
	}
	
	
	public static String getDeleteSQL(Object entity){
		TableInfo table=TableInfo.get(entity.getClass());
		
		Id id=table.getId();
		Object idvalue=id.getValue(entity);
		
		StringBuffer strSQL = new StringBuffer(getDeleteSqlBytableName(table.getTableName()));
		strSQL.append(" WHERE ");
		strSQL.append(getPropertyStrSql(id.getColumn(), idvalue));
		
		return strSQL.toString();
	}
	
	
	
	
	public static String getDeleteSQL(Class<?> clazz , Object idValue){
		TableInfo table=TableInfo.get(clazz);
		if(table == null){
			throw new RuntimeException("");
		}
		
		Id id=table.getId();
		if(null == id ) return null ; //û������������ɾ��
		if(null == idValue) return null ; //û������������ɾ��
		
		StringBuffer strSQL = new StringBuffer(getDeleteSqlBytableName(table.getTableName()));
		strSQL.append(" WHERE ");
		strSQL.append(getPropertyStrSql(id.getColumn(), idValue));
		
		return strSQL.toString();
	}


	/**
	 * 
	 * @param entity
	 * @param strWhere if strWhere is null,then delete all entity
	 * @return
	 */
	public static  String getDeleteSQL(Class<?> clazz,String ... strWhere){
		TableInfo table=TableInfo.get(clazz);
		
		StringBuffer strSQL = new StringBuffer(getDeleteSqlBytableName(table.getTableName()));
		
		if(strWhere != null && strWhere.length > 0){
			strSQL.append(" WHERE ");
			for(String whereSQL : strWhere){
				strSQL.append(" (").append(whereSQL).append(") ").append("AND");
			}
			strSQL.delete(strSQL.length()-3, strSQL.length());
		}
		
		return strSQL.toString();
	}

	
	////////////////////////////select sql start///////////////////////////////////////
	

	private static String getSelectSqlByTableName(String tableName){
		return new StringBuffer("SELECT * FROM ").append(tableName).toString();
	}


	public static String getSelectSQL(Class<?> clazz,Object idValue){
		TableInfo table=TableInfo.get(clazz);
		
		StringBuffer strSQL = new StringBuffer(getSelectSqlByTableName(table.getTableName()));
		strSQL.append(" WHERE ");
		strSQL.append(getPropertyStrSql(table.getId().getColumn(), idValue));
		
		return strSQL.toString();
	}
	
	
	public static String getSelectSQL(Class<?> clazz){
		return getSelectSqlByTableName(TableInfo.get(clazz).getTableName());
	}
	
	
	
	public static  String getSelectSQL(Class<?> clazz,String ... strWhere){
		
		StringBuffer strSQL = new StringBuffer(getSelectSqlByTableName(TableInfo.get(clazz).getTableName()));
		
		if(strWhere != null && strWhere.length > 0){
			strSQL.append(" WHERE ");
			for(String whereSQL : strWhere){
				strSQL.append(" (").append(whereSQL).append(") ").append("AND");
			}
			strSQL.delete(strSQL.length()-3, strSQL.length());
		}
		
		return strSQL.toString();
	}
	
	
	//////////////////////////////update sql start/////////////////////////////////////////////
	
	public static String getUpdateSQL(Object entity){
		
		TableInfo table=TableInfo.get(entity.getClass());
		Object idvalue=table.getId().getValue(entity);
		
		if(null == idvalue ) {//����ֵ����Ϊnull�������ܸ���
			throw new DbException("this entity["+entity.getClass()+"]'s id value is null");
		}
		
		List<KeyValue> keyValueList = new ArrayList<KeyValue>();
		//��������
		Collection<Property> propertys = table.propertyMap.values();
		for(Property property : propertys){
			KeyValue kv = property2KeyValue(property,entity) ;
			if(kv!=null)
				keyValueList.add(kv);
		}
		
		//������������һ��
		Collection<ManyToOne> manyToOnes = table.manyToOneMap.values();
		for(ManyToOne many:manyToOnes){
			KeyValue kv = manyToOne2KeyValue(many,entity);
			if(kv!=null) keyValueList.add(kv);
		}
		
		if(keyValueList == null || keyValueList.size()==0) return null ;
		
		StringBuffer strSQL=new StringBuffer("UPDATE ");
		strSQL.append(table.getTableName());
		strSQL.append(" SET ");
		for(KeyValue kv : keyValueList){
			strSQL.append(getPropertyStrSql(kv)).append(",");
		}
		strSQL.deleteCharAt(strSQL.length() - 1);
		strSQL.append(" WHERE ").append(getPropertyStrSql(table.getId().getColumn(), idvalue));

		return strSQL.toString();
	}
	
	/**
	 * @param entity
	 * @param strWhere if strWhere is empty,only return update by id sql
	 * @return
	 */
	public static String getUpdateSQL(Object entity,String ... strWhere){
		if(strWhere==null || strWhere.length == 0)
			return getUpdateSQL(entity);
		
		TableInfo table=TableInfo.get(entity.getClass());
		
		List<KeyValue> keyValueList = new ArrayList<KeyValue>();
		
		//��������
		Collection<Property> propertys = table.propertyMap.values();
		for(Property property : propertys){
			KeyValue kv = property2KeyValue(property,entity) ;
			if(kv!=null) keyValueList.add(kv);
		}
		
		//������������һ��
		Collection<ManyToOne> manyToOnes = table.manyToOneMap.values();
		for(ManyToOne many:manyToOnes){
			KeyValue kv = manyToOne2KeyValue(many,entity);
			if(kv!=null) keyValueList.add(kv);
		}
		
		if(keyValueList == null || keyValueList.size()==0) {
			throw new DbException("this entity["+entity.getClass()+"] has no property"); 
		}
		
		StringBuffer strSQL=new StringBuffer("UPDATE ");
		strSQL.append(table.getTableName());
		strSQL.append(" SET ");
		for(KeyValue kv : keyValueList){
			strSQL.append(getPropertyStrSql(kv)).append(",");
		}
		strSQL.deleteCharAt(strSQL.length() - 1);
		
		if(strWhere!=null && strWhere.length>0){
			strSQL.append(" WHERE ");
			for(String whereSQL : strWhere){
				strSQL.append(" (").append(whereSQL).append(") ").append("AND");
			}
			
			strSQL.delete(strSQL.length()-3, strSQL.length());
		}
		
		return strSQL.toString();
	}
	
	
	
	public static String getCreatTableSQL(Class<?> clazz){
		TableInfo table=TableInfo.get(clazz);
		
		Id id=table.getId();
		StringBuffer strSQL = new StringBuffer();
		strSQL.append("CREATE TABLE IF NOT EXISTS ");
		strSQL.append(table.getTableName());
		strSQL.append(" ( ");
		
		Class<?> primaryClazz = id.getDataType();
		if( primaryClazz == int.class || primaryClazz==Integer.class)
			strSQL.append("\"").append(id.getColumn()).append("\"    ").append("INTEGER PRIMARY KEY AUTOINCREMENT,");
		else
			strSQL.append("\"").append(id.getColumn()).append("\"    ").append("TEXT PRIMARY KEY,");
		
		Collection<Property> propertys = table.propertyMap.values();
		for(Property property : propertys){
			strSQL.append("\"").append(property.getColumn()).append("\",");
		}
		
		Collection<ManyToOne> manyToOnes = table.manyToOneMap.values();
		for(ManyToOne manyToOne : manyToOnes){
			strSQL.append("\"").append(manyToOne.getColumn()).append("\",");
		}
		strSQL.deleteCharAt(strSQL.length() - 1);
		strSQL.append(" )");
		return strSQL.toString();
	}
	
	/**
	 * @param keyvalue
	 * @return eg1: name='afinal'  eg2: id=100
	 */
	private static String getPropertyStrSql(KeyValue keyvalue){
		return keyvalue == null ? null : getPropertyStrSql(keyvalue.getKey(),keyvalue.getValue());
	}
	
	/**
	 * @param key
	 * @param value
	 * @return eg1: name='afinal'  eg2: id=100
	 */
	private static String getPropertyStrSql(String key,Object value){
		StringBuffer sbSQL = new StringBuffer(key).append("=");
		if(value instanceof String){
			sbSQL.append("'").append(value).append("'");
		}else{
			sbSQL.append(value);
		}
		return sbSQL.toString();
	}
	
	
	private static KeyValue property2KeyValue(Property property , Object entity){
		KeyValue kv = null ;
		String pcolumn=property.getColumn();
		Object value = property.getValue(entity);
		if(value!=null){
			kv = new KeyValue(pcolumn, value);
		}else{
			if(property.getDefaultValue()!=null && property.getDefaultValue().trim().length()!=0)
				kv = new KeyValue(pcolumn, property.getDefaultValue());
		}
		return kv;
	}
	
	
	private static KeyValue manyToOne2KeyValue(ManyToOne many , Object entity){
		KeyValue kv = null ;
		String manycolumn=many.getColumn();
		Object manyobject=many.getValue(entity);
		if(manyobject!=null){
			Object manyvalue = TableInfo.get(manyobject.getClass()).getId().getValue(manyobject);
			if(manycolumn!=null && manyvalue!=null){
				kv = new KeyValue(manycolumn, manyvalue);
			}
		}
		
		return kv;
	}
	
	
	
	
}