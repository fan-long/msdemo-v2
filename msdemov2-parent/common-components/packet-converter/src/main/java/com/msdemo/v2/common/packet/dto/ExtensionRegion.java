package com.msdemo.v2.common.packet.dto;

import io.swagger.annotations.ApiModelProperty;

public class ExtensionRegion {
	@ApiModelProperty(example="description",required=false)
	private String msg;

	@ApiModelProperty(example="debug_key",required=false)
	private String debug;
	
	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public String getDebug() {
		return debug;
	}

	public void setDebug(String debug) {
		this.debug = debug;
	}
	
}
