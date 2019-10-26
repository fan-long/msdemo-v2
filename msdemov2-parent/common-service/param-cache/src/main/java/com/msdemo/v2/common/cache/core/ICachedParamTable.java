package com.msdemo.v2.common.cache.core;

import java.util.List;

/**how to use parameter table cache component:
 *  1. mapper MUST implements this interface
 *  2. query name in mapper MUST leading with 'selectBy', and @CachedQuery(query parameter fields)
 *  3. selectAll should set fetchSize parameter like '<select id="selectAll" fetchSize="200" ...>'
 * @author LONGFAN
 *
 * @param <T>
 * 
 * TODO: to support selectByExample(Entity entity)
 */
public interface ICachedParamTable<T> {

	List<T> selectAll();

}
