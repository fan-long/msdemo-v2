package com.msdemo.v2.common.param.consistent;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.msdemo.v2.common.context.TransContext;
/**
 * 参数服务公共接口类，核对跨进程调用的参数数据版本一致，实现参数表操作的日期属性、版本属性一致性机制
 * 系统上下文中的版本号记录规则为：数据实体类名DASH逻辑主键值拼接字符串
 * @author LONGFAN
 * @param <T> 参数实体
 */
@SuppressWarnings("unchecked")
public interface IConsistentParamService<T> {
	
	/**服务层实现，按照逻辑主键查询方法
	 * @param criteria 查询条件
	 * @return 符合查询条件的参数记录列表
	 */
	List<T> findByLogicKey(T criteria);
	
	/**
	 * 创建和修改参数时更新版本号信息
	 * @param record
	 * @return
	 */
	void doCreate(T record);	
	default void create(T record){
		if (record instanceof IVersionAwareParam){
			if (StringUtils.isNotEmpty(((IVersionAwareParam) record).getVersion())){
				//TODO: 根据交易跟踪号设置版本号,确认版本号生成规则（数据库时间戳或交易流水号）
				((IVersionAwareParam) record).setVersion(TransContext.get().getTraceId());
			}
		}
		doCreate(record);
	}

	//带有版本号的参数表数据不允许update，应insert新记录
	//带有日期的参数表数据允许更新>当前会计日的记录
	void doUpdate(T record);
	default void update(T record){
		if (record instanceof IVersionAwareParam){
			doCreate(record);
		}else {
			doUpdate(record);			
		}
	}
	
	/**默认查询，自动根据实体属性及上下文获取和设置交易一致的参数信息
	 * 日期可通过查询条件设置，版本号仅从TransContext中获取
	 * @param criteria 查询参数
	 * @return 查询结果参数实体对象
	 */
	default T selectByLogicKey(T criteria) {
		List<T> records = findByLogicKey(criteria);
		if (records == null || records.size() == 0)
			return null;
		if (criteria instanceof IDateVersionAwareParam) {	
			//TODO: 是否需要同时满足日期和版本一致要求，同一日期应只有一个版本的参数记录？
			T result=getByLogicKeyDate((List<IDateAwareParam>)records,((IDateAwareParam) criteria).getDate());
			if (result==null) return null;
			String paramId= result.getClass().getSimpleName()+IVersionAwareParam.DASH+
					((IVersionAwareParam)criteria).getCombinedLogicKey();
			if (!TransContext.get().getParamVersion().containsKey(paramId)){
				//交易中第一次获取该参数，在TransContext中记录版本信息
				TransContext.get().getParamVersion().put(paramId, 
						((IDateVersionAwareParam) result).getVersion());
				return result;
			}else if (((IDateVersionAwareParam)result).getVersion()
					.equals(TransContext.get().getParamVersion().get(paramId))){
				//版本一致
				return result;
			}else{
				//FIXME: 日期区间条件满足但版本不一致, throw 参数版本检查异常
				throw new InconsistentVersionException(paramId,
						TransContext.get().getParamVersion().get(paramId),
						((IDateVersionAwareParam)result).getVersion());
			}
		}else if (criteria instanceof IDateAwareParam){
			//按照日期查询
			return getByLogicKeyDate((List<IDateAwareParam>)records,((IDateAwareParam) criteria).getDate());
		}else if (criteria instanceof IVersionAwareParam){
			//按照版本号查询
			return getByLogicKeyVersion((List<IVersionAwareParam>)records,criteria);		
		}else{
			//未实现专用接口，抛出
			throw new RuntimeException("incorrect consistent param entity: "+ criteria.getClass().getSimpleName());
		}
	}

	/**
	 * 根据日期条件匹配到<=指定日期的单笔参数
	 * @param records 根据逻辑主键查询到的参数记录列表
	 * @param date 指定的参数日期，如果为空，从TransContext中获取会计日期
	 * @return 符合日期条件的参数记录实体
	 */
	static <T> T getByLogicKeyDate(List<IDateAwareParam> records,String date) {
		String paramDate= Optional.ofNullable(date).orElse(TransContext.get().getAcctDate());
		if (paramDate !=null){
			//查找小于等于paramDate的第一笔参数
			T result=(T) records.stream()
					.filter(r -> paramDate.compareTo(r.getDate())>=0)
					.sorted((r1,r2) -> r2.getDate().compareTo(r1.getDate()))
					.findFirst().get();
			return result;
		}
		return null;
	}
	
	/**
	 * 根据版本匹配获取单笔参数
	 * @param records 根据逻辑主键查询到的参数记录列表
	 * @return 符合版本号的参数记录实体
	 */
	static <T> T getByLogicKeyVersion(List<IVersionAwareParam> records,T criteria) {
		IVersionAwareParam param=(IVersionAwareParam) criteria;
		String paramId= param.getClass().getSimpleName()+IVersionAwareParam.DASH+
				((IVersionAwareParam)criteria).getCombinedLogicKey();
		String paramVersion= Optional.ofNullable(param.getVersion())
				.orElse(TransContext.get().getParamVersion().get(paramId));
		if (paramVersion!=null){
			//按照paramVersion查找符合的参数记录
			for (IVersionAwareParam record: records){
				if (StringUtils.equals(paramVersion, record.getVersion()))
					return (T)record;
			}
			//FIXME: throw 参数版本检查异常
			throw new RuntimeException("inconsistent param version: #"+
					paramVersion + " not found on "+paramId);
		}else{
			//如果paramVersion为空，根据版本号排序返回最新版本参数记录,并设置TransContext中该参数的版本号
			T result=(T) records.stream().sorted((r1,r2) -> r2.getVersion().compareTo(r1.getVersion())).findFirst().get();
			TransContext.get().getParamVersion().put(paramId,((IVersionAwareParam)result).getVersion());
			return result;
		}
	}
}
