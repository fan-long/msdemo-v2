package com.msdemo.v2.common.packet.dto;

import java.util.Date;

import com.msdemo.v2.common.exception.TransException;
import com.msdemo.v2.parameter.facade.IAcctDateAware;
import com.msdemo.v2.parameter.facade.ITellerIdAware;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "Transaction Header",description = "Transaction common header fields")
public class Header implements ITellerIdAware, IAcctDateAware {
	@ApiModelProperty(position=1,example="111222",required=true)
	private String transCode;
	@ApiModelProperty(position=2,example="1234567890",required=true)
	private String transSeqNo;
	@ApiModelProperty(position=3,example="001",required=true)
	private String channel;
	@ApiModelProperty(position=4,hidden=true)
	private Date transDateTime;		
	@ApiModelProperty(position=5,example="20200601",required=true)
	private String acctDate;	
	@ApiModelProperty(position=6,example="teller1",required=true)
	private String tellerId;
	@ApiModelProperty(position=7,example="01:account1")
	private String routingKey;
	@ApiModelProperty(position=8,example="0",hidden=true)
	private Integer dataLength=0;
	@ApiModelProperty(position=9,hidden=true)
	private String respCode=TransException.RESPONSE_CODE_SUCCESS;
	@ApiModelProperty(position=10,hidden=true)
	private String respMessage;
	
	
	
	public String getTransCode() {
		return transCode;
	}
	public void setTransCode(String transCode) {
		this.transCode = transCode;
	}
	public String getChannel(){
		return channel;
	}
	public void setChannel(String channel){
		this.channel=channel;
	}
	public String getTellerId() {
		return tellerId;
	}
	public void setTellerId(String tellerId) {
		this.tellerId = tellerId;
	}
	public String getRespCode() {
		return respCode;
	}
	public void setRespCode(String respCode) {
		this.respCode = respCode;
	}
	public String getRespMessage() {
		return respMessage;
	}
	public void setRespMessage(String respMessage) {
		this.respMessage = respMessage;
	}
	public String getTransSeqNo() {
		return transSeqNo;
	}
	public void setTransSeqNo(String transSeqNo) {
		this.transSeqNo = transSeqNo;
	}
	public Integer getDataLength() {
		return dataLength;
	}
	public void setDataLength(Integer dataLength) {
		this.dataLength = dataLength;
	}
	public Date getTransDateTime() {
		return transDateTime;
	}
	public void setTransDateTime(Date transDateTime) {
		this.transDateTime = transDateTime;
	}
	public String getAcctDate() {
		return acctDate;
	}
	public void setAcctDate(String acctDate) {
		this.acctDate = acctDate;
	}
	public String getRoutingKey() {
		return routingKey;
	}
	public void setRoutingKey(String routingKey) {
		this.routingKey = routingKey;
	}
	
}
