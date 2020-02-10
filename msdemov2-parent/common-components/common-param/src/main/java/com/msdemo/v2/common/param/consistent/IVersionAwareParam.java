package com.msdemo.v2.common.param.consistent;

public interface IVersionAwareParam {
	public static final String DASH="-";
	public String getVersion();
	public void setVersion(String version);
	public String getCombinedLogicKey();

}
