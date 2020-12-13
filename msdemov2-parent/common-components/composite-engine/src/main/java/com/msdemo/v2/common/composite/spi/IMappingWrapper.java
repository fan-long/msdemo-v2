package com.msdemo.v2.common.composite.spi;

import com.msdemo.v2.common.composite.CompositionContext;

@FunctionalInterface
public interface IMappingWrapper {

	Object[] wrap(CompositionContext context);
}
