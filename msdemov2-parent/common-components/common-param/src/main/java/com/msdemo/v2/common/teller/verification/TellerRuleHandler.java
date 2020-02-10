package com.msdemo.v2.common.teller.verification;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.msdemo.v2.common.param.table.entity.Teller;
import com.msdemo.v2.common.teller.verification.TellerRuleHandler.ITellerVerificationRule;
import com.msdemo.v2.common.verification.BaseHandlerBuilder;
import com.msdemo.v2.common.verification.ITellerIdAware;
import com.msdemo.v2.common.verification.IVerificationAware;
import com.msdemo.v2.common.verification.BaseHandlerBuilder.EnumRuleCommand;
import com.msdemo.v2.common.verification.BaseHandlerBuilder.IRuleFactory;
import com.msdemo.v2.common.verification.IVerificationHandler;
import com.msdemo.v2.common.verification.IVerificationRule;
import com.msdemo.v2.common.verification.VerificationException;

public class TellerRuleHandler implements IVerificationHandler<ITellerVerificationRule<?>> {

	public static interface ITellerVerificationRule<T extends IVerificationAware> 
		extends IVerificationRule {	
	}
	
	private BaseHandlerBuilder<ITellerVerificationRule<?>> builder =
			new  BaseHandlerBuilder<ITellerVerificationRule<?>>(this) {
		
				@Override
				protected ITellerVerificationRule<?> parameterizedRule(String commandText, Object... params) {
					Command cmd= Command.valueOf(commandText.toUpperCase());
					if (cmd.getRule()==null){
						if(Command.BELONGS_TO.equals(cmd))
								return Factory.tellerBelongs(params[0].toString());
						throw new VerificationException(VerificationException.TELLER_ERROR,"undefined command: "+cmd );
					}
					return Command.valueOf(commandText).getRule();
				}		
	};
	
	@Override
	public BaseHandlerBuilder<ITellerVerificationRule<?>> getBuilder() {
		return builder;
	}
	
	public static enum Command implements EnumRuleCommand<ITellerVerificationRule<?>>{
		EXISTED(Factory.tellerExisted()), NOT_LOCKED(Factory.tellerNotLocked()),
		BELONGS_TO(null);
		private ITellerVerificationRule<? extends IVerificationAware> rule;

		Command(ITellerVerificationRule<? extends IVerificationAware> rule) {
			this.rule = rule;
		}

		public ITellerVerificationRule<? extends IVerificationAware> getRule() {
			return rule;
		}

	}

	@Override
	public IRuleFactory ruleFactory() {
		return Factory;
	}	
	
	private interface ITellerRuleFactory extends IRuleFactory{
		ITellerVerificationRule<ITellerIdAware> tellerExisted();

		ITellerVerificationRule<ITellerIdAware> tellerNotLocked();

		ITellerVerificationRule<ITellerIdAware> tellerBelongs(String branchId);
	}
	/**
	 * wrapped data verification functions
	 */
	public static ITellerRuleFactory Factory = new ITellerRuleFactory() {
		private Teller getTeller(String tellerId){
			return Optional.ofNullable(TellerVerificationHelper.getCachedTeller())
			.orElseGet(()->{
				TellerVerificationHelper.setCachedTeller(TellerVerificationHelper.TELLER_SERVICE.selectById(tellerId));
				return TellerVerificationHelper.getCachedTeller();});
		}
		public ITellerVerificationRule<ITellerIdAware> tellerExisted() {
			return (o) -> {
				String tellerId = ((ITellerIdAware) o).getTellerId();
				Teller teller=getTeller(tellerId);
				if (teller ==null || StringUtils.isEmpty(teller.getTellerId()))
					throw new VerificationException(VerificationException.TELLER_ERROR,"teller not exist" );
			}; 
		}

		@Override
		public ITellerVerificationRule<ITellerIdAware> tellerNotLocked() {
			return (o) -> {
				String tellerId = ((ITellerIdAware) o).getTellerId();
				Teller teller=getTeller(tellerId);
				//FIXME: replace with LockStatus enum
				if (StringUtils.equals("Y",teller.getLocked()))
					throw new VerificationException(VerificationException.TELLER_ERROR,"teller is locked" );
			}; 
		}

		@Override
		public ITellerVerificationRule<ITellerIdAware> tellerBelongs(String branchId) {
			return (o) -> {
				String tellerId = ((ITellerIdAware) o).getTellerId();
				Teller teller=getTeller(tellerId);
				if (!StringUtils.equals(branchId,teller.getBranchId()))
					throw new VerificationException(VerificationException.TELLER_ERROR, tellerId+" is not member of "+branchId );
			}; 
		}
	};
	
}
