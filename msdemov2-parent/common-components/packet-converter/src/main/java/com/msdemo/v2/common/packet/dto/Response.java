package com.msdemo.v2.common.packet.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 交易返回信息
 * @author LONGFAN
 *
 * @param <T> 用于返回交易处理结果的数据对象
 */
@ApiModel(value = "Transaction Response",description = "genericity object of transaction output")
public class Response<T> {
	
	@ApiModelProperty(position=1)
	private Header header;
	
	@ApiModelProperty(position=2)
	private T body;
        
    @ApiModelProperty(position=3)
	private ExtensionRegion extensionRegion;
	
	public Response(){}
	
	public Response(Header header,T body,ExtensionRegion extensionRegion){
		this.header=header;
		this.body=body;
		this.extensionRegion=extensionRegion;
	}
	
	public ExtensionRegion getExtensionRegion() {
		return extensionRegion;
	}
	public void setExtensionRegion(ExtensionRegion extensionRegion) {
		this.extensionRegion = extensionRegion;
	}
	public Header getHeader() {
		return header;
	}
	public void setHeader(Header header) {
		this.header = header;
	}
	public T getBody() {
		return body;
	}
	public void setBody(T body) {
		this.body = body;
	}
}
