package com.msdemo.v2.common.packet.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 交易请求信息
 * @author LONGFAN
 *
 * @param <T> 从报文体中进行转换后生成的请求信息数据对象
 */
@ApiModel(value = "Transaction Request",description = "genericity object of transaction input")
public class Request<T> {
	@ApiModelProperty(value="common transaction information",position=1)
	private Header header;
	
	@ApiModelProperty(value="transaction special data",position=2)
	private T body;
        
    @ApiModelProperty(value="transaction extension data",position=3,required=false)
	private ExtensionRegion extensionRegion;
	
	public Request(){		
	}
	
	public Request(Header header,T body,ExtensionRegion extensionRegion){
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
