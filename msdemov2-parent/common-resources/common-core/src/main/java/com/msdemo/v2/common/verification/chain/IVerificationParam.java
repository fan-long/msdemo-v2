package com.msdemo.v2.common.verification.chain;

public interface IVerificationParam {
	
	/** dynamic verification parameter
	 * parse expression and return result value
	 * @param context, which used by parser to get source context data 
	 * @return
	 */
	Object getValue(Object context);
	
	void setParam(String param);
	String getParam();
	
	@SuppressWarnings("unchecked")
	public static <I extends IVerificationParam> I newInstance(Class<I> clz,
			String param) {
		try {
			I instance =(I) Class.forName(clz.getName()).newInstance();
			instance.setParam(param);
			return instance;
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
	}
}
