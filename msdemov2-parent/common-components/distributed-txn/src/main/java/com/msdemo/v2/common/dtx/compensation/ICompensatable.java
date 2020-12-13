package com.msdemo.v2.common.dtx.compensation;

@FunctionalInterface
public interface ICompensatable {

	void compensate(Object... args);
	
	static class Empty implements ICompensatable{
		public void compensate(Object... args){
			
		}
	}
}
