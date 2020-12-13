package com.msdemo.v2.common.packet;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.msdemo.v2.common.convert.core.ConverterFactory;
import com.msdemo.v2.common.convert.core.IConverterContext;
import com.msdemo.v2.common.convert.core.IRootConverter;
import com.msdemo.v2.common.convert.definition.fix.FixConverterContext;
import com.msdemo.v2.common.exception.TransException;
import com.msdemo.v2.common.packet.dto.Header;
import com.msdemo.v2.common.packet.dto.Response;

@SuppressWarnings("unchecked")
@Component
public class FixPacketConverter {

	private static final String REQUEST_DEFINITION = "request";
	private static final String RESPONSE_DEFINITION = "response";
	private static final String ERROR_DEFINITION = "error";

	@Resource(name="fixConverterFactory")
	ConverterFactory fixConverterFactory;
	
	public <T> T marshal(String inputMsg) {
		T result = null;
		IRootConverter<?> converter = fixConverterFactory.getInstance(REQUEST_DEFINITION);
		IConverterContext context = converter.marshal(inputMsg.getBytes());
		if (result instanceof FixConverterContext) {
			return (T) context;
		} else
			return (T) context.getDto();
	}

	public String unmarshal(Response<?> resp) {
		if (StringUtils.equals(resp.getHeader().getRespCode(), TransException.RESPONSE_CODE_SUCCESS)) {
			IRootConverter<?> converter = fixConverterFactory.getInstance(RESPONSE_DEFINITION);
			return converter.unmarshal(resp);
		} else
			return unmarshalError(resp.getHeader());
	}

	public String unmarshalError(Header header) {
		IRootConverter<?> converter = fixConverterFactory.getInstance(ERROR_DEFINITION);
		Response<?> resp= new Response<>();
		resp.setHeader(header);
		return converter.unmarshal(resp);
	}
	
	public String unmarshalError(String transCode,String transSeqNo, String errorCode,String errorMessage){
		Header header = new Header();
		header.setTransCode(transCode);
		header.setTransSeqNo(transSeqNo);
		header.setRespCode(errorCode);	
		header.setRespMessage(errorMessage);
		return unmarshalError(header);
	}
	
}
